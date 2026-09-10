package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.net.game.packets.MapTransitionPacket
import de.fiereu.openmmo.net.game.packets.RenderScreenPacket
import de.fiereu.openmmo.net.game.packets.Season
import de.fiereu.openmmo.net.game.packets.SeasonPacket
import de.fiereu.openmmo.server.game.session.PENDING_MAP_LOAD
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import de.fiereu.openmmo.server.game.world.WarpExitRules
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val log = KotlinLogging.logger {}

// A player must not stay gated if the client never answers the transition.
private val ARRIVAL_TIMEOUT = 10.seconds

@Singleton
class WarpService
@Inject
constructor(
    private val mapLoadService: MapLoadService,
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val presenceService: PresenceService,
    private val warpRules: WarpRules,
    private val safari: javax.inject.Provider<SafariService>? = null,
) {

  fun executeWarp(ctx: SessionContext, charId: Long, tile: WarpTile) {
    val state = ctx.attributes[PLAYER_STATE]
    val stored = characterStore.getCharacter(charId) ?: return

    val warp = if (!tile.dynamic) tile else resolveDynamicWarp(stored) ?: return

    // Check the map first. Moving the player onto one we do not have would strand it there.
    val destMap = mapManager.getMap(warp.targetRegionId, warp.targetBankId, warp.targetMapId)
    if (destMap == null) {
      log.warn {
        "Map not found for warp target ${warp.targetRegionId}:${warp.targetBankId}:${warp.targetMapId}"
      }
      return
    }

    // Carried into the position write below - a separate store write here was overwritten by
    // that write's stale copy of the info (seen live: set, then gone three seconds later).
    val doorWarp =
        if (!tile.dynamic && destMap.warps.any { it.dynamic && it.x == warp.targetX && it.y == warp.targetY }) {
          log.info { "Dynamic warp set to the door just used: (${tile.x}, ${tile.y}) on ${stored.info.positionBankId}:${stored.info.positionMapId}" }
          de.fiereu.openmmo.common.DynamicWarp(
              stored.info.positionRegionId,
              stored.info.positionBankId,
              stored.info.positionMapId,
              tile.x.toShort(),
              tile.y.toShort(),
              de.fiereu.openmmo.common.enums.Direction.DOWN)
        } else stored.info.dynamicWarp
    state?.justWarped = true
    state?.pendingStepDir = null
    state?.pendingStepX = -1
    state?.pendingStepY = -1
    // Leave now, so the old map's observers do not keep a ghost for the whole transition.
    presenceService.leave(ctx)

    val sourceMap =
        mapManager.getMap(
            stored.info.positionRegionId, stored.info.positionBankId, stored.info.positionMapId)

    val knownOverride =
        WarpExitRules.getKnownOverride(sourceMap, destMap, warp.targetX, warp.targetY)
    val destBehavior = destMap.tileAt(warp.targetX, warp.targetY)?.behavior

    // The facing the player had when the warp fired - vanilla keeps it on ladder-style
    // arrivals (GetAdjustedInitialDirection returns playerStruct->direction for ladders).
    val entryFacing = state?.facingDirection ?: Direction.DOWN

    // Same rulebook as the NDS regions: the landing tile's own rule leads, and its required
    // press REVERSED is the exit facing (land on a press-UP door = walk out DOWN, land on the
    // paired east-wall stair = walk off LEFT) - the identical chain head that fixed the Gen4
    // first-trip facing. Per-tile rows in warp-rules.txt override per-type without a recompile.
    val destRule =
        warpRules.forTile(
            warp.targetRegionId.toInt(),
            warp.targetBankId.toInt(),
            warp.targetMapId.toInt(),
            warp.targetX,
            warp.targetY)
            ?: destBehavior?.let { warpRules.forName(warp.targetRegionId.toInt(), it.name) }

    val warpFacing =
        warp.exitFacing
            ?: knownOverride?.facing
            ?: destRule?.press?.opposite()
            ?: WarpExitRules.inferExitFacing(
                destTileBehavior = destBehavior,
                entryFacing = entryFacing,
            )

    state?.facingDirection = warpFacing

    var offsetX = warp.targetX
    var offsetY = warp.targetY

    // Arrival column of the same rulebook: STEP walks out, REST stays on the tile - vanilla's
    // SetUpWarpExitTask picks the walk-out for doors, non-anim doors and stair warps only;
    // everything else is Task_ExitNonDoor (no step). Untyped tiles (no behavior data) keep the
    // old map-type heuristic as the safety net.
    val shouldAutoStepOffWarp =
        knownOverride?.autoStep
            ?: destRule?.let { it.arrival == WarpRules.Arrival.STEP }
            ?: WarpExitRules.shouldAutoStep(
                sourceMap = sourceMap,
                destMap = destMap,
                destTileBehavior = destBehavior,
            )
    // The step arms only with an in-bounds, walkable, warp-free target; the validator moves
    // with it (acceptNextMoveSource at the send site).
    if (shouldAutoStepOffWarp && destMap.warps.any { it.x == offsetX && it.y == offsetY }) {
      val sx = warp.targetX + warpFacing.dx
      val sy = warp.targetY + warpFacing.dy
      val open =
          sx in 0 until destMap.width &&
              sy in 0 until destMap.height &&
              destMap.tileAt(sx, sy)?.blocksMovement() != true &&
              destMap.warps.none { it.x == sx && it.y == sy }
      if (open) state?.pendingStepDir = warpFacing
    }

    val playerZ =
        destMap.warps.find { it.x == warp.targetX && it.y == warp.targetY }?.elevation
            ?: warp.targetElevation

    log.info {
      "WARP EXIT: source=${sourceMap?.bankId}:${sourceMap?.mapId} dest=${destMap.bankId}:${destMap.mapId} target=(${warp.targetX},${warp.targetY}) final=($offsetX,$offsetY) z=$playerZ facing=$warpFacing autoStep=$shouldAutoStepOffWarp"
    }

    val newInfo =
        stored.info.copy(
            positionRegionId = warp.targetRegionId,
            positionBankId = warp.targetBankId,
            positionMapId = warp.targetMapId,
            positionX = offsetX.toShort(),
            positionY = offsetY.toShort(),
            positionFacing = warpFacing,
            dynamicWarp = doorWarp,
        )
    characterStore.updateCharacter(newInfo)
    characterStore.flushCharacterAsync(charId)

    if (state != null) {
      state.regionId = warp.targetRegionId.toInt()
      state.bankId = warp.targetBankId.toInt()
      state.mapId = warp.targetMapId.toInt()
      state.x = offsetX.toShort()
      state.y = offsetY.toShort()
      state.elevation = playerZ
    }

    // Leaving the Safari Zone's maps by any warp but the gate ends the Safari Game.
    safari?.get()?.onWarp(ctx, charId, destMap.sourceName)

    // Vanilla kicks you off the bike at the doorway: warps are doors/stairs/cave mouths, so
    // riding never survives one. The arrival spawn already carries transportation 0; clearing
    // the flag keeps the server's idea of the player in step with what the client will draw.
    state?.riding = false
    state?.surfing = false
    state?.underwater = false

    // Only fade out and send the map. onRequestPlayer does the arrival and fades back in.
    ctx.send(MapTransitionPacket())
    ctx.send(RenderScreenPacket(false))
    // The scripted-state lock (the same input removal every script uses) covers the whole
    // transition: ON here, re-asserted at arrival for the emergence step, OFF after the walk
    // plays. Sent after the transition packets in case the map change resets client state;
    // the failsafe covers a lost arrival, but never while a script owns the player.
    ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = true))
    run {
      val scope =
          ctx.attributes.getOrPut(SCRIPT_SCOPE) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
          }
      scope.launch {
        delay(5000)
        if (ctx.channel.isActive &&
            state?.blocksPlayerInput != true &&
            state?.scriptRunning != true) {
          ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
        }
      }
    }
    // The season is GLOBAL and real-clock driven - all regions have seasonal content (the
    // mechanic is Unova's but the textures exist for every ROM). One consistent value
    // everywhere also stops the mid-session flip-flop an NDS/GBA split caused: every value
    // change fires the client's refresh listeners (f.u2.X02) mid-transition, which is what
    // glitched GBA door-exit animations.
    ctx.send(SeasonPacket(Season.current()))

    mapLoadService.resetClientCache(ctx, destMap)
    ctx.send(mapManager.createLoadMapPacket(destMap, reloadPlayer = true, deleteCache = true))
    mapLoadService.preloadConnectedMaps(ctx, destMap, depth = 1, reloadPlayer = true)
    if (state != null) awaitArrival(ctx, state, charId)

    log.info { "Player $charId warped to bank=${warp.targetBankId} map=${warp.targetMapId}" }
  }

  /**
   * Pushes the client to a map the server holds no data for - the NDS-region probe. The client is
   * told only the ids plus a neutral lighting/weather/type header, so whatever appears is rendered
   * entirely from its own ROM data. Server-side collision and movement do not exist there; the
   * position is persisted so a relog does not snap back, and /tp by name leads back out.
   */
  fun executeRawWarp(
      ctx: SessionContext,
      charId: Long,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      x: Int,
      y: Int,
      railLine: Int = -1,
  ) {
    // Raw warps carry an NDS-format map header. Regions 0 and 1 are GBA regions the server
    // hosts - a raw warp there is malformed data that black-screens or kills the client, so it
    // is refused outright; hosted maps go through executeWarp.
    if (regionId == 0 || regionId == 1) {
      ctx.send(notice("Raw warps are for regions 2-4 (Unova, Sinnoh, Johto) only."))
      return
    }
    val state = ctx.attributes[PLAYER_STATE]
    val stored = characterStore.getCharacter(charId) ?: return
    state?.justWarped = true
    state?.pendingStepDir = null
    state?.pendingStepX = -1
    state?.pendingStepY = -1
    // Arrivals land on the partner warp's own tile; hold NDS warps until the player has stood on
    // a warp-free tile once, or wide boxes bounce the player straight back.
    state?.ndsWarpGuard = true
    // Consumed by the next LoadEntity: rail maps need the player attached to a rail line or the
    // client shows a blue void.
    state?.pendingRailLine = railLine
    presenceService.leave(ctx)
    characterStore.updateCharacter(
        stored.info.copy(
            positionRegionId = regionId.toByte(),
            positionBankId = bankId.toByte(),
            positionMapId = mapId.toByte(),
            positionX = x.toShort(),
            positionY = y.toShort(),
        ))
    characterStore.flushCharacterAsync(charId)
    if (state != null) {
      state.regionId = regionId
      state.bankId = bankId
      state.mapId = mapId
      state.x = x.toShort()
      state.y = y.toShort()
      state.loadedMaps.clear()
      state.spawnedNpcMaps.clear()
      state.scriptedNpcPoses.clear()
      // Same dismount rule as executeWarp - the fresh spawn draws on foot regardless.
      state.riding = false
    }
    ctx.send(MapTransitionPacket())
    ctx.send(RenderScreenPacket(false))
    // The scripted-state lock, same discipline as executeWarp: ON for the transition,
    // re-asserted at arrival, released after the emergence walk; failsafe for lost arrivals.
    ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = true))
    run {
      val scope =
          ctx.attributes.getOrPut(SCRIPT_SCOPE) {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
          }
      scope.launch {
        delay(5000)
        if (ctx.channel.isActive &&
            state?.blocksPlayerInput != true &&
            state?.scriptRunning != true) {
          ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
        }
      }
    }
    ctx.send(SeasonPacket(Season.current()))
    // deleteCache stays off: NDS maps come from the client's own ROM conversion and the cache is
    // always valid. Deleting it forces a reconvert, and a render frame between the wipe and the
    // rebuilt model crashes the client with a null current map (seen warping into Cold Storage
    // while the Driftveil matrix chunk was still converting).
    ctx.send(
        de.fiereu.openmmo.net.game.packets.LoadMapPacket(
            reloadPlayer = true,
            deleteCache = false,
            regionId = regionId and 0xFF,
            bankId = bankId and 0xFF,
            mapId = mapId and 0xFF,
            mapData =
                de.fiereu.openmmo.net.game.packets.MapData.NdsMapData(
                    lighting = de.fiereu.openmmo.common.enums.Lighting.REGULAR,
                    weather = de.fiereu.openmmo.common.enums.Weather.REGULAR_WEATHER,
                    mapType = NdsMapTypes.typeOf(regionId and 0xFF, bankId and 0xFF, mapId and 0xFF),
                )),
    )
    log.info { "Player $charId raw-warped to $regionId:$bankId:$mapId ($x, $y)" }
  }

  /**
   * Builds the real warp for a MAP_DYNAMIC tile, whose target fields are placeholders, from the
   * destination a script set on the player.
   */
  private fun resolveDynamicWarp(stored: StoredCharacter): WarpTile? {
    val d = stored.info.dynamicWarp
    if (d == null) {
      log.warn { "Dynamic warp tile stepped on but no dynamic warp is set for ${stored.info.id}" }
      return null
    }
    return WarpTile(
        x = 0,
        y = 0,
        targetRegionId = d.regionId,
        targetBankId = d.bankId,
        targetMapId = d.mapId,
        targetX = d.x.toInt(),
        targetY = d.y.toInt(),
        exitFacing = d.facing,
    )
  }

  /**
   * Releases the player if the client never asks for it. Without this a lost RequestPlayer leaves
   * the player faded out and unable to move for the rest of the session.
   */
  private fun awaitArrival(ctx: SessionContext, state: PlayerState, charId: Long) {
    val loaded = CompletableDeferred<Unit>()
    ctx.attributes[PENDING_MAP_LOAD] = loaded
    // The session's own scope, so a disconnect cancels this instead of reviving a dead session.
    val scope =
        ctx.attributes.getOrPut(SCRIPT_SCOPE) {
          CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    scope.launch {
      if (withTimeoutOrNull(ARRIVAL_TIMEOUT) { loaded.await() } != null) return@launch
      // A newer warp owns the gate now, leave it to its own deadline.
      if (ctx.attributes[PENDING_MAP_LOAD] !== loaded) return@launch
      if (!ctx.channel.isActive) return@launch
      ctx.attributes.remove(PENDING_MAP_LOAD)
      log.warn { "Character $charId never asked for its player after a warp" }
      state.justWarped = false
      ctx.send(RenderScreenPacket(true))
      presenceService.enter(ctx)
    }
  }
}
