package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.pokemon.retail.RetailMonsterData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

private const val TACKLE = 33
private const val GROWL = 45

class LearnsetRegistryTest :
    FunSpec({
      val learnsets = LearnsetRegistry()

      test("every species with stats has a learnset") {
        learnsets.size() shouldBe SpeciesRegistry().size()
      }

      test("a learnset is sorted by level") {
        for (dexId in 1..learnsets.size()) {
          val levels = learnsets.get(dexId).map { it.level }
          levels shouldBe levels.sorted()
        }
      }

      // Bulbasaur's exact levels depend on which list serves (retail, or the Expansion where the
      // retail dump is not on the working directory), so these pin only what both agree on. The
      // walk-and-push-out mechanics are pinned on synthetic learnsets in WildMonFactoryTest.
      test("Bulbasaur learns Tackle and Growl at level 1") {
        learnsets.movesAt(1, 1) shouldBe listOf(TACKLE, GROWL)
      }

      test("a level with no move learns nothing") { learnsets.movesAt(1, 2).shouldBeEmpty() }

      test("the initial moveset stops at the given level") {
        learnsets.initialMoveset(1, 1) shouldBe listOf(TACKLE, GROWL)
      }

      test("a retail species keeps every retail move in order and gains only Expansion moves") {
        // Needs the retail dump on the working directory, as the server has it; a module test run
        // does not, and skips - the rule itself is pinned in RetailPlusAdditionsTest.
        RetailMonsterData.get(1) ?: return@test
        val expansion = ExpansionSpeciesRegistry()
        for (dexId in 1..649) {
          val retail = RetailMonsterData.get(dexId)?.levelUpLearnset.orEmpty()
          if (retail.isEmpty()) continue
          val served = learnsets.get(dexId)
          var matched = 0
          served.forEach { if (matched < retail.size && it == retail[matched]) matched++ }
          matched shouldBe retail.size
          val retailMoves = retail.mapTo(HashSet()) { it.moveId }
          val expansionMoves =
              (expansion.getByServerId(dexId) ?: expansion.getByClientWireId(dexId))
                  ?.levelUpLearnset
                  ?.mapTo(HashSet()) { it.originalMoveId }
                  .orEmpty()
          served.map { it.moveId }.filter { it !in retailMoves && it !in expansionMoves }.shouldBeEmpty()
        }
      }

      test("no moveset is longer than four moves") {
        for (dexId in 1..learnsets.size()) {
          learnsets.initialMoveset(dexId, 100).size shouldBe
              minOf(MAX_MOVE_SLOTS, learnsets.get(dexId).map { it.moveId }.distinct().size)
        }
      }

      test("an unknown species has no learnset") {
        learnsets.get(9999).shouldBeEmpty()
        learnsets.initialMoveset(9999, 50).shouldBeEmpty()
      }

      test("every learned move is in the move registry") {
        val moves = MoveRegistry()
        val learned = (1..learnsets.size()).flatMap { learnsets.get(it) }
        learned.size shouldBeGreaterThan 0
        learned.filter { moves.get(it.moveId) == null }.shouldBeEmpty()
      }
    })
