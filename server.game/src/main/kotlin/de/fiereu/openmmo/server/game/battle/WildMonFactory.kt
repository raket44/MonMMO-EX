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

/** Seed rerolls for a nature-and-gender match: 25 natures x at most 2 genders, so a few hundred never misses in practice. */
private const val SEED_TRIES = 512

private const val TACKLE_ID = 33

private const val LAST_RETAIL_DEX = 649

/** A shiny takes a 1 in this many roll to be SECRET (owner, 2026-09-21). */
const val SECRET_SHINY_DENOMINATOR = 12

/**
 * Rolls a wild monster: random nature seed, random IVs, computed stats, full hp, and with a
 * [shinyDenominator] above zero a 1 in that many chance of being shiny (every monster rolls on
 * its own, so each horde member has the full chance). Zero, the default, never rolls one: a
 * trainer's monsters, starters and give commands stay plain.
 *
 * A shiny that rolls then takes one more roll to be SECRET, 1 in [SECRET_SHINY_DENOMINATOR]
 * (owner, 2026-09-21). Sweet Scent is the only thing that cannot produce one, so [secretAllowed]
 * is false only there; step hordes roll for it like anything else, and so do hatched eggs.
 */
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

  fun create(
      requestedDexId: Int,
      level: Int,
      rng: BattleRng,
      shinyDenominator: Int = 0,
      hints: WildRollHints? = null,
      secretAllowed: Boolean = true,
  ): Pokemon? {
    // ONE identity per species (operator-directed): an expansion-offset id whose original dex is
    // 1-649 collapses to the plain canonical id here, so a /giveexp Ditto and a wild-caught one
    // are the same monster server-side. Ids for genuinely new species (650+) keep the offset.
    val collapsedDexId =
        if (requestedDexId >= EXPANSION_SERVER_SPECIES_BASE &&
            requestedDexId - EXPANSION_SERVER_SPECIES_BASE in 1..LAST_RETAIL_DEX)
            requestedDexId - EXPANSION_SERVER_SPECIES_BASE
        else requestedDexId
    // A form the retail client owns is its species under the client's form number, never the
    // Expansion's copy (Rotom Heat is 479 form 1, with retail record 657's data).
    val (speciesId, form) =
        de.fiereu.openmmo.pokemon.expansion.RetailFormIdentity.normalize(collapsedDexId, 0)
    val dexId = speciesId
    val def =
        de.fiereu.openmmo.pokemon.retail.RetailForms.recordOf(speciesId, form)?.let(species::get)
            ?: species.get(speciesId)
            ?: return null
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
    val shiny = shinyDenominator > 0 && rng.pick(shinyDenominator) == 0
    // Only a shiny rolls for Secret, and only where the encounter allows it.
    val secret = shiny && secretAllowed && rng.pick(SECRET_SHINY_DENOMINATOR) == 0
    val mon =
        Pokemon(
            id = entityIds.newMonsterId(),
            ownerId = 0,
            container = PokemonContainer.PARTY,
            containerSlot = 0,
            dexId = dexId,
            seed = seedFor(def, rng, hints),
            ot = "",
            nickname = "",
            level = level.toByte(),
            hp = 0,
            xp = ExpCurves.totalXpFor(def.growthRate, level),
            eVs = EVs(),
            iVs = ivs,
            moves = moveset,
            isShiny = shiny,
            hasHiddenAbility = false,
            isAlpha = false,
            isSecret = secret,
            isFatefulEncounter = false,
            isRaidEncounter = false,
            caughtAt = LocalDateTime.now(),
            // The dex's wild held items: 50% the common one, 5% the rare one (60 / 20 under Compound Eyes).
            // Keyed by client species id, like the dex that lists them (a new species' server id is not).
            heldItem =
                heldItems.roll(
                    de.fiereu.openmmo.common.clientSpeciesId(dexId), rng.pick(100), hints?.compoundEyes == true),
        )
    // First or second ability 50/50, fixed for the monster's life (project owner, 2026-09-13). Rolled
    // last so every earlier roll keeps its place in a seeded sequence. The hidden slot is never
    // rolled; only something that grants a hidden ability sets it.
    return mon.copy(
        hp = StatCalculator.computeAll(def, mon).hp.toShort(), abilitySlot = rng.pick(2), form = form)
  }

  /**
   * A seed that lands on the nature Synchronize asks for and the gender Cute Charm asks for
   * (src/wild_encounter.c CreateWildMon), by rerolling: the seed decides both, so a matching one
   * turns up within a few dozen tries and every other seed-borne trait stays random.
   */
  private fun seedFor(def: de.fiereu.openmmo.pokemon.SpeciesDef, rng: BattleRng, hints: WildRollHints?): Int {
    var seed = rng.natureSeed()
    if (hints == null || (hints.nature == null && hints.gender == null)) return seed
    repeat(SEED_TRIES) {
      val natureOk = hints.nature == null || natureOf(seed) == hints.nature
      val genderOk = hints.gender == null || Gender.of(def.genderRatio, seed) == hints.gender
      if (natureOk && genderOk) return seed
      seed = rng.natureSeed()
    }
    return seed
  }

  private fun natureOf(seed: Int) =
      de.fiereu.openmmo.common.enums.PokemonNature.entries[((seed.toLong() and 0xFFFFFFFFL) % de.fiereu.openmmo.common.enums.PokemonNature.entries.size).toInt()]
}
