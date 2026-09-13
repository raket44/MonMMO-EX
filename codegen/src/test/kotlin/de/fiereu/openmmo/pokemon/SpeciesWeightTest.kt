package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.pokemon.retail.RetailMonsterData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private fun monster(dexId: Int, form: Int) =
    Pokemon(
        id = 1,
        ownerId = 0,
        container = PokemonContainer.PARTY,
        containerSlot = 0,
        dexId = dexId,
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
        form = form,
    )

class SpeciesWeightTest :
    FunSpec({
      val registry = SpeciesRegistry()

      test("retail species carry their dex weight in hectograms") {
        registry.get(25)!!.weight shouldBe 60
        registry.get(143)!!.weight shouldBe 4600
      }

      test("a species retail never had takes the Expansion's weight") {
        val toxapex = ExpansionSpeciesRegistry().get("SPECIES_TOXAPEX")!!
        registry.get(toxapex.serverId)!!.weight shouldBe toxapex.weight
      }

      test("every retail species with a dump weight uses it") {
        if (!RetailMonsterData.isLoaded()) return@test
        for (id in 1..649) {
          val retail = RetailMonsterData.get(id)?.weight?.takeIf { it > 0 } ?: continue
          registry.get(id)!!.weight shouldBe retail
        }
      }

      test("a form with its own record weighs what that record says") {
        registry.forMonster(monster(487, 0))!!.weight shouldBe 7500
        // The form records (655-667) come from the dump; without it a form falls back to its species.
        if (!RetailMonsterData.isLoaded()) return@test
        registry.forMonster(monster(487, 1))!!.weight shouldBe 6500
        registry.forMonster(monster(492, 1))!!.weight shouldBe 52
      }
    })
