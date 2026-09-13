package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.items.generated.Items
import de.fiereu.openmmo.pokemon.retail.RetailMonsterData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class EvolutionFamilyTest :
    FunSpec({
      test("a family holds its base form and every stage that grows from it") {
        // evolutions.csv: 668 -> 669 -> 670 by level.
        EvolutionTable.family(670) shouldBe setOf(668, 669, 670)
        EvolutionTable.family(668) shouldBe setOf(668, 669, 670)
        EvolutionTable.sameFamily(668, 670).shouldBeTrue()
        EvolutionTable.sameFamily(668, 671).shouldBeFalse()
        EvolutionTable.familyEvolvesByFriendship(668).shouldBeFalse()
      }

      test("retail families: Moon Stone and friendship evolutions reach every stage") {
        if (!RetailMonsterData.isLoaded()) return@test
        val moonStone = ItemRegistry().idOf(Items.MOON_STONE)
        // Clefairy's family (Cleffa 173 -> Clefairy 35 -> Clefable 36) evolves by Moon Stone.
        EvolutionTable.familyEvolvesByItem(173, moonStone).shouldBeTrue()
        EvolutionTable.familyEvolvesByItem(36, moonStone).shouldBeTrue()
        EvolutionTable.familyEvolvesByItem(25, moonStone).shouldBeFalse()
        // Zubat's family evolves into Crobat by friendship; so does Pichu into Pikachu.
        EvolutionTable.familyEvolvesByFriendship(41).shouldBeTrue()
        EvolutionTable.familyEvolvesByFriendship(26).shouldBeTrue()
        EvolutionTable.familyEvolvesByFriendship(129).shouldBeFalse()
        EvolutionTable.sameFamily(172, 26).shouldBeTrue()
      }
    })
