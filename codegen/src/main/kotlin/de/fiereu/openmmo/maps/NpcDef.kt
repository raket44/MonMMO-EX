package de.fiereu.openmmo.maps

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MovementType

data class NpcDef(
    val entityIdx: Int,
    val graphicsId: Int,
    val x: Int,
    val y: Int,
    val elevation: Int,
    val movementType: MovementType,
    val movementRangeX: Int,
    val movementRangeY: Int,
    val trainerType: Int,
    /** Tiles of trainer line-of-sight (trainer_sight_or_berry_tree_id); 0 = talk only. */
    val sightRange: Int = 0,
    val facing: Direction,
    val script: String = "0x0",
    /** Story flag that hides this npc by default, or "" when it is always shown. */
    val hideFlag: String = "",
    /** The floor-below boulder's hide flag a pushable boulder reveals when it drops through a hole. */
    val revealFlag: String = "",
)
