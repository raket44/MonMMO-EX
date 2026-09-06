package de.fiereu.openmmo.net.game.packets

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

  /**
   * The anchor the client counts from. Its weekday is the in-game one - (in-game seconds %
   * 604800) / 86400, 0 Sunday .. 6 Saturday (f/gz) - which advances every six real hours, so
   * it cannot track the real calendar. The anchor is placed so the weekday is the real one at
   * the moment of joining: today's midnight moved back by whole six-hour steps (a multiple of
   * one in-game day, so the hour is untouched) until the in-game weekday equals today's.
   */
  fun dayStartSecond(): Int {
    val now = now()
    val midnight = now.toLocalDate().atStartOfDay(zone).toEpochSecond()
    val slot = ((now.toEpochSecond() - midnight) / REAL_SECONDS_PER_GAME_DAY).toInt()
    val weekday = now.dayOfWeek.value % 7 // Sunday 0 .. Saturday 6, the client's numbering
    val steps = Math.floorMod(weekday - slot, 7)
    return (midnight - steps * REAL_SECONDS_PER_GAME_DAY).toInt()
  }

  /** Six real hours: one in-game day at the client's 4x rate. */
  private const val REAL_SECONDS_PER_GAME_DAY = 21_600L

  fun nowSecond(): Int = (System.currentTimeMillis() / 1000).toInt()
}
