package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.RetailPlusAdditions

/**
 * Expansion content goes on top of retail, never over it (project owner, 2026-09-12).
 *
 * The client applies these sections by assignment - the last record for a species wins - so
 * appending an Expansion record after a retail one silently replaced retail. Instead each retail
 * record is edited in place, keeping everything it had, and only species or categories retail has
 * no record for get a new one. `:launcher:checkRetailPreserved` holds the build to exactly this.
 */
object RetailMerge {

  /**
   * Retail's level-up list with its order untouched, plus every Expansion move the species does not
   * already learn by level-up, each slotted in after the last retail entry at or below its level.
   * Additions that would push past the one-byte move count are dropped, never retail entries.
   */
  fun levelUp(stock: List<LevelUpLearnset>, expansion: List<LevelUpLearnset>): List<LevelUpLearnset> {
    val additions = expansion.associateBy { it.speciesId }
    val lastRetail = stock.indices.associateBy { stock[it].speciesId }
    val merged =
        stock.mapIndexed { index, retail ->
          val extra = additions[retail.speciesId]
          if (extra == null || lastRetail[retail.speciesId] != index) return@mapIndexed retail
          retail.copy(
              moves =
                  RetailPlusAdditions.levelUp(
                      retail.moves,
                      extra.moves,
                      moveId = { it.moveId },
                      level = { it.level },
                      limit = LevelUpLearnset.MAX_MOVES,
                  ))
        }
    return merged + expansion.filter { it.speciesId !in lastRetail }
  }

  /** Per species and category: retail's moves in retail's order, then the Expansion's missing ones. */
  fun extra(stock: List<ExtraLearnset>, expansion: List<ExtraLearnset>): List<ExtraLearnset> {
    val additions =
        expansion.groupBy { it.speciesId to it.category }.mapValues { (_, lists) -> lists.flatMap { it.moves } }
    val lastRetail = stock.indices.associateBy { stock[it].speciesId to stock[it].category }
    val merged =
        stock.mapIndexed { index, retail ->
          val key = retail.speciesId to retail.category
          val extra = additions[key]
          if (extra == null || lastRetail[key] != index) return@mapIndexed retail
          val added = extra.filter { it !in retail.moves }.distinct()
          if (added.isEmpty()) retail else retail.copy(moves = retail.moves + added)
        }
    return merged + expansion.filter { (it.speciesId to it.category) !in lastRetail }
  }

  /**
   * A retail species keeps its detail record; the Expansion may only ADD to it (project owner,
   * 2026-09-12/13): egg groups and types for the Fairy retypes, evolutions retail never had (Eevee to
   * Sylveon), and form entries after retail's own (Charizard's Megas after its Royal costume).
   * Species retail has no record for take the Expansion's record whole; for a species the ROM
   * defines, staging writes only [ADDITIVE_BITS] and `:launcher:checkRetailPreserved` holds it there.
   */
  fun details(stock: List<SpeciesDetail>, expansion: List<SpeciesDetail>): List<SpeciesDetail> {
    val additions = expansion.associateBy { it.speciesId }
    val lastRetail = stock.indices.associateBy { stock[it].speciesId }
    val merged =
        stock.mapIndexed { index, retail ->
          val extra = additions[retail.speciesId]
          if (extra == null || lastRetail[retail.speciesId] != index) return@mapIndexed retail
          addTo(retail, extra)
        }
    return merged + expansion.filter { it.speciesId !in lastRetail }
  }

  /** The detail bits an addition may set on a retail species. */
  const val ADDITIVE_BITS =
      SpeciesDetail.EGG_GROUPS or
          SpeciesDetail.TYPES or
          SpeciesDetail.EVOLUTIONS or
          SpeciesDetail.SPECIAL_VARIANTS

  private fun addTo(retail: SpeciesDetail, extra: SpeciesDetail): SpeciesDetail {
    val retailVariants = retail.specialVariants
    val extraVariants = extra.specialVariants
    // A variant list is its count byte, then 17 bytes per entry: retail's entries stay first.
    val variants =
        if (retailVariants == null || extraVariants == null) extraVariants ?: retailVariants
        else
            listOf(retailVariants[0] + extraVariants[0]) +
                retailVariants.drop(1) +
                extraVariants.drop(1)
    return retail.copy(
        flags = retail.flags or (extra.flags and ADDITIVE_BITS),
        eggGroups = extra.eggGroups ?: retail.eggGroups,
        types = extra.types ?: retail.types,
        evolutions = (retail.evolutions.orEmpty() + extra.evolutions.orEmpty()).ifEmpty { null },
        specialVariants = variants,
    )
  }
}
