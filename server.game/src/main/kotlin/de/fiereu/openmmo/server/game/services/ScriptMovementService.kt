package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.DynamicWarp
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.DialogDataPacket
import de.fiereu.openmmo.net.game.packets.NpcUpdatePacket
import de.fiereu.openmmo.server.game.script.MovementStep
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.ScriptedNpcPose
import de.fiereu.openmmo.server.game.session.scriptedNpcKey
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

private val log = KotlinLogging.logger {}

/**
 * Drives scripted overworld movement (the decomp applymovement/waitmovement). Steps play one tile
 * at a time with a short delay between them, so the call only returns once the whole path is done,
 * which is exactly waitmovement.
 *
 * Story cutscenes are per player, so the movement is sent to the acting player only and is not
 * broadcast to other players who happen to share the map. The whole action sequence goes in one
 * movement packet, matching what the real client expects (from packet captures).
 */
@Singleton
class ScriptMovementService
@Inject
constructor(
    private val mapManager: MapManager,
    private val npcService: NpcService,
    private val characterStore: CharacterStore,
    private val ndsNpcs: NdsNpcs = NdsNpcs(),
    /** Lazily: MovementService depends on this service; the edge crossing lives there. */
    private val movementService: javax.inject.Provider<MovementService>? = null,
    private val presence: PresenceService? = null,
) {

  /** A live change to the player's entity: to the player and to everyone watching them. */
  fun announce(ctx: de.fiereu.network.SessionContext, packet: Any) {
    presence?.announce(ctx, packet) ?: ctx.send(packet)
  }
  /** DS maps have no MapDef; the ROM npc table gives an npc's resting pose. */
  private fun ndsNpcPose(regionId: Int, bankId: Int, mapId: Int, localId: Int): Pose? {
    if (regionId !in 2..4) return null
    val npc = ndsNpcs.of(regionId, bankId, mapId).firstOrNull { it.index == localId } ?: return null
    val facing =
        when (npc.facing) {
          0 -> Direction.UP
          1 -> Direction.DOWN
          2 -> Direction.LEFT
          else -> Direction.RIGHT
        }
    return Pose(npc.x, npc.y, facing)
  }

  data class Pose(val x: Int, val y: Int, val facing: Direction)

  /** The hide flag of a map npc on the player's current map (already namespaced), if set. */
  fun npcHideFlag(state: PlayerState, localId: Int): String? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    val regionId = info.positionRegionId.toInt()
    val bankId = info.positionBankId.toInt() and 0xFF
    val mapId = info.positionMapId.toInt() and 0xFF
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
            ?: return ndsNpcs.of(regionId, bankId, mapId).firstOrNull { it.index == localId }
                ?.flag?.takeIf { it != 0 }?.let { NdsStoryFlags.key(regionId, it) }
    return map.npcs.firstOrNull { it.entityIdx == localId }?.hideFlag?.takeIf { it.isNotBlank() }
  }

  /**
   * The GBA object system respawns a map npc once its hide flag clears; clearflag in a script
   * mirrors that here (Oak reappearing behind his desk mid-scene).
   */
  fun respawnNpcByHideFlag(session: SessionContext, state: PlayerState, flag: String) {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val npc = map.npcs.firstOrNull { it.hideFlag == flag } ?: return
    // An npc a scene already put on the map (addobject, then walked) is where the scene left it;
    // the GBA clearflag spawns nothing by itself. Re-spawning it here sent May back to her map
    // default - the stairs tile - after she had walked to her PC, and she blocked the only exit
    // of her bedroom (2026-09-12).
    if (scriptedNpcPose(state, npc.entityIdx) != null) return
    npcService.spawnNpc(
        session,
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        npc.entityIdx,
    )
  }

  /** The story-var key holding an npc's setobjectxyperm override on the player's current map. */
  fun npcMovementOverrideKey(state: PlayerState, localId: Int): String? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    return npcService.movementOverrideKey(
        info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId)
  }

  fun npcXyOverrideKey(state: PlayerState, localId: Int): String? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    return npcService.xyOverrideKey(
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
    )
  }

  /** The player's current tile, for getplayerxy. */
  fun playerXy(state: PlayerState): Pair<Int, Int>? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    return info.positionX.toInt() to info.positionY.toInt()
  }

  /** Walk a map npc (its decomp local id, that is its entityIdx) through [steps] for the player. */
  suspend fun moveNpc(
      session: SessionContext,
      state: PlayerState,
      localId: Int,
      steps: List<MovementStep>,
      /** The npc's own map (bank to map) when the script names one - it may differ from the player's. */
      mapOverride: Pair<Int, Int>? = null,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val regionId = info.positionRegionId.toInt()
    val bankId = mapOverride?.first ?: (info.positionBankId.toInt() and 0xFF)
    val mapId = mapOverride?.second ?: info.positionMapId.toInt()
    val map = mapManager.getMap(info.positionRegionId, bankId.toByte(), mapId.toByte())
    // Where the npc really stands: a scripted walk earlier this visit, else its spawned tile
    // (story placement and setobjectxyperm included), else the DS event table.
    val stored = characterStore.getCharacter(charId)
    val npc =
        state.scriptedNpcPoses[scriptedNpcKey(regionId, bankId, mapId, localId)]?.let { Pose(it.x, it.y, it.facing) }
            ?: map?.npcs?.firstOrNull { it.entityIdx == localId }?.let {
              val spawned =
                  npcService.effectiveNpc(
                      regionId,
                      bankId,
                      mapId,
                      it,
                      stored?.storyFlags.orEmpty(),
                      stored?.storyVars.orEmpty())
              Pose(spawned.x, spawned.y, spawned.facing)
            }
            ?: ndsNpcPose(regionId, bankId, mapId, localId)
            ?: return
    val entityId = npcService.entityIdFor(regionId, bankId, mapId, localId)
    // No invented turn here: the source games only turn the player when the script says so
    // (applymovement LOCALID_PLAYER behind a VAR_FACING branch), and those branches run now. The
    // 2026-09-08 "glance" that aimed the player at a walking npc START tile turned them the
    // wrong way whenever the npc ended somewhere else (Oak walking up after the League).
    val end = drive(session, entityId, npc, steps)
    state.scriptedNpcPoses[scriptedNpcKey(regionId, bankId, mapId, localId)] = ScriptedNpcPose(end.x, end.y, end.facing)
  }

  /**
   * Where a DS map npc stands now (HeartGold GetPersonCoords): a scripted walk earlier this visit,
   * else its place in the ROM event table; null when the map has no such npc.
   */
  fun ndsNpcXy(state: PlayerState, localId: Int): Pair<Int, Int>? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    val pose =
        scriptedNpcPose(state, localId)
            ?: ndsNpcPose(info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId)
            ?: return null
    return pose.x to pose.y
  }

  /** The tile a script walked this npc to earlier in the visit, if any. */
  fun scriptedNpcPose(state: PlayerState, localId: Int): Pose? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    val pose =
        state.scriptedNpcPoses[
            scriptedNpcKey(info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId)]
            ?: return null
    return Pose(pose.x, pose.y, pose.facing)
  }

  /** Starts concurrent NPC movement paths. */
  suspend fun moveNpcs(
      session: SessionContext,
      state: PlayerState,
      paths: List<Pair<Int, List<MovementStep>>>,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    val resolved =
        paths.mapNotNull { (localId, steps) ->
          val known =
              map?.npcs?.any { it.entityIdx == localId }
                  ?: (ndsNpcPose(info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId) != null)
          if (!known) return@mapNotNull null
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              (info.positionBankId.toInt() and 0xFF),
              info.positionMapId.toInt(),
              localId,
          ) to steps
        }
    resolved.forEach { (entityId, steps) -> sendActions(session, entityId, steps) }
    delay(resolved.maxOfOrNull { (_, steps) -> durationMs(steps) } ?: 0)
  }

  /** Starts player and NPC movement together. */
  suspend fun moveSelfAndNpcs(
      session: SessionContext,
      state: PlayerState,
      selfSteps: List<MovementStep>,
      paths: List<Pair<Int, List<MovementStep>>>,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    val resolved =
        paths.mapNotNull { (localId, steps) ->
          val known =
              map?.npcs?.any { it.entityIdx == localId }
                  ?: (ndsNpcPose(info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId) != null)
          if (!known) return@mapNotNull null
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              (info.positionBankId.toInt() and 0xFF),
              info.positionMapId.toInt(),
              localId,
          ) to steps
        }
    // A still-animating previous walk finishes first - the queue appends, so sending now is
    // safe, but the server must not count this sequence as started before the client can.
    awaitSelfActions(state)
    state.selfActionsEndAt =
        maxOf(System.currentTimeMillis(), state.selfActionsEndAt) +
            durationMs(selfSteps) +
            CLIENT_LAG_PAD_MS
    sendActions(session, info.id, selfSteps)
    resolved.forEach { (entityId, steps) -> sendActions(session, entityId, steps) }
    delay(
        maxOf(
            durationMs(selfSteps),
            resolved.maxOfOrNull { (_, steps) -> durationMs(steps) } ?: 0,
        ))

    val start = Pose(info.positionX.toInt(), info.positionY.toInt(), state.facingDirection)
    val end = applySteps(start, selfSteps)
    if (map != null) {
      check(commitScriptedWalk(session, charId, state, map, start, selfSteps)) { "Scripted player movement ended off the map" }
    } else {
      // DS maps: no tile table to validate against, the ROM script is trusted.
      state.x = end.x.toShort()
      state.y = end.y.toShort()
      state.facingDirection = end.facing
      characterStore.updatePosition(charId, end.x.toShort(), end.y.toShort(), facing = end.facing)
    }
  }

  /** Show a normally hidden map npc to the player for a cutscene (the decomp addobject). */
  fun showNpc(session: SessionContext, state: PlayerState, localId: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    npcService.spawnNpc(
        session,
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
    )
  }

  /** Show a normally hidden map npc at an overridden cutscene position. */
  fun showNpcAt(session: SessionContext, state: PlayerState, localId: Int, x: Int, y: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    npcService.spawnNpcAt(
        session,
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
        x,
        y,
    )
  }

  /** Relocate an npc that the normal map spawn already created. */
  fun repositionNpc(session: SessionContext, state: PlayerState, localId: Int, x: Int, y: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    // A DS actor that was PLACED stands there for everything after it: moveNpc starts a walk from
    // the scripted pose, else the ROM tile - and a placement was not a pose. Nuvema's on-load
    // script puts Bianca by the player's house; her walk home then started from her ROM tile at
    // the lab, and she missed her mark in Mom's Town Map scene too (2026-09-20). DS maps only:
    // on a GBA map a recorded pose also changes how clearflag respawns behave.
    if (mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) == null) {
      val regionId = info.positionRegionId.toInt()
      val bankId = info.positionBankId.toInt() and 0xFF
      val mapId = info.positionMapId.toInt() and 0xFF
      val facing =
          state.scriptedNpcPoses[scriptedNpcKey(regionId, bankId, mapId, localId)]?.facing
              ?: ndsNpcPose(regionId, bankId, mapId, localId)?.facing
              ?: Direction.DOWN
      state.scriptedNpcPoses[scriptedNpcKey(regionId, bankId, mapId, localId)] = ScriptedNpcPose(x, y, facing)
    }
    npcService.repositionNpc(
        session,
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
        x,
        y,
    )
  }

  /** Repositions the player and synchronizes server state. */
  fun repositionSelf(
      session: SessionContext,
      state: PlayerState,
      x: Int,
      y: Int,
      facing: Direction,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    if (map == null) {
      // DS map: no tile table to check against; the ROM script's own tile is trusted.
      characterStore.updatePosition(charId, x.toShort(), y.toShort(), facing = facing)
      state.x = x.toShort()
      state.y = y.toShort()
      state.facingDirection = facing
      state.scriptedFacingSet = true
    } else if (!commitPose(charId, state, map, Pose(x, y, facing))) return
    session.send(
        NpcUpdatePacket(
            entityId = info.id,
            regionId = info.positionRegionId.toInt(),
            bankId = (info.positionBankId.toInt() and 0xFF),
            mapId = info.positionMapId.toInt(),
            x = x,
            y = y,
            facing = 0xF6,
            unk = facing.ordinal,
        ))
  }

  /** Removes a cutscene NPC and its collision. */
  /** Despawns every npc of the current map whose hide flag is set by now (the field reload after a battle). */
  fun despawnHiddenNpcs(session: SessionContext, state: PlayerState) =
      npcService.refreshDynamicNpcs(session, state.regionId, state.bankId, state.mapId)

  fun removeNpc(session: SessionContext, state: PlayerState, localId: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    state.scriptedNpcPoses.remove(
        scriptedNpcKey(info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt(), localId))
    npcService.despawnNpc(
        session,
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
    )
  }

  /** Resolves local NPC ids to entity ids. */
  fun npcEntityId(state: PlayerState, localId: Int): Long? {
    val charId = state.characterId ?: return null
    val info = characterStore.getCharacter(charId)?.info ?: return null
    return npcService.entityIdFor(
        info.positionRegionId.toInt(),
        (info.positionBankId.toInt() and 0xFF),
        info.positionMapId.toInt(),
        localId,
    )
  }

  /** Fails before an interpreted movement starts if the map-local npc does not exist. */
  fun requireNpc(state: PlayerState, localId: Int) {
    val charId = checkNotNull(state.characterId) { "Scene has no selected character" }
    val info =
        checkNotNull(characterStore.getCharacter(charId)?.info) { "Character $charId is missing" }
    val regionId = info.positionRegionId.toInt()
    val bankId = info.positionBankId.toInt() and 0xFF
    val mapId = info.positionMapId.toInt() and 0xFF
    // A DS map has no MapDef; its npcs come from the ROM's zone events (NdsNpcs), which the
    // movement paths below already know how to pose. Demanding the MapDef here killed every DS
    // cutscene that moves an npc ("No map 4:60:0", New Bark, 2026-09-19).
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    if (map == null) {
      check(ndsNpcPose(regionId, bankId, mapId, localId) != null) {
        "No npc with local id $localId on DS map $regionId:$bankId:$mapId"
      }
      return
    }
    check(map.npcs.any { it.entityIdx == localId }) {
      "No npc with local id $localId on map ${map.regionId}:${map.bankId}:${map.mapId}"
    }
  }

  /** Resolves the selected runtime entity back to this map's normalized local npc id. */
  fun localIdForEntity(state: PlayerState, entityId: Long): Int? {
    val charId = state.characterId ?: return null
    val info = characterStore.getCharacter(charId)?.info ?: return null
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
            ?: return null
    return map.npcs
        .firstOrNull { npc ->
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              (info.positionBankId.toInt() and 0xFF),
              info.positionMapId.toInt(),
              npc.entityIdx,
          ) == entityId
        }
        ?.entityIdx
  }

  /** Turns the currently selected entity toward the player without creating a movement wait. */
  fun facePlayer(session: SessionContext, entityId: Long, playerFacing: Direction) {
    if (entityId < 0) return
    // The tile delta is the ground truth for a normal adjacent talk; the player's tracked facing
    // only decides when the npc has been cutscene-moved off its template tile.
    val step =
        adjacentFaceStep(session, entityId)
            ?: when (playerFacing.opposite()) {
              Direction.DOWN -> MovementStep.FACE_DOWN
              Direction.UP -> MovementStep.FACE_UP
              Direction.LEFT -> MovementStep.FACE_LEFT
              Direction.RIGHT -> MovementStep.FACE_RIGHT
              Direction.DIVE,
              Direction.EMERGE -> return
            }
    sendActions(session, entityId, listOf(step))
  }

  private fun adjacentFaceStep(session: SessionContext, entityId: Long): MovementStep? {
    val state =
        session.attributes[de.fiereu.openmmo.server.game.session.PLAYER_STATE] ?: return null
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
            ?: return ndsAdjacentFaceStep(state, info, entityId)
    val template =
        map.npcs.firstOrNull {
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              (info.positionBankId.toInt() and 0xFF),
              info.positionMapId.toInt(),
              it.entityIdx,
          ) == entityId
        } ?: return null
    // The spawned position, not the template's - story placement and setobjectxyperm move npcs,
    // and the template tile then fails the adjacency test (wrong-way facing on moved npcs).
    val stored = state.characterId?.let(characterStore::getCharacter)
    val npc =
        npcService.effectiveNpc(
            info.positionRegionId.toInt(),
            (info.positionBankId.toInt() and 0xFF),
            info.positionMapId.toInt(),
            template,
            stored?.storyFlags.orEmpty(),
            stored?.storyVars.orEmpty(),
        )
    val walked = scriptedNpcPose(state, template.entityIdx)
    // A wanderer's tile is the client's to know: measuring against its template tile turned it
    // the wrong way whenever it had strolled off. The player's facing (kept current by the turn
    // packet) is the truth for those; the delta only serves npcs that stand where the server put them.
    if (walked == null && npc.movementType.wanders) return null
    val dx = info.positionX.toInt() - (walked?.x ?: npc.x)
    val dy = info.positionY.toInt() - (walked?.y ?: npc.y)
    if (Math.abs(dx) + Math.abs(dy) != 1) return null
    return when {
      dy > 0 -> MovementStep.FACE_DOWN
      dy < 0 -> MovementStep.FACE_UP
      dx < 0 -> MovementStep.FACE_LEFT
      else -> MovementStep.FACE_RIGHT
    }
  }

  /**
   * The DS half of [adjacentFaceStep]: a ROM npc that stands still (movement 0) or that a script
   * walked is where the server knows it to be, so the tile delta is the truth - and it corrects
   * the server's record of the PLAYER's facing too. That record goes stale when the client turns
   * the player itself (a tap-to-talk sends no turn packet); re-asserted before the dialog, the
   * stale facing turned the owner away from the npc he had just faced (2026-09-20).
   */
  private fun ndsAdjacentFaceStep(state: PlayerState, info: de.fiereu.openmmo.common.CharacterInfo, entityId: Long): MovementStep? {
    val regionId = info.positionRegionId.toInt()
    val bankId = info.positionBankId.toInt() and 0xFF
    val mapId = info.positionMapId.toInt() and 0xFF
    val npc = ndsNpcs.of(regionId, bankId, mapId).firstOrNull { npcService.entityIdFor(regionId, bankId, mapId, it.index) == entityId } ?: return null
    val walked = scriptedNpcPose(state, npc.index)
    if (walked == null && (npc.movement and 0xFF) != 0) return null
    val dx = info.positionX.toInt() - (walked?.x ?: npc.x)
    val dy = info.positionY.toInt() - (walked?.y ?: npc.y)
    if (Math.abs(dx) + Math.abs(dy) != 1) return null
    state.facingDirection =
        when {
          dy > 0 -> Direction.UP
          dy < 0 -> Direction.DOWN
          dx < 0 -> Direction.RIGHT
          else -> Direction.LEFT
        }
    return when {
      dy > 0 -> MovementStep.FACE_DOWN
      dy < 0 -> MovementStep.FACE_UP
      dx < 0 -> MovementStep.FACE_LEFT
      else -> MovementStep.FACE_RIGHT
    }
  }

  /** Returns the created player's gender. */
  fun playerGender(state: PlayerState): Byte? =
      state.characterId?.let(characterStore::getCharacter)?.info?.rivalSex

  /**
   * Set the destination a MAP_DYNAMIC warp resolves to for this player (the decomp setdynamicwarp).
   */
  fun setDynamicWarp(state: PlayerState, warp: DynamicWarp) {
    val charId = state.characterId ?: return
    characterStore.setDynamicWarp(charId, warp)
  }

  /** Walk the player's own avatar through [steps] and commit the final tile as authoritative. */
  suspend fun moveSelf(
      session: SessionContext,
      state: PlayerState,
      steps: List<MovementStep>,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    // A still-animating previous walk finishes first; these steps append behind it.
    awaitSelfActions(state)
    state.selfActionsEndAt =
        maxOf(System.currentTimeMillis(), state.selfActionsEndAt) +
            durationMs(steps) +
            CLIENT_LAG_PAD_MS
    val start = Pose(info.positionX.toInt(), info.positionY.toInt(), state.facingDirection)
    val end = drive(session, info.id, start, steps)
    if (map == null) {
      // DS map: no tile table to walk against (moveNpc trusts the ROM script the same way).
      // Returning before the drive, as this did, left the player frozen through every DS scene
      // (Nuvema: never turning to Bianca, 2026-09-19).
      characterStore.updatePosition(charId, end.x.toShort(), end.y.toShort(), facing = end.facing)
      state.x = end.x.toShort()
      state.y = end.y.toShort()
      state.facingDirection = end.facing
      state.scriptedFacingSet = true
      return
    }
    // A player walked off the map with no neighbour there means the scene ran from a position it
    // never expected (a login in the middle of a cutscene map). Failing the script rolls its
    // writes back for a clean retry from the proper entry.
    check(commitScriptedWalk(session, charId, state, map, start, steps)) {
      "Scripted player movement ended off the map at (${end.x}, ${end.y})"
    }
  }

  /** Sends one packet per step from [start] and waits between them. Returns the final pose. */
  suspend fun drive(
      session: SessionContext,
      entityId: Long,
      start: Pose,
      steps: List<MovementStep>,
  ): Pose {
    if (steps.isEmpty()) return start
    sendActions(session, entityId, steps)
    // waitmovement: hold until the client has had time to play the sequence.
    delay(durationMs(steps))
    return applySteps(start, steps)
  }

  /**
   * Commits where a cutscene left the player, unless that is off the map, which would persist a
   * position every later step reads as a desync and snaps back from.
   */
  /**
   * The server-side track of a scripted player walk the client is already playing: step by step,
   * and where a step leaves the map, the same edge crossing a free step gets (the Wally tutorial
   * walks Petalburg's east edge into Route 102, where the cartridge stages the catch). The walk
   * then continues on the neighbour from its landing tile.
   */
  private fun commitScriptedWalk(
      session: SessionContext,
      charId: Long,
      state: PlayerState,
      map: MapDef,
      start: Pose,
      steps: List<MovementStep>,
  ): Boolean {
    var current = map
    var pose = start
    for (step in steps) {
      val next = applySteps(pose, listOf(step))
      if (next.x in 0 until current.width && next.y in 0 until current.height) {
        pose = next
        continue
      }
      val crossed =
          movementService?.get()?.crossEdge(session, charId, state, current, pose.x, pose.y, step.direction) ?: false
      if (!crossed) {
        log.warn { "Scripted walk of character $charId left ${current.bankId}:${current.mapId} at (${next.x}, ${next.y}) with no neighbour there" }
        return false
      }
      val info = characterStore.getCharacter(charId)?.info ?: return false
      current = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return false
      pose = Pose(info.positionX.toInt(), info.positionY.toInt(), step.direction)
    }
    return commitPose(charId, state, current, pose)
  }

  private fun commitPose(charId: Long, state: PlayerState, map: MapDef, pose: Pose): Boolean {
    if (pose.x !in 0 until map.width || pose.y !in 0 until map.height) {
      log.warn {
        "Refused a scripted move of character $charId to (${pose.x}, ${pose.y}) on " +
            "${map.bankId}:${map.mapId}, which is outside the map"
      }
      return false
    }
    characterStore.updatePosition(charId, pose.x.toShort(), pose.y.toShort(), facing = pose.facing)
    state.x = pose.x.toShort()
    state.y = pose.y.toShort()
    state.facingDirection = pose.facing
    return true
  }

  private fun applySteps(start: Pose, steps: List<MovementStep>): Pose {
    var pose = start
    for (step in steps) {
      pose =
          if (step.walks)
              Pose(pose.x + step.direction.dx * step.tiles, pose.y + step.direction.dy * step.tiles, step.direction)
          else if (step.changesFacing) pose.copy(facing = step.direction) else pose
    }
    return pose
  }

  /** The client's set_visible on the player's own sprite - a door entry it started hides it. */
  fun showSelf(session: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    state.spriteHidden = false
    sendActions(session, charId, listOf(MovementStep.SET_VISIBLE))
  }

  /** The client's set_invisible on the player's own sprite (hideplayer). */
  fun hideSelf(session: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    state.spriteHidden = true
    sendActions(session, charId, listOf(MovementStep.SET_INVISIBLE))
  }

  /** A single facing re-assert with no hold - the scripted-state flag does the actual locking. */
  fun reassertScriptedFacing(session: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    // A DS map: the record may be stale (see PlayerState.scriptedFacingSet) - only a facing the
    // script itself set is pushed back at the client.
    if (state.regionId in 2..4 && !state.scriptedFacingSet) return
    val face = faceStepOf(state.facingDirection) ?: return
    sendActions(session, charId, listOf(face))
  }

  /**
   * THE lock, per the operator's directive: the client's own scripted-state flag (0x0E true -> f/j1
   * -> ln1.t5 -> A70), the same state a dialog box sets - its busy-check FS1 gates the overworld
   * input surface (COm5) and the movement controller (NV0) themselves, bytecode-verified. Input is
   * REMOVED at the source from script start until [releasePlayerHold] declares the player free; no
   * action-queue holds, nothing to clear. A face action rides along so the sprite shows the held
   * facing.
   */
  fun holdPlayer(session: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    log.info { "HOLD scripted-state ON facing=${state.facingDirection}" }
    session.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = true))
    state.scriptedFacingSet = false
    // Outside the arrival window only - the emergence walk owns the queue inside it. Not on a DS
    // map: the client knows its own facing better than the server does there.
    if (System.currentTimeMillis() >= state.moveIgnoreUntil && state.regionId !in 2..4) {
      faceStepOf(state.facingDirection)?.let { face -> sendActions(session, charId, listOf(face)) }
    }
  }

  /** The ROM freed the player: scripted state OFF. Nothing else to undo. */
  fun releasePlayerHold(session: SessionContext, state: PlayerState) {
    log.info { "RELEASE scripted-state OFF at (${state.x}, ${state.y})" }
    session.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
  }

  /**
   * Waits until the client should be done animating the last scripted player movement AND until the
   * warp-arrival choreography window has passed - the emergence walk shares the client's action
   * queue and its timing, so scripted steps and the final release both wait it out.
   */
  suspend fun awaitSelfActions(state: PlayerState) {
    val remaining =
        maxOf(state.selfActionsEndAt, state.moveIgnoreUntil) - System.currentTimeMillis()
    if (remaining > 0) delay(remaining.coerceAtMost(8000))
  }

  private fun faceStepOf(direction: Direction): MovementStep? =
      when (direction) {
        Direction.UP -> MovementStep.FACE_UP
        Direction.DOWN -> MovementStep.FACE_DOWN
        Direction.LEFT -> MovementStep.FACE_LEFT
        Direction.RIGHT -> MovementStep.FACE_RIGHT
        else -> null
      }

  /** One facing change with no wait - the trainer-facing driver's whole vocabulary. */
  fun turnNpc(session: SessionContext, entityId: Long, direction: Direction) {
    val step =
        when (direction) {
          Direction.UP -> MovementStep.FACE_UP
          Direction.DOWN -> MovementStep.FACE_DOWN
          Direction.LEFT -> MovementStep.FACE_LEFT
          Direction.RIGHT -> MovementStep.FACE_RIGHT
          else -> return
        }
    sendActions(session, entityId, listOf(step))
  }

  private fun sendActions(
      session: SessionContext,
      entityId: Long,
      steps: List<MovementStep>,
  ) {
    if (steps.isEmpty()) return
    // Send each movement sequence in one packet.
    val actions = ByteArray(steps.size) { steps[it].action.toByte() }
    session.send(DialogDataPacket(entityId, unk1 = 0, type = steps.size, data = actions))
  }

  private fun durationMs(steps: List<MovementStep>): Long =
      steps.sumOf {
        it.holdMs ?: if (it.fast) FAST_STEP_MS else if (it.walks) WALK_STEP_MS else FACE_STEP_MS
      }

  private companion object {
    // GBA-frame-accurate client step timings (walk_normal 16 frames, fast/face 8 frames at
    // ~60fps). The old rounded-down values under-counted by ~7% per step, which accumulated
    // across long scenes until the script-end queue clear cut the final steps of a walk.
    const val WALK_STEP_MS = 275L
    const val FAST_STEP_MS = 135L
    const val FACE_STEP_MS = 135L

    /** Extra margin the client gets to finish animating past the server's step-time estimate. */
    const val CLIENT_LAG_PAD_MS = 500L
  }
}
