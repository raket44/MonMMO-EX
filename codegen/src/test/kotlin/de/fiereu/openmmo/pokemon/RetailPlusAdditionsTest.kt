package de.fiereu.openmmo.pokemon

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Pins retail plus additions, the rule both the client build and the server learnsets follow. */
class RetailPlusAdditionsTest :
    FunSpec({
      fun moves(vararg pairs: Pair<Int, Int>) = pairs.map { (level, move) -> LevelUpMove(level, move) }

      fun merge(retail: List<LevelUpMove>, expansion: List<LevelUpMove>, limit: Int = Int.MAX_VALUE) =
          RetailPlusAdditions.levelUp(retail, expansion, { it.moveId }, { it.level }, limit)

      test("retail entries all stay in retail's order, duplicates included") {
        val retail = moves(1 to 33, 1 to 45, 3 to 22, 3 to 45, 9 to 73)

        merge(retail, emptyList()) shouldBe retail
        merge(retail, moves(1 to 45, 7 to 73, 13 to 22)) shouldBe retail
      }

      test("a move retail lacks is slotted in after the last retail entry at or below its level") {
        val retail = moves(1 to 33, 1 to 45, 9 to 73, 14 to 77)

        merge(retail, moves(1 to 574, 10 to 585, 50 to 76)) shouldBe
            moves(1 to 33, 1 to 45, 1 to 574, 9 to 73, 10 to 585, 14 to 77, 50 to 76)
      }

      test("an addition is added once even when the Expansion lists it at two levels") {
        merge(moves(1 to 33), moves(5 to 574, 20 to 574)) shouldBe moves(1 to 33, 5 to 574)
      }

      test("a limit drops additions, never retail entries") {
        val retail = moves(1 to 33, 4 to 45)

        merge(retail, moves(2 to 574, 3 to 585), limit = 3) shouldBe moves(1 to 33, 2 to 574, 4 to 45)
        merge(retail, moves(2 to 574), limit = 1) shouldBe retail
      }
    })
