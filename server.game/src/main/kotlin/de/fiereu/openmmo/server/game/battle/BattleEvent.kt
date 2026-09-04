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

  data class Protected(val targetId: Long) : BattleEvent
}

enum class CantMoveReason {
  ASLEEP,
  FROZEN,
  PARALYZED,
  FLINCHED,
  CONFUSED,
  RECHARGING,
}
