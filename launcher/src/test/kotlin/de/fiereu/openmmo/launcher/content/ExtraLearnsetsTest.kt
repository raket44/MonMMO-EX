package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.nio.file.Path

class ExtraLearnsetsTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val moveIds = MoveText.ids(root)

      test("teachable moves come through per species") {
        val teachable = ExtraLearnsets.teachable(root, moveIds)
        teachable.size shouldBeGreaterThan 800
        // Bulbasaur can be taught Solar Beam.
        teachable.getValue("BULBASAUR") shouldContain moveIds.getValue("MOVE_SOLAR_BEAM")
      }

      test("egg moves resolve through the species reference") {
        val egg = ExtraLearnsets.eggMoves(root, moveIds)
        egg.size shouldBeGreaterThan 300
        egg.getValue("BULBASAUR") shouldContain moveIds.getValue("MOVE_CURSE")
      }
    })
