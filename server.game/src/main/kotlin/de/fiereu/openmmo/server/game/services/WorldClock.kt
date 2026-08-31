package de.fiereu.openmmo.server.game.services

import java.time.LocalDateTime
import java.time.temporal.WeekFields

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
 * Real-world time driving encounters, PokeMMO-style: seasons rotate weekly through the Unova cycle
 * rather than tracking real months, so a player sees all four within a month, and the day is split
 * into morning, day and night bands on local server time.
 */
object WorldClock {

  fun season(now: LocalDateTime = LocalDateTime.now()): Season {
    val week = now.get(WeekFields.ISO.weekOfWeekBasedYear())
    return Season.entries[week % Season.entries.size]
  }

  fun timeOfDay(now: LocalDateTime = LocalDateTime.now()): TimeOfDay =
      when (now.hour) {
        in 4..10 -> TimeOfDay.MORNING
        in 11..20 -> TimeOfDay.DAY
        else -> TimeOfDay.NIGHT
      }
}
