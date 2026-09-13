package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.pokemon.generated.GeneratedLearnsets
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class LevelUpMove(val level: Int, val moveId: Int)

/** The level up learnsets from the decomp, keyed by national dex id and sorted by level. */
@Singleton
class LearnsetRegistry
@Inject
constructor(
    private val expansion: ExpansionSpeciesRegistry = ExpansionSpeciesRegistry(),
) {

  private val learnsets = ConcurrentHashMap<Int, List<LevelUpMove>>()

  init {
    GeneratedLearnsets.loadInto(this)
  }

  fun register(dexId: Int, moves: List<LevelUpMove>) {
    learnsets[dexId] = moves
  }

  fun get(dexId: Int): List<LevelUpMove> {
    val expansionMoves =
        (expansion.getByServerId(dexId) ?: expansion.getByClientWireId(dexId))
            ?.levelUpLearnset
            ?.map { LevelUpMove(it.level, it.originalMoveId) }
            .orEmpty()
    val retail = de.fiereu.openmmo.pokemon.retail.RetailMonsterData.get(dexId)?.levelUpLearnset.orEmpty()
    // Retail species (national dex 1-649) keep PokeMMO's own list and only gain the Expansion moves
    // they lack - retail plus additions (project owner, 2026-09-12), the same RetailPlusAdditions
    // rule the client build writes into data.pak, so the summary screen and the server agree.
    // Levels are clamped the way the client build clamps them: the Expansion's level-0 evolution
    // moves arrive at level 1 on both sides.
    if (dexId in RETAIL_DEX_IDS && retail.isNotEmpty()) {
      return RetailPlusAdditions.levelUp(
          retail,
          expansionMoves.map { it.copy(level = it.level.coerceIn(1, 100)) }.sortedBy { it.level },
          moveId = { it.moveId },
          level = { it.level },
      )
    }
    // Everything else keeps the old precedence: the Expansion, then the retail dump (its 1000+
    // form records), then the decomp tables.
    return expansionMoves.takeIf { it.isNotEmpty() }
        ?: retail.takeIf { it.isNotEmpty() }
        ?: learnsets[dexId].orEmpty()
  }

  private companion object {
    /** National dex numbers retail PokeMMO ships, which are also the canonical server ids. */
    val RETAIL_DEX_IDS = 1..649
  }

  fun movesAt(dexId: Int, level: Int): List<Int> =
      get(dexId).filter { it.level == level }.map { it.moveId }

  /**
   * The moveset a monster generated at this level starts with. Like the games, the learnset is
   * walked in order and a fifth move pushes out the oldest one.
   */
  fun initialMoveset(dexId: Int, level: Int): List<Int> {
    val slots = ArrayList<Int>(MAX_MOVE_SLOTS)
    for (entry in get(dexId)) {
      if (entry.level > level) break
      if (entry.moveId in slots) continue
      if (slots.size == MAX_MOVE_SLOTS) slots.removeAt(0)
      slots += entry.moveId
    }
    return slots
  }

  fun size(): Int = learnsets.size
}
