package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MovementType
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.net.game.packets.EntityLeavePacket
import de.fiereu.openmmo.net.game.packets.NpcSpawnPacket
import de.fiereu.openmmo.net.game.packets.NpcUpdatePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

@Singleton
class NpcService
@Inject
constructor(
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val ferry: FerryPlacements = FerryPlacements(mapManager),
    private val raid: CrystalOnixRaidPlacement = CrystalOnixRaidPlacement(mapManager),
    private val scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
    private val ndsNpcs: NdsNpcs = NdsNpcs(),
) {

  private val npcEntityIdCounter = AtomicLong(0x1A69000000000000L)
  private val npcEntityIds = mutableMapOf<String, Long>()

  fun getNpcEntityId(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): Long? {
    return npcEntityIds[key(regionId, bankId, mapId, entityIdx)]
  }

  /**
   * Spawns the map's npcs plus every connected neighbour's: the zoomed-out camera sees across
   * connections, and without the neighbours their people popped into existence at the border. The
   * per-session spawned-set stops duplicates when connections are re-crossed; a warp's cache reset
   * clears it.
   */
  fun spawnNpcsWithNeighbors(ctx: SessionContext, bankId: Int, mapId: Int, regionId: Int) {
    spawnNpcsOnce(ctx, bankId, mapId, regionId)
    val map = mapManager.getMap(regionId, bankId, mapId) ?: return
    for (connection in map.connections) {
      spawnNpcsOnce(ctx, connection.targetBank, connection.targetMap, regionId)
    }
  }

  /** [spawnNpcsForMap] unless this session already holds that map's NPCs (PlayerState.spawnedNpcMaps). */
  private fun spawnNpcsOnce(ctx: SessionContext, bankId: Int, mapId: Int, regionId: Int) {
    val state = ctx.attributes[PLAYER_STATE]
    if (state != null && !state.spawnedNpcMaps.add(de.fiereu.openmmo.server.game.session.mapCacheKey(regionId, bankId, mapId))) return
    spawnNpcsForMap(ctx, bankId, mapId, regionId)
  }

  fun spawnNpcsForMap(ctx: SessionContext, bankId: Int, mapId: Int, regionId: Int) {
    val state = ctx.attributes[PLAYER_STATE]
    // The /probe npcs experiment: with spawns suppressed, an empty map proves NPCs are
    // server-fed; a populated one proves the client spawns its own.
    if (state?.suppressNpcSpawns == true) {
      log.info { "NPC spawns suppressed for $regionId:$bankId:$mapId (probe)" }
      return
    }
    val map = mapManager.getMap(regionId, bankId, mapId)
    if (map == null) {
      spawnNdsNpcs(ctx, regionId, bankId, mapId)
      return
    }
    val stored = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)
    val storyFlags = stored?.storyFlags.orEmpty()
    val storyVars = stored?.storyVars.orEmpty()

    for (npc in map.npcs) {
      // An object placed outside its own map is the ROM's stand-in for a neighbour's object,
      // drawn while that map is not loaded (nine exist, all scriptless: Cerulean's second cut
      // tree at (50,18) sits on Route 9's (2,8), where Route 9 keeps the real one). The GBA never
      // shows both; we spawned both, so cutting Route 9's tree left Cerulean's twin standing
      // and blocking (2026-09-08). Neighbours are spawned from their own lists here.
      if (npc.x !in 0 until map.width || npc.y !in 0 until map.height) continue
      // Decoration slots are not normal NPCs.
      if (npc.hideFlag.substringAfter('/').startsWith(DECORATION_FLAG_PREFIX)) continue

      // Only set hide flags suppress NPCs.
      if (shouldHideNpc(bankId, mapId, npc, storyFlags)) continue
      val resolved =
          resolveDynamicGraphics(
              ctx,
              applyXyOverride(
                  regionId,
                  bankId,
                  mapId,
                  applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
                  storyVars),
              regionId,
              storyVars)
      // A still-unresolved VAR sprite would be an invisible but solid entity on the client; the
      // GBA does not spawn these either. The post-script refresh sends it once its var is set.
      if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) continue
      ctx.send(
          buildSpawnPacket(
              resolved,
              entityIdFor(regionId, bankId, mapId, npc.entityIdx),
              regionId,
              bankId,
              mapId,
              berryState = berryState(storyVars, regionId, bankId, mapId, resolved),
          ))
    }
    // The region-link ferry captain is not in the ROM; he stands in the harbour town.
    ferry.at(regionId, bankId, mapId)?.let { p ->
      ctx.send(buildSpawnPacket(p.npc(), entityIdFor(regionId, bankId, mapId, FerryPlacements.LOCAL_ID), regionId, bankId, mapId, look = p.look))
    }
    spawnRaidBoss(ctx, regionId, bankId, mapId, storyVars)
  }

  /** The Crystal Onix raid boss, gone for the rest of the day once this character has beaten it. */
  private fun spawnRaidBoss(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, storyVars: Map<String, Int>) {
    if (!raid.isHere(regionId, bankId, mapId) ||
        de.fiereu.openmmo.server.game.battle.CrystalOnixRaid.beatenToday(storyVars, WorldClock.today())) {
      return
    }
    ctx.send(
        buildSpawnPacket(
            raid.npc(), entityIdFor(regionId, bankId, mapId, CrystalOnixRaidPlacement.LOCAL_ID), regionId, bankId, mapId,
            spriteRegionId = CrystalOnixRaidPlacement.SPRITE_REGION,
            // The hit box, in tiles (f/ni6 adds dw2.QX()/oy() to the tile position). The follower
            // graphics id has no sprite of its own in the set, so the size is given: one tile.
            spriteSize = 1.toByte() to 1.toByte()))
  }

  /** Takes the raid boss off the map once it has been beaten (it returns the next day). */
  fun despawnRaidBoss(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int) {
    despawnNpc(ctx, regionId, bankId, mapId, CrystalOnixRaidPlacement.LOCAL_ID)
  }

  /** Puts the raid boss back in front of a player standing on its map, once today's win is cleared (/devraid). */
  fun respawnRaidBoss(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int) {
    if (!raid.isHere(regionId, bankId, mapId)) return
    val storyVars = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)?.storyVars.orEmpty()
    // Replace, not stack: the client may still hold the boss under this entity id.
    despawnNpc(ctx, regionId, bankId, mapId, CrystalOnixRaidPlacement.LOCAL_ID)
    spawnRaidBoss(ctx, regionId, bankId, mapId, storyVars)
  }

  /**
   * Re-sends the npcs whose sprite or position depends on story state, after a map's ON_TRANSITION
   * script wrote the vars they read. The decomp runs that script before objects load; here the
   * script runs after the spawn, so these npcs are corrected in place.
   */
  fun refreshDynamicNpcs(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, forceEntities: Set<Int> = emptySet()) {
    val map = mapManager.getMap(regionId, bankId, mapId) ?: return
    val stored = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)
    val storyFlags = stored?.storyFlags.orEmpty()
    val storyVars = stored?.storyVars.orEmpty()
    for (npc in map.npcs) {
      if (npc.hideFlag.substringAfter('/').startsWith(DECORATION_FLAG_PREFIX)) continue
      // ON_TRANSITION also sets hide flags (Vermilion hides Oak's aide once he has been talked
      // to; the cartridge runs it before objects load, we run it after the spawn), so an npc the
      // fresh flags hide leaves now instead of standing there until the next visit.
      if (shouldHideNpc(bankId, mapId, npc, storyFlags)) {
        despawnNpc(ctx, regionId, bankId, mapId, npc.entityIdx)
        continue
      }
      val dynamicGfx = npc.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3
      val hasOverride = xyOverrideKey(regionId, bankId, mapId, npc.entityIdx) in storyVars
      if (!dynamicGfx && !hasOverride && npc.entityIdx !in forceEntities) continue
      val resolved =
          resolveDynamicGraphics(
              ctx,
              applyXyOverride(
                  regionId,
                  bankId,
                  mapId,
                  applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
                  storyVars),
              regionId,
              storyVars)
      if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) continue
      // Replace, not stack: despawn whatever the client holds under this entity id first.
      despawnNpc(ctx, regionId, bankId, mapId, npc.entityIdx)
      ctx.send(
          buildSpawnPacket(
              resolved,
              entityIdFor(regionId, bankId, mapId, npc.entityIdx),
              regionId,
              bankId,
              mapId,
          ))
    }
  }

  /**
   * The npc as this character actually sees it spawned - the template adjusted by story placement
   * and any setobjectxyperm override, the same pipeline the spawn uses. Position math (facing,
   * adjacency) must use this, never the raw MapDef template: a story-moved npc read from the
   * template fails the adjacency test and faces the wrong way.
   */
  fun effectiveNpc(
      regionId: Int,
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
      storyVars: Map<String, Int>,
  ): NpcDef =
      applyXyOverride(
          regionId,
          bankId,
          mapId,
          applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
          storyVars)

  /** A soil spot's packed plant state for this character, null for any other npc. */
  private fun berryState(storyVars: Map<String, Int>, regionId: Int, bankId: Int, mapId: Int, npc: NpcDef): Int? =
      if (npc.graphicsId != BerryPlots.BERRY_TREE_GFX) null
      else BerryPlots.packedState(BerryPlots.view(BerryPlots.Plot(storyVars, "berry/$regionId/$bankId/$mapId/${npc.entityIdx}")))

  private fun sessionStoryVars(ctx: SessionContext): Map<String, Int> =
      ctx.attributes[PLAYER_STATE]
          ?.characterId
          ?.let(characterStore::getCharacter)
          ?.storyVars
          .orEmpty()

  /** Every xy override key of one map starts with this. */
  fun xyOverridePrefix(regionId: Int, bankId: Int, mapId: Int): String {
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    return "$namespace/objxy/$bankId:$mapId:"
  }

  /** The story-var key setobjectxyperm writes an npc's overridden tile into (x shl 12 or y). */
  fun xyOverrideKey(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): String {
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    return "$namespace/objxy/$bankId:$mapId:$entityIdx"
  }

  /** The story-var key setobjectmovementtype writes (MovementType ordinal + 1). */
  fun movementOverrideKey(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): String {
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    return "$namespace/objmov/$bankId:$mapId:$entityIdx"
  }

  fun movementOverridePrefix(regionId: Int, bankId: Int, mapId: Int): String {
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    return "$namespace/objmov/$bankId:$mapId:"
  }

  /**
   * The ROM's setobjectmovementtype: an entry script pins a wanderer in place (Pallet's sign lady
   * faces the route entrance while she waits for you) and the client must be told the pinned
   * type, or it wanders her off the tile her trigger sits beside.
   */
  private fun applyMovementOverride(regionId: Int, bankId: Int, mapId: Int, npc: NpcDef, storyVars: Map<String, Int>): NpcDef {
    val stored = storyVars[movementOverrideKey(regionId, bankId, mapId, npc.entityIdx)] ?: return npc
    val type = MovementType.entries.getOrNull(stored - 1) ?: return npc
    val facing =
        when (type) {
          MovementType.FACE_UP -> Direction.UP
          MovementType.FACE_DOWN -> Direction.DOWN
          MovementType.FACE_LEFT -> Direction.LEFT
          MovementType.FACE_RIGHT -> Direction.RIGHT
          else -> npc.facing
        }
    return npc.copy(movementType = type, facing = facing)
  }

  private fun applyXyOverride(
      regionId: Int,
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyVars: Map<String, Int>,
  ): NpcDef {
    val moved = applyMovementOverride(regionId, bankId, mapId, npc, storyVars)
    val packed = storyVars[xyOverrideKey(regionId, bankId, mapId, npc.entityIdx)] ?: return moved
    return moved.copy(x = packed shr 12, y = packed and 0xFFF)
  }

  /** Allocate (or return) the stable entity id for a map npc by its decomp local id. */
  /** The ROM npc behind a DS-map entity id, for interaction. */
  fun ndsNpcForEntity(regionId: Int, bankId: Int, mapId: Int, entityId: Long): NdsNpcs.Npc? =
      ndsNpcs.of(regionId, bankId, mapId).firstOrNull { entityIdFor(regionId, bankId, mapId, it.index) == entityId }

  fun entityIdFor(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): Long =
      npcEntityIds.getOrPut(key(regionId, bankId, mapId, entityIdx)) {
        npcEntityIdCounter.incrementAndGet()
      }

  /** Spawn a single npc (including a normally hidden one) for one player, for cutscenes. */
  fun spawnNpc(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, localId: Int) {
    if (mapManager.getMap(regionId, bankId, mapId) == null) {
      // DS map: the npc comes from the ROM's zone events, not a MapDef (a script's AddNPC after
      // it cleared the npc's hide flag - Nuvema's Bianca, New Bark's Elm).
      val rom = findNdsNpc(regionId, bankId, mapId, localId) ?: return
      // Where a script already placed this actor this visit, not its ROM tile.
      val pose = ctx.attributes[PLAYER_STATE]?.scriptedNpcPoses?.get(de.fiereu.openmmo.server.game.session.scriptedNpcKey(regionId, bankId, mapId, localId))
      val npc = if (pose == null) rom else rom.copy(x = pose.x, y = pose.y)
      log.info { "Scripted DS spawn $regionId:$bankId:$mapId local=$localId sprite=${npc.sprite} at (${npc.x}, ${npc.y})" }
      sendAfterArrival(ctx, ndsSpawnPacket(regionId, bankId, mapId, npc))
      return
    }
    val npc = findNpc(regionId, bankId, mapId, localId) ?: return
    val storyVars = sessionStoryVars(ctx)
    val resolved =
        resolveDynamicGraphics(
            ctx, applyXyOverride(regionId, bankId, mapId, npc, storyVars), regionId, storyVars)
    if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) return
    log.info { "Scripted spawn $regionId:$bankId:$mapId local=$localId gfx=${resolved.graphicsId} at (${resolved.x}, ${resolved.y}) elev=${resolved.elevation} hideFlag=${npc.hideFlag}" }
    sendAfterArrival(
        ctx,
        buildSpawnPacket(
            resolved, entityIdFor(regionId, bankId, mapId, localId), regionId, bankId, mapId,
            berryState = berryState(storyVars, regionId, bankId, mapId, resolved)))
  }

  /**
   * A spawn sent inside the arrival choreography window (LoginService.moveIgnoreUntil) never
   * showed: the client is still bringing the map up and drops it, so a scene that adds its npc
   * as its first command (Four Island's rival, Six Island's Pokemon Center) played to an
   * invisible actor. Scripted moves already wait that window out (awaitSelfActions); the spawn
   * now waits the same way, so it still lands before the moves.
   */
  /** Packets held for a session's arrival window, sent in the order they were queued. */
  private val heldForArrival = java.util.concurrent.ConcurrentHashMap<SessionContext, java.util.ArrayDeque<Any>>()

  /**
   * Spawns and repositions during the arrival window are dropped by the client (it is still
   * loading the map), so they wait for the window to close - in ONE queue per session, in order.
   * Separate timers let a reposition overtake the spawn it belonged to: Littleroot's entry
   * script clears mom's hide flag (a spawn at her map default, the door tile) and then sets her
   * tile to the step in front of it; the reposition went out first and the late spawn put her
   * back in the doorway (2026-09-12).
   */
  fun sendAfterArrival(ctx: SessionContext, packet: Any) {
    val state = ctx.attributes[PLAYER_STATE]
    val wait = maxOf(state?.moveIgnoreUntil ?: 0L, state?.sceneHoldUntil ?: 0L) - System.currentTimeMillis()
    val pending = heldForArrival[ctx]
    if (wait <= 0 && pending == null) {
      ctx.send(packet)
      return
    }
    val queue = heldForArrival.computeIfAbsent(ctx) { java.util.ArrayDeque() }
    synchronized(queue) { queue.add(packet) }
    if (pending != null) return
    log.info { "Scripted ${packet.javaClass.simpleName} held ${wait}ms for the arrival window" }
    scope.launch {
      kotlinx.coroutines.delay(wait.coerceAtLeast(0L))
      val drained = heldForArrival.remove(ctx) ?: return@launch
      val packets = synchronized(drained) { drained.toList() }
      if (ctx.channel.isActive) packets.forEach { ctx.send(it) }
    }
  }

  /** Spawns an NPC at a cutscene position. */
  fun spawnNpcAt(
      ctx: SessionContext,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      localId: Int,
      x: Int,
      y: Int,
  ) {
    if (mapManager.getMap(regionId, bankId, mapId) == null) {
      val npc = findNdsNpc(regionId, bankId, mapId, localId) ?: return
      sendAfterArrival(ctx, ndsSpawnPacket(regionId, bankId, mapId, npc.copy(x = x, y = y)))
      return
    }
    val npc = findNpc(regionId, bankId, mapId, localId) ?: return
    sendAfterArrival(
        ctx,
        buildSpawnPacket(
            resolveDynamicGraphics(ctx, npc.copy(x = x, y = y), regionId, sessionStoryVars(ctx)),
            entityIdFor(regionId, bankId, mapId, localId),
            regionId,
            bankId,
            mapId,
        ))
  }

  /** Repositions an existing NPC. */
  fun repositionNpc(
      ctx: SessionContext,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      localId: Int,
      x: Int,
      y: Int,
  ) {
    val facing =
        if (mapManager.getMap(regionId, bankId, mapId) == null) {
          val npc = findNdsNpc(regionId, bankId, mapId, localId) ?: return
          // A DS map's on-load level script places its actors before the cartridge draws anything
          // (Bianca's house: dad to 8,4 and Bianca to 6,4 for the argument). Ours runs after the
          // spawn, and a held update landed after the fade-in - she sat at the counter, then
          // popped across the room (2026-09-19). The client takes DS spawns while it loads, so
          // during the arrival window the npc is simply spawned again at its new tile.
          val state = ctx.attributes[PLAYER_STATE]
          // The on-load script is running ahead of the spawn (MapScriptService.onNdsEnter): the
          // pose is recorded by the caller, and the spawn that follows uses it. Nothing to send.
          if (state?.ndsPlacementOnly == true) return
          val held = maxOf(state?.moveIgnoreUntil ?: 0L, state?.sceneHoldUntil ?: 0L) > System.currentTimeMillis()
          if (held) {
            // Through the SAME ordered queue as a scripted spawn. Sent directly, this overtook the
            // AddNPC spawn still waiting in that queue: leaving Juniper's lab, Bianca appeared at
            // the door, began her walk - and the late spawn snapped her to her ROM tile by the lab
            // (the owner's "poof to the default hidden npc", 2026-09-20).
            sendAfterArrival(ctx, ndsSpawnPacket(regionId, bankId, mapId, npc.copy(x = x, y = y)))
            return
          }
          ndsSpawnPacket(regionId, bankId, mapId, npc).facing
        } else {
          (findNpc(regionId, bankId, mapId, localId) ?: return).facing.ordinal
        }
    sendAfterArrival(
        ctx,
        NpcUpdatePacket(
            entityId = entityIdFor(regionId, bankId, mapId, localId),
            regionId = regionId,
            bankId = bankId,
            mapId = mapId,
            x = x,
            y = y,
            // Captures use 0xF6 followed by the direction for this update packet.
            facing = 0xF6,
            unk = facing,
        ))
  }

  /** Removes a cutscene NPC. */
  fun despawnNpc(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, localId: Int) {
    val entityId = getNpcEntityId(regionId, bankId, mapId, localId) ?: return
    ctx.send(EntityLeavePacket(entityId))
  }

  private fun findNpc(regionId: Int, bankId: Int, mapId: Int, localId: Int): NpcDef? {
    val npc =
        mapManager.getMap(regionId, bankId, mapId)?.npcs?.firstOrNull { it.entityIdx == localId }
    if (npc == null) log.warn { "npc $localId not found on $regionId:$bankId:$mapId" }
    return npc
  }

  private fun findNdsNpc(regionId: Int, bankId: Int, mapId: Int, localId: Int): NdsNpcs.Npc? {
    val npc = ndsNpcs.of(regionId, bankId, mapId).firstOrNull { it.index == localId }
    if (npc == null) log.warn { "ROM npc $localId not found on DS map $regionId:$bankId:$mapId" }
    return npc
  }

  private fun key(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int) =
      "$regionId:$bankId:$mapId:$entityIdx"

  /**
   * A DS map's npcs straight from the ROM's zone events (NdsNpcs): the ROM sprite id resolves in
   * the client's own table for that region, movement and ranges ride the same wire fields the
   * GBA npcs use, and facing is the Gen 4 order (north, south, west, east) turned into ours.
   * An npc whose ROM hide flag is SET in the story store stays away, exactly as on the GBA (the
   * DS games set their later-story npcs' flags in the new-game init script, NewGameStarts) -
   * until 2026-09-19 every placed npc showed.
   */
  private fun spawnNdsNpcs(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int) {
    val npcs = ndsNpcs.of(regionId, bankId, mapId)
    if (npcs.isEmpty()) return
    val storyFlags = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)?.storyFlags.orEmpty()
    // An actor the map's on-load script placed (it runs before this spawn) appears where the script
    // left it, and so does one a scene moved if the map's npcs are ever spawned again this visit.
    val poses = ctx.attributes[PLAYER_STATE]?.scriptedNpcPoses
    val alive = ctx.attributes[PLAYER_STATE]?.madeNdsNpcs
    for (npc in npcs) {
      if (NdsStoryFlags.isHidden(regionId, npc.flag, storyFlags)) continue
      // A script-made actor exists only for the player whose script made it.
      if (ndsNpcs.isMade(regionId, bankId, mapId, npc.index) &&
          alive?.contains(de.fiereu.openmmo.server.game.session.scriptedNpcKey(regionId, bankId, mapId, npc.index)) != true) continue
      val pose = poses?.get(de.fiereu.openmmo.server.game.session.scriptedNpcKey(regionId, bankId, mapId, npc.index))
      ctx.send(ndsSpawnPacket(regionId, bankId, mapId, if (pose == null) npc else npc.copy(x = pose.x, y = pose.y)))
    }
    log.info { "Spawned ${npcs.size} ROM npcs on DS map $regionId:$bankId:$mapId" }
  }

  /** One ROM npc's spawn packet on a DS map (field notes in spawnNdsNpcs). */
  private fun ndsSpawnPacket(regionId: Int, bankId: Int, mapId: Int, npc: NdsNpcs.Npc): NpcSpawnPacket {
    val movementId = npc.movement and 0xFF
    val unk4 =
        if (movementId in 1..6 || movementId in 25..52) ((npc.xRange and 0xFF) shl 8) or (npc.yRange and 0xFF)
        else 0
    val facing =
        when (npc.facing) {
          0 -> Direction.UP.ordinal
          1 -> Direction.DOWN.ordinal
          2 -> Direction.LEFT.ordinal
          else -> Direction.RIGHT.ordinal
        }
    return NpcSpawnPacket(
        entityId = entityIdFor(regionId, bankId, mapId, npc.index),
        spriteRegionId = regionId,
        graphicsId = npc.sprite,
        unk3 = (movementId shl 8) or 0x02,
        unk4 = unk4,
        regionId = regionId,
        bankId = bankId,
        mapId = mapId,
        x = npc.x,
        y = npc.y,
        facing = facing,
        // The client reads this byte as the terrain layer (low two bits, f/p01: `& 3` into
        // Wi1.RW1) plus a flag bit 8. Layer 2 is the GBA default; DS ground is layer 0,
        // and anything else sinks the sprite into the terrain.
        unk5 = 0,
        unk6 = 8,
    )
  }

  private fun buildSpawnPacket(
      npc: NpcDef,
      entityId: Long,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      look: de.fiereu.openmmo.net.game.packets.NpcLook? = null,
      /** The sprite set graphicsId indexes: the map's region's ROM set, or 10 for the client's own. */
      spriteRegionId: Int = regionId,
      /** Width/height overrides (NpcSpawnPacket.spriteSize); a region-10 sprite needs (-1, -1). */
      spriteSize: Pair<Byte, Byte>? = null,
      /** The packed berry-plot state for a soil spot (BerryPlots.packedState), else unused. */
      berryState: Int? = null,
  ): NpcSpawnPacket {
    val region = requireNotNull(Region.byId(regionId)) { "Unknown region id $regionId" }
    val movementId = npc.movementType.forRegion(region).id
    val unk3 = ((movementId and 0xFF) shl 8) or 0x02
    val unk4 =
        if (npc.graphicsId == BerryPlots.BERRY_TREE_GFX) {
          // A soil spot: the plant's growth stage and droplets for this character (BerryPlots).
          berryState ?: BerryPlots.packedState(BerryPlots.View(BerryPlots.STAGE_EMPTY, 0, null, null, 0.0, 0.0))
        } else if (movementId in 1..6 || (movementId in 25..52)) {
          ((npc.movementRangeX and 0xFF) shl 8) or (npc.movementRangeY and 0xFF)
        } else {
          0
        }
    // The position's elevation byte (client f/Wi1, one below the GBA number: floor 3 -> 2). It
    // was a constant 2, so an npc on a raised tile - the Fan Club chairman and his clerk on their
    // dais, GBA elevation 4 - sat a level under its own floor: drawn clipped by the tiles, no
    // hitbox for the player at the counter (2026-09-08). The floor tile's elevation decides;
    // the template's own value (minus one) is the fallback, 2 when it says "any".
    val floor = mapManager.getMap(regionId, bankId, mapId)?.tileAt(npc.x, npc.y)
    val floorElevation = floor?.let { ((it.collision.toInt() and 0xFF) shr 2) - 1 } ?: -1
    val elevation =
        when {
          floorElevation >= 0 -> floorElevation
          npc.elevation > 0 -> npc.elevation - 1
          else -> 2
        }
    return NpcSpawnPacket(
        entityId = entityId,
        spriteRegionId = spriteRegionId,
        graphicsId = npc.graphicsId,
        unk3 = unk3,
        unk4 = unk4,
        regionId = regionId,
        bankId = bankId,
        mapId = mapId,
        x = npc.x,
        y = npc.y,
        facing = npc.facing.ordinal,
        unk5 = elevation,
        unk6 = 8,
        look = look,
        spriteSize = spriteSize,
    )
  }

  private fun resolveDynamicGraphics(
      ctx: SessionContext,
      npc: NpcDef,
      regionId: Int,
      storyVars: Map<String, Int>,
  ): NpcDef {
    if (npc.graphicsId !in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) return npc

    // The map's ON_TRANSITION script chose the sprite (setvar VAR_OBJ_GFX_ID_N, gfx) - the
    // Viridian old man and friends. The rival heuristic below covers npcs whose var no script
    // has written yet.
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    val slot = npc.graphicsId - DYNAMIC_GFX_VAR_0
    storyVars["$namespace/VAR_OBJ_GFX_ID_$slot"]?.let {
      return npc.copy(graphicsId = it)
    }

    if (!(npc.script.contains("Rival", ignoreCase = true) ||
        npc.hideFlag.contains("RIVAL", ignoreCase = true))) {
      return npc
    }
    val playerGender =
        ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)?.info?.rivalSex
    // The rival is the opposite gender from the player.
    val graphicsId = if (playerGender == FEMALE) RIVAL_BRENDAN_NORMAL else RIVAL_MAY_NORMAL
    return npc.copy(graphicsId = graphicsId)
  }

  private fun applyStoryPlacement(
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
      storyVars: Map<String, Int>,
  ): NpcDef {
    if (bankId != LITTLEROOT_BANK || mapId != LITTLEROOT_MAP || npc.entityIdx != LITTLEROOT_TWIN) {
      return npc
    }
    if (RESCUED_BIRCH_FLAG in storyFlags) return npc
    return if (storyVars[LITTLEROOT_STATE_VAR].orZero() == 0) {
      npc.copy(x = 7, y = 2, facing = Direction.DOWN, movementType = MovementType.FACE_DOWN)
    } else {
      npc.copy(x = 10, y = 1, facing = Direction.UP, movementType = MovementType.FACE_UP)
    }
  }

  private fun Int?.orZero(): Int = this ?: 0

  private fun shouldHideNpc(
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
  ): Boolean {
    if (npc.hideFlag in storyFlags) return true
    // Completed rescues must not restore Zigzagoon.
    return bankId == ROUTE_101_BANK &&
        mapId == ROUTE_101_MAP &&
        npc.entityIdx == ROUTE_101_ZIGZAGOON &&
        (RESCUED_BIRCH_FLAG in storyFlags || ROUTE_101_RESCUE_HIDDEN_FLAG in storyFlags)
  }

  companion object {
    const val DECORATION_FLAG_PREFIX = "FLAG_DECORATION_"
    const val DYNAMIC_GFX_VAR_0 = 240
    const val DYNAMIC_GFX_VAR_3 = 243
    const val RIVAL_BRENDAN_NORMAL = 100
    const val RIVAL_MAY_NORMAL = 105
    const val FEMALE: Byte = 1
    const val LITTLEROOT_BANK = 50
    const val LITTLEROOT_MAP = 9
    const val LITTLEROOT_TWIN = 0
    const val RESCUED_BIRCH_FLAG = "hoenn/FLAG_RESCUED_BIRCH"
    const val LITTLEROOT_STATE_VAR = "hoenn/VAR_LITTLEROOT_TOWN_STATE"
    const val ROUTE_101_BANK = 50
    const val ROUTE_101_MAP = 16
    const val ROUTE_101_ZIGZAGOON = 3
    const val ROUTE_101_RESCUE_HIDDEN_FLAG = "hoenn/FLAG_HIDE_ROUTE_101_BIRCH_ZIGZAGOON_BATTLE"
  }
}
