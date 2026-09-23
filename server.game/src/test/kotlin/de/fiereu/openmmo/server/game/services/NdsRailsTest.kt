package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The rail graph against what the live client did on Skyarrow Bridge (2026-09-23 logs): from the
 * Castelia gate it arrived on line 12 at x 4, walked to x 0 and jumped to (35, 0) - line 5, which
 * ends at line 12's from-point; from the Pinwheel gate it arrived on line 13 at x 0, walked to x 7
 * and jumped to (0, 0) - line 0, which starts at line 13's to-point. Both were the moments the
 * old "any row on the tile" pick warped the player across the bridge.
 */
class NdsRailsTest :
    FunSpec({
      val rails = NdsRails()
      val skyarrow = rails.areaOf(2, 249, 0).shouldNotBeNull()

      test("Skyarrow is a rail map with the 20 lines the ROM draws, Castelia's museum is not") {
        skyarrow.lines.size shouldBe 20
        rails.line(skyarrow, 13)!!.length shouldBe 8
        rails.line(skyarrow, 5)!!.length shouldBe 36
        rails.isRailMap(2, 17, 0) shouldBe false
      }

      test("walking off line 12's start lands on line 5 at its far end") {
        rails.transition(skyarrow, 12, lastX = 0, newX = 35)!!.id shouldBe 5
      }

      test("walking off line 13's end lands on line 0 at its start") {
        rails.transition(skyarrow, 13, lastX = 7, newX = 0)!!.id shouldBe 0
      }

      test("a jump that fits no neighbouring line is refused rather than guessed") {
        rails.transition(skyarrow, 13, lastX = 7, newX = 20).shouldBeNull()
      }

      // Live moves: Skyarrow's line 13 walked UP as x 0->3 and LEFT as y 0->-3; Castelia's
      // streets walked LEFT as x 6->27 and UP as y 0->6. The Pokecenter exit on Castelia's line 3
      // lands facing DOWN and its one-step walk-off goes y-1, (8,3) -> (8,2).
      test("a screen direction moves along the axis the line's mode says") {
        val bridgeEnd = rails.line(skyarrow, 13)!!
        bridgeEnd.mode shouldBe 1
        rails.delta(bridgeEnd, Direction.UP) shouldBe (1 to 0)
        rails.delta(bridgeEnd, Direction.LEFT) shouldBe (0 to -1)
        val street = rails.line(rails.areaOf(2, 28, 0)!!, 3)!!
        street.mode shouldBe 4
        rails.delta(street, Direction.LEFT) shouldBe (1 to 0)
        rails.delta(street, Direction.UP) shouldBe (0 to 1)
        rails.delta(street, Direction.DOWN) shouldBe (0 to -1)
      }

      test("Castelia's streets chain through their shared points") {
        val castelia = rails.areaOf(2, 28, 0).shouldNotBeNull()
        castelia.lines.size shouldBe 6
        // Line 2 runs point 2 -> 3 (30 long); line 3 runs 3 -> 4. Off the end of 2 is the start of 3.
        rails.transition(castelia, 2, lastX = 29, newX = 0)!!.id shouldBe 3
        // Back off the start of line 2 at point 2: lines 0 (2 long) and 1 (5 long) both end there,
        // told apart by where the client says it is.
        rails.transition(castelia, 2, lastX = 0, newX = 1)!!.id shouldBe 0
        rails.transition(castelia, 2, lastX = 0, newX = 4)!!.id shouldBe 1
      }
    })
