package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.pokemon.generated.GeneratedSpecies
import de.fiereu.openmmo.pokemon.retail.RetailMonsterData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeciesRegistry
@Inject
constructor(
    private val expansion: ExpansionSpeciesRegistry = ExpansionSpeciesRegistry(),
) {

  private val species = ConcurrentHashMap<Int, SpeciesDef>()
  private val retailMerged = ConcurrentHashMap<Int, SpeciesDef>()

  init {
    GeneratedSpecies.loadInto(this)
  }

  fun register(def: SpeciesDef) {
    species[def.id] = def
  }

  private val expansionResolved = ConcurrentHashMap<Int, SpeciesDef>()

  /**
   * Precedence (operator-directed): the EXPANSION catalogue is the modern truth - it is where the
   * Fairy retypes and current tables came from - and replaces the counterpart data for every
   * species it resolves, old dex numbers included. The retail dump (the client's own DUMP DEX
   * output: complete for retail species but OUTDATED movesets and no new species) fills species the
   * expansion cannot resolve, merged over the decomp def for its missing fields. The decomp tables
   * are the last fallback.
   */
  fun get(id: Int): SpeciesDef? {
    expansionResolved[id]?.let {
      return it
    }
    expansion.runtimeDefinition(id)?.let { resolved ->
      val filled = backfillEconomy(resolved, id)
      expansionResolved[id] = filled
      return filled
    }
    val retail = RetailMonsterData.get(id)
    if (retail != null) {
      return retailMerged.computeIfAbsent(id) { mergeRetail(retail, species[id]) }
    }
    return species[id]
  }

  fun all(): Collection<SpeciesDef> = species.values

  fun size(): Int = species.size

  /**
   * The pre-generated expansion catalogue carries ZERO exp yields (and sometimes catch rates and EV
   * yields) for old species - a Chansey win paid 0 xp. A zero in those fields is never valid data,
   * so it backfills from the retail dump, then the decomp def.
   */
  private fun backfillEconomy(def: SpeciesDef, id: Int): SpeciesDef {
    val retail = RetailMonsterData.get(id)
    val decomp = species[id]
    val expYield =
        def.expYield.takeIf { it > 0 }
            ?: retail?.yields?.exp?.takeIf { it > 0 }
            ?: decomp?.expYield
            ?: 0
    val catchRate =
        def.catchRate.takeIf { it > 0 }
            ?: retail?.catchRate?.takeIf { it > 0 }
            ?: decomp?.catchRate
            ?: 0
    val hasEvYields =
        def.evYieldHp +
            def.evYieldAttack +
            def.evYieldDefense +
            def.evYieldSpeed +
            def.evYieldSpAttack +
            def.evYieldSpDefense > 0
    val out =
        if (hasEvYields || (retail == null && decomp == null)) def
        else if (retail != null &&
            retail.yields.let {
              it.evHp + it.evAttack + it.evDefense + it.evSpeed + it.evSpAttack + it.evSpDefense
            } > 0)
            def.copy(
                evYieldHp = retail.yields.evHp,
                evYieldAttack = retail.yields.evAttack,
                evYieldDefense = retail.yields.evDefense,
                evYieldSpeed = retail.yields.evSpeed,
                evYieldSpAttack = retail.yields.evSpAttack,
                evYieldSpDefense = retail.yields.evSpDefense,
            )
        else if (decomp != null)
            def.copy(
                evYieldHp = decomp.evYieldHp,
                evYieldAttack = decomp.evYieldAttack,
                evYieldDefense = decomp.evYieldDefense,
                evYieldSpeed = decomp.evYieldSpeed,
                evYieldSpAttack = decomp.evYieldSpAttack,
                evYieldSpDefense = decomp.evYieldSpDefense,
            )
        else def
    return out.copy(expYield = expYield, catchRate = catchRate)
  }

  private fun mergeRetail(
      retail: RetailMonsterData.RetailMonster,
      base: SpeciesDef?,
  ): SpeciesDef {
    fun type(index: Int): PokemonType =
        retail.typeNames.getOrNull(index)?.let { name ->
          PokemonType.entries.firstOrNull { it.name == name }
        }
            ?: retail.typeNames.getOrNull(0)?.let { name ->
              PokemonType.entries.firstOrNull { it.name == name }
            }
            ?: base?.type1
            ?: PokemonType.NORMAL
    fun ability(abilityId: Int, fallback: Ability?): Ability =
        Ability.entries.getOrNull(abilityId) ?: fallback ?: Ability.NONE
    val groups = retail.eggGroups
    return SpeciesDef(
        id = retail.id,
        name = retail.name.ifEmpty { base?.name ?: "Species ${retail.id}" },
        baseHp = retail.stats.hp,
        baseAttack = retail.stats.attack,
        baseDefense = retail.stats.defense,
        baseSpeed = retail.stats.speed,
        baseSpAttack = retail.stats.spAttack,
        baseSpDefense = retail.stats.spDefense,
        type1 = type(0),
        type2 = type(1),
        catchRate = retail.catchRate,
        expYield = retail.yields.exp,
        evYieldHp = retail.yields.evHp,
        evYieldAttack = retail.yields.evAttack,
        evYieldDefense = retail.yields.evDefense,
        evYieldSpeed = retail.yields.evSpeed,
        evYieldSpAttack = retail.yields.evSpAttack,
        evYieldSpDefense = retail.yields.evSpDefense,
        itemCommon = base?.itemCommon ?: 0,
        itemRare = base?.itemRare ?: 0,
        genderRatio = retail.genderRatio,
        eggCycles = base?.eggCycles ?: 20,
        friendship = base?.friendship ?: 70,
        growthRate = retail.growthRate,
        eggGroup1 = groups.getOrElse(0) { base?.eggGroup1 ?: EggGroup.NONE },
        eggGroup2 =
            groups.getOrElse(1) { groups.getOrElse(0) { base?.eggGroup2 ?: EggGroup.NONE } },
        ability1 = ability(retail.primaryAbilityId, base?.ability1),
        ability2 = ability(retail.secondaryAbilityId, base?.ability2),
        safariZoneFleeRate = base?.safariZoneFleeRate ?: 0,
        bodyColor = base?.bodyColor ?: de.fiereu.openmmo.common.enums.BodyColor.RED,
        noFlip = base?.noFlip ?: false,
    )
  }
}
