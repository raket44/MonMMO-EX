package de.fiereu.openmmo.server.game.battle

/**
 * What happened during a turn, in order. [BattlePacketEmitter] turns the list into the client's
 * move-event stream: a [MoveUsed] or [TurnEffect] opens a packet and the target events that follow
 * ride under it until the next one.
 */
sealed interface BattleEvent {
  data class MoveUsed(val attackerId: Long, val moveId: Short, val moveSlot: Int, val ppLeft: Int) :
      BattleEvent

  /** Opens a packet for something that happens outside a move: poison damage, leftovers, weather. */
  data class TurnEffect(val sourceId: Long) : BattleEvent

  sealed interface MoveWithoutTarget : BattleEvent {
    val attackerId: Long
    val moveId: Short
  }

  data class MoveMissed(override val attackerId: Long, override val moveId: Short) :
      MoveWithoutTarget

  data class MoveFailed(override val attackerId: Long, override val moveId: Short) :
      MoveWithoutTarget

  data class DamageDealt(
      val targetId: Long,
      val newHp: Int,
      val crit: Boolean,
      val effectiveness: Int,
  ) : BattleEvent

  /** An hp change that is not a hit: healing, recoil, drain, status and weather damage. */
  data class HpChanged(val targetId: Long, val newHp: Int) : BattleEvent

  data class StageChanged(
      val targetId: Long,
      val stat: BattleStat,
      val stage: Int,
      val value: Int,
      val delta: Int,
      val failed: Boolean,
  ) : BattleEvent

  /** The target's non-volatile status byte is now [status] (0 = cured). */
  data class StatusChanged(val targetId: Long, val status: Int) : BattleEvent

  data class Fainted(val targetId: Long) : BattleEvent

  /** The field weather changed; null when it cleared. */
  data class WeatherChanged(val weather: Weather?) : BattleEvent

  /** The attacker lost its action; the client has no verified line for these yet. */
  data class CantMove(val attackerId: Long, val reason: CantMoveReason) : BattleEvent

  /** First half of a two-turn move: the attacker charges or goes semi-invulnerable. */
  data class Charging(val attackerId: Long, val moveId: Short) : BattleEvent

  data class MultiHit(val attackerId: Long, val hits: Int) : BattleEvent

  /** The monster left the field of view (Fly, Dig...) or came back. */
  data class Hidden(val targetId: Long, val hidden: Boolean) : BattleEvent

  /**
   * One of the client's fixed battle lines, with its body values; lines that carry an hp move the
   * bar as well, so no separate [HpChanged] is needed for them.
   */
  data class Line(
      val targetId: Long,
      val line: de.fiereu.openmmo.net.game.packets.battle.BattleLine,
      val values: List<Int> = emptyList(),
  ) : BattleEvent

  /** The target's Protect blocked the move: the client prints "{00} protected itself!". */
  data class Protected(val targetId: Long) : BattleEvent

  /** The move's type cannot touch the target: "It doesn't affect {00}...". */
  data class Immune(val targetId: Long) : BattleEvent
}

enum class CantMoveReason {
  ASLEEP,
  FROZEN,
  PARALYZED,
  FLINCHED,
  CONFUSED,
  RECHARGING,
}
