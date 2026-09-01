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
) {
  data class Pose(val x: Int, val y: Int, val facing: Direction)

  /** The hide flag of a map npc on the player's current map (already namespaced), if set. */
  fun npcHideFlag(state: PlayerState, localId: Int): String? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
            ?: return null
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
    npcService.spawnNpc(
        session,
        info.positionRegionId.toInt(),
        info.positionBankId.toInt(),
        info.positionMapId.toInt(),
        npc.entityIdx,
    )
  }

  /** The story-var key holding an npc's setobjectxyperm override on the player's current map. */
  fun npcXyOverrideKey(state: PlayerState, localId: Int): String? {
    val info = state.characterId?.let(characterStore::getCharacter)?.info ?: return null
    return npcService.xyOverrideKey(
        info.positionRegionId.toInt(),
        info.positionBankId.toInt(),
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
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val npc = map.npcs.firstOrNull { it.entityIdx == localId } ?: return
    val entityId =
        npcService.entityIdFor(
            info.positionRegionId.toInt(),
            info.positionBankId.toInt(),
            info.positionMapId.toInt(),
            localId,
        )
    drive(session, entityId, Pose(npc.x, npc.y, npc.facing), steps)
  }

  /** Starts concurrent NPC movement paths. */
  suspend fun moveNpcs(
      session: SessionContext,
      state: PlayerState,
      paths: List<Pair<Int, List<MovementStep>>>,
  ) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val resolved =
        paths.mapNotNull { (localId, steps) ->
          if (map.npcs.none { it.entityIdx == localId }) return@mapNotNull null
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              info.positionBankId.toInt(),
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
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val resolved =
        paths.mapNotNull { (localId, steps) ->
          if (map.npcs.none { it.entityIdx == localId }) return@mapNotNull null
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              info.positionBankId.toInt(),
              info.positionMapId.toInt(),
              localId,
          ) to steps
        }
    sendActions(session, info.id, selfSteps)
    resolved.forEach { (entityId, steps) -> sendActions(session, entityId, steps) }
    delay(
        maxOf(
            durationMs(selfSteps),
            resolved.maxOfOrNull { (_, steps) -> durationMs(steps) } ?: 0,
        ))

    val start = Pose(info.positionX.toInt(), info.positionY.toInt(), state.facingDirection)
    check(commitPose(charId, state, map, applySteps(start, selfSteps))) {
      "Scripted player movement ended off the map"
    }
  }

  /** Show a normally hidden map npc to the player for a cutscene (the decomp addobject). */
  fun showNpc(session: SessionContext, state: PlayerState, localId: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    npcService.spawnNpc(
        session,
        info.positionRegionId.toInt(),
        info.positionBankId.toInt(),
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
        info.positionBankId.toInt(),
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
    npcService.repositionNpc(
        session,
        info.positionRegionId.toInt(),
        info.positionBankId.toInt(),
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
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    if (!commitPose(charId, state, map, Pose(x, y, facing))) return
    session.send(
        NpcUpdatePacket(
            entityId = info.id,
            regionId = info.positionRegionId.toInt(),
            bankId = info.positionBankId.toInt(),
            mapId = info.positionMapId.toInt(),
            x = x,
            y = y,
            facing = 0xF6,
            unk = facing.ordinal,
        ))
  }

  /** Removes a cutscene NPC and its collision. */
  fun removeNpc(session: SessionContext, state: PlayerState, localId: Int) {
    val charId = state.characterId ?: return
    val info = characterStore.getCharacter(charId)?.info ?: return
    npcService.despawnNpc(
        session,
        info.positionRegionId.toInt(),
        info.positionBankId.toInt(),
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
        info.positionBankId.toInt(),
        info.positionMapId.toInt(),
        localId,
    )
  }

  /** Fails before an interpreted movement starts if the map-local npc does not exist. */
  fun requireNpc(state: PlayerState, localId: Int) {
    val charId = checkNotNull(state.characterId) { "Scene has no selected character" }
    val info =
        checkNotNull(characterStore.getCharacter(charId)?.info) { "Character $charId is missing" }
    val map =
        checkNotNull(
            mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)) {
              "No map ${info.positionRegionId}:${info.positionBankId}:${info.positionMapId}"
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
              info.positionBankId.toInt(),
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
            ?: return null
    val template =
        map.npcs.firstOrNull {
          npcService.entityIdFor(
              info.positionRegionId.toInt(),
              info.positionBankId.toInt(),
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
            info.positionBankId.toInt(),
            info.positionMapId.toInt(),
            template,
            stored?.storyFlags.orEmpty(),
            stored?.storyVars.orEmpty(),
        )
    val dx = info.positionX.toInt() - npc.x
    val dy = info.positionY.toInt() - npc.y
    if (Math.abs(dx) + Math.abs(dy) != 1) return null
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
    val map =
        mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val start = Pose(info.positionX.toInt(), info.positionY.toInt(), state.facingDirection)
    val end = drive(session, info.id, start, steps)
    // A player walked off the map means the scene ran from a position it never expected (a login
    // in the middle of a cutscene map). Failing the script rolls its writes back for a clean
    // retry from the proper entry.
    check(commitPose(charId, state, map, end)) {
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
              Pose(pose.x + step.direction.dx, pose.y + step.direction.dy, step.direction)
          else if (step.changesFacing) pose.copy(facing = step.direction) else pose
    }
    return pose
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
      steps.sumOf { if (it.fast) FAST_STEP_MS else if (it.walks) WALK_STEP_MS else FACE_STEP_MS }

  private companion object {
    // Rough client step timings, tune if the animation and server drift apart.
    const val WALK_STEP_MS = 250L
    const val FAST_STEP_MS = 130L
    const val FACE_STEP_MS = 120L
  }
}
