package de.fiereu.openmmo.common

import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.PokemonNature
import java.time.LocalDateTime

const val MAX_MOVE_SLOTS = 4

/**
 * PP given to a move the server has no definition for.
 *
 * Its own table stops at the 354 moves of the Emerald decomp, so every Expansion move above that
 * arrives without one. Two places used to disagree about this - the factory said 35 and the give
 * path said 0 - and the give path won, which is why imported moves showed no PP at all. It is a
 * placeholder either way: the real number lives in the Expansion, and only a move table built from
 * it will get Moonblast to 15 and Oblivion Wing to 10.
 */
const val DEFAULT_MOVE_PP = 35

const val MAX_PARTY_SIZE = 6

data class Pokemon(
    val id: Long,
    val ownerId: Long,
    val container: PokemonContainer,
    val containerSlot: Short,
    val dexId: Int,
    val seed: Int,
    val ot: String,
    val nickname: String,
    val level: Byte,
    val hp: Short,
    val xp: Int,
    val eVs: EVs,
    val iVs: IVs,
    val moves: List<PokemonMove>,
    val isShiny: Boolean,
    val hasHiddenAbility: Boolean,
    val isAlpha: Boolean,
    val isSecret: Boolean,
    val isFatefulEncounter: Boolean,
    val isRaidEncounter: Boolean,
    val caughtAt: LocalDateTime,
    val isEgg: Boolean = false,
    /**
     * Client item id of the held item, 0 for none. Rides the monster record in the short right
     * after current HP (client field k91.eE0; the held-item getter vh1() falls back to it whenever
     * the battle override z21 is -1, bytecode-verified).
     */
    val heldItem: Int = 0,
) {
  // seed is an unsigned 32-bit value on the wire, so mask before the modulo to avoid a negative
  // index when the high bit is set.
  val nature: PokemonNature =
      PokemonNature.entries[((seed.toLong() and 0xFFFFFFFFL) % PokemonNature.entries.size).toInt()]

  init {
    require(moves.size <= MAX_MOVE_SLOTS) { "A Pokemon can't have more than $MAX_MOVE_SLOTS moves" }
  }
}

data class PokemonMove(val id: Short, var pp: Byte)
