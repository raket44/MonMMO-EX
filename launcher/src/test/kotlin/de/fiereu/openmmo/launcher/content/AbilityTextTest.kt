package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

class AbilityTextTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val ids = AbilityText.ids(root)
      val abilities = AbilityText.parse(root, ids).associateBy { it.id }

      test("ability ids match the header, including generation boundaries") {
        ids.getValue("ABILITY_OVERGROW") shouldBe 65
        ids.getValue("ABILITY_FAIRY_AURA") shouldBe 187
        // Declared as = ABILITIES_COUNT_GEN3 rather than a literal.
        ids.getValue("ABILITY_TANGLED_FEET") shouldBe 77
      }

      test("names and descriptions come through for the abilities we ship") {
        abilities.getValue(187).name shouldBe "Fairy Aura"
        abilities.getValue(187).description shouldBe "Boosts Fairy moves."
        abilities.getValue(65).name shouldBe "Overgrow"
      }

      test("every ability has a name, so no species falls back to a placeholder") {
        abilities.values.count { it.name.isBlank() } shouldBe 0
      }
    })
