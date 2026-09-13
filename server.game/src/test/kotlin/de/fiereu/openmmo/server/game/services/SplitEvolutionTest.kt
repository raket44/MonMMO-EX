package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Region-gated split evolutions happen at night (project owner, 2026-09-13), and a regional form
 * counts toward its base species' National entry. Pinned against the staged evolution table.
 */
class SplitEvolutionTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()

      fun wire(symbol: String) = registry.get(symbol)!!.clientWireId!!

      fun level(level: Int, daytime: Boolean) =
          EvolutionTable.LevelContext(
              level = level,
              attack = 10,
              defense = 10,
              seed = 0,
              female = false,
              heldItem = 0,
              daytime = daytime,
          )

      val thunderStone = 5083

      // The retail branches (Raichu, Marowak) come from data/pokemmo/monsters.json, which a module
      // test run does not have on its working directory (see LearnsetRegistryTest); the night
      // branches come from the staged table on the classpath and are always checked.
      val retailDump = de.fiereu.openmmo.pokemon.retail.RetailMonsterData.get(25) != null

      test("a Thunder Stone makes Pikachu an Alolan Raichu at night and never by day") {
        EvolutionTable.itemEvolution(25, thunderStone, daytime = false) shouldBe wire("SPECIES_RAICHU_ALOLA")
        EvolutionTable.itemEvolution(25, thunderStone, daytime = true) shouldNotBe wire("SPECIES_RAICHU_ALOLA")
        if (retailDump) EvolutionTable.itemEvolution(25, thunderStone, daytime = true) shouldBe 26
      }

      test("Cubone at level 28 becomes an Alolan Marowak at night and never by day") {
        EvolutionTable.levelEvolution(104, level(28, daytime = false)) shouldBe wire("SPECIES_MAROWAK_ALOLA")
        EvolutionTable.levelEvolution(104, level(28, daytime = true)) shouldNotBe wire("SPECIES_MAROWAK_ALOLA")
        if (retailDump) EvolutionTable.levelEvolution(104, level(28, daytime = true)) shouldBe 105
      }

      test("a regional form marks its base species in the dex tiers") {
        DexProgressService.withBaseSpecies(listOf(wire("SPECIES_VULPIX_ALOLA"))) shouldContainAll
            listOf(wire("SPECIES_VULPIX_ALOLA"), 37)
      }
    })
