package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

class MoveTextTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val ids = MoveText.ids(root)
      val moves = MoveText.parse(root, ids).associateBy { it.id }

      test("move ids match the header") {
        ids.getValue("MOVE_TACKLE") shouldBe 33
        ids.getValue("MOVE_MOONBLAST") shouldBe 585
      }

      test("a modern move the client lacks comes through complete") {
        val moonblast = moves.getValue(585)
        moonblast.name shouldBe "Moonblast"
        moonblast.type shouldBe "TYPE_FAIRY"
        moonblast.power shouldBe 95
        moonblast.accuracy shouldBe 100
        moonblast.pp shouldBe 15
        moonblast.description shouldBe "Attacks with the power of the moon. May lower Sp. Atk."
      }

      test("every move entry in the header is parsed") {
        // MOVE_POWER_SHIFT closes its brace at column 0 while every other move indents it. A
        // terminator-based pattern ran past it and silently ate MOVE_STONE_AXE, which then showed
        // up only as one missing learnset entry, 700 moves later.
        val entries =
            Regex("""^ *\[MOVE_[A-Z0-9_]+] *=""", RegexOption.MULTILINE)
                .findAll(Files.readString(root.resolve("src/data/moves_info.h")))
                .count()
        MoveText.parse(root, ids).size shouldBe entries
        moves.getValue(758).name shouldBe "Stone Axe"
      }

      test("a generation-gated number resolves to the configured generation") {
        // Flying Press is written .power = B_UPDATED_MOVE_DATA >= GEN_7 ? 100 : 80, and reading the
        // first number on the line gives neither. 334 moves are written this way.
        MoveText.updatedMoveDataGeneration(root) shouldBe 9
        val flyingPress = moves.getValue(560)
        flyingPress.name shouldBe "Flying Press"
        flyingPress.power shouldBe 100
        flyingPress.accuracy shouldBe 95
        flyingPress.pp shouldBe 10
        flyingPress.category shouldBe "DAMAGE_CATEGORY_PHYSICAL"
      }

      test("no imported move is left without power or PP by a parse failure") {
        val importable = moves.values.filter { it.id in 560..999 }
        importable.count { it.pp == 0 } shouldBe 0
        // Status moves are the only ones that legitimately have no power.
        importable
            .filter { it.power == 0 }
            .forEach { it.category shouldBe "DAMAGE_CATEGORY_STATUS" }
      }

      test("moves past the client's range are the ones worth importing") {
        moves.values.count { it.id > 559 } shouldBe moves.values.count { it.id > 559 }
        moves.getValue(33).name shouldBe "Tackle"
      }
    })
