package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.items.generated.Items

/** Where the wild monster was met; the Dive, Dusk and Lure Balls read it. */
data class EncounterContext(
    val surfing: Boolean = false,
    /** A cave map: GBA underground maps, DS cave-floor tiles. */
    val cave: Boolean = false,
    /** Always false until rods exist (EncounterService's fishing TODO), so the Lure Ball stays 1x. */
    val fishing: Boolean = false,
)

/**
 * PokeMMO's ball and status catch bonuses, in hundredths (150 = 1.5x), fed into the Gen 3 catch
 * roll PokeMMO keeps. Ball values follow the PokeMMO wiki (pokemmo.shoutwiki.com, one page per
 * ball) and the PokeMMO catch calculators, read 2026-09-13; the Great, Dive and Friend Balls come
 * from the calculators alone (their wiki pages were unreadable), and the status bonuses are
 * unconfirmed (project owner's call). Sport and Park Balls have no known PokeMMO rule and stay 1x.
 * The Master Ball never rolls, so it is not handled here.
 */
object CatchModifiers {

  data class BallContext(
      /** The battle turn the ball is thrown on, 1 for the first. */
      val turn: Int = 1,
      val targetLevel: Int = 1,
      /** The player's active monster. */
      val userLevel: Int = 1,
      /** The target's own typing, unchanged by Transform and the like. */
      val targetTypes: Set<PokemonType> = emptySet(),
      val targetBaseSpeed: Int = 0,
      /** Hectograms. */
      val targetWeight: Int = 0,
      /** Client gender codes: 0 male, 1 female, -1 genderless. */
      val targetGender: Int = -1,
      val userGender: Int = -1,
      val sameEvolutionFamily: Boolean = false,
      val familyEvolvesByMoonStone: Boolean = false,
      val familyEvolvesByFriendship: Boolean = false,
      val night: Boolean = false,
      val encounter: EncounterContext = EncounterContext(),
      /** Consecutive turns the target has been asleep, 0 while awake. */
      val targetSleepTurns: Int = 0,
      /** The player's Repeat Ball chain for the target's species (RepeatBallStreak). */
      val repeatChain: Int = 0,
  )

  fun ballRate(ball: ItemDef, c: BallContext): Int =
      when (ball) {
        Items.GREAT_BALL, Items.SAFARI_BALL, Items.PREMIER_BALL -> 150
        Items.ULTRA_BALL, Items.CHERISH_BALL -> 200
        Items.HEAL_BALL -> 125
        Items.NET_BALL ->
            if (PokemonType.WATER in c.targetTypes || PokemonType.BUG in c.targetTypes) 350 else 100
        Items.DIVE_BALL -> if (c.encounter.surfing) 350 else 100
        Items.NEST_BALL -> nestBallRate(c.targetLevel)
        Items.TIMER_BALL -> timerBallRate(c.turn)
        Items.QUICK_BALL -> if (c.turn <= 1) 500 else 100
        Items.DUSK_BALL -> if (c.night || c.encounter.cave) 250 else 100
        Items.REPEAT_BALL -> repeatBallRate(c.repeatChain)
        Items.DREAM_BALL -> dreamBallRate(c.targetSleepTurns)
        Items.HEAVY_BALL -> WeightMechanics.heavyBallRate(c.targetWeight)
        Items.FAST_BALL -> if (c.targetBaseSpeed >= 100) 400 else 100
        Items.LEVEL_BALL -> if (c.targetLevel <= c.userLevel) 400 else 100
        Items.LURE_BALL -> if (c.encounter.fishing) 400 else 100
        Items.LOVE_BALL ->
            if (c.sameEvolutionFamily && c.targetGender >= 0 && c.userGender >= 0 && c.targetGender != c.userGender) 800
            else 100
        Items.MOON_BALL -> if (c.familyEvolvesByMoonStone) 400 else 100
        Items.FRIEND_BALL -> if (c.familyEvolvesByFriendship) 250 else 100
        Items.LUXURY_BALL -> if (c.familyEvolvesByFriendship) 200 else 100
        else -> 100
      }

  /** 4x up to level 16, 0.2x less per level after, 1x from level 31. */
  fun nestBallRate(targetLevel: Int): Int =
      if (targetLevel <= 16) 400 else (400 - 20 * (targetLevel - 16)).coerceAtLeast(100)

  /** 1x on the first turn, +0.3x each turn after, 4x from turn 11. */
  fun timerBallRate(turn: Int): Int = (100 + 30 * (turn - 1)).coerceIn(100, 400)

  /** 1x, +0.1x per link of the chain, 2.5x at 15 links. */
  fun repeatBallRate(chain: Int): Int = 100 + 10 * chain.coerceIn(0, RepeatBallStreak.MAX_CHAIN)

  /** By consecutive sleep turns: 0 1x, 1 1.5x, 2 2.5x, 3 or more 4x. */
  fun dreamBallRate(sleepTurns: Int): Int =
      when {
        sleepTurns <= 0 -> 100
        sleepTurns == 1 -> 150
        sleepTurns == 2 -> 250
        else -> 400
      }

  /** Asleep or frozen 2.5x, any other status 1.5x. */
  fun statusRate(status: Int): Int =
      when {
        StatusCondition.isAsleep(status) || StatusCondition.isFrozen(status) -> 250
        status != 0 -> 150
        else -> 100
      }

  /**
   * The modified catch rate the shake checks roll against: catch rate * ball bonus, scaled by
   * (3 max hp - 2 hp) / (3 max hp), then by the status bonus; never below 1.
   */
  fun odds(catchRate: Int, ballRate: Int, maxHp: Int, currentHp: Int, status: Int): Int {
    val max = maxHp.coerceAtLeast(1)
    val scaled = (catchRate * ballRate / 100) * (max * 3 - currentHp * 2) / (max * 3)
    return (scaled * statusRate(status) / 100).coerceAtLeast(1)
  }
}
