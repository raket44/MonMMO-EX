package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.DEFAULT_MOVE_PP
import de.fiereu.openmmo.common.EXPANSION_SERVER_SPECIES_BASE
import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.storage.EntityIdService
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private const val TACKLE_ID = 33

private const val LAST_RETAIL_DEX = 649

/** Rolls a wild monster: random nature seed, random IVs, computed stats, full hp. */
@Singleton
class WildMonFactory
@Inject
constructor(
    private val species: SpeciesRegistry,
    private val moves: MoveRegistry,
    private val learnsets: LearnsetRegistry,
    private val entityIds: EntityIdService,
    private val heldItems: de.fiereu.openmmo.server.game.services.RetailHeldItems =
        de.fiereu.openmmo.server.game.services.RetailHeldItems(),
) {

  fun create(requestedDexId: Int, level: Int, rng: BattleRng): Pokemon? {
    // ONE identity per species (operator-directed): an expansion-offset id whose original dex is
    // 1-649 collapses to the plain canonical id here, so a /giveexp Ditto and a wild-caught one
    // are the same monster server-side. Ids for genuinely new species (650+) keep the offset.
    val dexId =
        if (requestedDexId >= EXPANSION_SERVER_SPECIES_BASE &&
            requestedDexId - EXPANSION_SERVER_SPECIES_BASE in 1..LAST_RETAIL_DEX)
            requestedDexId - EXPANSION_SERVER_SPECIES_BASE
        else requestedDexId
    val def = species.get(dexId) ?: return null
    val ivs =
        IVs().apply {
          hp = rng.ivRoll()
          atk = rng.ivRoll()
          this.def = rng.ivRoll()
          spAtk = rng.ivRoll()
          spDef = rng.ivRoll()
          spd = rng.ivRoll()
        }
    // Species without a learnset fall back to Tackle so the monster can still attack.
    //
    // Moves the server has no definition for are kept rather than dropped. Its own table stops at
    // the 354 moves of the Emerald decomp, so filtering here left an Expansion species holding
    // whichever one or two of its moves happened to predate Gen 4 - a fresh Yveltal arrived
    // knowing only Psychic. The client defines these moves now, and the battle engine already
    // treats a missing definition as nullable, so the moveset displays correctly and only the
    // in-battle effect is still missing.
    val moveIds =
        learnsets
            .initialMoveset(dexId, level)
            .ifEmpty { listOf(TACKLE_ID) }
            // The most recently learned moves, and never more than the client has slots for.
            .takeLast(MAX_MOVE_SLOTS)
    val moveset =
        moveIds.map { PokemonMove(it.toShort(), (moves.get(it)?.pp ?: DEFAULT_MOVE_PP).toByte()) } +
            List(MAX_MOVE_SLOTS - moveIds.size) { PokemonMove(0, 0) }
    val mon =
        Pokemon(
            id = entityIds.newMonsterId(),
            ownerId = 0,
            container = PokemonContainer.PARTY,
            containerSlot = 0,
            dexId = dexId,
            seed = rng.natureSeed(),
            ot = "",
            nickname = "",
            level = level.toByte(),
            hp = 0,
            xp = ExpCurves.totalXpFor(def.growthRate, level),
            eVs = EVs(),
            iVs = ivs,
            moves = moveset,
            isShiny = false,
            hasHiddenAbility = false,
            isAlpha = false,
            isSecret = false,
            isFatefulEncounter = false,
            isRaidEncounter = false,
            caughtAt = LocalDateTime.now(),
            // The dex's wild held items: 50% the common one, 5% the rare one.
            heldItem = heldItems.roll(dexId, rng.pick(100)),
        )
    return mon.copy(hp = StatCalculator.computeAll(def, mon).hp.toShort())
  }
}
