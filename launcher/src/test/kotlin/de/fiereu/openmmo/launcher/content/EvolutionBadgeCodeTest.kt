package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** The badge byte of a data evolution, as f/fi7 stores it and f/j67 draws it. */
class EvolutionBadgeCodeTest :
    FunSpec({
      test("badge codes round-trip: none, day, night, then Mega, alpha and omega") {
        listOf(null, "day", "night", "mega", "alpha", "omega").forEachIndexed { code, badge ->
          evolutionTimeCode(badge) shouldBe code
          evolutionTime(code) shouldBe badge
        }
      }
    })
