package de.fiereu.openmmo.server.game.battle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

private const val MAGIKARP = 129
private const val PIKACHU = 25

class RepeatBallStreakTest :
    FunSpec({
      val monday = LocalDate.of(2026, 9, 14)

      test("a player with no catches has no chain") {
        RepeatBallStreak.chainFor(emptyMap(), MAGIKARP, monday) shouldBe 0
      }

      test("each catch of the same species adds a link") {
        var vars = RepeatBallStreak.recordCatch(emptyMap(), MAGIKARP, monday)
        vars[RepeatBallStreak.COUNT_KEY] shouldBe 1
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday) shouldBe 1
        vars = RepeatBallStreak.recordCatch(vars, MAGIKARP, monday)
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday) shouldBe 2
      }

      test("the chain only applies to its own species") {
        val vars = RepeatBallStreak.recordCatch(emptyMap(), MAGIKARP, monday)
        RepeatBallStreak.chainFor(vars, PIKACHU, monday) shouldBe 0
      }

      test("catching a different species starts a new chain") {
        var vars = emptyMap<String, Int>()
        repeat(5) { vars = RepeatBallStreak.recordCatch(vars, MAGIKARP, monday) }
        vars = RepeatBallStreak.recordCatch(vars, PIKACHU, monday)
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday) shouldBe 0
        RepeatBallStreak.chainFor(vars, PIKACHU, monday) shouldBe 1
      }

      test("the chain stops growing at 15") {
        var vars = emptyMap<String, Int>()
        repeat(20) { vars = RepeatBallStreak.recordCatch(vars, MAGIKARP, monday) }
        vars[RepeatBallStreak.COUNT_KEY] shouldBe 15
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday) shouldBe 15
      }

      test("a chain survives two days without a catch and lapses on the third") {
        val vars = RepeatBallStreak.recordCatch(emptyMap(), MAGIKARP, monday)
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday.plusDays(1)) shouldBe 1
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday.plusDays(2)) shouldBe 1
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday.plusDays(3)) shouldBe 0
        // A catch after the lapse starts over rather than continuing.
        RepeatBallStreak.recordCatch(vars, MAGIKARP, monday.plusDays(3))[RepeatBallStreak.COUNT_KEY] shouldBe 1
      }

      test("a catch within the window refreshes the day") {
        var vars = RepeatBallStreak.recordCatch(emptyMap(), MAGIKARP, monday)
        vars = RepeatBallStreak.recordCatch(vars, MAGIKARP, monday.plusDays(2))
        RepeatBallStreak.chainFor(vars, MAGIKARP, monday.plusDays(4)) shouldBe 2
      }
    })
