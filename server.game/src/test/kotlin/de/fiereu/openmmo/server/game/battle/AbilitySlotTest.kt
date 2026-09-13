package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.storage.EntityIdService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The ability a battle uses is the one the client's summary shows: the species ability in the
 * monster's stored slot (client gT0.In0), never a pick from the personality. A Miltank that showed
 * Thick Fat used to fire Scrappy because the two sides chose differently.
 */
class AbilitySlotTest :
    FunSpec({
      val species = SpeciesRegistry()
      val factory = WildMonFactory(species, MoveRegistry(), LearnsetRegistry(), EntityIdService())
      val miltank =
          species
              .get(241)!!
              .copy(
                  ability1 = Ability.THICK_FAT,
                  ability2 = Ability.SCRAPPY,
                  hiddenAbility = Ability.SAP_SIPPER,
              )
      // An odd seed: the old personality rule would have picked the second slot for every case below.
      val mon = factory.create(241, 20, BattleRng(seed = 5))!!.copy(seed = 1)

      test("slot 0 is the first ability and slot 1 the second, whatever the personality says") {
        Abilities.of(miltank, mon.copy(abilitySlot = 0)) shouldBe Ability.THICK_FAT
        Abilities.of(miltank, mon.copy(abilitySlot = 1)) shouldBe Ability.SCRAPPY
      }

      test("an empty second slot falls back to the first ability, as the client does") {
        val oneAbility = miltank.copy(ability2 = Ability.NONE)
        Abilities.of(oneAbility, mon.copy(abilitySlot = 1)) shouldBe Ability.THICK_FAT
      }

      test("slot 2 is the hidden ability only when the monster has one") {
        Abilities.of(miltank, mon.copy(abilitySlot = 2, hasHiddenAbility = true)) shouldBe
            Ability.SAP_SIPPER
        Abilities.of(miltank, mon.copy(abilitySlot = 2, hasHiddenAbility = false)) shouldBe
            Ability.THICK_FAT
        Abilities.of(miltank.copy(hiddenAbility = Ability.NONE), mon.copy(abilitySlot = 2, hasHiddenAbility = true)) shouldBe
            Ability.THICK_FAT
      }

      test("wild monsters roll the first or second slot and never the hidden one") {
        val slots = (1..200).map { factory.create(241, 20, BattleRng())!!.abilitySlot }.toSet()
        slots shouldBe setOf(0, 1)
      }
    })
