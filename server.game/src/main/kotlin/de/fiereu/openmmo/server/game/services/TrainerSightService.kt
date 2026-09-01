package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.server.game.script.MovementStep
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.script.gbaScriptSource
import de.fiereu.openmmo.server.game.script.interpreter.TrainerStoryState
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Vanilla trainer line-of-sight: a trainer whose gaze ray crosses the tile the player just stepped
 * onto walks up and starts their own script (which is the trainerbattle). The GBA runs this after
 * every step; so does this - MovementService calls [onStep] once the step commits.
 *
 * Facing honesty: NPC wandering is CLIENT-side, so the server only trusts facings that cannot
 * drift - fixed-facing types see along their one direction, multi-facing/rotating types are checked
 * along every direction they cycle through (vanilla-aggressive but never wrong-way), and trainers
 * who physically wander are click-to-battle only until server-side movement exists.
 */
@Singleton
class TrainerSightService
@Inject
constructor(
    private val npcService: NpcService,
    private val characterStore: CharacterStore,
    private val storyService: StoryService,
    private val scriptRegistry: ScriptRegistry,
    private val scriptRunner: ScriptRunner,
    private val battleService: BattleService,
) {

  /** True when a trainer spotted the player and the approach script was launched. */
  fun onStep(
      ctx: SessionContext,
      state: PlayerState,
      map: MapDef,
      playerX: Int,
      playerY: Int,
  ): Boolean {
    val region = Region.byId(state.regionId) ?: return false
    val source = gbaScriptSource(state.regionId) ?: return false
    val charId = state.characterId ?: return false
    val stored = characterStore.getCharacter(charId) ?: return false
    val storyFlags = stored.storyFlags
    val storyVars = stored.storyVars
    val regionId = map.regionId.toInt()
    val bankId = map.bankId.toInt()
    val mapId = map.mapId.toInt()

    for (npc in map.npcs) {
      if (npc.sightRange <= 0) continue
      if (npc.trainerType != TRAINER_TYPE_NORMAL && npc.trainerType != TRAINER_TYPE_ALL_DIRS)
          continue
      if (npc.hideFlag.isNotEmpty() && npc.hideFlag in storyFlags) continue
      // Spinners and multi-face trainers keep their CLIENT-side animation (per-player, like
      // retail) and take the operator's "unfair sight" rule: they catch along every direction
      // they cycle through, whatever their sprite happened to show that frame.
      val directions =
          if (npc.trainerType == TRAINER_TYPE_ALL_DIRS) CARDINALS
          else sightDirections(npc) ?: continue
      val eff = npcService.effectiveNpc(regionId, bankId, mapId, npc, storyFlags, storyVars)

      for (dir in directions) {
        val distance =
            approachDistance(map, eff.x, eff.y, dir, npc.sightRange, playerX, playerY) ?: continue
        val constant = scriptRegistry.trainerConstant(npc.script, source) ?: continue
        val trainer = battleService.resolveTrainer(region, constant) ?: continue
        val defeatedKey = TrainerStoryState.defeated(region.name.lowercase(), trainer.id)
        if (storyService.isFlagSet(charId, defeatedKey)) continue

        val script =
            runCatching { scriptRegistry.forLabel(npc.script, source) }.getOrNull() ?: continue
        launchApproach(ctx, state, regionId, bankId, mapId, npc, dir, distance, script)
        log.info {
          "Trainer sight: ${trainer.constant} at (${eff.x},${eff.y}) spotted player at " +
              "($playerX,$playerY) dir=$dir distance=$distance"
        }
        return true
      }
    }
    return false
  }

  /**
   * The distance (in tiles) from the trainer to the player along [dir], or null when the player is
   * not on that ray within [range] or the approach path is blocked. Matches vanilla: sight needs a
   * walkable corridor - a fence or ledge between them blocks the approach.
   */
  private fun approachDistance(
      map: MapDef,
      fromX: Int,
      fromY: Int,
      dir: Direction,
      range: Int,
      playerX: Int,
      playerY: Int,
  ): Int? {
    var x = fromX
    var y = fromY
    for (d in 1..range) {
      x += dir.dx
      y += dir.dy
      if (x == playerX && y == playerY) return d
      // Intermediate tiles must be open both to see through and to walk through.
      if (map.tileAt(x, y)?.blocksMovement() != false) return null
    }
    return null
  }

  private fun launchApproach(
      ctx: SessionContext,
      state: PlayerState,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      dir: Direction,
      distance: Int,
      script: Script,
  ) {
    val walk = walkStep(dir) ?: return
    val face = faceStep(dir) ?: return
    // Vanilla rhythm: the trainer turns, the "!" bubble pops with its spot sound (bytecode-
    // verified action 0x62), then they walk to the tile adjacent to the player and the gaze
    // lands on them.
    val steps = listOf(face, MovementStep.EMOTE_EXCLAMATION) + List(distance - 1) { walk } + face
    val entityId = npcService.entityIdFor(regionId, bankId, mapId, npc.entityIdx)
    // The scripted-state lock (taken by the runner at script start) removes the player's input
    // for the whole approach; all the player themselves needs is the vanilla turn toward the
    // spotting trainer.
    val playerFace = faceStep(dir.opposite()) ?: return
    val approach = Script { scriptCtx ->
      scriptCtx.lockAll()
      scriptCtx.moveSelfAndNpcs(listOf(playerFace), npc.entityIdx to steps)
      script.run(scriptCtx)
    }
    scriptRunner.run(ctx, state, approach, entityId)
  }

  /**
   * The directions this trainer can be trusted to face. Null = the npc physically wanders
   * (client-simulated, position unknowable) - line of sight stays off for it.
   */
  private fun sightDirections(npc: NpcDef): List<Direction>? {
    val name = npc.movementType.name
    return when {
      name.startsWith("WANDER") || name.startsWith("WALK_SEQUENCE") -> null
      name == "WALK_UP_AND_DOWN" ||
          name == "WALK_DOWN_AND_UP" ||
          name == "WALK_LEFT_AND_RIGHT" ||
          name == "WALK_RIGHT_AND_LEFT" -> null
      // Spinner/multi-face types are server-driven now and handled before this is called;
      // reaching here with one means the driver classified it out - fall back to its cycle.
      name == "LOOK_AROUND" || name.startsWith("ROTATE") -> CARDINALS
      name.startsWith("FACE_") && name.contains("_AND_") ->
          name.split('_').mapNotNull(::directionToken).distinct().ifEmpty { listOf(npc.facing) }
      else -> listOf(npc.facing)
    }
  }

  private fun directionToken(token: String): Direction? =
      when (token) {
        "UP" -> Direction.UP
        "DOWN" -> Direction.DOWN
        "LEFT" -> Direction.LEFT
        "RIGHT" -> Direction.RIGHT
        else -> null
      }

  private fun walkStep(dir: Direction): MovementStep? =
      when (dir) {
        Direction.UP -> MovementStep.WALK_UP
        Direction.DOWN -> MovementStep.WALK_DOWN
        Direction.LEFT -> MovementStep.WALK_LEFT
        Direction.RIGHT -> MovementStep.WALK_RIGHT
        else -> null
      }

  private fun faceStep(dir: Direction): MovementStep? =
      when (dir) {
        Direction.UP -> MovementStep.FACE_UP
        Direction.DOWN -> MovementStep.FACE_DOWN
        Direction.LEFT -> MovementStep.FACE_LEFT
        Direction.RIGHT -> MovementStep.FACE_RIGHT
        else -> null
      }

  private companion object {
    const val TRAINER_TYPE_NORMAL = 1
    const val TRAINER_TYPE_ALL_DIRS = 2
    val CARDINALS = listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)
  }
}
