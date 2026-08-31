package de.fiereu.openmmo.server.game.world

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MapType
import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.maps.MapDef

data class WarpExitOverride(
    val facing: Direction,
    val autoStep: Boolean,
)

object WarpExitRules {

  /**
   * Vanilla's GetAdjustedInitialDirection (pokeemerald-expansion src/overworld.c): the landing
   * tile's behavior decides the facing, ladders keep the facing the player warped in with, and
   * everything else faces DOWN - there is no map-type or edge guessing in the real game. The old
   * building/underground/edge heuristics produced the wrong facing whenever they disagreed with the
   * tile (the "first trip" facing bugs), so they are gone.
   */
  fun inferExitFacing(
      destTileBehavior: TileBehavior?,
      entryFacing: Direction,
  ): Direction =
      when (destTileBehavior) {
        TileBehavior.DOOR,
        TileBehavior.NON_ANIMATED_DOOR -> Direction.DOWN
        TileBehavior.NORTH_ARROW_WARP -> Direction.DOWN
        TileBehavior.SOUTH_ARROW_WARP -> Direction.UP
        TileBehavior.WEST_ARROW_WARP -> Direction.RIGHT
        TileBehavior.EAST_ARROW_WARP -> Direction.LEFT
        TileBehavior.STAIR_WARP_EAST -> Direction.LEFT
        TileBehavior.STAIR_WARP_WEST -> Direction.RIGHT
        // Ladders, escalators and warp pads: keep the pre-warp facing, per vanilla.
        TileBehavior.LADDER -> entryFacing
        else -> Direction.DOWN
      }

  fun getKnownOverride(
      sourceMap: MapDef?,
      destMap: MapDef?,
      destX: Int,
      destY: Int,
  ): WarpExitOverride? {
    if (destMap == null) return null

    if (destMap.bankId.toInt() == 74 && destMap.mapId.toInt() == 4) {
      return when {
        destX == 4 && destY == 10 -> WarpExitOverride(Direction.UP, autoStep = false)
        destX == 29 && destY == 16 -> WarpExitOverride(Direction.UP, autoStep = false)
        destX == 18 && destY == 20 -> WarpExitOverride(Direction.UP, autoStep = false)
        else -> null
      }
    }

    if (destMap.bankId.toInt() == 74 && destMap.mapId.toInt() == 11) {
      return when {
        (destX == 14 && destY == 5) || (destX == 15 && destY == 5) ->
            WarpExitOverride(Direction.DOWN, autoStep = false)
        (destX == 16 && destY == 38) ||
            (destX == 17 && destY == 38) ||
            (destX == 36 && destY == 38) ||
            (destX == 37 && destY == 38) -> WarpExitOverride(Direction.UP, autoStep = false)
        else -> null
      }
    }

    return null
  }

  /**
   * Vanilla's SetUpWarpExitTask (pokeemerald-expansion src/field_screen_effect.c): doors, non-anim
   * doors and directional stair warps play a walk-out on arrival; every other landing is
   * Task_ExitNonDoor - no step. Only a tile with NO behavior data falls back to the old map-type
   * heuristic (exiting a building or entering the underground steps).
   */
  fun shouldAutoStep(
      sourceMap: MapDef?,
      destMap: MapDef?,
      destTileBehavior: TileBehavior? = null,
  ): Boolean {
    if (sourceMap == null || destMap == null) return false

    if (destTileBehavior != null) {
      // Only doors walk out: the GBA client animates the stair walk-off itself, so a server
      // step on stair arrivals doubled it (play-verified).
      return when (destTileBehavior) {
        TileBehavior.DOOR,
        TileBehavior.NON_ANIMATED_DOOR -> true
        else -> false
      }
    }

    val sourceBuilding =
        sourceMap.mapType == MapType.INSIDE || sourceMap.mapType == MapType.SECRET_BASE

    val destBuilding = destMap.mapType == MapType.INSIDE || destMap.mapType == MapType.SECRET_BASE

    val enteringUnderground =
        destMap.mapType == MapType.UNDERGROUND && sourceMap.mapType != MapType.UNDERGROUND

    if (sourceBuilding && !destBuilding) return true
    if (enteringUnderground) return true

    return false
  }
}
