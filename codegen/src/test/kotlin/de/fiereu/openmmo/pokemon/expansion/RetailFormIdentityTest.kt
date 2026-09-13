package de.fiereu.openmmo.pokemon.expansion

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.retail.RetailForms
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/** One identity per form: a form the client owns is its species + form number, never a copy. */
class RetailFormIdentityTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()

      fun serverIdOf(symbol: String) = registry.get(symbol)!!.serverId

      test("an Expansion copy of a retail form normalizes to the species and the client's form number") {
        RetailFormIdentity.normalize(serverIdOf("SPECIES_UNOWN_B"), 0) shouldBe (201 to 1)
        RetailFormIdentity.normalize(serverIdOf("SPECIES_ROTOM_HEAT"), 0) shouldBe (479 to 1)
        RetailFormIdentity.normalize(serverIdOf("SPECIES_DEOXYS_SPEED"), 0) shouldBe (386 to 3)
      }

      test("everything else passes through unchanged") {
        RetailFormIdentity.normalize(201, 5) shouldBe (201 to 5)
        RetailFormIdentity.normalize(serverIdOf("SPECIES_CHARIZARD_MEGA_X"), 0) shouldBe
            (serverIdOf("SPECIES_CHARIZARD_MEGA_X") to 0)
      }

      test("a retail form with its own data has a record; appearance forms and costumes do not") {
        RetailForms.recordOf(479, 1) shouldBe 657
        RetailForms.recordOf(386, 3) shouldBe 652
        RetailForms.recordOf(201, 1).shouldBeNull()
        // Royal Charmander is a costume on Charmander's own record.
        RetailForms.recordOf(4, 1).shouldBeNull()
      }

      test("a monster's definition falls back to its species when no form record resolves") {
        val rotomHeat =
            Pokemon(
                id = 1,
                ownerId = 0,
                container = PokemonContainer.PARTY,
                containerSlot = 0,
                dexId = 479,
                seed = 0,
                ot = "",
                nickname = "",
                level = 5,
                hp = 10,
                xp = 0,
                eVs = EVs(),
                iVs = IVs(),
                moves = emptyList(),
                isShiny = false,
                hasHiddenAbility = false,
                isAlpha = false,
                isSecret = false,
                isFatefulEncounter = false,
                isRaidEncounter = false,
                caughtAt = LocalDateTime.now(),
                form = 1,
            )
        SpeciesRegistry().forMonster(rotomHeat).shouldNotBeNull()
      }
    })
