package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

/** Reads the real Expansion tree, since the point is that the chart matches that source. */
class TypeChartTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val chart = TypeChart.parse(root)

      test("Fairy attacks the types it should") {
        val fairy = chart.getValue("FAIRY")
        fairy.getValue("FIGHTING") shouldBe 2.0
        fairy.getValue("DRAGON") shouldBe 2.0
        fairy.getValue("DARK") shouldBe 2.0
        fairy.getValue("POISON") shouldBe 0.5
        fairy.getValue("STEEL") shouldBe 0.5
        fairy.getValue("FIRE") shouldBe 0.5
        fairy.getValue("NORMAL") shouldBe 1.0
      }

      test("Fairy defends as it should") {
        chart.getValue("POISON").getValue("FAIRY") shouldBe 2.0
        chart.getValue("STEEL").getValue("FAIRY") shouldBe 2.0
        chart.getValue("FIGHTING").getValue("FAIRY") shouldBe 0.5
        chart.getValue("BUG").getValue("FAIRY") shouldBe 0.5
        chart.getValue("DARK").getValue("FAIRY") shouldBe 0.5
        chart.getValue("DRAGON").getValue("FAIRY") shouldBe 0.0
      }

      test("the rest of the chart still reads correctly") {
        chart.getValue("GHOST").getValue("NORMAL") shouldBe 0.0
        chart.getValue("WATER").getValue("FIRE") shouldBe 2.0
        chart.getValue("ELECTRIC").getValue("GROUND") shouldBe 0.0
        chart.getValue("GRASS").getValue("GRASS") shouldBe 0.5
      }
    })
