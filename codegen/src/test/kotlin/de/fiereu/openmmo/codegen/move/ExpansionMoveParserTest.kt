package de.fiereu.openmmo.codegen.move

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * The server's move data, checked against moves whose numbers are not going to change.
 *
 * Every failure mode this parser has hit loses data quietly rather than throwing, so the assertions
 * are about values rather than about parsing succeeding.
 */
class ExpansionMoveParserTest :
    FunSpec({
      val root = File("../../pokeemerald-expansion")
      val moves =
          if (root.isDirectory) ExpansionMoveParser(root).parseAll().associateBy { it.id }
          else emptyMap()

      test("every move in the header is parsed") {
        if (moves.isEmpty()) return@test
        val headers =
            Regex("""^ *\[MOVE_[A-Z0-9_]+] *=""", RegexOption.MULTILINE)
                .findAll(File(root, "src/data/moves_info.h").readText())
                .count()
        moves.size shouldBe headers
      }

      test("a Gen 1 move the old table also had is unchanged") {
        if (moves.isEmpty()) return@test
        val tackle = moves.getValue(33)
        tackle.name shouldBe "Tackle"
        tackle.power shouldBe 40
        tackle.accuracy shouldBe 100
        tackle.pp shouldBe 35
        tackle.type shouldBe "PokemonType.NORMAL"
      }

      test("the Gen 6 moves that had no data at all now have their own") {
        if (moves.isEmpty()) return@test
        val moonblast = moves.getValue(585)
        moonblast.name shouldBe "Moonblast"
        moonblast.type shouldBe "PokemonType.FAIRY"
        moonblast.power shouldBe 95
        moonblast.pp shouldBe 15
        moonblast.secondaryEffectChance shouldBe 30

        // Its power is written as a generation ternary, which used to read as zero.
        val flyingPress = moves.getValue(560)
        flyingPress.power shouldBe 100
        flyingPress.accuracy shouldBe 95
        flyingPress.pp shouldBe 10

        val oblivionWing = moves.getValue(613)
        oblivionWing.pp shouldBe 10
        oblivionWing.type shouldBe "PokemonType.FLYING"
      }

      test("priority is read, including the negative and the high ones") {
        if (moves.isEmpty()) return@test
        moves.getValue(98).priority shouldBe 1 // Quick Attack
        moves.getValue(588).priority shouldBe 4 // King's Shield
        moves.values.count { it.priority < 0 } shouldBeGreaterThan 0
      }

      test("no move is left without PP by a silent parse failure") {
        if (moves.isEmpty()) return@test
        // MOVE_NONE is the only entry with nothing to say.
        val missing = moves.values.filter { it.pp == 0 && it.id != 0 }.map { it.name }
        missing shouldContainExactly emptyList()
      }

      test("flags follow the Expansion rather than being assumed") {
        if (moves.isEmpty()) return@test
        // Tackle makes contact; Growl does not, and can be reflected.
        moves.getValue(33).flags shouldContainExactly
            listOf(
                "MoveFlag.MAKES_CONTACT",
                "MoveFlag.PROTECT_AFFECTED",
                "MoveFlag.MIRROR_MOVE_AFFECTED",
                "MoveFlag.KINGS_ROCK_AFFECTED",
            )
        moves.getValue(45).flags.contains("MoveFlag.MAGIC_COAT_AFFECTED") shouldBe true
      }

      test("the damage category is the move's own, not its type's") {
        if (moves.isEmpty()) return@test
        moves.getValue(247).category shouldBe "SPECIAL" // Shadow Ball, a Ghost move
        moves.getValue(242).category shouldBe "PHYSICAL" // Crunch, a Dark move
        moves.getValue(45).category shouldBe "STATUS" // Growl
      }
    })
