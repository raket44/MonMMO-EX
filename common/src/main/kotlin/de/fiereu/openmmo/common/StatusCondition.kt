package de.fiereu.openmmo.common

/**
 * A monster's non-volatile status, packed the way the cartridge (and the client) keep it: the low
 * three bits count sleep turns, the rest are one flag each. This byte rides the monster record
 * right after the nickname (client field k91.Vy1, verified: the party cell's status icon and the
 * battle predicates compare it against exactly these values) and the in-battle status event.
 */
object StatusCondition {
  const val NONE = 0
  const val SLEEP_MASK = 0x07
  const val POISON = 0x08
  const val BURN = 0x10
  const val FREEZE = 0x20
  const val PARALYSIS = 0x40
  const val TOXIC = 0x80

  fun isAsleep(status: Int): Boolean = status and SLEEP_MASK != 0

  fun sleepTurns(status: Int): Int = status and SLEEP_MASK

  fun isPoisoned(status: Int): Boolean = status and (POISON or TOXIC) != 0

  fun isBadlyPoisoned(status: Int): Boolean = status and TOXIC != 0

  fun isBurned(status: Int): Boolean = status and BURN != 0

  fun isFrozen(status: Int): Boolean = status and FREEZE != 0

  fun isParalyzed(status: Int): Boolean = status and PARALYSIS != 0

  fun hasAny(status: Int): Boolean = status and 0xFF != 0

  /**
   * Sleep counters. The engine subtracts one on each attempt to move and the monster wakes (and
   * acts) when it reaches zero, so a counter of N is N-1 sleeping turns - the same decrement-
   * then-check as the cartridges and Showdown. Moves roll 2..4 (1-3 sleeping turns, the Gen 5+
   * rule PokeMMO plays by); Gen 3's 2..5 was one turn too long at the top.
   */
  fun asleep(turns: Int): Int = turns.coerceIn(1, 7)
}
