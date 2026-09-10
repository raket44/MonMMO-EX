package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.PokemonNature
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.Abilities
import de.fiereu.openmmo.server.game.battle.Gender
import de.fiereu.openmmo.server.game.battle.WildRollHints
import de.fiereu.openmmo.server.game.storage.CharacterStore
import kotlin.random.Random

/**
 * What the lead party monster's ability and held item do to wild encounters, FireRed's
 * src/wild_encounter.c rules:
 * - encounter rate: Stench, White Smoke and Quick Feet halve it, Illuminate, Arena Trap and
 *   No Guard double it; a Cleanse Tag held by the lead cuts it to two thirds.
 * - level: Hustle, Pressure and Vital Spirit pick the slot's top level half the time; Keen Eye and
 *   Intimidate turn away a wild monster five or more levels below the lead half the time.
 * - species: Static and Magnet Pull pick an Electric or Steel slot half the time when the table
 *   has one.
 * - nature: Synchronize hands the wild monster the lead's nature half the time.
 * - gender: Cute Charm makes the wild monster the opposite gender two times in three.
 * - held item: Compound Eyes raises the wild held-item odds (60% / 20% instead of 50% / 5%).
 *
 * The lead is the first party member that is not an egg and is not fainted, as on the cartridge.
 */
class OverworldAbilities(private val species: SpeciesRegistry, private val cleanseTagItemId: Int) {

  class Lead(val ability: Ability, val level: Int, val nature: PokemonNature, val gender: Byte, val heldItem: Int)

  fun leadOf(store: CharacterStore, charId: Long): Lead? {
    val mon: Pokemon =
        store.getCharacter(charId)?.pokemon?.sortedBy { it.containerSlot }?.firstOrNull { !it.isEgg && it.hp > 0 } ?: return null
    val def = species.get(mon.dexId) ?: return null
    return Lead(Abilities.of(def, mon), mon.level.toInt(), mon.nature, Gender.of(def.genderRatio, mon.seed), mon.heldItem)
  }

  /** The encounter chance out of ENCOUNTER_ROLL_MAX after the lead's ability and held item. */
  fun scaledRate(base: Int, lead: Lead?): Int {
    var chance = base * ENCOUNTER_RATE_SCALE
    if (lead == null) return chance
    when (lead.ability) {
      Ability.STENCH, Ability.WHITE_SMOKE, Ability.QUICK_FEET -> chance /= 2
      Ability.ILLUMINATE, Ability.ARENA_TRAP, Ability.NO_GUARD -> chance *= 2
      else -> {}
    }
    if (cleanseTagItemId != 0 && lead.heldItem == cleanseTagItemId) chance = chance * 2 / 3
    return chance
  }

  /** Static / Magnet Pull: a same-type slot half the time, when the table offers one. */
  fun <T> biasSlot(pool: List<T>, dexOf: (T) -> Int, lead: Lead?, random: Random): List<T> {
    val type =
        when (lead?.ability) {
          Ability.STATIC -> PokemonType.ELECTRIC
          Ability.MAGNET_PULL -> PokemonType.STEEL
          else -> return pool
        }
    if (random.nextInt(2) == 0) return pool
    val typed = pool.filter { species.get(dexOf(it))?.types?.contains(type) == true }
    return typed.ifEmpty { pool }
  }

  /**
   * The wild level for a slot, or null when Keen Eye / Intimidate turns the encounter away
   * (half the time, if the wild would be five or more levels below the lead).
   */
  fun levelFor(minLevel: Int, maxLevel: Int, lead: Lead?, random: Random): Int? {
    var level = random.nextInt(minLevel, maxLevel + 1)
    when (lead?.ability) {
      Ability.HUSTLE, Ability.PRESSURE, Ability.VITAL_SPIRIT -> if (random.nextInt(2) == 0) level = maxLevel
      Ability.KEEN_EYE, Ability.INTIMIDATE -> if (random.nextInt(2) == 0 && level <= lead.level - 5) return null
      else -> {}
    }
    return level
  }

  /** Synchronize, Cute Charm and Compound Eyes, for the monster about to be rolled. */
  fun hints(lead: Lead?, wildDexId: Int, random: Random): WildRollHints? {
    if (lead == null) return null
    val nature = if (lead.ability == Ability.SYNCHRONIZE && random.nextInt(2) == 0) lead.nature else null
    val gender =
        if (lead.ability == Ability.CUTE_CHARM && random.nextInt(3) != 0) {
          val ratio = species.get(wildDexId)?.genderRatio ?: 255
          when {
            ratio == 0 || ratio == 254 || ratio == 255 -> null
            lead.gender == Gender.MALE -> Gender.FEMALE
            lead.gender == Gender.FEMALE -> Gender.MALE
            else -> null
          }
        } else null
    val compoundEyes = lead.ability == Ability.COMPOUND_EYES
    if (nature == null && gender == null && !compoundEyes) return null
    return WildRollHints(nature, gender, compoundEyes)
  }

  companion object {
    const val ENCOUNTER_RATE_SCALE = 16
  }
}
