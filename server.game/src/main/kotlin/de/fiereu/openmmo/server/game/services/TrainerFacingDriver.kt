package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Which directions a sighted trainer's facing cycles through when the SERVER drives it, or null
 * when the trainer is not server-driven (fixed facing, plain npc, or a physical wanderer the server
 * cannot move yet). Shared by the spawner (to switch off the client-side animation for driven
 * trainers) and the driver/sight engine (to turn them and aim their gaze).
 */
internal fun drivenFacingCycle(npc: NpcDef): List<Direction>? {
  if (npc.sightRange <= 0 || npc.trainerType !in 1..2) return null
  val name = npc.movementType.name
  return when {
    name == "LOOK_AROUND" -> listOf(Direction.DOWN, Direction.UP, Direction.LEFT, Direction.RIGHT)
    name == "ROTATE_CLOCKWISE" ->
        listOf(Direction.UP, Direction.RIGHT, Direction.DOWN, Direction.LEFT)
    name == "ROTATE_COUNTERCLOCKWISE" ->
        listOf(Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT)
    name.startsWith("FACE_") && name.contains("_AND_") ->
        name
            .split('_')
            .mapNotNull {
              when (it) {
                "UP" -> Direction.UP
                "DOWN" -> Direction.DOWN
                "LEFT" -> Direction.LEFT
                "RIGHT" -> Direction.RIGHT
                else -> null
              }
            }
            .distinct()
            .takeIf { it.size > 1 }
    else -> null
  }
}

/**
 * Turns this session's spinner/look-around trainers on a vanilla-ish timer. They spawn with the
 * client-side animation OFF (movement type NONE on the wire), so what the player sees is exactly
 * the facing the server last sent - which is what makes their line of sight fair: the recorded
 * facing in [de.fiereu.openmmo.server.game.session.PlayerState.drivenNpcFacings] is both the gaze
 * the sight engine checks and the sprite on screen. Per-session, like every other npc concern:
 * story state moves and hides npcs differently for every player.
 */
@Singleton
class TrainerFacingDriver
@Inject
constructor(
    private val mapManager: MapManager,
    private val npcService: NpcService,
    private val characterStore: CharacterStore,
    private val scriptMovement: ScriptMovementService,
) {

  /** Replaces this session's driver with one for the map just spawned. */
  fun restart(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    state.npcFacingDriverJob?.cancel()
    state.drivenNpcFacings.clear()

    val map = mapManager.getMap(regionId, bankId, mapId) ?: return
    val stored = state.characterId?.let(characterStore::getCharacter)
    val storyFlags = stored?.storyFlags.orEmpty()
    val driven =
        map.npcs.mapNotNull { npc ->
          if (npc.hideFlag.isNotEmpty() && npc.hideFlag in storyFlags) return@mapNotNull null
          val cycle = drivenFacingCycle(npc) ?: return@mapNotNull null
          val entityId = npcService.entityIdFor(regionId, bankId, mapId, npc.entityIdx)
          Driven(entityId, cycle)
        }
    if (driven.isEmpty()) return

    val scope =
        ctx.attributes.getOrPut(SCRIPT_SCOPE) {
          CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    state.npcFacingDriverJob =
        scope.launch {
          // Stagger the spinners so a room of them does not turn in lockstep.
          var tick = 0
          while (isActive && ctx.channel.isActive) {
            delay(TURN_INTERVAL_MS)
            tick++
            // A running script owns its npcs (an approach walk must not be turned mid-stride).
            if (state.scriptRunning || state.blocksPlayerInput) continue
            driven.forEachIndexed { index, npc ->
              val facing = npc.cycle[(tick + index) % npc.cycle.size]
              state.drivenNpcFacings[npc.entityId] = facing
              scriptMovement.turnNpc(ctx, npc.entityId, facing)
            }
          }
        }
  }

  private data class Driven(val entityId: Long, val cycle: List<Direction>)

  private companion object {
    // Vanilla look-around trainers idle a beat or two between turns.
    const val TURN_INTERVAL_MS = 1600L
  }
}
