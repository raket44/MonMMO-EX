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
