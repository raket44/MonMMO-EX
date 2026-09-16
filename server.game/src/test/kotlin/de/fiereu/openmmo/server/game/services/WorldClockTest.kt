package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.WorldClock as GameClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WorldClockTest :
    FunSpec({
      // Four in-game hours pass per real hour, from midnight in the world clock zone.
      fun atInGameHour(hour: Int): Long = GameClock.dayStartSecond().toLong() + hour * 3_600L / 4

      test("evolutions read day from the in-game clock: morning and day bands are day, night is not") {
        WorldClock.isDaytime(atInGameHour(4)) shouldBe true
        WorldClock.isDaytime(atInGameHour(12)) shouldBe true
        WorldClock.isDaytime(atInGameHour(20)) shouldBe true
        WorldClock.isDaytime(atInGameHour(21)) shouldBe false
        WorldClock.isDaytime(atInGameHour(2)) shouldBe false
      }

      test("today is the date in the world clock zone, not the server machine's") {
        WorldClock.today() shouldBe java.time.LocalDate.now(GameClock.zone)
      }
    })

// The join anchor is a Sunday midnight in the world zone (the patched client's weekday = real days
// since it mod 7); moving it back by whole days must not shift the in-game hour (2026-09-16).
class WorldClockAnchorTest :
    io.kotest.core.spec.style.FunSpec({
      test("the anchor is a Sunday midnight in the zone and the in-game hour still counts from local midnight") {
        val anchor = java.time.Instant.ofEpochSecond(GameClock.dayStartSecond().toLong()).atZone(GameClock.zone)
        anchor.dayOfWeek shouldBe java.time.DayOfWeek.SUNDAY
        anchor.toLocalTime() shouldBe java.time.LocalTime.MIDNIGHT
        val todayMidnight = GameClock.now().toLocalDate().atStartOfDay(GameClock.zone).toEpochSecond()
        for (h in 0 until 24) WorldClock.inGameHour(todayMidnight + h * 900L) shouldBe h
        ((todayMidnight - anchor.toEpochSecond()) / 86_400L).toInt() shouldBe GameClock.weekday()
      }
    })
