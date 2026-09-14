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

  /**
   * The retail moves staged as Fairy: 186 Sweet Kiss, 204 Charm, 236 Moonlight (project owner,
   * 2026-09-13: "client's numbers but our move retyping"). They are the ONLY retail move records
   * staging changes, and only in their type.
   *
   * The builder does not take this set on trust, it derives it ([fairyMoveRetypes]): a move the Gen 5
   * ROMs number that the Expansion types TYPE_FAIRY can only be a Gen 6 retype, because the ROMs
   * predate the type - moves_info.h spells each one `B_UPDATED_MOVE_TYPES >= GEN_6 ? TYPE_FAIRY :
   * TYPE_NORMAL`. The derived set has to equal this one, so an Expansion update that retypes another
   * move stops the build until it is approved here, and `:launcher:checkRetailPreserved` allows
   * exactly these ids.
   */
  val APPROVED_FAIRY_MOVE_RETYPES: Set<Int> = setOf(186, 204, 236)

  /** `f/eb6` ordinals: NORMAL is retail's, FAIRY the one the client Fairy patch adds. */
  const val NORMAL_TYPE = 0
  const val FAIRY_TYPE = 19

  /** ROM-numbered moves (1..[lastRomMoveId]) the Expansion types Fairy, checked against the approval. */
  fun fairyMoveRetypes(expansionTypes: Map<Int, String>, lastRomMoveId: Int): Set<Int> {
    val derived =
        expansionTypes.filter { (id, type) -> id in 1..lastRomMoveId && type == "TYPE_FAIRY" }.keys.toSortedSet()
    require(derived == APPROVED_FAIRY_MOVE_RETYPES) {
      "Expansion Fairy retypes of retail moves are $derived, approved are " +
          "${APPROVED_FAIRY_MOVE_RETYPES.sorted()}: approve the change in RetailMerge first"
    }
    return derived
  }

  /**
   * Section 4: retail's records with [fairyRetypes] applied in place, then [added] (moves the client
   * has no record for).
   *
   * r32645 `f/fi7` case 4 looks each id up in the table the ROM built and overwrites only what the
   * record's flags carry (the category always), so a retype is retail's own record with bit 0x8 set
   * and type 19 - every other byte stays. Appending a second record instead would re-assign the
   * category and every flagged field from whatever that record says. As with the learnsets, when
   * retail lists an id twice only the last record, the one the client ends up with, is edited.
   */
  fun moves(stock: List<MoveRecord>, fairyRetypes: Set<Int>, added: List<MoveRecord>): List<MoveRecord> {
    val lastRetail = stock.indices.associateBy { stock[it].moveId }
    val missing = fairyRetypes.filter { it !in lastRetail }
    require(missing.isEmpty()) { "No retail move record to retype in place for ids $missing" }
    val clashes = added.map { it.moveId }.filter(lastRetail::containsKey)
    require(clashes.isEmpty()) { "Move ids already defined by the client: $clashes" }
    val merged =
        stock.mapIndexed { index, retail ->
          if (retail.moveId in fairyRetypes && lastRetail[retail.moveId] == index) fairyRetype(retail)
          else retail
        }
    return merged + added
  }

  /** Retail's record as Fairy: bit 0x8 and type 19, nothing else. Only a Normal move is retyped. */
  fun fairyRetype(retail: MoveRecord): MoveRecord {
    require(retail.type == null || retail.type == NORMAL_TYPE) {
      "Retail move ${retail.moveId} is explicitly type ${retail.type}, not Normal; refusing to retype it"
    }
    return retail.copy(flags = retail.flags or MoveRecord.TYPE, type = FAIRY_TYPE)
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
