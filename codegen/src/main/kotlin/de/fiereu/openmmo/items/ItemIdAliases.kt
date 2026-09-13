package de.fiereu.openmmo.items

/**
 * Item ids we once created for things the client has itself, to the client's own id (project
 * owner, 2026-09-13: never duplicate a retail item). The imported "TM Thunderbolt" became TM24, the
 * Day Care's Ability Pill became the client's 1018. Resource monmmo/item-id-aliases.csv
 * (`oldId;retailId`) is written by the client staging, which decides what is created.
 *
 * An inventory can still hold an old id - given before the change - and the client no longer defines
 * it, so the id is folded into the retail one wherever items are read.
 */
object ItemIdAliases {
  private val retailById: Map<Int, Int> by lazy {
    ItemIdAliases::class.java.getResourceAsStream("/monmmo/item-id-aliases.csv")?.bufferedReader()?.useLines { lines ->
      lines
          .mapNotNull { line ->
            val parts = line.split(';')
            val old = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val retail = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            old to retail
          }
          .toMap()
    } ?: emptyMap()
  }

  /** The client's own id for [itemId], or [itemId] itself when it was never retired. */
  fun canonical(itemId: Int): Int = retailById[itemId] ?: itemId

  /** An inventory with every retired id folded into its retail item, quantities added. */
  fun canonicalize(items: Map<Int, Int>): MutableMap<Int, Int> {
    val folded = LinkedHashMap<Int, Int>()
    items.forEach { (id, quantity) -> folded.merge(canonical(id), quantity, Int::plus) }
    return folded
  }
}
