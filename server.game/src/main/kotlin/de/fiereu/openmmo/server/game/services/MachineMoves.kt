package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Which move a TM or HM item teaches, straight from the client's own tools table
 * (`monmmo/tool-moves.csv`, dumped by the DexPatch "dumptoolsfile" diagnostic): every retail TM
 * block (5328+, the 6xxx twins, the high TMs at 5618+), every regional HM block (339+, 5420+,
 * 8420+, 9420+). Our imported TMs (20000+, "TM <move>") are not in that dump and resolve by name.
 */
object MachineMoves {
  private class Tool(val moveId: Int, val name: String)

  private val tools: Map<Int, Tool> by lazy {
    val stream = MachineMoves::class.java.getResourceAsStream("/monmmo/tool-moves.csv")
    if (stream == null) {
      log.warn { "missing monmmo/tool-moves.csv - no bag TM or HM teaches anything" }
      emptyMap()
    } else {
      stream.bufferedReader().useLines { lines ->
        lines
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
              val parts = line.split(',', limit = 3)
              val id = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
              val move = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
              id to Tool(move, parts.getOrNull(2)?.trim().orEmpty())
            }
            .toMap()
      }
    }
  }

  /** HMs are never used up; the client names every HM item "HM ..." in its tools table. */
  fun isHm(itemId: Int): Boolean = tools[itemId]?.name?.startsWith("HM") == true

  /**
   * The move [itemId] teaches, or null for an item that is no machine. [moveIdByName] answers for
   * imported "TM <move>" items that the client dump does not list.
   */
  fun moveFor(itemId: Int, itemName: String?, moveIdByName: (String) -> Int?): Int? {
    tools[itemId]?.let { return it.moveId }
    val name = itemName ?: return null
    if (!name.startsWith("TM ") && !name.startsWith("HM ")) return null
    return moveIdByName(name.substring(3).trim())
  }
}
