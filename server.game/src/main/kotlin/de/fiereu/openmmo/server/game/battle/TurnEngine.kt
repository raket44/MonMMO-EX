package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.MoveAdditionalEffect
import de.fiereu.openmmo.common.enums.MoveEffect
import de.fiereu.openmmo.common.enums.MoveFlag
import de.fiereu.openmmo.common.enums.MoveTarget
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.moves.AdditionalEffect
import de.fiereu.openmmo.moves.MoveDef
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.typechart.TypeChart
import javax.inject.Inject
import javax.inject.Singleton

private const val TACKLE_ID = 33
private const val STRUGGLE_ID = 165
private const val WEATHER_TURNS = 5
private const val SCREEN_TURNS = 5
private const val CONFUSION_SELF_HIT_POWER = 40

// Gen 3 critical stages: 1/16 base, Focus Energy +2, a high-crit move +1.
private val CRIT_DENOMINATORS = intArrayOf(16, 8, 4, 3, 2)

private data class TurnAction(
    val attacker: BattleMonState,
    val defender: BattleMonState,
    val move: MoveDef?,
)

private data class StageEffect(val stat: BattleStat, val delta: Int, val onSelf: Boolean)

private class HitResult(val damage: Int, val crit: Boolean)

/**
 * Resolves one battle turn with the Gen 3 rules: priority and speed order, the pre-move checks
 * (sleep, freeze, paralysis, flinch, confusion, recharge), accuracy with stages and
 * semi-invulnerability, the damage formula with crit, STAB, burn, screens, weather and the type
 * chart, the move effects (status, stat stages, healing, recoil, drain, multi-hit, fixed damage,
 * OHKO, two-turn moves, Protect, screens, Leech Seed, weather), each move's secondary effects,
 * and the end-of-turn phase (weather, status and Leech Seed damage, counters). Effects the engine
 * has no rule for fail without touching state.
 */
@Singleton
class TurnEngine
@Inject
constructor(
    private val moves: MoveRegistry,
    private val typeChart: TypeChart,
) {

  fun resolveTurn(battle: BattleInstance, playerMoveId: Short): List<BattleEvent> {
    val events = mutableListOf<BattleEvent>()
    val player = battle.activeMon()
    val enemy = battle.opponentMon()

    val playerAction = TurnAction(player, enemy, chooseMove(battle, player, moves.get(playerMoveId.toInt())))
    val enemyAction = TurnAction(enemy, player, chooseMove(battle, enemy, pickEnemyMove(battle, enemy)))

    val ordered = order(battle, playerAction, enemyAction)
    for ((index, action) in ordered.withIndex()) {
      if (action.attacker.fainted) continue
      val movesLast = index == ordered.lastIndex
      execute(battle, action, movesLast, events)
      if (player.fainted || enemy.fainted) break
    }
    if (!player.fainted && !enemy.fainted) endOfTurn(battle, events)
    player.endTurn()
    enemy.endTurn()
    return events
  }

  /** A voluntary switch spends the player's turn, so the enemy attacks the incoming monster. */
  fun resolveSwitchTurn(battle: BattleInstance): List<BattleEvent> {
    val events = mutableListOf<BattleEvent>()
    val enemy = battle.opponentMon()
    val player = battle.activeMon()
    if (enemy.fainted) return events
    val move = chooseMove(battle, enemy, pickEnemyMove(battle, enemy))
    execute(battle, TurnAction(enemy, player, move), movesLast = true, events)
    if (!player.fainted && !enemy.fainted) endOfTurn(battle, events)
    player.endTurn()
    enemy.endTurn()
    return events
  }

  /** The enemy AI picks a random usable move, falling back to Struggle with no pp left. */
  private fun pickEnemyMove(battle: BattleInstance, enemy: BattleMonState): MoveDef? {
    val usable = enemy.moves.filter { it.id.toInt() != 0 && it.pp > 0 }
    if (usable.isEmpty()) return moves.get(STRUGGLE_ID) ?: moves.get(TACKLE_ID)
    return moves.get(usable[battle.rng.pick(usable.size)].id.toInt())
  }

  /** A two-turn move in progress overrides the choice; an empty move slot means Struggle. */
  private fun chooseMove(battle: BattleInstance, mon: BattleMonState, chosen: MoveDef?): MoveDef? {
    if (mon.chargingMoveId != 0) return moves.get(mon.chargingMoveId) ?: chosen
    if (chosen == null) return null
    val slot = mon.moves.indexOfFirst { it.id.toInt() == chosen.id }
    if (slot >= 0 && mon.moves[slot].pp <= 0) return moves.get(STRUGGLE_ID) ?: chosen
    return chosen
  }

  private fun order(battle: BattleInstance, a: TurnAction, b: TurnAction): List<TurnAction> {
    val pa = a.move?.priority ?: 0
    val pb = b.move?.priority ?: 0
    if (pa != pb) return if (pa > pb) listOf(a, b) else listOf(b, a)
    val sa = speedOf(a.attacker)
    val sb = speedOf(b.attacker)
    if (sa != sb) return if (sa > sb) listOf(a, b) else listOf(b, a)
    return if (battle.rng.coinFlip()) listOf(a, b) else listOf(b, a)
  }

  private fun speedOf(mon: BattleMonState): Int {
    val speed = mon.effective(BattleStat.SPEED)
    return if (StatusCondition.isParalyzed(mon.status)) speed / 4 else speed
  }

  // ---------------------------------------------------------------------------------------------
  // Executing one action

  private fun execute(
      battle: BattleInstance,
      action: TurnAction,
      movesLast: Boolean,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val move = action.move
    attacker.movedThisTurn = true
    if (move == null) {
      events += BattleEvent.MoveFailed(attacker.entityId, 0)
      return
    }
    if (!canMove(battle, attacker, move, events)) return

    val continuing = attacker.chargingMoveId == move.id
    if (!continuing) spendPp(attacker, move, events)
    else events += BattleEvent.MoveUsed(attacker.entityId, move.id.toShort(), slotOf(attacker, move), ppOf(attacker, move))

    // Two-turn moves: the first use charges (or hides), the second one strikes.
    if (isTwoTurn(battle, move) && !continuing) {
      attacker.chargingMoveId = move.id
      attacker.semiInvulnerable = move.effect == MoveEffect.SEMI_INVULNERABLE
      if (move.effect == MoveEffect.SKULL_BASH) applyStage(action, StageEffect(BattleStat.DEFENSE, 1, true), events)
      events += BattleEvent.Charging(attacker.entityId, move.id.toShort())
      return
    }
    attacker.chargingMoveId = 0
    attacker.semiInvulnerable = false

    if (move.power > 0 || isDamagingEffect(move)) {
      attack(battle, action, move, events)
    } else {
      statusMove(battle, action, move, movesLast, events)
    }
  }

  private fun spendPp(attacker: BattleMonState, move: MoveDef, events: MutableList<BattleEvent>) {
    val slot = slotOf(attacker, move)
    if (slot >= 0 && attacker.moves[slot].pp > 0) {
      attacker.moves[slot].pp = (attacker.moves[slot].pp - 1).toByte()
    }
    events += BattleEvent.MoveUsed(attacker.entityId, move.id.toShort(), slot.coerceAtLeast(0), ppOf(attacker, move))
  }

  private fun slotOf(mon: BattleMonState, move: MoveDef): Int =
      mon.moves.indexOfFirst { it.id.toInt() == move.id }

  private fun ppOf(mon: BattleMonState, move: MoveDef): Int =
      mon.moves.getOrNull(slotOf(mon, move))?.pp?.toInt() ?: 0

  /** The Gen 3 pre-move checks, in the cartridge order. False when the action is lost. */
  private fun canMove(
      battle: BattleInstance,
      mon: BattleMonState,
      move: MoveDef,
      events: MutableList<BattleEvent>,
  ): Boolean {
    if (mon.mustRecharge) {
      mon.mustRecharge = false
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.RECHARGING)
      return false
    }
    if (StatusCondition.isAsleep(mon.status)) {
      val left = StatusCondition.sleepTurns(mon.status) - 1
      mon.status = (mon.status and StatusCondition.SLEEP_MASK.inv()) or left.coerceAtLeast(0)
      if (left > 0) {
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.ASLEEP)
        return false
      }
      mon.nightmare = false
      events += BattleEvent.StatusChanged(mon.entityId, mon.status)
    }
    if (StatusCondition.isFrozen(mon.status)) {
      if (move.hasFlag(MoveFlag.THAWS_USER) || battle.rng.accuracyRoll() <= 20) {
        mon.status = mon.status and StatusCondition.FREEZE.inv()
        events += BattleEvent.StatusChanged(mon.entityId, mon.status)
      } else {
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.FROZEN)
        return false
      }
    }
    if (mon.flinched) {
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.FLINCHED)
      return false
    }
    if (mon.confusionTurns > 0) {
      mon.confusionTurns--
      if (mon.confusionTurns > 0 && battle.rng.coinFlip()) {
        // Hits itself: a typeless 40-power physical hit with its own attack and defense.
        val damage = baseDamage(mon, mon, CONFUSION_SELF_HIT_POWER, physical = true, crit = false, battle.rng)
        mon.currentHp = (mon.currentHp - damage.coerceAtLeast(1)).coerceAtLeast(0)
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.CONFUSED)
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
        if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
        return false
      }
    }
    if (StatusCondition.isParalyzed(mon.status) && battle.rng.accuracyRoll() <= 25) {
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.PARALYZED)
      return false
    }
    return true
  }

  private fun isTwoTurn(battle: BattleInstance, move: MoveDef): Boolean =
      when (move.effect) {
        MoveEffect.SEMI_INVULNERABLE,
        MoveEffect.TWO_TURNS_ATTACK,
        MoveEffect.SKULL_BASH,
        MoveEffect.RAZOR_WIND,
        MoveEffect.SKY_ATTACK -> true
        MoveEffect.SOLAR_BEAM -> battle.weather != Weather.SUN
        else -> false
      }

  private fun isDamagingEffect(move: MoveDef): Boolean =
      when (move.effect) {
        MoveEffect.OHKO,
        MoveEffect.LEVEL_DAMAGE,
        MoveEffect.PSYWAVE,
        MoveEffect.SUPER_FANG,
        MoveEffect.FIXED_HP_DAMAGE,
        MoveEffect.FIXED_PERCENT_DAMAGE,
        MoveEffect.ENDEAVOR,
        MoveEffect.REFLECT_DAMAGE,
        MoveEffect.COUNTER,
        MoveEffect.MIRROR_COAT,
        MoveEffect.FLAIL,
        MoveEffect.RETURN,
        MoveEffect.FRUSTRATION,
        MoveEffect.HIDDEN_POWER,
        MoveEffect.LOW_KICK,
        MoveEffect.MAGNITUDE,
        MoveEffect.PRESENT,
        MoveEffect.POWER_BASED_ON_USER_HP,
        MoveEffect.POWER_BASED_ON_TARGET_HP,
        MoveEffect.WEATHER_BALL,
        MoveEffect.PAIN_SPLIT,
        MoveEffect.BIDE -> true
        else -> false
      }

  // ---------------------------------------------------------------------------------------------
  // Damaging moves

  private fun attack(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val moveId = move.id.toShort()

    if (defender.protectedThisTurn && move.hasFlag(MoveFlag.PROTECT_AFFECTED)) {
      events += BattleEvent.Protected(defender.entityId)
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    if (move.effect == MoveEffect.DREAM_EATER && !StatusCondition.isAsleep(defender.status)) {
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    if (move.effect == MoveEffect.SUCKER_PUNCH && defender.movedThisTurn) {
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    if (move.effect == MoveEffect.PAIN_SPLIT) {
      val total = attacker.currentHp + defender.currentHp
      attacker.currentHp = (total / 2).coerceIn(1, attacker.maxHp)
      defender.currentHp = (total - total / 2).coerceIn(1, defender.maxHp)
      events += BattleEvent.HpChanged(defender.entityId, defender.currentHp)
      events += BattleEvent.HpChanged(attacker.entityId, attacker.currentHp)
      return
    }
    if (move.effect == MoveEffect.REFLECT_DAMAGE ||
        move.effect == MoveEffect.COUNTER ||
        move.effect == MoveEffect.MIRROR_COAT) {
      val physical = MoveCategory.isPhysical(move.type)
      if (attacker.lastDamageTaken <= 0 || attacker.lastDamagePhysical != physical) {
        events += BattleEvent.MoveFailed(attacker.entityId, moveId)
        return
      }
      dealFixed(action, attacker.lastDamageTaken * 2, events)
      return
    }

    val effectiveType = moveType(battle, attacker, move)
    val effectiveness = effectivenessAgainst(effectiveType, defender)
    if (effectiveness == 0 && move.effect != MoveEffect.OHKO) {
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    if (!accuracyCheck(battle, action, move, events)) {
      events += BattleEvent.MoveMissed(attacker.entityId, moveId)
      if (move.effect == MoveEffect.RECOIL_IF_MISS) {
        val wouldDeal = hit(battle, action, move, effectiveType, effectiveness, powerOf(battle, action, move)).damage
        loseHp(attacker, (wouldDeal / 2).coerceAtLeast(1), events)
      }
      return
    }

    // Fixed-damage effects skip the formula.
    when (move.effect) {
      MoveEffect.OHKO -> {
        defender.currentHp = 0
        events += BattleEvent.DamageDealt(defender.entityId, 0, crit = false, effectiveness = TypeChart.NEUTRAL)
        events += BattleEvent.Fainted(defender.entityId)
        return
      }
      MoveEffect.LEVEL_DAMAGE -> return dealFixed(action, attacker.level, events)
      MoveEffect.PSYWAVE -> return dealFixed(action, (attacker.level * (50 + battle.rng.pick(101)) / 100).coerceAtLeast(1), events)
      MoveEffect.SUPER_FANG -> return dealFixed(action, (defender.currentHp / 2).coerceAtLeast(1), events)
      MoveEffect.FIXED_HP_DAMAGE -> return dealFixed(action, move.argumentValue ?: 20, events)
      MoveEffect.FIXED_PERCENT_DAMAGE ->
          return dealFixed(action, (defender.currentHp * (move.argumentValue ?: 50) / 100).coerceAtLeast(1), events)
      MoveEffect.ENDEAVOR -> {
        if (defender.currentHp <= attacker.currentHp) {
          events += BattleEvent.MoveFailed(attacker.entityId, moveId)
          return
        }
        return dealFixed(action, defender.currentHp - attacker.currentHp, events)
      }
      MoveEffect.BIDE -> {
        events += BattleEvent.MoveFailed(attacker.entityId, moveId)
        return
      }
      else -> Unit
    }

    val hits = hitCount(battle, move)
    var totalDamage = 0
    var lastCrit = false
    var struck = 0
    for (i in 0 until hits) {
      if (defender.fainted) break
      if (i > 0 && rerollsPerHit(move) && !accuracyCheck(battle, action, move, events)) break
      val power = powerOf(battle, action, move) * (if (move.effect == MoveEffect.TRIPLE_KICK) i + 1 else 1)
      if (move.effect == MoveEffect.PRESENT && power == 0) {
        // The present healed instead.
        healHp(defender, defender.maxHp / 4, events)
        return
      }
      val result = hit(battle, action, move, effectiveType, effectiveness, power)
      var damage = result.damage
      if (damage >= defender.currentHp) {
        val survive = move.effect == MoveEffect.FALSE_SWIPE || defender.enduring
        damage = if (survive) (defender.currentHp - 1).coerceAtLeast(0) else defender.currentHp
      }
      defender.currentHp -= damage
      defender.lastDamageTaken = damage
      defender.lastDamagePhysical = MoveCategory.isPhysical(effectiveType)
      totalDamage += damage
      lastCrit = result.crit
      struck++
      events += BattleEvent.DamageDealt(defender.entityId, defender.currentHp, result.crit, effectiveness)
    }
    if (struck > 1) events += BattleEvent.MultiHit(attacker.entityId, struck)
    if (struck == 0) return

    // A Fire hit thaws; Smelling Salt cures the paralysis it doubled on.
    if (effectiveType == PokemonType.FIRE && StatusCondition.isFrozen(defender.status)) {
      defender.status = defender.status and StatusCondition.FREEZE.inv()
      events += BattleEvent.StatusChanged(defender.entityId, defender.status)
    }
    if (move.effect == MoveEffect.BRICK_BREAK) {
      battle.sideOf(defender).reflectTurns = 0
      battle.sideOf(defender).lightScreenTurns = 0
    }
    if (move.effect == MoveEffect.RAPID_SPIN) {
      attacker.leechSeeded = false
      attacker.trappedTurns = 0
    }

    if (defender.fainted) events += BattleEvent.Fainted(defender.entityId)

    // After-hit effects on the user.
    when (move.effect) {
      MoveEffect.RECOIL, MoveEffect.DOUBLE_EDGE -> loseHp(attacker, (totalDamage * (move.argumentValue ?: 25) / 100).coerceAtLeast(1), events)
      MoveEffect.MAX_HP_50_RECOIL -> loseHp(attacker, (attacker.maxHp / 2).coerceAtLeast(1), events)
      MoveEffect.STRUGGLE -> loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events)
      MoveEffect.ABSORB, MoveEffect.DREAM_EATER ->
          healHp(attacker, (totalDamage * (move.argumentValue ?: 50) / 100).coerceAtLeast(1), events)
      MoveEffect.EXPLOSION -> {
        attacker.currentHp = 0
        events += BattleEvent.TurnEffect(attacker.entityId)
        events += BattleEvent.HpChanged(attacker.entityId, 0)
        events += BattleEvent.Fainted(attacker.entityId)
      }
      else -> Unit
    }
    secondaryEffects(battle, action, move, events)
  }

  private fun dealFixed(action: TurnAction, amount: Int, events: MutableList<BattleEvent>) {
    val defender = action.defender
    var damage = amount.coerceAtLeast(1)
    if (damage >= defender.currentHp) damage = if (defender.enduring) defender.currentHp - 1 else defender.currentHp
    defender.currentHp -= damage
    defender.lastDamageTaken = damage
    events += BattleEvent.DamageDealt(defender.entityId, defender.currentHp, crit = false, effectiveness = TypeChart.NEUTRAL)
    if (defender.fainted) events += BattleEvent.Fainted(defender.entityId)
  }

  private fun hitCount(battle: BattleInstance, move: MoveDef): Int =
      when {
        move.hasFlag(MoveFlag.MULTI_HIT) || move.effect == MoveEffect.MULTI_HIT -> {
          // 2-5 hits at 3/8, 3/8, 1/8, 1/8.
          val roll = battle.rng.pick(8)
          when {
            roll < 3 -> 2
            roll < 6 -> 3
            roll < 7 -> 4
            else -> 5
          }
        }
        move.strikeCount > 1 -> move.strikeCount
        move.effect == MoveEffect.DOUBLE_HIT || move.effect == MoveEffect.TWINEEDLE -> 2
        move.effect == MoveEffect.TRIPLE_KICK -> 3
        else -> 1
      }

  /** Triple Kick and Population Bomb re-roll accuracy per strike; other multi-hits do not. */
  private fun rerollsPerHit(move: MoveDef): Boolean =
      move.effect == MoveEffect.TRIPLE_KICK || move.strikeCount >= 10

  private fun moveType(battle: BattleInstance, attacker: BattleMonState, move: MoveDef): PokemonType =
      when (move.effect) {
        MoveEffect.HIDDEN_POWER -> hiddenPowerType(attacker)
        MoveEffect.WEATHER_BALL ->
            when (battle.weather) {
              Weather.RAIN -> PokemonType.WATER
              Weather.SUN -> PokemonType.FIRE
              Weather.SANDSTORM -> PokemonType.ROCK
              Weather.HAIL -> PokemonType.ICE
              null -> PokemonType.NORMAL
            }
        else -> move.type
      }

  private fun effectivenessAgainst(type: PokemonType, defender: BattleMonState): Int {
    val species = defender.species
    var eff = typeChart.effectiveness(type, species.type1, species.type2)
    // Foresight lets Normal and Fighting hit a Ghost.
    if (eff == 0 && defender.identified && species.hasType(PokemonType.GHOST) &&
        (type == PokemonType.NORMAL || type == PokemonType.FIGHTING)) {
      val other = if (species.type1 == PokemonType.GHOST) species.type2 else species.type1
      eff = typeChart.multiplier(type, other)
    }
    return eff
  }

  private fun powerOf(battle: BattleInstance, action: TurnAction, move: MoveDef): Int {
    val attacker = action.attacker
    val defender = action.defender
    val hp = attacker.currentHp
    val max = attacker.maxHp
    var power =
        when (move.effect) {
          MoveEffect.FLAIL -> {
            val ratio = hp * 48 / max.coerceAtLeast(1)
            when {
              ratio < 2 -> 200
              ratio < 5 -> 150
              ratio < 10 -> 100
              ratio < 17 -> 80
              ratio < 33 -> 40
              else -> 20
            }
          }
          MoveEffect.RETURN -> (attacker.source.friendship * 10 / 25).coerceAtLeast(1)
          MoveEffect.FRUSTRATION -> ((255 - attacker.source.friendship) * 10 / 25).coerceAtLeast(1)
          MoveEffect.HIDDEN_POWER -> hiddenPowerBase(attacker)
          MoveEffect.LOW_KICK, MoveEffect.HEAT_CRASH -> 60
          MoveEffect.MAGNITUDE -> {
            val roll = battle.rng.pick(100)
            when {
              roll < 5 -> 10
              roll < 15 -> 30
              roll < 35 -> 50
              roll < 65 -> 70
              roll < 85 -> 90
              roll < 95 -> 110
              else -> 150
            }
          }
          MoveEffect.PRESENT -> {
            val roll = battle.rng.pick(100)
            when {
              roll < 40 -> 40
              roll < 70 -> 80
              roll < 80 -> 120
              else -> 0
            }
          }
          MoveEffect.POWER_BASED_ON_USER_HP -> (150 * hp / max.coerceAtLeast(1)).coerceAtLeast(1)
          MoveEffect.POWER_BASED_ON_TARGET_HP ->
              (120 * defender.currentHp / defender.maxHp.coerceAtLeast(1)).coerceAtLeast(1)
          MoveEffect.WEATHER_BALL -> if (battle.weather != null) 100 else 50
          MoveEffect.SOLAR_BEAM ->
              if (battle.weather != null && battle.weather != Weather.SUN) move.power / 2 else move.power
          else -> move.power
        }
    when (move.effect) {
      MoveEffect.FACADE ->
          if (StatusCondition.isBurned(attacker.status) || StatusCondition.isPoisoned(attacker.status) ||
              StatusCondition.isParalyzed(attacker.status))
              power *= 2
      MoveEffect.REVENGE, MoveEffect.PAYBACK, MoveEffect.ASSURANCE ->
          if (attacker.lastDamageTaken > 0) power *= 2
      MoveEffect.DOUBLE_POWER_ON_ARG_STATUS -> {
        val bits = statusBits(move.argument?.value)
        if (defender.status and bits != 0) power *= 2
      }
      MoveEffect.EARTHQUAKE, MoveEffect.MAGNITUDE ->
          if (defender.semiInvulnerable && defender.chargingMoveId != 0) power *= 2
      MoveEffect.GUST, MoveEffect.TWISTER -> if (defender.semiInvulnerable) power *= 2
      else -> Unit
    }
    return power
  }

  private fun hit(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      type: PokemonType,
      effectiveness: Int,
      power: Int,
  ): HitResult {
    val attacker = action.attacker
    val defender = action.defender
    val physical = MoveCategory.isPhysical(type)
    val critStage =
        (if (attacker.focusEnergy) 2 else 0) +
            (if (move.effect == MoveEffect.HIGH_CRITICAL) 1 else 0) +
            move.criticalHitStage
    val crit =
        move.hasFlag(MoveFlag.ALWAYS_CRIT) ||
            battle.rng.critRoll(CRIT_DENOMINATORS[critStage.coerceIn(0, CRIT_DENOMINATORS.lastIndex)])
    var dmg = baseDamage(attacker, defender, power, physical, crit, battle.rng, explosion = move.effect == MoveEffect.EXPLOSION)
    if (physical && StatusCondition.isBurned(attacker.status)) dmg /= 2
    val side = battle.sideOf(defender)
    if (!crit && ((physical && side.reflectTurns > 0) || (!physical && side.lightScreenTurns > 0))) dmg /= 2
    when (battle.weather) {
      Weather.RAIN -> if (type == PokemonType.WATER) dmg = dmg * 3 / 2 else if (type == PokemonType.FIRE) dmg /= 2
      Weather.SUN -> if (type == PokemonType.FIRE) dmg = dmg * 3 / 2 else if (type == PokemonType.WATER) dmg /= 2
      else -> Unit
    }
    if (crit) dmg *= 2
    if (attacker.species.hasType(type)) dmg = dmg * 3 / 2
    dmg = dmg * effectiveness / TypeChart.NEUTRAL
    dmg = dmg * battle.rng.damageRoll() / 100
    return HitResult(dmg.coerceAtLeast(1), crit)
  }

  /** The Gen 3 core: (2L/5 + 2) * power * atk / def / 50 + 2, with the crit stage rule. */
  private fun baseDamage(
      attacker: BattleMonState,
      defender: BattleMonState,
      power: Int,
      physical: Boolean,
      crit: Boolean,
      rng: BattleRng,
      explosion: Boolean = false,
  ): Int {
    val atkStat = if (physical) BattleStat.ATTACK else BattleStat.SP_ATTACK
    val defStat = if (physical) BattleStat.DEFENSE else BattleStat.SP_DEFENSE
    // A crit ignores the attacker's negative stages and the defender's positive stages.
    val atk =
        if (crit && attacker.stage(atkStat) < 0) attacker.unstaged(atkStat) else attacker.effective(atkStat)
    var def =
        if (crit && defender.stage(defStat) > 0) defender.unstaged(defStat) else defender.effective(defStat)
    if (explosion) def = (def / 2).coerceAtLeast(1)
    return (2 * attacker.level / 5 + 2) * power * atk / def.coerceAtLeast(1) / 50 + 2
  }

  private fun accuracyCheck(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      events: MutableList<BattleEvent>,
  ): Boolean {
    val attacker = action.attacker
    val defender = action.defender
    if (attacker.lockedOn) {
      attacker.lockedOn = false
      return true
    }
    if (defender.semiInvulnerable) {
      val allowed =
          when (move.effect) {
            MoveEffect.EARTHQUAKE, MoveEffect.MAGNITUDE -> true
            MoveEffect.GUST, MoveEffect.TWISTER, MoveEffect.THUNDER, MoveEffect.SKY_UPPERCUT -> true
            else -> false
          }
      if (!allowed) return false
    }
    if (move.effect == MoveEffect.OHKO) {
      if (defender.level > attacker.level) return false
      return battle.rng.accuracyRoll() <= 30 + (attacker.level - defender.level)
    }
    var accuracy = move.accuracy
    if (move.effect == MoveEffect.THUNDER) {
      when (battle.weather) {
        Weather.RAIN -> return true
        Weather.SUN -> accuracy = 50
        else -> Unit
      }
    }
    if (accuracy == 0) return true
    val stage = attacker.stage(BattleStat.ACCURACY) - defender.stage(BattleStat.EVASION)
    val threshold = StatStages.scaleAccuracy(accuracy, stage)
    return battle.rng.accuracyRoll() <= threshold
  }

  // ---------------------------------------------------------------------------------------------
  // Secondary effects

  private fun secondaryEffects(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    for (extra in move.additionalEffects) {
      if (extra.chance < 100 && battle.rng.accuracyRoll() > extra.chance) continue
      if (!extra.self && defender.fainted) continue
      applyAdditional(battle, action, move, extra, events)
    }
    // The collapsed Gen 3 names the table keeps for single-stat side effects.
    secondaryStage(move.effect)?.let { stage ->
      if (move.secondaryEffectChance > 0 && battle.rng.accuracyRoll() <= move.secondaryEffectChance &&
          (stage.onSelf || !defender.fainted)) {
        applyStage(action, stage, events)
      }
    }
    if (attacker.fainted) return
  }

  private fun applyAdditional(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      extra: AdditionalEffect,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val target = if (extra.self) attacker else defender
    when (extra.effect) {
      MoveAdditionalEffect.SLEEP -> inflictStatus(battle, attacker, target, StatusCondition.asleep(2 + battle.rng.pick(4)), events)
      MoveAdditionalEffect.POISON -> inflictStatus(battle, attacker, target, StatusCondition.POISON, events)
      MoveAdditionalEffect.TOXIC -> inflictStatus(battle, attacker, target, StatusCondition.TOXIC, events)
      MoveAdditionalEffect.BURN -> inflictStatus(battle, attacker, target, StatusCondition.BURN, events)
      MoveAdditionalEffect.FREEZE,
      MoveAdditionalEffect.FREEZE_OR_FROSTBITE -> inflictStatus(battle, attacker, target, StatusCondition.FREEZE, events)
      MoveAdditionalEffect.PARALYSIS -> inflictStatus(battle, attacker, target, StatusCondition.PARALYSIS, events)
      MoveAdditionalEffect.TRI_ATTACK -> {
        val status = listOf(StatusCondition.BURN, StatusCondition.FREEZE, StatusCondition.PARALYSIS)[battle.rng.pick(3)]
        inflictStatus(battle, attacker, target, status, events)
      }
      MoveAdditionalEffect.SECRET_POWER -> inflictStatus(battle, attacker, target, StatusCondition.PARALYSIS, events)
      MoveAdditionalEffect.CONFUSION -> confuse(battle, attacker, target, events)
      MoveAdditionalEffect.FLINCH -> if (!target.movedThisTurn) target.flinched = true
      MoveAdditionalEffect.STAT_PLUS, MoveAdditionalEffect.STAT_MINUS -> {
        val sign = if (extra.effect == MoveAdditionalEffect.STAT_PLUS) 1 else -1
        for ((name, stages) in extra.stats) {
          val stat = battleStat(name) ?: continue
          applyStage(action, StageEffect(stat, sign * stages, extra.self), events)
        }
      }
      MoveAdditionalEffect.WRAP -> if (target.trappedTurns == 0) target.trappedTurns = 2 + battle.rng.pick(4)
      MoveAdditionalEffect.PREVENT_ESCAPE, MoveAdditionalEffect.TRAP_BOTH -> if (target.trappedTurns == 0) target.trappedTurns = 99
      MoveAdditionalEffect.RECHARGE -> attacker.mustRecharge = true
      MoveAdditionalEffect.RECOIL_HP_25 -> loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events)
      MoveAdditionalEffect.REMOVE_STATUS -> {
        val bits = statusBits(move.argument?.value)
        if (target.status and bits != 0) {
          target.status = target.status and bits.inv()
          events += BattleEvent.StatusChanged(target.entityId, target.status)
        }
      }
      MoveAdditionalEffect.LEECH_SEED -> seed(attacker, target, events)
      MoveAdditionalEffect.RAIN -> setWeather(battle, Weather.RAIN, events)
      MoveAdditionalEffect.SUN -> setWeather(battle, Weather.SUN, events)
      MoveAdditionalEffect.SANDSTORM -> setWeather(battle, Weather.SANDSTORM, events)
      MoveAdditionalEffect.HAIL -> setWeather(battle, Weather.HAIL, events)
      MoveAdditionalEffect.HAZE -> {
        for (stat in BattleStat.entries) {
          attacker.changeStage(stat, -attacker.stage(stat))
          defender.changeStage(stat, -defender.stage(stat))
        }
      }
      MoveAdditionalEffect.AROMATHERAPY, MoveAdditionalEffect.HEAL_TEAM -> cureStatus(attacker, events)
      else -> Unit
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Status moves

  private fun statusMove(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      movesLast: Boolean,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val moveId = move.id.toShort()
    val targetsFoe = move.target != MoveTarget.USER && move.target != MoveTarget.FIELD
    if (targetsFoe && defender.protectedThisTurn && move.hasFlag(MoveFlag.PROTECT_AFFECTED)) {
      events += BattleEvent.Protected(defender.entityId)
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    if (targetsFoe && defender.semiInvulnerable && move.accuracy > 0) {
      events += BattleEvent.MoveMissed(attacker.entityId, moveId)
      return
    }
    fun fail() {
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
    }
    fun accurate(): Boolean {
      if (accuracyCheck(battle, action, move, events)) return true
      events += BattleEvent.MoveMissed(attacker.entityId, moveId)
      return false
    }

    stageEffect(move.effect)?.let { stage ->
      if (!stage.onSelf && !accurate()) return
      val applied = applyStage(action, stage, events)
      if (move.effect == MoveEffect.MINIMIZE) attacker.minimized = true
      if (!applied) fail()
      return
    }

    when (move.effect) {
      MoveEffect.STAT_CHANGE, MoveEffect.CALM_MIND, MoveEffect.BULK_UP, MoveEffect.DRAGON_DANCE,
      MoveEffect.COSMIC_POWER, MoveEffect.TICKLE, MoveEffect.GROWTH, MoveEffect.DEFENSE_CURL -> {
        val hostile = move.additionalEffects.any { !it.self }
        if (hostile && !accurate()) return
        var applied = false
        for (extra in move.additionalEffects) {
          val sign = if (extra.effect == MoveAdditionalEffect.STAT_MINUS) -1 else 1
          for ((name, stages) in extra.stats) {
            val stat = battleStat(name) ?: continue
            if (applyStage(action, StageEffect(stat, sign * stages, extra.self), events)) applied = true
          }
        }
        if (move.effect == MoveEffect.DEFENSE_CURL) attacker.defenseCurled = true
        if (!applied && move.additionalEffects.isNotEmpty()) fail()
        if (move.additionalEffects.isEmpty()) fail()
      }
      MoveEffect.NON_VOLATILE_STATUS, MoveEffect.SLEEP, MoveEffect.POISON, MoveEffect.TOXIC,
      MoveEffect.PARALYZE, MoveEffect.WILL_O_WISP, MoveEffect.DARK_VOID, MoveEffect.YAWN -> {
        val status =
            when (move.effect) {
              MoveEffect.SLEEP, MoveEffect.DARK_VOID -> StatusCondition.asleep(2 + battle.rng.pick(4))
              MoveEffect.POISON -> StatusCondition.POISON
              MoveEffect.TOXIC -> StatusCondition.TOXIC
              MoveEffect.PARALYZE -> StatusCondition.PARALYSIS
              MoveEffect.WILL_O_WISP -> StatusCondition.BURN
              MoveEffect.YAWN -> 0
              else -> statusFor(battle, move.argumentEffect)
            }
        if (move.effect == MoveEffect.YAWN) {
          if (StatusCondition.hasAny(defender.status) || defender.drowsyTurns > 0) return fail()
          if (!accurate()) return
          defender.drowsyTurns = 2
          return
        }
        if (status == 0) return fail()
        if (effectivenessAgainst(move.type, defender) == 0) return fail()
        if (!canReceiveStatus(battle, attacker, defender, status)) return fail()
        if (!accurate()) return
        inflictStatus(battle, attacker, defender, status, events)
      }
      MoveEffect.CONFUSE, MoveEffect.TEETER_DANCE -> {
        if (defender.confusionTurns > 0 || battle.sideOf(defender).safeguardTurns > 0) return fail()
        if (!accurate()) return
        confuse(battle, attacker, defender, events)
      }
      MoveEffect.SWAGGER, MoveEffect.FLATTER -> {
        if (!accurate()) return
        val stat = if (move.effect == MoveEffect.SWAGGER) BattleStat.ATTACK else BattleStat.SP_ATTACK
        applyStage(action, StageEffect(stat, if (move.effect == MoveEffect.SWAGGER) 2 else 1, false), events)
        confuse(battle, attacker, defender, events)
      }
      MoveEffect.PROTECT, MoveEffect.ENDURE -> {
        val method = move.argument?.value
        if (move.effect == MoveEffect.PROTECT && method != null && method != "PROTECT_NORMAL") return fail()
        val denominator = 1 shl attacker.protectStreak.coerceAtMost(3)
        if (movesLast || (denominator > 1 && battle.rng.pick(denominator) != 0)) {
          attacker.protectStreak = 0
          return fail()
        }
        attacker.protectStreak++
        if (move.effect == MoveEffect.PROTECT) attacker.protectedThisTurn = true else attacker.enduring = true
      }
      MoveEffect.RESTORE_HP, MoveEffect.SOFTBOILED, MoveEffect.ROOST, MoveEffect.SHORE_UP -> {
        if (attacker.currentHp >= attacker.maxHp) return fail()
        healHp(attacker, (attacker.maxHp / 2).coerceAtLeast(1), events)
      }
      MoveEffect.MORNING_SUN, MoveEffect.SYNTHESIS, MoveEffect.MOONLIGHT -> {
        if (attacker.currentHp >= attacker.maxHp) return fail()
        val amount =
            when (battle.weather) {
              null -> attacker.maxHp / 2
              Weather.SUN -> attacker.maxHp * 2 / 3
              else -> attacker.maxHp / 4
            }
        healHp(attacker, amount.coerceAtLeast(1), events)
      }
      MoveEffect.REST -> {
        if (attacker.currentHp >= attacker.maxHp || StatusCondition.isAsleep(attacker.status)) return fail()
        attacker.status = StatusCondition.asleep(3)
        attacker.toxicCounter = 0
        attacker.nightmare = false
        events += BattleEvent.StatusChanged(attacker.entityId, attacker.status)
        healHp(attacker, attacker.maxHp, events)
      }
      MoveEffect.WEATHER, MoveEffect.RAIN_DANCE, MoveEffect.SUNNY_DAY, MoveEffect.SANDSTORM, MoveEffect.HAIL -> {
        val weather =
            when (move.effect) {
              MoveEffect.RAIN_DANCE -> Weather.RAIN
              MoveEffect.SUNNY_DAY -> Weather.SUN
              MoveEffect.SANDSTORM -> Weather.SANDSTORM
              MoveEffect.HAIL -> Weather.HAIL
              else ->
                  when (move.argument?.value) {
                    "BATTLE_WEATHER_RAIN" -> Weather.RAIN
                    "BATTLE_WEATHER_SUN" -> Weather.SUN
                    "BATTLE_WEATHER_SANDSTORM" -> Weather.SANDSTORM
                    "BATTLE_WEATHER_HAIL", "BATTLE_WEATHER_SNOW" -> Weather.HAIL
                    else -> null
                  }
            }
        if (weather == null || battle.weather == weather) return fail()
        setWeather(battle, weather, events)
      }
      MoveEffect.LEECH_SEED -> {
        if (defender.leechSeeded || defender.species.hasType(PokemonType.GRASS)) return fail()
        if (!accurate()) return
        seed(attacker, defender, events)
      }
      MoveEffect.LIGHT_SCREEN -> {
        val side = battle.sideOf(attacker)
        if (side.lightScreenTurns > 0) return fail()
        side.lightScreenTurns = SCREEN_TURNS
      }
      MoveEffect.REFLECT -> {
        val side = battle.sideOf(attacker)
        if (side.reflectTurns > 0) return fail()
        side.reflectTurns = SCREEN_TURNS
      }
      MoveEffect.SAFEGUARD -> {
        val side = battle.sideOf(attacker)
        if (side.safeguardTurns > 0) return fail()
        side.safeguardTurns = SCREEN_TURNS
      }
      MoveEffect.MIST -> {
        val side = battle.sideOf(attacker)
        if (side.mistTurns > 0) return fail()
        side.mistTurns = SCREEN_TURNS
      }
      MoveEffect.HAZE -> {
        for (stat in BattleStat.entries) {
          attacker.changeStage(stat, -attacker.stage(stat))
          defender.changeStage(stat, -defender.stage(stat))
        }
      }
      MoveEffect.FOCUS_ENERGY, MoveEffect.LASER_FOCUS -> {
        if (attacker.focusEnergy) return fail()
        attacker.focusEnergy = true
      }
      MoveEffect.CURSE -> {
        if (attacker.species.hasType(PokemonType.GHOST)) {
          if (defender.cursed) return fail()
          defender.cursed = true
          loseHp(attacker, (attacker.maxHp / 2).coerceAtLeast(1), events)
        } else {
          applyStage(action, StageEffect(BattleStat.SPEED, -1, true), events)
          applyStage(action, StageEffect(BattleStat.ATTACK, 1, true), events)
          applyStage(action, StageEffect(BattleStat.DEFENSE, 1, true), events)
        }
      }
      MoveEffect.BELLY_DRUM, MoveEffect.STAT_CHANGE_HALF_HP -> {
        if (attacker.currentHp <= attacker.maxHp / 2 || attacker.stage(BattleStat.ATTACK) >= StatStages.MAX) return fail()
        loseHp(attacker, attacker.maxHp / 2, events)
        applyStage(action, StageEffect(BattleStat.ATTACK, 12, true), events)
      }
      MoveEffect.PSYCH_UP -> {
        for (stat in BattleStat.entries) attacker.changeStage(stat, defender.stage(stat) - attacker.stage(stat))
      }
      MoveEffect.INGRAIN -> {
        if (attacker.ingrained) return fail()
        attacker.ingrained = true
      }
      MoveEffect.WISH -> {
        if (attacker.wishTurns > 0) return fail()
        attacker.wishTurns = 2
      }
      MoveEffect.REFRESH -> {
        val bits = StatusCondition.POISON or StatusCondition.TOXIC or StatusCondition.BURN or StatusCondition.PARALYSIS
        if (attacker.status and bits == 0) return fail()
        attacker.status = attacker.status and bits.inv()
        attacker.toxicCounter = 0
        events += BattleEvent.StatusChanged(attacker.entityId, attacker.status)
      }
      MoveEffect.HEAL_BELL -> cureStatus(attacker, events)
      MoveEffect.MEAN_LOOK -> {
        if (defender.trappedTurns > 0) return fail()
        defender.trappedTurns = 99
      }
      MoveEffect.LOCK_ON -> attacker.lockedOn = true
      MoveEffect.FORESIGHT, MoveEffect.MIRACLE_EYE -> {
        if (defender.identified) return fail()
        defender.identified = true
      }
      MoveEffect.NIGHTMARE -> {
        if (!StatusCondition.isAsleep(defender.status) || defender.nightmare) return fail()
        defender.nightmare = true
      }
      MoveEffect.SLEEP_TALK -> {
        if (!StatusCondition.isAsleep(attacker.status)) return fail()
        val options = attacker.moves.filter { it.id.toInt() != 0 && it.id.toInt() != move.id }
        if (options.isEmpty()) return fail()
        val picked = moves.get(options[battle.rng.pick(options.size)].id.toInt()) ?: return fail()
        callMove(battle, action, picked, movesLast, events)
      }
      MoveEffect.METRONOME -> {
        var picked: MoveDef? = null
        repeat(20) {
          if (picked == null) {
            val candidate = moves.get(1 + battle.rng.pick(354))
            if (candidate != null && candidate.effect != MoveEffect.METRONOME && candidate.power >= 0) picked = candidate
          }
        }
        val chosen = picked ?: return fail()
        callMove(battle, action, chosen, movesLast, events)
      }
      else -> fail()
    }
  }

  /** Runs another move as this action, the way Metronome and Sleep Talk do. */
  private fun callMove(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      movesLast: Boolean,
      events: MutableList<BattleEvent>,
  ) {
    events += BattleEvent.MoveUsed(action.attacker.entityId, move.id.toShort(), 0, 0)
    if (move.power > 0 || isDamagingEffect(move)) attack(battle, action, move, events)
    else statusMove(battle, action, move, movesLast, events)
  }

  private fun statusFor(battle: BattleInstance, effect: MoveAdditionalEffect?): Int =
      when (effect) {
        MoveAdditionalEffect.SLEEP -> StatusCondition.asleep(2 + battle.rng.pick(4))
        MoveAdditionalEffect.POISON -> StatusCondition.POISON
        MoveAdditionalEffect.TOXIC -> StatusCondition.TOXIC
        MoveAdditionalEffect.BURN -> StatusCondition.BURN
        MoveAdditionalEffect.FREEZE, MoveAdditionalEffect.FREEZE_OR_FROSTBITE -> StatusCondition.FREEZE
        MoveAdditionalEffect.PARALYSIS -> StatusCondition.PARALYSIS
        else -> 0
      }

  /** The `STATUS1_*` tokens a move argument can name, as status bits. */
  private fun statusBits(token: String?): Int =
      when (token) {
        "STATUS1_SLEEP" -> StatusCondition.SLEEP_MASK
        "STATUS1_POISON" -> StatusCondition.POISON
        "STATUS1_PSN_ANY" -> StatusCondition.POISON or StatusCondition.TOXIC
        "STATUS1_TOXIC_POISON" -> StatusCondition.TOXIC
        "STATUS1_BURN" -> StatusCondition.BURN
        "STATUS1_FREEZE" -> StatusCondition.FREEZE
        "STATUS1_PARALYSIS" -> StatusCondition.PARALYSIS
        "STATUS1_ANY" -> 0xFF
        else -> 0
      }

  private fun canReceiveStatus(battle: BattleInstance, source: BattleMonState, target: BattleMonState, status: Int): Boolean {
    if (StatusCondition.hasAny(target.status)) return false
    if (source !== target && battle.sideOf(target).safeguardTurns > 0) return false
    val species = target.species
    return when {
      status and StatusCondition.BURN != 0 -> !species.hasType(PokemonType.FIRE)
      status and StatusCondition.FREEZE != 0 -> !species.hasType(PokemonType.ICE)
      status and (StatusCondition.POISON or StatusCondition.TOXIC) != 0 ->
          !species.hasType(PokemonType.POISON) && !species.hasType(PokemonType.STEEL)
      else -> true
    }
  }

  private fun inflictStatus(
      battle: BattleInstance,
      source: BattleMonState,
      target: BattleMonState,
      status: Int,
      events: MutableList<BattleEvent>,
  ): Boolean {
    if (target.fainted || !canReceiveStatus(battle, source, target, status)) return false
    target.status = status
    if (status and StatusCondition.TOXIC != 0) target.toxicCounter = 0
    events += BattleEvent.StatusChanged(target.entityId, target.status)
    return true
  }

  private fun confuse(battle: BattleInstance, source: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>) {
    if (target.fainted || target.confusionTurns > 0) return
    if (source !== target && battle.sideOf(target).safeguardTurns > 0) return
    target.confusionTurns = 2 + battle.rng.pick(4)
  }

  private fun seed(source: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>) {
    if (target.leechSeeded || target.species.hasType(PokemonType.GRASS)) return
    target.leechSeeded = true
  }

  private fun cureStatus(mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (!StatusCondition.hasAny(mon.status)) return
    mon.status = StatusCondition.NONE
    mon.toxicCounter = 0
    mon.nightmare = false
    events += BattleEvent.StatusChanged(mon.entityId, mon.status)
  }

  private fun setWeather(battle: BattleInstance, weather: Weather, events: MutableList<BattleEvent>) {
    if (battle.weather == weather) return
    battle.weather = weather
    battle.weatherTurns = WEATHER_TURNS
    events += BattleEvent.WeatherChanged(weather)
  }

  private fun healHp(mon: BattleMonState, amount: Int, events: MutableList<BattleEvent>) {
    if (mon.fainted) return
    val before = mon.currentHp
    mon.currentHp = (mon.currentHp + amount).coerceAtMost(mon.maxHp)
    if (mon.currentHp != before) events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
  }

  private fun loseHp(mon: BattleMonState, amount: Int, events: MutableList<BattleEvent>) {
    if (mon.fainted) return
    mon.currentHp = (mon.currentHp - amount).coerceAtLeast(0)
    events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
    if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
  }

  /** Returns false when the stage was capped and nothing moved. */
  private fun applyStage(action: TurnAction, effect: StageEffect, events: MutableList<BattleEvent>): Boolean {
    val target = if (effect.onSelf) action.attacker else action.defender
    if (target.fainted) return false
    val applied = target.changeStage(effect.stat, effect.delta)
    val value =
        when (effect.stat) {
          BattleStat.ACCURACY,
          BattleStat.EVASION -> 0
          else -> target.effective(effect.stat)
        }
    events +=
        BattleEvent.StageChanged(
            target.entityId, effect.stat, target.stage(effect.stat), value, effect.delta, applied == 0)
    return applied != 0
  }

  // ---------------------------------------------------------------------------------------------
  // End of turn

  private fun endOfTurn(battle: BattleInstance, events: MutableList<BattleEvent>) {
    val player = battle.activeMon()
    val enemy = battle.opponentMon()
    val order = if (speedOf(player) >= speedOf(enemy)) listOf(player, enemy) else listOf(enemy, player)

    // Weather: counts down, then hurts whoever it hurts.
    if (battle.weather != null) {
      battle.weatherTurns--
      if (battle.weatherTurns <= 0) {
        battle.weather = null
        events += BattleEvent.WeatherChanged(null)
      } else {
        for (mon in order) {
          if (mon.fainted) continue
          val hurt =
              when (battle.weather) {
                Weather.SANDSTORM ->
                    !mon.species.hasType(PokemonType.ROCK) && !mon.species.hasType(PokemonType.GROUND) &&
                        !mon.species.hasType(PokemonType.STEEL)
                Weather.HAIL -> !mon.species.hasType(PokemonType.ICE)
                else -> false
              }
          if (hurt && !mon.semiInvulnerable) {
            events += BattleEvent.TurnEffect(mon.entityId)
            loseHp(mon, (mon.maxHp / 16).coerceAtLeast(1), events)
          }
        }
      }
    }

    for (mon in order) {
      if (mon.fainted) continue
      val other = battle.opponentOf(mon)
      if (mon.ingrained && mon.currentHp < mon.maxHp) {
        events += BattleEvent.TurnEffect(mon.entityId)
        healHp(mon, (mon.maxHp / 16).coerceAtLeast(1), events)
      }
      if (mon.wishTurns > 0) {
        mon.wishTurns--
        if (mon.wishTurns == 0 && mon.currentHp < mon.maxHp) {
          events += BattleEvent.TurnEffect(mon.entityId)
          healHp(mon, (mon.maxHp / 2).coerceAtLeast(1), events)
        }
      }
      if (mon.leechSeeded && !other.fainted) {
        val drained = (mon.maxHp / 8).coerceAtLeast(1).coerceAtMost(mon.currentHp)
        events += BattleEvent.TurnEffect(other.entityId)
        loseHp(mon, drained, events)
        healHp(other, drained, events)
      }
      if (mon.fainted) continue
      if (StatusCondition.isBadlyPoisoned(mon.status)) {
        mon.toxicCounter = (mon.toxicCounter + 1).coerceAtMost(15)
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp * mon.toxicCounter / 16).coerceAtLeast(1), events)
      } else if (StatusCondition.isPoisoned(mon.status)) {
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp / 8).coerceAtLeast(1), events)
      } else if (StatusCondition.isBurned(mon.status)) {
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp / 8).coerceAtLeast(1), events)
      }
      if (mon.fainted) continue
      if (mon.nightmare && StatusCondition.isAsleep(mon.status)) {
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp / 4).coerceAtLeast(1), events)
      }
      if (mon.fainted) continue
      if (mon.cursed) {
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp / 4).coerceAtLeast(1), events)
      }
      if (mon.fainted) continue
      if (mon.trappedTurns in 1..98) {
        mon.trappedTurns--
        events += BattleEvent.TurnEffect(mon.entityId)
        loseHp(mon, (mon.maxHp / 16).coerceAtLeast(1), events)
      }
      if (mon.fainted) continue
      if (mon.drowsyTurns > 0) {
        mon.drowsyTurns--
        if (mon.drowsyTurns == 0 && !StatusCondition.hasAny(mon.status)) {
          mon.status = StatusCondition.asleep(2 + battle.rng.pick(4))
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.StatusChanged(mon.entityId, mon.status)
        }
      }
    }
    battle.playerSide.tick()
    battle.opponentSide.tick()
  }

  // ---------------------------------------------------------------------------------------------
  // Hidden Power (Gen 3): type and power from the IV low bits.

  private fun hiddenPowerType(mon: BattleMonState): PokemonType {
    val ivs = mon.source.iVs
    val bits =
        (ivs.hp and 1) or ((ivs.atk and 1) shl 1) or ((ivs.def and 1) shl 2) or ((ivs.spd and 1) shl 3) or
            ((ivs.spAtk and 1) shl 4) or ((ivs.spDef and 1) shl 5)
    return HIDDEN_POWER_TYPES[bits * 15 / 63]
  }

  private fun hiddenPowerBase(mon: BattleMonState): Int {
    val ivs = mon.source.iVs
    val bits =
        ((ivs.hp shr 1) and 1) or (((ivs.atk shr 1) and 1) shl 1) or (((ivs.def shr 1) and 1) shl 2) or
            (((ivs.spd shr 1) and 1) shl 3) or (((ivs.spAtk shr 1) and 1) shl 4) or (((ivs.spDef shr 1) and 1) shl 5)
    return bits * 40 / 63 + 30
  }

  private fun battleStat(name: String): BattleStat? =
      when (name) {
        "ATTACK" -> BattleStat.ATTACK
        "DEFENSE" -> BattleStat.DEFENSE
        "SPEED" -> BattleStat.SPEED
        "SPECIAL_ATTACK" -> BattleStat.SP_ATTACK
        "SPECIAL_DEFENSE" -> BattleStat.SP_DEFENSE
        "ACCURACY" -> BattleStat.ACCURACY
        "EVASION" -> BattleStat.EVASION
        else -> null
      }

  private fun stageEffect(effect: MoveEffect): StageEffect? =
      when (effect) {
        MoveEffect.ATTACK_UP -> StageEffect(BattleStat.ATTACK, 1, true)
        MoveEffect.DEFENSE_UP -> StageEffect(BattleStat.DEFENSE, 1, true)
        MoveEffect.SPEED_UP -> StageEffect(BattleStat.SPEED, 1, true)
        MoveEffect.SPECIAL_ATTACK_UP -> StageEffect(BattleStat.SP_ATTACK, 1, true)
        MoveEffect.SPECIAL_DEFENSE_UP -> StageEffect(BattleStat.SP_DEFENSE, 1, true)
        MoveEffect.ACCURACY_UP -> StageEffect(BattleStat.ACCURACY, 1, true)
        MoveEffect.EVASION_UP, MoveEffect.MINIMIZE -> StageEffect(BattleStat.EVASION, 1, true)
        MoveEffect.ATTACK_UP_2 -> StageEffect(BattleStat.ATTACK, 2, true)
        MoveEffect.DEFENSE_UP_2 -> StageEffect(BattleStat.DEFENSE, 2, true)
        MoveEffect.SPEED_UP_2 -> StageEffect(BattleStat.SPEED, 2, true)
        MoveEffect.SPECIAL_ATTACK_UP_2 -> StageEffect(BattleStat.SP_ATTACK, 2, true)
        MoveEffect.SPECIAL_DEFENSE_UP_2 -> StageEffect(BattleStat.SP_DEFENSE, 2, true)
        MoveEffect.ACCURACY_UP_2 -> StageEffect(BattleStat.ACCURACY, 2, true)
        MoveEffect.EVASION_UP_2 -> StageEffect(BattleStat.EVASION, 2, true)
        MoveEffect.ATTACK_DOWN -> StageEffect(BattleStat.ATTACK, -1, false)
        MoveEffect.DEFENSE_DOWN -> StageEffect(BattleStat.DEFENSE, -1, false)
        MoveEffect.SPEED_DOWN -> StageEffect(BattleStat.SPEED, -1, false)
        MoveEffect.SPECIAL_ATTACK_DOWN -> StageEffect(BattleStat.SP_ATTACK, -1, false)
        MoveEffect.SPECIAL_DEFENSE_DOWN -> StageEffect(BattleStat.SP_DEFENSE, -1, false)
        MoveEffect.ACCURACY_DOWN -> StageEffect(BattleStat.ACCURACY, -1, false)
        MoveEffect.EVASION_DOWN -> StageEffect(BattleStat.EVASION, -1, false)
        MoveEffect.ATTACK_DOWN_2 -> StageEffect(BattleStat.ATTACK, -2, false)
        MoveEffect.DEFENSE_DOWN_2 -> StageEffect(BattleStat.DEFENSE, -2, false)
        MoveEffect.SPEED_DOWN_2 -> StageEffect(BattleStat.SPEED, -2, false)
        MoveEffect.SPECIAL_ATTACK_DOWN_2 -> StageEffect(BattleStat.SP_ATTACK, -2, false)
        MoveEffect.SPECIAL_DEFENSE_DOWN_2 -> StageEffect(BattleStat.SP_DEFENSE, -2, false)
        MoveEffect.ACCURACY_DOWN_2 -> StageEffect(BattleStat.ACCURACY, -2, false)
        MoveEffect.EVASION_DOWN_2 -> StageEffect(BattleStat.EVASION, -2, false)
        else -> null
      }

  private fun secondaryStage(effect: MoveEffect): StageEffect? =
      when (effect) {
        MoveEffect.ATTACK_DOWN_HIT -> StageEffect(BattleStat.ATTACK, -1, false)
        MoveEffect.DEFENSE_DOWN_HIT -> StageEffect(BattleStat.DEFENSE, -1, false)
        MoveEffect.SPEED_DOWN_HIT -> StageEffect(BattleStat.SPEED, -1, false)
        MoveEffect.SPECIAL_ATTACK_DOWN_HIT -> StageEffect(BattleStat.SP_ATTACK, -1, false)
        MoveEffect.SPECIAL_DEFENSE_DOWN_HIT -> StageEffect(BattleStat.SP_DEFENSE, -1, false)
        MoveEffect.ACCURACY_DOWN_HIT -> StageEffect(BattleStat.ACCURACY, -1, false)
        MoveEffect.EVASION_DOWN_HIT -> StageEffect(BattleStat.EVASION, -1, false)
        MoveEffect.ATTACK_UP_HIT -> StageEffect(BattleStat.ATTACK, 1, true)
        MoveEffect.DEFENSE_UP_HIT -> StageEffect(BattleStat.DEFENSE, 1, true)
        else -> null
      }

  private companion object {
    val HIDDEN_POWER_TYPES =
        listOf(
            PokemonType.FIGHTING,
            PokemonType.FLYING,
            PokemonType.POISON,
            PokemonType.GROUND,
            PokemonType.ROCK,
            PokemonType.BUG,
            PokemonType.GHOST,
            PokemonType.STEEL,
            PokemonType.FIRE,
            PokemonType.WATER,
            PokemonType.GRASS,
            PokemonType.ELECTRIC,
            PokemonType.PSYCHIC,
            PokemonType.ICE,
            PokemonType.DRAGON,
            PokemonType.DARK,
        )
  }
}
