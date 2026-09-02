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

  /**
   * Retail-dump data wins for species it covers (operator-directed: the modern types, stats and
   * tables the client itself shows replace the GBA decomp's), with the decomp def filling the
   * fields the dump lacks (egg cycles, friendship, safari flee rate, body color). Everything else
   * falls back exactly as before: generated decomp defs, then the expansion catalogue.
   */
  fun get(id: Int): SpeciesDef? {
    val retail = RetailMonsterData.get(id)
    if (retail != null) {
      return retailMerged.computeIfAbsent(id) { mergeRetail(retail, species[id]) }
    }
    return species[id] ?: expansion.runtimeDefinition(id)
  }

  fun all(): Collection<SpeciesDef> = species.values

  fun size(): Int = species.size

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
