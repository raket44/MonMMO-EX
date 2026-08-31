package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Base stats are not always plain numbers in the Expansion: Jigglypuff writes `.baseSpAttack =
 * P_UPDATED_STATS >= GEN_2 ? 45 : 25`. Reading past that produced a zero rather than an error,
 * which is invisible until a Pokemon has no Special Attack.
 */
class ExpansionStatsTest :
    FunSpec({
      val all = ExpansionSpeciesRegistry().all()

      test("no species has a base stat of zero") {
        val broken =
            all.filter { entry ->
                  listOf(
                          entry.baseHp,
                          entry.baseAttack,
                          entry.baseDefense,
                          entry.baseSpeed,
                          entry.baseSpAttack,
                          entry.baseSpDefense,
                      )
                      .any { it == 0 }
                }
                .map { it.symbol }
        broken shouldContainExactly emptyList()
      }

      test("the generation-gated stats resolve to the modern value") {
        all.single { it.symbol == "SPECIES_JIGGLYPUFF" }.baseSpAttack shouldBe 45
        all.single { it.symbol == "SPECIES_CLEFAIRY" }.baseSpDefense shouldBe 65
      }

      test("the Gen 6 retypes carry Fairy, which the client's own species data cannot") {
        val jigglypuff = all.single { it.symbol == "SPECIES_JIGGLYPUFF" }
        jigglypuff.typeSymbols shouldContainExactly listOf("TYPE_NORMAL", "TYPE_FAIRY")
        all.single { it.symbol == "SPECIES_MAWILE" }.typeSymbols shouldContainExactly
            listOf("TYPE_STEEL", "TYPE_FAIRY")
      }
    })
