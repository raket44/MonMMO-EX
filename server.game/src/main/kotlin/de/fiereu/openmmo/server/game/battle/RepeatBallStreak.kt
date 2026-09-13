package de.fiereu.openmmo.server.game.battle

import java.time.LocalDate

/**
 * PokeMMO's Repeat Ball chain, kept in the character's saved vars. Any catch counts, whatever ball
 * made it: a catch of the chain's species adds a link (up to 15), any other species starts a new
 * chain, and a chain lapses once two whole calendar days (server time) pass without a catch - a
 * Monday catch still counts on Wednesday and is gone on Thursday (project owner, 2026-09-13).
 */
object RepeatBallStreak {
  const val SPECIES_KEY = "monmmo.repeat_ball.species"
  const val COUNT_KEY = "monmmo.repeat_ball.count"
  const val DAY_KEY = "monmmo.repeat_ball.last_catch_day"
  const val MAX_CHAIN = 15

  /** Days that may pass between two catches of one chain. */
  private const val LAPSE_DAYS = 2

  /** The chain a ball thrown at [species] on [today] gets: 0 unless it continues the saved one. */
  fun chainFor(vars: Map<String, Int>, species: Int, today: LocalDate): Int {
    val count = vars[COUNT_KEY] ?: 0
    if (count <= 0 || vars[SPECIES_KEY] != species || lapsed(vars, today)) return 0
    return count.coerceAtMost(MAX_CHAIN)
  }

  /** The vars to save after catching [species] on [today]. */
  fun recordCatch(vars: Map<String, Int>, species: Int, today: LocalDate): Map<String, Int> =
      mapOf(
          SPECIES_KEY to species,
          COUNT_KEY to (chainFor(vars, species, today) + 1).coerceAtMost(MAX_CHAIN),
          DAY_KEY to today.toEpochDay().toInt(),
      )

  private fun lapsed(vars: Map<String, Int>, today: LocalDate): Boolean {
    val last = vars[DAY_KEY] ?: return true
    return today.toEpochDay() - last > LAPSE_DAYS
  }
}
