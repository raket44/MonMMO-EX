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

  /**
   * The definition a monster battles and displays with: its form's own record when the form has one
   * (Rotom 479 form 1 -> retail record 657, Deoxys 386 form 3 -> 652), otherwise its species.
   * Appearance-only forms (Unown B) and costumes share the species' definition.
   */
  fun forMonster(mon: de.fiereu.openmmo.common.Pokemon): SpeciesDef? =
      (if (mon.form > 0) de.fiereu.openmmo.pokemon.retail.RetailForms.recordOf(mon.dexId, mon.form) else null)
          ?.let(::get)
          ?: get(mon.dexId)

  fun all(): Collection<SpeciesDef> = species.values

  fun size(): Int = species.size

  /**
   * Economy fields are RETAIL-FIRST (operator-directed): PokeMMO hand-tuned its exp and EV yields
   * for its own leveling economy - Pikachu pays 105, matching no cartridge table - so the dump's
   * values override the Expansion's modern ones wherever the dump knows the species. The Expansion
   * (then the decomp) only fills species retail never had, and a zero in any of these fields is
   * never valid data.
   */
  private fun backfillEconomy(def: SpeciesDef, id: Int): SpeciesDef {
    val retail = RetailMonsterData.get(id)
    val decomp = species[id]
    val expYield =
        retail?.yields?.exp?.takeIf { it > 0 }
            ?: def.expYield.takeIf { it > 0 }
            ?: decomp?.expYield
            ?: 0
    val catchRate =
        retail?.catchRate?.takeIf { it > 0 }
            ?: def.catchRate.takeIf { it > 0 }
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
        if (retail == null && (hasEvYields || decomp == null)) def
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
    // Egg groups are retail-first for the same reason as yields: PokeMMO tuned its breeding
    // rules - Nidorina and Nidoqueen breed there while every cartridge says they cannot - and
    // the dex shows the ROM groups, so breeding must read the same table the player sees.
    val retailGroups = retail?.eggGroups.orEmpty()
    val withGroups =
        if (retailGroups.isEmpty()) out
        else
            out.copy(
                eggGroup1 = retailGroups[0],
                eggGroup2 = retailGroups.getOrElse(1) { retailGroups[0] },
            )
    // Abilities are retail-first for retail species (project owner, 2026-09-13), for the same reason
    // as egg groups: the client's summary shows the ability from ITS species table - retail's three
    // slots - so battles must read those slots too, or a monster shows one ability and uses another.
    // The Expansion supplies abilities only for species retail never had. Retail writes a missing
    // second slot as the primary again and a missing hidden one as id 0.
    val withAbilities =
        if (retail == null || id !in RETAIL_DEX_IDS) withGroups
        else
            withGroups.copy(
                ability1 = AbilityWireIds.ability(retail.primaryAbilityId) ?: withGroups.ability1,
                ability1Id = retail.primaryAbilityId,
                ability2 = AbilityWireIds.ability(retail.secondaryAbilityId) ?: withGroups.ability2,
                ability2Id = retail.secondaryAbilityId,
                hiddenAbility =
                    if (retail.hiddenAbilityId == 0) Ability.NONE
                    else AbilityWireIds.ability(retail.hiddenAbilityId) ?: withGroups.hiddenAbility,
                hiddenAbilityId = retail.hiddenAbilityId,
            )
    // Growth rate and gender ratio are retail-first too: the client's exp bar and the summary's
    // gender both read retail's table, and the Expansion copy had quietly replaced them for 1-649.
    // Weight is retail-first for retail species like the rest of the dex page; the Expansion
    // supplies it for species retail never had. A zero is no data, never a weight.
    val weight =
        retail?.weight?.takeIf { id in RETAIL_DEX_IDS && it > 0 }
            ?: withAbilities.weight.takeIf { it > 0 }
            ?: decomp?.weight
            ?: 0
    return withAbilities.copy(
        expYield = expYield,
        catchRate = catchRate,
        growthRate = retail?.growthRate ?: withAbilities.growthRate,
        genderRatio = retail?.genderRatio ?: withAbilities.genderRatio,
        weight = weight,
    )
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
    // Retail ability ids are the standard numbering (the client's), not the enum's ordinals.
    fun ability(abilityId: Int, fallback: Ability?): Ability =
        AbilityWireIds.ability(abilityId) ?: fallback ?: Ability.NONE
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
        ability1Id = retail.primaryAbilityId,
        ability2Id = retail.secondaryAbilityId,
        hiddenAbility =
            if (retail.hiddenAbilityId == 0) Ability.NONE else ability(retail.hiddenAbilityId, null),
        hiddenAbilityId = retail.hiddenAbilityId,
        weight = retail.weight.takeIf { it > 0 } ?: base?.weight ?: 0,
    )
  }
}

/** National dex numbers retail PokeMMO ships, which are also the canonical server ids. */
private val RETAIL_DEX_IDS = 1..649
