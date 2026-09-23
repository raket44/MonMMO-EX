package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldBeIn
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

      // The three in-place / wrong-way walk-offs the owner showed 2026-09-23, each a landing
      // cell with exactly one open neighbour in the rail grid: the pier's (0,1) on line 1 (its
      // whole first column blocked), Skyarrow's gate end (4,0) on line 12 (columns 4-5 blocked),
      // and 40:0's second door at (2,-6) on Castelia's line 2 (the building on three sides).
      test("a landing's one open neighbour in the rail grid is the walk-off") {
        fun openSides(bank: Int, lineId: Int, x: Int, y: Int): List<Direction> {
          val area = rails.areaOf(2, bank, 0)!!
          val line = rails.line(area, lineId)!!
          return Direction.entries.filter { d ->
            val (dx, dy) = rails.delta(line, d)
            !rails.blocked(area, lineId, x + dx, y + dy)
          }
        }
        openSides(36, 1, 0, 1) shouldBe listOf(Direction.DOWN) // mode 3: x+1 -> (1,1)
        openSides(249, 12, 4, 0) shouldBe listOf(Direction.DOWN) // mode 1: x-1 -> (3,0)
        openSides(28, 2, 2, -6) shouldBe listOf(Direction.UP) // mode 4: y+1 -> (2,-5)
      }

      // The Castelia plaza is a hybrid: rail spokes around a square of plain tiles. Two logged
      // walks off line 0 onto the tiles (cell (0,-2) then tile x 6; cell (1,-5) then tile x 9)
      // fix the lateral sign; a report of (6,18) fits no plaza line (none is wider than 11), so
      // it is the tiles; and a step from a tile back onto a spoke's cell finds that spoke.
      test("the plaza's cells sit where the logged walk-offs say, and tiles are told from rails") {
        val plaza = rails.areaOf(2, 30, 0).shouldNotBeNull()
        val (ax, _) = rails.world(plaza, 0, 0, -2)!!
        val (bx, _) = rails.world(plaza, 0, 1, -5)!!
        kotlin.math.round(ax).toInt() shouldBe 6
        kotlin.math.round(bx).toInt() shouldBe 9
        rails.transition(plaza, 0, lastX = 0, newX = 6, lastY = -2, newY = 18).shouldBeNull()
        // Line 0 runs from point 12 (4.5, 20.5) and the stub line 11 ends there: a tile just
        // north-east of that point steps onto the west spoke - whichever of the two the cell fits.
        rails.enterFromTiles(plaza, tileX = 6, tileY = 19, x = 0, y = -2)!!.first.id shouldBeIn listOf(0, 11)
      }

      // The west spoke as the client walked it (12:14, 2026-09-23): line 0 x 0,1,2 then a reset
      // to 0 - line 13 - then 0..3 and a reset - line 1, where the gym-street box sits at x 4..6.
      // The second spoke's stub (line 2) is two cells: its reset 1 -> 0 is invisible, but the
      // next reports x 2, 3... which line 2 cannot hold, so x 3 puts the player on line 14.
      test("the plaza's spokes are followed segment by segment, resets seen or repaired") {
        val plaza = rails.areaOf(2, 30, 0).shouldNotBeNull()
        rails.transition(plaza, 0, lastX = 2, newX = 0, lastY = 0, newY = 0)!!.id shouldBe 13
        rails.transition(plaza, 13, lastX = 3, newX = 0, lastY = 0, newY = 0)!!.id shouldBe 1
        rails.repair(plaza, 2, x = 2).shouldBeNull() // one past the end: still line 2
        rails.repair(plaza, 2, x = 3)!!.id shouldBe 14
        rails.repair(plaza, 14, x = 7)!!.id shouldBe 3
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
