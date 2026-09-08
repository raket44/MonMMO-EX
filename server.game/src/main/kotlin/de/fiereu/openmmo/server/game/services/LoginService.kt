package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.CharacterInfo
import de.fiereu.openmmo.common.auth.SessionTokenVerifier
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.ChatType
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Language
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.CharacterEntry
import de.fiereu.openmmo.net.game.packets.CharactersListPacket
import de.fiereu.openmmo.net.game.packets.ChatMessagePacket
import de.fiereu.openmmo.net.game.packets.CreateCharacterPacket
import de.fiereu.openmmo.net.game.packets.CreateCharacterResultPacket
import de.fiereu.openmmo.net.game.packets.DeleteCharacterPacket
import de.fiereu.openmmo.net.game.packets.DeleteCharacterResultPacket
import de.fiereu.openmmo.net.game.packets.EntityMovePacket
import de.fiereu.openmmo.net.game.packets.JoinPacket
import de.fiereu.openmmo.net.game.packets.JoinResponsePacket
import de.fiereu.openmmo.net.game.packets.MenuPagePayloadPacket
import de.fiereu.openmmo.net.game.packets.NewAuthData
import de.fiereu.openmmo.net.game.packets.ObjectiveProgressBulkPacket
import de.fiereu.openmmo.net.game.packets.PokedexSpeciesResetPacket
import de.fiereu.openmmo.net.game.packets.RailEntityMovePacket
import de.fiereu.openmmo.net.game.packets.RenderScreenPacket
import de.fiereu.openmmo.net.game.packets.RequestCharactersPacket
import de.fiereu.openmmo.net.game.packets.RequestPlayerPacket
import de.fiereu.openmmo.net.game.packets.Season
import de.fiereu.openmmo.net.game.packets.SeasonPacket
import de.fiereu.openmmo.net.game.packets.SelectCharacterPacket
import de.fiereu.openmmo.net.game.packets.SelectedCharacterPacket
import de.fiereu.openmmo.net.game.packets.ViewScalePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleRatingBulkPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleStateBytePacket
import de.fiereu.openmmo.server.game.session.PENDING_MAP_LOAD
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

private const val DELETE_SUCCESS = 0
private const val DELETE_REJECTED = 1

/** The plain Bicycle (FRLG ITEM_BICYCLE = 360) - the item the client's bike feature is tied to. */
private const val BICYCLE_ITEM_ID = 360

/** Granted by mistake before the real Bicycle id was found; reclaimed at login. */
private const val MACH_BIKE_ITEM_ID = 259

/**
 * The twelve bicycle colors (client formula 2000 + BIKE slot 11 * 256 + addon 0..11): Red, Black,
 * Motorcycle, Blue, Brown, Green, Orange, Pink, Purple, Silver, White, Yellow.
 */
private val BIKE_SKIN_ITEM_RANGE = 4816..4827

/** Client-generated cosmetic item ids. */
private val COSMETIC_BAND = 2000..4887

/** See [CosmeticsRegistry.variantAltItems] - alts select through the base item's window. */
private val VARIANT_ALT_ITEMS = CosmeticsRegistry.variantAltItems

/**
 * NO pause before the arrival walk - it fires with the spawn, so the scripted move seizes the
 * movement controller before buffered player input can (the input-lock packet alone proved too weak
 * to stop it). The door animation plays over the walk, as in vanilla.
 */
private const val EMERGENCE_STEP_DELAY_MS = 0L

/** GBA steps wait for the fade-in, or the walk happens invisibly during the black screen. */
private const val GBA_STEP_DELAY_MS = 300L

/** Rail walks wait this long for the client to finish attaching the spawn to the rail. */
private const val RAIL_STEP_DELAY_MS = 250L

/** How long the one-tile walk itself takes; input unlocks after it. */
private const val EMERGENCE_STEP_WALK_MS = 400L

@Singleton
class LoginService
@Inject
constructor(
    private val mapLoadService: MapLoadService,
    private val npcService: NpcService,
    private val multiplayerService: MultiplayerService,
    private val socialService: SocialService,
    private val sessionRegistry: SessionRegistry,
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val presenceService: PresenceService,
    private val mapScriptService: MapScriptService,
    private val tokenVerifier: SessionTokenVerifier,
    private val worldStateService: WorldStateService,
    private val ndsWarps: NdsWarps,
    private val ocarinas: OcarinaService,
    private val guildService: GuildService,
    private val linkService: LinkService,
) {

  suspend fun onJoinGame(event: PacketEvent<JoinPacket>) {
    val ctx = event.session
    val authData = event.packet.authData

    if (authData !is NewAuthData) {
      log.warn { "Rejected join with unsupported auth data ${authData::class.simpleName}" }
      rejectJoin(ctx)
      return
    }

    val token = tokenVerifier.verify(authData.sessionKey)
    if (token == null) {
      log.warn {
        "Rejected join for claimed userId=${authData.userId}, " +
            "session token invalid or expired (${authData.sessionKey.size} bytes)"
      }
      rejectJoin(ctx)
      return
    }

    // The claim in the packet is the client's, the token is the login server's. Compare before
    // narrowing, so a value that does not fit an Int cannot match by truncation.
    if (token.userId != authData.userId.toLong() || token.userId <= 0) {
      log.warn { "Join claimed userId=${authData.userId} but its token says ${token.userId}" }
      rejectJoin(ctx)
      return
    }
    val userId = token.userId.toInt()

    ctx.attributes[PLAYER_STATE] = PlayerState(userId = userId)
    sessionRegistry.register(ctx)
    log.info { "Session created for user $userId" }

    // The first int is not playtime: the client (f/OQ -> NZ1.mP, read by the menu header f/gz)
    // treats it as the Donator Status expiry in epoch seconds and shows the status while it lies
    // in the future. The other two are still unidentified.
    val donatorUntil = characterStore.donatorUntil(userId)?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: 0
    ctx.send(JoinResponsePacket.acceptNow(playtime = donatorUntil, rewardPoints = 420, balance = 187))
  }

  // Closing is what keeps a refused peer from going on to send packets the handlers would
  // otherwise answer without a PlayerState. Close only once the refusal itself has been written.
  private fun rejectJoin(ctx: SessionContext) {
    ctx.send(JoinResponsePacket.reject()).addListener { ctx.close { "join rejected" } }
  }

  /**
   * Character creation answers with s2c 0x03 (client f/SU1): a result code the creation window
   * turns into a string and shows in place, buttons re-enabled - see [CreateCharacterResultPacket].
   * An accepted character is delivered as the refreshed list, which the client takes equally.
   */
  suspend fun onCreateCharacter(event: PacketEvent<CreateCharacterPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE]
    if (state == null) {
      log.warn { "Create character from unknown session" }
      return
    }
    val name = event.packet.name.trim()
    if (name.isEmpty() || name.length > 32 || characterStore.isNameTaken(name)) {
      log.info { "Rejected character name '${event.packet.name}' for userId=${state.userId}: taken or unusable" }
      ctx.send(CreateCharacterResultPacket(CreateCharacterResultPacket.NAME_UNAVAILABLE))
      return
    }
    val gender = CharacterGender.byWireValue(event.packet.gender)
    val startingRegion = Region.byWireValue(event.packet.startingRegion)
    if (gender == null || startingRegion == null) {
      log.warn {
        "Rejected character options gender=${event.packet.gender} " +
            "region=${event.packet.startingRegion} for userId=${state.userId}"
      }
      ctx.send(
          CreateCharacterResultPacket(
              if (gender == null) CreateCharacterResultPacket.INVALID_GENDER
              else CreateCharacterResultPacket.SYSTEM_ERROR))
      return
    }
    log.info { "Creating character '$name' for userId=${state.userId}" }
    val appearance = event.packet.appearance
    try {
      characterStore.createCharacter(
          state.userId,
          name,
          gender,
          startingRegion,
          skins = appearance.toMap(),
          skinRegionSelectionIndex = appearance.regionSelectionIndex,
      )
    } catch (e: Exception) {
      // The unique index caught a creation that raced ours, or the database is unavailable.
      log.warn(e) { "Character '$name' for userId=${state.userId} could not be stored" }
      ctx.send(CreateCharacterResultPacket(CreateCharacterResultPacket.SYSTEM_ERROR))
      return
    }
    ctx.send(buildCharacterList(state.userId))
  }

  suspend fun onCharacterRequest(event: PacketEvent<RequestCharactersPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE]
    if (state != null) {
      ctx.send(buildCharacterList(state.userId))
    } else {
      log.warn { "Character request from unauthenticated session" }
      ctx.send(CharactersListPacket(emptyList()))
    }
  }

  suspend fun onDeleteCharacter(event: PacketEvent<DeleteCharacterPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE]
    val userId = state?.userId
    val characterId = event.packet.characterId
    val deleted =
        userId != null &&
            state.characterId == null &&
            characterStore.deleteCharacter(userId, characterId)
    if (deleted) {
      log.info { "Deleted character $characterId for userId=$userId" }
    } else {
      log.warn { "Rejected character deletion id=$characterId from ${ctx.remoteAddress}" }
    }
    ctx.send(
        DeleteCharacterResultPacket(if (deleted) DELETE_SUCCESS else DELETE_REJECTED, characterId))
  }

  private suspend fun buildCharacterList(userId: Int): CharactersListPacket {
    val characters = characterStore.getCharactersByUser(userId)
    val entries =
        characters.map { stored ->
          CharacterEntry(
              characterInfo = stored.info,
              skinSet = SkinSet(stored.info.skinRegionSelectionIndex, stored.skins),
              guildId = null,
              // Character selection shows the complete party.
              pokemon = stored.pokemon,
          )
        }
    return CharactersListPacket(entries)
  }

  suspend fun onCharacterSelected(event: PacketEvent<SelectCharacterPacket>) {
    val ctx = event.session
    val charId = event.packet.characterId
    val state = ctx.attributes[PLAYER_STATE]
    if (state == null) {
      log.warn { "No session for channel" }
      return
    }
    val stored = characterStore.getOrLoadCharacter(charId)
    if (stored == null) {
      log.warn { "Character $charId not found" }
      return
    }
    if (stored.info.userId != state.userId) {
      log.warn { "User ${state.userId} tried to select character $charId owned by another account" }
      return
    }

    state.characterId = charId
    sessionRegistry.bindCharacter(ctx, charId)
    log.info { "Player selected character '${stored.info.name}' (id=$charId)" }

    // The Bicycle is earned through each region's own bike quest (StoryPlayerService maps every
    // region's bike item onto the client's Bicycle); the old unconditional grant is gone.
    // The twelve bicycle colors, as cosmetic-category items. PROVEN by two live sessions: the
    // customization dialog's option lists are built from the bag (when the 793-cosmetic grant
    // was in the bag, EVERY mount was listed; with an empty bag only the client's two built-in
    // leftovers show) - unlock flags alone cannot conjure entries. So the colors ride along in
    // the bag's cosmetic pocket, the dialog lists them, and the chosen one becomes the stored
    // BIKE skin that the Bicycle's Use mounts. The bag-use resolver skips this band entirely,
    // so the Bicycle itself cannot be shadowed again. Everything else in the band is reclaimed
    // - event mounts stay gated on deliberate grants.
    var granted = 0
    for (itemId in BIKE_SKIN_ITEM_RANGE) {
      if (itemId !in stored.items) {
        characterStore.addItem(charId, itemId, 1)
        granted++
      }
    }
    // Variant-alt cosmetics (Ur0.AL1's table: Noble Steed (Alt), Flaming Skull's alt, ...) are
    // chosen through the BASE item's variant window - holding the alt item makes it show as a
    // separate standalone entry, which retail never does. Reclaim them, and collapse cosmetic
    // stacks to one (five Flaming Skulls list no better than one).
    var cleaned = 0
    for ((itemId, quantity) in stored.items) {
      if (itemId in VARIANT_ALT_ITEMS && quantity > 0) {
        characterStore.addItem(charId, itemId, -quantity)
        cleaned++
      } else if (itemId in COSMETIC_BAND && quantity > 1) {
        characterStore.addItem(charId, itemId, 1 - quantity)
        cleaned++
      }
    }
    if (granted > 0 || cleaned > 0) {
      characterStore.flushCharacterAsync(charId)
      log.info { "Cosmetics: granted $granted bike colors, cleaned $cleaned stacks ($charId)" }
    }
    // Take back the Hoenn-exclusive Mach Bike an earlier build handed out by mistake.
    val machBikes = stored.items[MACH_BIKE_ITEM_ID] ?: 0
    if (machBikes > 0) {
      characterStore.addItem(charId, MACH_BIKE_ITEM_ID, -machBikes)
      characterStore.flushCharacterAsync(charId)
    }

    // Re-read AFTER the grants/reclaims above: this call builds the bag snapshot and the
    // per-item unlock variables, and the pre-grant `stored` reference is a stale copy - the
    // 793-cosmetic incident shipped a bag without any of the granted items because of it.
    worldStateService.send(ctx, characterStore.getCharacter(charId) ?: stored)

    val info = stored.info
    val now = LocalDateTime.now()
    val updatedInfo = info.copy(lastLogin = now)
    characterStore.updateCharacter(updatedInfo)

    ctx.send(SelectedCharacterPacket(info.copy(sweetScentPp = ocarinas.ppLeft(stored))))
    // Re-read after the bicycle grant so the join payload carries the current bag, and push the
    // stack explicitly - belt and braces against whichever packet the client trusts for the bag.
    val current = characterStore.getCharacter(charId) ?: stored
    sendJoinState(ctx, current)
    ctx.send(itemStackUpdatePacket(BICYCLE_ITEM_ID, current.items[BICYCLE_ITEM_ID] ?: 0))
    ctx.send(
        ChatMessagePacket(
            ChatType.GAME_NOTIFICATIONS,
            Language.EN,
            "Welcome to OpenMMO!",
            "",
        ))

    preloadMapAndJoin(ctx, state, info)
  }

  // Small state packets the real server sends in the join flow. The battle-state byte and menu
  // payloads initialise state the battle bag reads, so without them opening the bag crashes.
  private fun sendJoinState(ctx: SessionContext, stored: StoredCharacter) {
    ctx.send(ViewScalePacket(viewScale = 5))
    ctx.send(BattleRatingBulkPacket(emptyList()))
    ctx.send(BattleStateBytePacket(state = 0x19))
    // Which species show as revealed rather than silhouettes. Real data: what the character holds.
    // The seen/owned/OT tiers travel separately, in the world-flag table the join flow sends.
    ctx.send(
        PokedexSpeciesResetPacket(
            (stored.pokemon + stored.pcStorage)
                .map { clientSpeciesId(it.dexId).toShort() }
                .filter { it > 0 }
                .distinct()))
    ctx.send(ObjectiveProgressBulkPacket(emptyList()))
    ctx.send(MenuPagePayloadPacket(menuType = 0, page = null))
    ctx.send(MenuPagePayloadPacket(menuType = 1, page = null))
  }

  private fun preloadMapAndJoin(
      ctx: SessionContext,
      state: PlayerState,
      info: CharacterInfo,
  ) {
    // The season is GLOBAL, real-clock driven, and identical on every path (login and all
    // warps): all regions carry seasonal content, and a consistent value means the client's
    // season-change refresh listeners never fire mid-transition (mixed NDS/GBA values did, and
    // glitched GBA door-exit animations).
    ctx.send(SeasonPacket(Season.current()))
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    if (map != null) {
      log.info {
        "Placing '${info.name}' at ${info.positionRegionId}:${info.positionBankId}:" +
            "${info.positionMapId} (${info.positionX}, ${info.positionY})"
      }
      mapLoadService.resetClientCache(ctx, map)
      ctx.send(mapManager.createLoadMapPacket(map, reloadPlayer = true, deleteCache = true))
      mapLoadService.preloadConnectedMaps(ctx, map)
    } else if ((info.positionRegionId.toInt() and 0xFF) in 2..4) {
      // An NDS position: the server holds no map data - the client renders it from its own ROM,
      // exactly like a raw warp does. Without this send a login inside Unova sat on a black
      // screen until a GM warp rescued it.
      val regionId = info.positionRegionId.toInt() and 0xFF
      val bankId = info.positionBankId.toInt() and 0xFF
      val mapId = info.positionMapId.toInt() and 0xFF
      // deleteCache=true is safe HERE, unlike in executeRawWarp: at login no map is being
      // rendered yet, so the wipe cannot race a render frame (the Cold Storage NPE). It is also
      // what makes seasons appear - the client bakes the season into its converted map models,
      // so a cache converted under the old permanent-summer sends stays summer forever unless a
      // login reconverts it under the current season.
      ctx.send(
          de.fiereu.openmmo.net.game.packets.LoadMapPacket(
              reloadPlayer = true,
              deleteCache = true,
              regionId = regionId,
              bankId = bankId,
              mapId = mapId,
              mapData =
                  de.fiereu.openmmo.net.game.packets.MapData.NdsMapData(
                      lighting = de.fiereu.openmmo.common.enums.Lighting.REGULAR,
                      weather = de.fiereu.openmmo.common.enums.Weather.REGULAR_WEATHER,
                      mapType = NdsMapTypes.typeOf(regionId.toInt() and 0xFF, bankId.toInt() and 0xFF, mapId.toInt() and 0xFF),
                  )),
      )
      // The rail line is not persisted across sessions; a relog on a rail tile must re-attach or
      // the map renders as a blue void. Best effort: recover the line from the warp table.
      val rows =
          ndsWarps.rowsAt(regionId, bankId, mapId, info.positionX.toInt(), info.positionY.toInt())
      state.pendingRailLine = rows.firstOrNull { it.srcLine >= 0 }?.srcLine ?: -1
      // Logging out on a door mat is the norm now (interior arrivals rest there); hold
      // direction-less warps until the player has stood on a warp-free tile once.
      state.ndsWarpGuard = rows.isNotEmpty()
    } else {
      log.warn {
        "Map not found for position ${info.positionRegionId}:${info.positionBankId}:${info.positionMapId}"
      }
    }

    // Bank/map are stored as bytes; NDS banks run past 127 (Cold Storage is 192), so widening
    // must be unsigned or the tracked position goes negative and every warp lookup misses.
    state.regionId = info.positionRegionId.toInt() and 0xFF
    state.bankId = info.positionBankId.toInt() and 0xFF
    state.mapId = info.positionMapId.toInt() and 0xFF
    state.x = info.positionX
    state.y = info.positionY
    state.facingDirection = info.positionFacing
    // Elevation is left alone on purpose. Spawning the player on the tile's own elevation makes
    // the client refuse every step, and a warp has already set the one value that works.

    multiplayerService.broadcastMessage(
        ChatMessagePacket(
            ChatType.GAME_NOTIFICATIONS,
            Language.EN,
            "Player ${info.name} joined the game.",
            "",
        ))
  }

  /**
   * The arrival emergence step: one client-walked tile the way the player faces, sent DIRECTLY
   * after the self LoadEntity so nothing can claim the movement controller first. NDS-gated: hosted
   * GBA maps validate moves against the stored position, and committing an unwalked step there
   * froze the controller once. Rail walks use the rail-attached 0xEC variant.
   */
  private fun sendEmergenceStep(
      ctx: SessionContext,
      state: PlayerState,
      charId: Long,
      info: CharacterInfo,
  ) {
    val regionId = info.positionRegionId.toInt() and 0xFF
    val stepDir = state.pendingStepDir
    state.pendingStepDir = null
    if (stepDir == null) {
      state.pendingStepX = -1
      state.pendingStepY = -1
      if (System.currentTimeMillis() < state.moveIgnoreUntil) {
        // A DUPLICATE RequestPlayer inside the choreography window - outdoor GBA maps ask for
        // their player once per map connection, and the first request already consumed the
        // step and started the walk. Releasing here freed the player mid-emergence (run off,
        // rubber-band back); re-assert instead and let the choreography's own timed release
        // fire when the walk is done.
        ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = true))
        return
      }
      // No step on this arrival (GBA rest-on-tile warps, logins) - release the warp-start lock
      // right away. Idempotent when no lock was sent, and it heals any stale lock as a bonus.
      // Never over a running script though: an entry cutscene that started owns the lock now.
      if (!state.scriptRunning && !state.blocksPlayerInput) {
        ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
      }
      return
    }
    // RAIL maps mirror BOTH axes relative to normal maps (observed live on the Castelia plaza:
    // pressing RIGHT decrements x, pressing DOWN decrements y), so a rail step NEGATES the
    // facing deltas - the one constant rule that replaces the deleted approach recorder.
    // Verified: Pokecenter mat (8,3) facing down -> (8,2); lower city (2,-5) facing up -> (2,-4).
    val onRail = state.pendingRailLine >= 0
    val sdx = if (onRail) -stepDir.dx else stepDir.dx
    val sdy = if (onRail) -stepDir.dy else stepDir.dy
    val tx = if (state.pendingStepX >= 0) state.pendingStepX else info.positionX + sdx
    val ty = if (state.pendingStepX >= 0) state.pendingStepY else info.positionY + sdy
    state.pendingStepX = -1
    state.pendingStepY = -1
    // GBA arrivals fade in slower than the instant NDS renders: an immediate step finishes
    // during the black screen and reads as "spawned one tile down, no animation". Delay the
    // walk past the fade-in there; NDS keeps the same-flush send that beats buffered input.
    val stepDelay = if (regionId in 2..4) 0L else GBA_STEP_DELAY_MS
    // Movement stays refused server-side for the whole choreography window - the lock packet
    // races in-flight moves on the wire, and a stale move used to fire a fresh warp.
    state.moveIgnoreUntil = System.currentTimeMillis() + stepDelay + EMERGENCE_STEP_WALK_MS + 100
    // Re-assert the scripted-state lock AT arrival: map transitions reset client state, so
    // the warp-start lock may have been wiped by the time the new map is up.
    ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = true))
    val railLine = state.pendingRailLine
    // NDS commits the stepped tile at send (its move handling overwrites from the client's own
    // reports anyway, so a dropped walk self-heals). GBA does NOT commit: under rapid in/out
    // cycling the client sometimes never plays the EntityMove (still loading the re-sent map),
    // and a pre-committed stepped tile then disagreed with the client's real mat position -
    // the desync fight ended with the client's door fade waiting on a warp the server refused
    // (the black screen). The server stays on the mat and the client's FIRST move confirms the
    // real tile (acceptNextMoveSource): walked = syncs to the stepped tile, dropped = already
    // agrees. Either way both sides match and every door entry stays a client-walked entry.
    val sendStep: () -> Unit = {
      if (regionId in 2..4) {
        characterStore.updatePosition(charId, tx.toShort(), ty.toShort())
        state.x = tx.toShort()
        state.y = ty.toShort()
      } else {
        // The client may play this walk LATE (queued behind the map load) and never reports
        // server-commanded moves back; the validator's one-tile heal reconciles whichever
        // tile the client's first real move claims.
        state.acceptNextMoveSource = true
      }
      if (railLine >= 0) {
        log.info { "Emergence step (rail): char=$charId -> ($tx, $ty) dir=$stepDir line=$railLine" }
        ctx.send(
            RailEntityMovePacket(
                entityId = charId,
                railLine = railLine,
                x = tx,
                y = ty,
                direction = stepDir,
            ))
      } else {
        // Walk direction from the ACTUAL displacement toward the target, so the sprite cannot
        // face one way while moving another (the sideways-slide bug).
        val dxs = tx - info.positionX
        val dys = ty - info.positionY
        val moveDir =
            Direction.entries.firstOrNull {
              it.dx == dxs.coerceIn(-1, 1) && it.dy == dys.coerceIn(-1, 1)
            } ?: stepDir
        log.info { "Emergence step: char=$charId -> ($tx, $ty) dir=$moveDir" }
        ctx.send(EntityMovePacket(entityId = charId, x = tx, y = ty, direction = moveDir))
      }
    }
    val scope =
        ctx.attributes.getOrPut(SCRIPT_SCOPE) {
          CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    // The release only fires once the walk has played, and never over a running script - an
    // entry cutscene that started during the choreography owns the lock and releases it itself.
    val releaseLock: () -> Unit = {
      if (ctx.channel.isActive && !state.scriptRunning && !state.blocksPlayerInput) {
        ctx.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
      }
    }
    // The +120ms keeps the release AFTER the server's own move-refusal window closes - freed
    // input whose first step lands inside the window would be dropped and read as a desync.
    if (stepDelay == 0L) {
      sendStep()
      scope.launch {
        delay(EMERGENCE_STEP_WALK_MS + 120)
        releaseLock()
      }
    } else {
      scope.launch {
        delay(stepDelay)
        if (ctx.channel.isActive) sendStep()
        delay(EMERGENCE_STEP_WALK_MS + 120)
        releaseLock()
      }
    }
  }

  fun onRequestPlayer(event: PacketEvent<RequestPlayerPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE]
    if (state == null) {
      log.warn { "RequestPlayer from unknown session" }
      return
    }
    val charId = state.characterId
    if (charId == null) {
      log.warn { "RequestPlayer without active character" }
      return
    }
    // The client asks for its player once a map transition is done, so the warp ends here. The
    // waiter is only cleared at the end, so a throw in between leaves the deadline to rescue it.
    state.justWarped = false
    val pendingLoad = ctx.attributes[PENDING_MAP_LOAD]

    val stored = characterStore.getCharacter(charId)
    if (stored == null) {
      log.warn { "RequestPlayer for unknown character $charId" }
      return
    }
    val info = stored.info

    log.info { "Sending LoadEntity for character '${info.name}'" }
    val facing = state.facingDirection
    val loadEntity =
        mapLoadService.createLoadEntity(
            info,
            facing,
            state.elevation,
            party = stored.pokemon,
            skins = stored.skins,
            railLine = state.pendingRailLine,
            // Bit 1 = riding (f.ti.U7): a re-spawn while on the bike keeps the mounted frames.
            transportation = if (state.riding) 0x02 else state.transportOverride,
            followerId = state.followerMonId,
        )
    ctx.send(loadEntity)
    // The emergence step goes out DIRECTLY after the spawn packet - NPC spawns, presence
    // snapshots and the fade-in used to sit between them, which on heavy maps (Castelia)
    // delayed the walk long enough for input to slip in.
    sendEmergenceStep(ctx, state, charId, info)

    // The story flags from the character-select world-state block never land: the client's
    // 0x2A handler drops updates until its game state exists. Re-send them ONCE, now that the
    // client is demonstrably in-world (it asked for its player) - badges, fly points and gates
    // survive a relog because of this, not the login block.
    if (!state.storyFlagsSynced) {
      state.storyFlagsSynced = true
      StoryClientState.flags(info.positionRegionId, stored.storyFlags).forEach(ctx::send)
    }

    npcService.spawnNpcsWithNeighbors(
        ctx,
        info.positionBankId.toInt() and 0xFF,
        info.positionMapId.toInt() and 0xFF,
        info.positionRegionId.toInt() and 0xFF,
    )
    if (info.positionRegionId.toInt() in 2..4) {
      mapScriptService.onNdsEnter(ctx, state, info.positionRegionId.toInt(), info.positionBankId.toInt() and 0xFF, info.positionMapId.toInt() and 0xFF)
    }

    // Unsigned on purpose: NDS banks run past 127 (see preloadMapAndJoin).
    val bankId = info.positionBankId.toInt() and 0xFF
    val mapId = info.positionMapId.toInt() and 0xFF
    val regionId = info.positionRegionId.toInt() and 0xFF
    state.regionId = regionId
    state.bankId = bankId
    state.mapId = mapId
    state.x = info.positionX
    state.y = info.positionY

    // The client dropped its entities with the map cache, so always re-exchange snapshots.
    presenceService.enter(ctx)

    ctx.send(RenderScreenPacket(true))

    // An entry script may fade back out, so it runs after the fade this arrival owns.
    mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)?.let { map ->
      mapScriptService.onMapEnter(ctx, state, map)
    }

    socialService.sendFriendList(ctx)
    guildService.sendMembership(ctx)
    linkService.sendTo(ctx, charId)
    if (ctx.attributes[PENDING_MAP_LOAD] === pendingLoad) ctx.attributes.remove(PENDING_MAP_LOAD)
    pendingLoad?.complete(Unit)

    log.info { "Player $charId spawned in bank=$bankId map=$mapId" }
  }
}

/**
 * The last id in the client's contiguous species range: 649 canonical, its own 1000-1052 block, and
 * every imported base species. Forms sit above this and are not listed by the Pokedex.
 */
