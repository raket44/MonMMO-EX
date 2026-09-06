package de.fiereu.openmmo.net.game.packets

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The real-world clock the game world is anchored to. The client (f/u2.Ei1) runs its in-game
 * clock as (serverNow - serverDayStart) * 4 from the two seconds in the join response, so an
 * in-game day is six real hours and four in-game days pass per real day, with in-game midnight
 * at 00:00, 06:00, 12:00 and 18:00 of this zone. The Gen 5 season rotates by calendar month in
 * the same zone. Default America/Chicago (the operator's), override with -Dmonmmo.timeZone.
 */
object WorldClock {
  val zone: ZoneId =
      System.getProperty("monmmo.timeZone")?.let { runCatching { ZoneId.of(it) }.getOrNull() }
          ?: ZoneId.of("America/Chicago")

  fun now(): ZonedDateTime = ZonedDateTime.now(zone)

  /** Epoch second of today's midnight in [zone]. */
  fun dayStartSecond(): Int = LocalDate.now(zone).atStartOfDay(zone).toEpochSecond().toInt()

  fun nowSecond(): Int = (System.currentTimeMillis() / 1000).toInt()
}
