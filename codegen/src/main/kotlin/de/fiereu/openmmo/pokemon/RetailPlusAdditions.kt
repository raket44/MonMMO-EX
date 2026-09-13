package de.fiereu.openmmo.pokemon

/**
 * The one rule for putting Expansion learnsets on a retail species: retail plus additions (project
 * owner, 2026-09-12). The client build and the server both go through here, so the summary screen
 * and the move the server teaches come from the same list.
 *
 * Retail's entries all stay, in retail's order. Each Expansion move the species does not already
 * learn by level-up is slotted in after the last retail entry at or below its level. When [limit]
 * caps the list, additions past it are dropped - never retail entries.
 */
object RetailPlusAdditions {
  fun <M> levelUp(
      retail: List<M>,
      expansion: List<M>,
      moveId: (M) -> Int,
      level: (M) -> Int,
      limit: Int = Int.MAX_VALUE,
  ): List<M> {
    val known = retail.mapTo(HashSet(), moveId)
    val room = (limit - retail.size).coerceAtLeast(0)
    val added = expansion.filter { moveId(it) !in known }.distinctBy(moveId).take(room)
    if (added.isEmpty()) return retail
    val merged = retail.toMutableList()
    added.sortedBy(level).forEach { move ->
      merged.add(merged.indexOfLast { level(it) <= level(move) } + 1, move)
    }
    return merged
  }
}
