package de.fiereu.openmmo.server.game.battle

import java.time.LocalDate

/**
 * The Crystal Onix raid, fought three on three (project owner, 2026-09-14, modelled on retail
 * PokeMMO's Ho-Oh boss): the boss stands in the middle between two wild Onix it summoned, and
 * calls a fresh one whenever one of them faints. The battle is won when the boss faints.
 *
 * Phases, run by [TurnEngine]:
 * - turns 1-3 the crystal holds: no hit can knock the boss out, and it heals half its hp at the
 *   end of any turn it was hurt in;
 * - turn 4 the crystal cracks: +2 Defense and Sp. Def, half its hp back, Sandstorm, and its
 *   cracked move set;
 * - every 3rd turn crystal shards hit the player's side for a third of their max hp and whip the
 *   Sandstorm up again;
 * - the first time it drops below a quarter of its hp it heals half and takes +1 Defense and
 *   Sp. Def;
 * - every 5th turn it clears the player's stat boosts and its own drops and status.
 */
class RaidBossState(val bossEntityId: Long) {
  var cracked = false
  /** The one below-a-quarter rally has happened. */
  var rallied = false
  /** The boss lost hp this turn (the phase-one end-of-turn heal). */
  var hurtThisTurn = false

  fun isBoss(mon: BattleMonState): Boolean = mon.entityId == bossEntityId

  /** While the crystal holds, a hit that would knock the boss out leaves it at 1 hp. */
  fun endures(mon: BattleMonState): Boolean = isBoss(mon) && !cracked
}

object CrystalOnixRaid {
  const val SPECIES_SYMBOL = "SPECIES_ONIX_CRYSTAL"
  const val HELPER_DEX = 95
  const val BOSS_LEVEL = 60
  const val HELPER_LEVEL = 50
  /**
   * Summoned Onix: two start on the field, the rest wait to answer the boss's call. The client
   * holds at most six monsters a side (f/f8.Rl0 -> f/at0.ci into a six-slot party array; seven
   * crashed the battle open with ArrayIndexOutOfBounds, 2026-09-14), so boss + 5. Once the bench
   * is spent the boss "calls" a fallen helper back at full health.
   */
  const val HELPERS = 5

  const val CRACK_TURN = 4
  const val SHARD_EVERY = 3
  const val CLEANSE_EVERY = 5

  const val STONE_EDGE = 444
  const val MOONBLAST = 585
  const val ROCK_SLIDE = 157
  const val STEALTH_ROCK = 446
  const val DIAMOND_STORM = 591
  const val PLAY_ROUGH = 583
  const val EARTHQUAKE = 89
  const val IRON_DEFENSE = 334
  val OPENING_MOVES = listOf(STONE_EDGE, MOONBLAST, ROCK_SLIDE, STEALTH_ROCK)
  val CRACKED_MOVES = listOf(DIAMOND_STORM, PLAY_ROUGH, EARTHQUAKE, IRON_DEFENSE)

  /** Before the Amulet Coin, which doubles it like any prize. */
  const val PRIZE_MONEY = 25_000
  const val BATTLE_POINTS = 2_500
  const val COSMETIC_CHANCE_PERCENT = 2

  /** Epoch day of the character's last win: one per day, reset at midnight in the world clock zone (Texas). */
  const val WIN_DAY_KEY = "monmmo.crystal_onix.last_win_day"

  fun beatenToday(vars: Map<String, Int>, today: LocalDate): Boolean =
      vars[WIN_DAY_KEY] == today.toEpochDay().toInt()

  /**
   * The rare drop: [COSMETIC_CHANCE_PERCENT] percent of wins pick one of [candidates] at random;
   * null when the roll misses or nothing is left to give.
   */
  fun <T> rareDrop(candidates: List<T>, random: kotlin.random.Random): T? =
      if (random.nextInt(100) >= COSMETIC_CHANCE_PERCENT) null else candidates.randomOrNull(random)

  /**
   * The summoned Onix's own moves: single-target and foe-only spreads, so a helper never hits the
   * boss or its partner, never resets the raid's weather and never blows itself up (their wild
   * level-up set had Earthquake, Sandstorm and Double-Edge, 2026-09-14).
   */
  const val ROCK_TOMB = 317
  const val IRON_TAIL = 231
  const val BIND = 20
  val HELPER_MOVES = listOf(ROCK_SLIDE, ROCK_TOMB, IRON_TAIL, BIND)

  /**
   * The boss's powers, each fired as an ability of its own (project owner, 2026-09-14): the
   * client's ability banner (battle event kind 51, f/ua2) shows client string 210000 + id as the
   * name, and ids 400-405 are neither retail abilities (names stop at 210318) nor in f/ua2's text
   * switch, so the banner comes alone and the power's line follows it. Names are staged by the
   * launcher (ExpansionClientContentMain RAID_ABILITY_NAMES).
   */
  const val CRYSTAL_SHELL = 400
  const val LIVING_CRYSTAL = 401
  const val CRYSTAL_BREAK = 402
  const val SHARD_STORM = 403
  const val LAST_LIGHT = 404
  const val CLEAR_LIGHT = 405

  /**
   * In-battle raid lines: client strings printed by battle event kind 76, shape 0 (no name lookup).
   * Kind -22, the free-text line, crashed the client's renderer once a side had an empty field
   * position (f/uf1 streams the side's positions) and garbled every sentence into its "is being
   * controlled by" template - never use it.
   */
  const val MEND_LINE = 16790010
  const val CRACK_LINE = 16790011
  const val SHARDS_LINE = 16790012
  const val RALLY_LINE = 16790013
  const val CLEANSE_LINE = 16790014
  const val SHELL_LINE = 16790015

  /**
   * Client strings staged by the launcher (ExpansionClientContentMain RAID_STRINGS). There is no
   * "come back tomorrow" line: a beaten boss is not on the map for that character until the next day.
   */
  const val INTRO_TEXT = 16790000
  const val CHALLENGE_TEXT = 16790001
  const val WIN_TEXT = 16790003
  const val COSMETIC_TEXT = 16790004
  const val AWAKEN_TEXT = 16790005
}
