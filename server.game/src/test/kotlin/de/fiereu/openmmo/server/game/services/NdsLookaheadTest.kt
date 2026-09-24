package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** The pure ray rule behind the player's line-of-sight stops. */
class NdsLookaheadTest :
    FunSpec({
      val open = { _: Int, _: Int -> true }

      test("a trigger row ahead: the stop is the tile past the row, the row itself is never walled") {
        val row = (5..9).map { it to 10 }.toSet()
        val stop = NdsLookahead.stopAhead(7, 13, Direction.UP, row, emptySet(), open)!!
        stop.stopX to stop.stopY shouldBe (7 to 10)
        stop.wallX to stop.wallY shouldBe (7 to 9)
        stop.distance shouldBe 3
        ((stop.wallX to stop.wallY) in row) shouldBe false
      }

      test("a two-deep trigger box is crossed as one stop") {
        val box = (5..9).flatMap { x -> listOf(x to 10, x to 9) }.toSet()
        val stop = NdsLookahead.stopAhead(7, 12, Direction.UP, box, emptySet(), open)!!
        stop.wallX to stop.wallY shouldBe (7 to 8)
      }

      test("a trainer's sight line met head on: the stop is the next tile down the line") {
        val line = setOf(9 to 5, 8 to 5, 7 to 5)
        val stop = NdsLookahead.stopAhead(4, 5, Direction.RIGHT, emptySet(), line, open)!!
        stop.stopX to stop.stopY shouldBe (7 to 5)
        stop.wallX to stop.wallY shouldBe (8 to 5)
        stop.kind shouldBe "sight line"
      }

      test("a sight line crossed sideways: the stop is the tile beyond the line") {
        val line = setOf(9 to 5, 8 to 5, 7 to 5)
        val stop = NdsLookahead.stopAhead(8, 8, Direction.UP, emptySet(), line, open)!!
        stop.wallX to stop.wallY shouldBe (8 to 4)
      }

      test("nothing armed on the ray, or beyond range, gives no stop") {
        val row = (5..9).map { it to 10 }.toSet()
        NdsLookahead.stopAhead(7, 13, Direction.DOWN, row, emptySet(), open).shouldBeNull()
        NdsLookahead.stopAhead(7, 20, Direction.UP, row, emptySet(), open).shouldBeNull()
      }

      test("a wall tile that the map already blocks, or a blocked ray, gives no stop") {
        val row = (5..9).map { it to 10 }.toSet()
        NdsLookahead.stopAhead(7, 13, Direction.UP, row, emptySet()) { x, y -> !(x == 7 && y == 9) }.shouldBeNull()
        NdsLookahead.stopAhead(7, 13, Direction.UP, row, emptySet()) { x, y -> !(x == 7 && y == 12) }.shouldBeNull()
      }
    })
