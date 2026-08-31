package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs against the installed client's own type enum. The names in it are obfuscated and change
 * between builds, so the patch reads them back rather than assuming them; this proves that works.
 */
class FairyTypePatchTest :
    FunSpec({
      val classFile =
          Path.of(
              System.getProperty("user.home"),
              "AppData/Local/Temp/claude/C--Users-raket-AppData-Local-OpenMMO",
              "09f0899a-8f87-44cd-bcd3-eb800d3ab989/scratchpad/cls/UP_rK1.class")

      test("the client's type enum is read back correctly").config(
          enabled = Files.isRegularFile(classFile)) {
            val shape = FairyTypePatch.inspect(Files.readAllBytes(classFile))

            shape.constants.map { it.first } shouldBe
                listOf(
                    "NORMAL",
                    "FIGHTING",
                    "FLYING",
                    "POISON",
                    "GROUND",
                    "ROCK",
                    "BUG",
                    "GHOST",
                    "STEEL",
                    "QUESTIONQUESTIONQUESTION",
                    "FIRE",
                    "WATER",
                    "GRASS",
                    "ELECTRIC",
                    "PSYCHIC",
                    "ICE",
                    "DRAGON",
                    "DARK",
                    "NONE")
            // Every ordinal doubles as the wire byte, which is why Fairy can simply become 19.
            shape.ordinalOf("DARK") shouldBe 17
            shape.ordinalOf("NONE") shouldBe 18
            // The table is one slot per real type, so it has to widen before Fairy can index it.
            shape.tableSize shouldBe 18
            shape.valuesField.shouldNotBeEmpty()
            shape.effectivenessMethod.shouldNotBeEmpty()
          }
    })
