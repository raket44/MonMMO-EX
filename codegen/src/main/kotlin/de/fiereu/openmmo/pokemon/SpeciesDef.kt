package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.BodyColor
import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.GrowthRate
import de.fiereu.openmmo.common.enums.PokemonType

data class SpeciesDef(
    val id: Int,
    val name: String,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val baseSpAttack: Int,
    val baseSpDefense: Int,
    val type1: PokemonType,
    val type2: PokemonType,
    val catchRate: Int,
    val expYield: Int,
    val evYieldHp: Int,
    val evYieldAttack: Int,
    val evYieldDefense: Int,
    val evYieldSpeed: Int,
    val evYieldSpAttack: Int,
    val evYieldSpDefense: Int,
    val itemCommon: Int,
    val itemRare: Int,
    val genderRatio: Int,
    val eggCycles: Int,
    val friendship: Int,
    val growthRate: GrowthRate,
    val eggGroup1: EggGroup,
    val eggGroup2: EggGroup,
    val ability1: Ability,
    val ability2: Ability,
    val safariZoneFleeRate: Int,
    val bodyColor: BodyColor,
    val noFlip: Boolean,
    val ability1Id: Int = ability1.ordinal,
    val ability2Id: Int = ability2.ordinal,
    val abilityMechanicsSupported: Boolean = true,
    /** The hidden ability (a monster's slot 2), [Ability.NONE] when the species has none. */
    val hiddenAbility: Ability = Ability.NONE,
    val hiddenAbilityId: Int = 0,
    /** Hectograms (0.1 kg), as the dex stores it; 0 when no source knows the species' weight. */
    val weight: Int = 0,
) {
  val types: Set<PokemonType> = setOf(type1, type2)

  fun hasType(type: PokemonType): Boolean = type1 == type || type2 == type
}
