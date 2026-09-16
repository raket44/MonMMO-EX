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
   * The anchor the client counts from: midnight of the most recent Sunday in [zone]. In-game
   * midnight still falls every six real hours from a local midnight (whole days are multiples of
   * the six-hour in-game day, so the hour and the day/night bands are untouched), and the
   * MonMMO-EX client's menu-header weekday (f/nb4.D60, patched) is real days since this anchor
   * mod 7 - the calendar weekday in this zone, which is what the owner wants shown. Retail's
   * D60 divides the 4x in-game counter instead, so an unpatched client shows a weekday that
   * changes every six hours.
   */
  fun dayStartSecond(): Int {
    val today = now().toLocalDate()
    val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
    return sunday.atStartOfDay(zone).toEpochSecond().toInt()
  }

  /** Sunday 0 .. Saturday 6, the client's weekday numbering, for the calendar day in [zone]. */
  fun weekday(): Int = now().dayOfWeek.value % 7

  fun nowSecond(): Int = (System.currentTimeMillis() / 1000).toInt()
}
