package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The wild held items the retail dex lists per species (staged from monsters.json into
 * `retail-held-items.csv` by the launcher's stageRetailData). Rows are `species;slot;item;name`;
 * the first slot is the common item, the second the rare one, as the cartridges roll them.
 */
@Singleton
class RetailHeldItems @Inject constructor() {
  private val bySpecies: Map<Int, List<Int>>

  init {
    // Retail's table, then the one the client staging writes for the species retail never had -
    // the same items the client's dex lists for them. Both are keyed by client species id.
    bySpecies =
        TABLES.flatMap { path ->
              val stream = RetailHeldItems::class.java.getResourceAsStream(path)
              if (stream == null) {
                log.warn { "${path.substringAfterLast('/')} missing; those species hold nothing" }
                return@flatMap emptyList()
              }
              stream.bufferedReader().useLines { lines ->
                lines
                    .mapNotNull { line ->
                      val parts = line.split(';')
                      if (parts.size < 3) return@mapNotNull null
                      val species = parts[0].toIntOrNull() ?: return@mapNotNull null
                      val slot = parts[1].toIntOrNull() ?: return@mapNotNull null
                      val item = parts[2].toIntOrNull() ?: return@mapNotNull null
                      Triple(species, slot, item)
                    }
                    .toList()
              }
            }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, slots) -> slots.sortedBy { it.first }.map { it.second } }
            .also { log.info { "Wild held items: ${it.size} species" } }
  }

  /** The items a wild monster of [species] may carry, common first. */
  fun of(species: Int): List<Int> = bySpecies[species].orEmpty()

  /**
   * Rolls a wild monster's held item the cartridge way: 50% for the common item, 5% for the rare
   * one (a species with a single listed item uses the common rate). 0 = nothing.
   */
  /** The wild held item for a 0..99 roll; Compound Eyes on the lead raises the odds to 60% / 20%. */
  fun roll(species: Int, percent: Int, compoundEyes: Boolean = false): Int {
    val items = of(species)
    if (items.isEmpty()) return 0
    val common = if (compoundEyes) COMPOUND_EYES_COMMON_PERCENT else COMMON_PERCENT
    val rare = if (compoundEyes) COMPOUND_EYES_RARE_PERCENT else RARE_PERCENT
    if (percent < common) return items[0]
    if (items.size > 1 && percent < common + rare) return items[1]
    return 0
  }

  private companion object {
    val TABLES = listOf("/monmmo/retail-held-items.csv", "/monmmo/expansion-held-items.csv")
    const val COMMON_PERCENT = 50
    const val RARE_PERCENT = 5
    const val COMPOUND_EYES_COMMON_PERCENT = 60
    const val COMPOUND_EYES_RARE_PERCENT = 20
  }
}
