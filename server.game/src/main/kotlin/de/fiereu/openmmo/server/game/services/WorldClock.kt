package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.Season as ClientSeason
import de.fiereu.openmmo.net.game.packets.WorldClock as GameClock

/** The season axis retail encounter data varies over. */
enum class Season {
  SPRING,
  SUMMER,
  AUTUMN,
  WINTER;

  /** The label the retail tables use, e.g. "Spring"; "Any" matches every season. */
  val label: String = name.lowercase().replaceFirstChar(Char::uppercase)
}

/** The time-of-day axis; each retail location entry carries one rarity per band. */
enum class TimeOfDay {
  MORNING,
  DAY,
  NIGHT
}

/**
 * The season and time of day encounters roll against: the SAME ones the player sees. The season
 * is the one the season packet sends (the real calendar in the world clock zone), and the time
 * band comes from the in-game clock the client runs - four in-game days per real day from the
 * anchor in the join response - banded the way retail does it: morning 04-10, day 11-20, night
 * 21-03 in-game hours. Rolling on the real server hour put morning encounters into in-game night.
 */
object WorldClock {

  fun season(): Season = Season.entries.first { it.name == ClientSeason.current().name }

  /** The in-game hour, 0-23, exactly as the client computes it (f/u2.GW). */
  fun inGameHour(nowSecond: Long = GameClock.nowSecond().toLong()): Int {
    val gameSeconds = (nowSecond - GameClock.dayStartSecond()) * 4
    return (Math.floorMod(gameSeconds, 86_400L) / 3_600L).toInt()
  }

  fun timeOfDay(nowSecond: Long = GameClock.nowSecond().toLong()): TimeOfDay =
      when (inGameHour(nowSecond)) {
        in 4..10 -> TimeOfDay.MORNING
        in 11..20 -> TimeOfDay.DAY
        else -> TimeOfDay.NIGHT
      }

  /**
   * Day for day/night evolutions: the in-game clock the player sees, morning and day bands
   * (project owner, 2026-09-14). It was the server's own UTC hour, 6:00-17:59.
   */
  fun isDaytime(nowSecond: Long = GameClock.nowSecond().toLong()): Boolean = timeOfDay(nowSecond) != TimeOfDay.NIGHT

  /**
   * Today's real date in the world clock zone (the operator's, America/Chicago), for daily resets
   * and weekdays. The server machine runs on UTC, so LocalDate.now() rolled over at 7 PM Texas time.
   */
  fun today(): java.time.LocalDate = GameClock.now().toLocalDate()
}
