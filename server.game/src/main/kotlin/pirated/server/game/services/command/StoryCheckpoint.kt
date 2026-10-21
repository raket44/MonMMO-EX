package pirated.server.game.services.command

import pirated.common.enums.Direction
import pirated.common.enums.Region
import pirated.items.ItemDef

data class CheckpointMon(val dexId: Int, val level: Int, val moveIds: List<Int>)

/**
 * [storyFlags] and [storyVars] replace the character's whole story state rather than merge into it.
 */
data class StoryCheckpoint(
    val name: String,
    val description: String,
    val region: Region,
    val bankId: Int,
    val mapId: Int,
    val x: Int,
    val y: Int,
    val facing: Direction,
    val storyFlags: Set<String>,
    val storyVars: Map<String, Int> = emptyMap(),
    val party: List<CheckpointMon> = emptyList(),
    val items: Map<ItemDef, Int> = emptyMap(),
)
