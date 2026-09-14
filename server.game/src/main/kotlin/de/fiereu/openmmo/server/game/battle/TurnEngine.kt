package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.DamageCategory
import de.fiereu.openmmo.common.enums.MoveAdditionalEffect
import de.fiereu.openmmo.common.enums.MoveEffect
import de.fiereu.openmmo.common.enums.MoveFlag
import de.fiereu.openmmo.common.enums.MoveTarget
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.moves.AdditionalEffect
import de.fiereu.openmmo.moves.MoveDef
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.battle.BattleLine
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

internal data class TurnAction(
    val attacker: BattleMonState,
    val defender: BattleMonState,
    val move: MoveDef?,
    /** One of several targets of a spread move: damage is three quarters. */
    val spread: Boolean = false,
    /** A later target of the same use: the pp is spent and the move announced on the first. */
    val followUp: Boolean = false,
    /** Sent back by Magic Coat or Magic Bounce, so it cannot bounce again. */
    val reflected: Boolean = false,
)

/** A turn paused for the player's pick after U-turn or Baton Pass: the actions still to run. */
internal class TurnResume(
    val remaining: List<TurnAction>,
    val charging: MutableSet<BattleMonState>,
    val faintedBefore: Pair<Int, Int>,
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
    itemRegistry: de.fiereu.openmmo.items.ItemRegistry = de.fiereu.openmmo.items.ItemRegistry(),
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry = de.fiereu.openmmo.pokemon.SpeciesRegistry(),
    private val formChanges: de.fiereu.openmmo.pokemon.expansion.FormChangeRegistry =
        de.fiereu.openmmo.pokemon.expansion.FormChangeRegistry(),
) {
  private val items = HeldItems(itemRegistry)

  // ---------------------------------------------------------------------------------------------
  // Form changes: the Expansion tables drive Zen Mode, Stance Change, Disguise and the rest

  private fun abilityMatches(mon: BattleMonState, symbol: String?): Boolean =
      symbol == null || !symbol.startsWith("ABILITY_") || mon.ability.name == symbol.removePrefix("ABILITY_")

  private fun moveMatches(move: MoveDef, symbol: String?): Boolean =
      symbol != null && symbol.startsWith("MOVE_") &&
          symbol.removePrefix("MOVE_").filter { it.isLetterOrDigit() } == move.name.uppercase().filter { it.isLetterOrDigit() }

  private fun categoryMatches(move: MoveDef, symbol: String?): Boolean =
      when (symbol) {
        "DAMAGE_CATEGORY_PHYSICAL" -> move.category == DamageCategory.PHYSICAL
        "DAMAGE_CATEGORY_SPECIAL" -> move.category == DamageCategory.SPECIAL
        "DAMAGE_CATEGORY_STATUS" -> move.power == 0
        else -> false
      }

  private fun weatherMatches(weather: Weather?, symbol: String?): Boolean {
    if (symbol == null) return false
    val negated = symbol.startsWith("~")
    val wanted =
        when (symbol.removePrefix("~")) {
          "B_WEATHER_SUN", "B_WEATHER_SUN_PRIMAL", "B_WEATHER_SUN_ANY" -> Weather.SUN
          "B_WEATHER_RAIN", "B_WEATHER_RAIN_PRIMAL", "B_WEATHER_RAIN_ANY" -> Weather.RAIN
          "B_WEATHER_SANDSTORM" -> Weather.SANDSTORM
          "B_WEATHER_HAIL", "B_WEATHER_SNOW", "B_WEATHER_ICY_ANY" -> Weather.HAIL
          "B_WEATHER_NONE" -> null
          else -> return false
        }
    return if (negated) weather != wanted else weather == wanted
  }

  /** Params: ability, HP_HIGHER_THAN / HP_LOWER_EQ_THAN, threshold percent, then a level or move. */
  private fun hpPercentMatches(mon: BattleMonState, change: de.fiereu.openmmo.pokemon.expansion.FormChange): Boolean {
    val p = change.params
    if (p.size < 3 || !abilityMatches(mon, p[0])) return false
    val threshold = p[2].toIntOrNull() ?: return false
    val pct = mon.currentHp * 100 / mon.maxHp.coerceAtLeast(1)
    return when (p[1]) {
      "HP_HIGHER_THAN" -> pct > threshold
      "HP_LOWER_EQ_THAN" -> pct <= threshold
      else -> false
    }
  }

  /** Swaps [mon] into the form [change] names; stats follow the new species, hp stays. */
  private fun changeForm(
      mon: BattleMonState,
      change: de.fiereu.openmmo.pokemon.expansion.FormChange,
      events: MutableList<BattleEvent>?,
  ): Boolean {
    val target = formChanges.targetServerId(change) ?: return false
    // A row pointing back at the species it came in as restores the very definition it started with.
    val def =
        if (de.fiereu.openmmo.common.clientSpeciesId(target) == de.fiereu.openmmo.common.clientSpeciesId(mon.originalSpecies.id)) mon.originalSpecies
        else speciesRegistry.get(target) ?: return false
    if (def.id == mon.species.id) return false
    mon.species = def
    mon.stats = StatCalculator.computeAll(def, mon.source).copy(hp = mon.stats.hp)
    if (events != null) {
      if (change.params.any { it.startsWith("ABILITY_") }) events += BattleEvent.AbilityShown(mon.entityId, mon.ability)
      events += BattleEvent.SpeciesShown(mon.entityId, mon.wireSpeciesId().toInt())
    }
    return true
  }

  /**
   * Transform and Imposter (the Expansion's Cmd_transformdataexecution): [mon] takes [target]'s
   * species, stats but hp, stat stages, ability and moves, each move at 5 pp or its maximum if
   * lower, until it leaves the field. Fails on a target that is out of reach, already transformed
   * or disguised, and for a user that already transformed.
   */
  private fun transform(mon: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>): Boolean {
    if (target === mon || target.fainted || target.semiInvulnerable || target.transformed || target.illusionOf != null ||
        target.substituteHp > 0 || mon.transformed) return false
    mon.transformedFrom = mon.moves.map { PokemonMove(it.id, it.pp) }
    mon.species = target.species
    mon.typeOverride = target.typeOverride
    mon.stats = target.stats.copy(hp = mon.stats.hp)
    for (stat in BattleStat.entries) mon.changeStage(stat, target.stage(stat) - mon.stage(stat))
    mon.ability = target.ability
    mon.choiceLockedMove = 0
    mon.moves.clear()
    for (move in target.moves) {
      val pp = if (move.id.toInt() == 0) 0 else (moves.get(move.id.toInt())?.pp ?: TRANSFORM_PP).coerceAtMost(TRANSFORM_PP)
      mon.moves += PokemonMove(move.id, pp.toByte())
    }
    events +=
        BattleEvent.Transformed(
            transformerId = mon.entityId,
            copiedId = target.entityId,
            wireSpecies = mon.wireSpeciesId().toInt(),
            personality = target.source.seed,
            moves = mon.moves.map { it.id },
            ability = mon.ability,
            stages = BattleStat.entries.associateWith { mon.stage(it) },
            type1 = mon.type1,
            type2 = mon.type2,
            // Gen 4+ Transform takes on the target's shininess too.
            shiny = target.source.isShiny)
    return true
  }

  /** Undoes [transform]: the real species, stats and moves come back. */
  private fun revertTransform(mon: BattleMonState) {
    val own = mon.transformedFrom ?: return
    mon.moves.clear()
    mon.moves += own
    mon.transformedFrom = null
    mon.typeOverride = null
    mon.species = mon.originalSpecies
    mon.stats = StatCalculator.computeAll(mon.originalSpecies, mon.source).copy(hp = mon.stats.hp)
    mon.ability = Abilities.of(mon.species, mon.source)
  }

  /**
   * What a move just used leaves behind: the memory Encore, Disable, Torment, Mirror Move, Copycat
   * and Stomping Tantrum read, the streaks of Fury Cutter, Rollout and Echoed Voice, and Last
   * Resort's list. A turn lost before the move went out (sleep, flinch) records nothing.
   */
  private fun recordMove(battle: BattleInstance, action: TurnAction, produced: List<BattleEvent>) {
    val mon = action.attacker
    val move = action.move ?: return
    if (produced.none { it is BattleEvent.MoveUsed && it.attackerId == mon.entityId }) return
    val failed =
        produced.any {
          (it is BattleEvent.MoveWithoutTarget && it.attackerId == mon.entityId) || it is BattleEvent.Immune || it is BattleEvent.Protected
        }
    mon.consecutive = if (failed) 0 else if (mon.lastMoveId == move.id && !mon.lastMoveFailed) mon.consecutive + 1 else 1
    mon.lastMoveId = move.id
    mon.lastMoveFailed = failed
    mon.usedMoves += move.id
    battle.field.lastMoveId = move.id
    battle.field.lastMoveThisTurn = move.id
    if (move.effect == MoveEffect.ROUND && !failed) battle.sideOf(mon).roundUsedThisTurn = true
    if (move.effect == MoveEffect.ECHOED_VOICE && !failed) battle.field.echoedVoiceUsedThisTurn = true
    // Charge lasts until the next Electric attack.
    if (move.type == PokemonType.ELECTRIC && move.power > 0) mon.charged = false
    if (mon.encoreTurns > 0 && mon.moves.none { it.id.toInt() == mon.encoreMoveId && it.pp > 0 }) {
      mon.encoreTurns = 0
      mon.encoreMoveId = 0
    }
  }

  /** Applies [move]'s stat changes from the data to [target]; true when any stage moved. */
  private fun moveStats(action: TurnAction, move: MoveDef, target: BattleMonState, events: MutableList<BattleEvent>): Boolean {
    val onUser = target === action.attacker
    val aimed = if (onUser) action else action.copy(defender = target)
    var applied = false
    for (extra in move.additionalEffects) {
      val sign = if (extra.effect == MoveAdditionalEffect.STAT_MINUS) -1 else 1
      for ((name, stages) in extra.stats) {
        val stat = battleStat(name) ?: continue
        if (applyStage(aimed, StageEffect(stat, sign * stages, onUser), events)) applied = true
      }
    }
    return applied
  }

  /**
   * On the ground for terrains and the floor hazards: not a Flying type, Levitating, magnetised or
   * held up by an Air Balloon - unless Gravity, Smack Down or an Iron Ball holds it down.
   */
  private fun isGrounded(battle: BattleInstance, mon: BattleMonState): Boolean {
    if (battle.field.gravityTurns > 0 || mon.grounded || items.of(mon) == de.fiereu.openmmo.items.generated.Items.IRON_BALL) return true
    return !(mon.hasType(PokemonType.FLYING) || mon.ability == Ability.LEVITATE || mon.magnetRiseTurns > 0 || mon.telekinesisTurns > 0 ||
        (items.of(mon) == de.fiereu.openmmo.items.generated.Items.AIR_BALLOON && !mon.airBalloonPopped))
  }

  /** The type the current terrain lends Terrain Pulse and Camouflage. */
  private fun terrainType(battle: BattleInstance): PokemonType? =
      when (battle.field.terrain) {
        Terrain.ELECTRIC -> PokemonType.ELECTRIC
        Terrain.GRASSY -> PokemonType.GRASS
        Terrain.MISTY -> PokemonType.FAIRY
        Terrain.PSYCHIC -> PokemonType.PSYCHIC
        null -> null
      }

  private fun team(battle: BattleInstance, mon: BattleMonState): List<BattleMonState> =
      if (battle.isPlayerSide(mon.entityId)) battle.party else battle.opponent

  /** Teatime: [mon] eats its berry now, getting what the berry gives. */
  private fun eatBerry(mon: BattleMonState, events: MutableList<BattleEvent>) {
    val eaten = items.of(mon) ?: return
    consumeItem(mon, events)
    val heal = items.halfHpHeal(eaten, mon.maxHp) + items.quarterHpHeal(eaten, mon.maxHp)
    if (heal > 0) healHp(mon, heal, events)
    items.pinchStat(eaten)?.let { ownStage(mon, it, 1, events) }
  }

  /** After You pulls [mon]'s remaining actions right behind the current one; Quash sends them last. */
  private fun reorderQueue(queue: MutableList<TurnAction>, current: Int, mon: BattleMonState, next: Boolean) {
    val later = queue.subList(current + 1, queue.size)
    val moved = later.filter { it.attacker === mon }
    if (moved.isEmpty()) return
    later.removeAll { it.attacker === mon }
    if (next) queue.addAll(current + 1, moved) else queue.addAll(moved)
  }

  /**
   * Pursuit: a foe that knows it catches [leaving] as the player switches it out, at double power,
   * and spends its turn doing so.
   */
  fun pursuit(battle: BattleInstance, leaving: BattleMonState): List<BattleEvent> {
    val events = mutableListOf<BattleEvent>()
    for (foe in battle.foesOf(leaving)) {
      if (foe.movedThisTurn) continue
      val known = foe.moves.firstOrNull { m -> m.pp > 0 && moves.get(m.id.toInt())?.effect == MoveEffect.PURSUIT } ?: continue
      val move = moves.get(known.id.toInt()) ?: continue
      leaving.pursued = true
      execute(battle, TurnAction(foe, leaving, move), movesLast = false, events)
      leaving.pursued = false
      if (leaving.fainted) break
    }
    return events
  }

  /** Beat Up's strikers: every party member still standing and free of status, the user included. */
  private fun beatUpCrew(battle: BattleInstance, mon: BattleMonState): List<BattleMonState> =
      team(battle, mon).filter { !it.fainted && !StatusCondition.hasAny(it.status) }

  /** The type a move's data names (Soak's Water, Burn Up's Fire). */
  private fun argType(move: MoveDef): PokemonType? =
      move.argument?.value?.removePrefix("TYPE_")?.let { runCatching { PokemonType.valueOf(it) }.getOrNull() }

  /** Defog and Tidy Up sweep every entry hazard off both sides. */
  private fun clearHazards(battle: BattleInstance) {
    for (side in listOf(battle.playerSide, battle.opponentSide)) {
      side.spikes = 0
      side.toxicSpikes = 0
      side.stealthRock = false
      side.stickyWeb = false
    }
  }

  /** Earlier successful uses in a row of [move], for the moves that grow with each one. */
  private fun priorStreak(mon: BattleMonState, move: MoveDef): Int =
      if (mon.lastMoveId == move.id && !mon.lastMoveFailed) mon.consecutive else 0

  private fun positiveStages(mon: BattleMonState): Int = BattleStat.entries.sumOf { mon.stage(it).coerceAtLeast(0) }

  /** Sound moves, the moves marked for it and Infiltrator go straight past a Substitute. */
  private fun bypassesSubstitute(attacker: BattleMonState, move: MoveDef): Boolean =
      move.hasFlag(MoveFlag.IGNORES_SUBSTITUTE) || move.hasFlag(MoveFlag.SOUND) || attacker.ability == Ability.INFILTRATOR

  /** Follow Me and Rage Powder draw every single-target move from the other side onto their user. */
  private fun attention(battle: BattleInstance, action: TurnAction): TurnAction {
    val move = action.move ?: return action
    // Snipe Shot ignores the lure.
    if (action.spread || move.target != MoveTarget.SELECTED || move.effect == MoveEffect.SNIPE_SHOT) return action
    val defenderOnPlayerSide = battle.isPlayerSide(action.defender.entityId)
    if (defenderOnPlayerSide == battle.isPlayerSide(action.attacker.entityId)) return action
    val lure =
        (if (defenderOnPlayerSide) battle.playerActives() else battle.opponentActives()).firstOrNull { it.centerOfAttention && !it.fainted }
            ?: return action
    return action.copy(defender = lure)
  }

  /** Spit Up and Swallow use the stockpile up and take back the stages it gave. */
  private fun releaseStockpile(mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.stockpile == 0) return
    if (mon.stockpileDef > 0) ownStage(mon, BattleStat.DEFENSE, -mon.stockpileDef, events)
    if (mon.stockpileSpDef > 0) ownStage(mon, BattleStat.SP_DEFENSE, -mon.stockpileSpDef, events)
    mon.stockpile = 0
    mon.stockpileDef = 0
    mon.stockpileSpDef = 0
  }

  /**
   * What waits for [mon] as it comes in, once per stay: a Healing Wish or Lunar Dance, then Stealth
   * Rock, and for a grounded monster Spikes, Toxic Spikes and Sticky Web.
   */
  private fun arrive(battle: BattleInstance, mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.arrived) return
    mon.arrived = true
    val side = battle.sideOf(mon)
    if (side.healingWish != 0) {
      val lunar = side.healingWish == 2
      side.healingWish = 0
      events += BattleEvent.TurnEffect(mon.entityId)
      healHp(mon, mon.maxHp, events)
      cureStatus(mon, events)
      if (lunar) for (slot in mon.moves) moves.get(slot.id.toInt())?.let { slot.pp = it.pp.toByte() }
    }
    fun hurt(amount: Int) {
      if (mon.fainted || Abilities.noIndirectDamage(mon)) return
      mon.currentHp = (mon.currentHp - amount.coerceAtLeast(1)).coerceAtLeast(0)
      events += BattleEvent.TurnEffect(mon.entityId)
      events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
      if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
    }
    if (side.stealthRock) hurt(mon.maxHp * effectivenessAgainst(PokemonType.ROCK, mon) / TypeChart.NEUTRAL / 8)
    if (!isGrounded(battle, mon)) return
    if (side.spikes > 0) hurt(mon.maxHp / when (side.spikes) { 1 -> 8; 2 -> 6; else -> 4 })
    if (side.toxicSpikes > 0 && !mon.fainted) {
      val status = if (side.toxicSpikes >= 2) StatusCondition.TOXIC else StatusCondition.POISON
      if (mon.hasType(PokemonType.POISON)) side.toxicSpikes = 0
      else if (!mon.hasType(PokemonType.STEEL) && canReceiveStatus(battle, mon, mon, status)) inflictStatus(battle, mon, mon, status, events)
    }
    if (side.stickyWeb && !mon.fainted) ownStage(mon, BattleStat.SPEED, -1, events)
  }

  /** Applies the first row of [kind] whose params pass [filter]; false when nothing changed. */
  private fun formChange(
      mon: BattleMonState,
      kind: String,
      events: MutableList<BattleEvent>?,
      filter: (de.fiereu.openmmo.pokemon.expansion.FormChange) -> Boolean = { true },
  ): Boolean {
    if (mon.fainted && kind != "FORM_CHANGE_FAINT") return false
    for (change in formChanges.of(mon.species.id)) {
      if (change.kind != kind || !filter(change)) continue
      if (changeForm(mon, change, events)) return true
    }
    return false
  }

  private fun weatherForms(battle: BattleInstance, events: MutableList<BattleEvent>) {
    val weather = weather(battle)
    for (mon in battle.actives()) {
      formChange(mon, "FORM_CHANGE_BATTLE_WEATHER", events) {
        weatherMatches(weather, it.params.getOrNull(0)) && abilityMatches(mon, it.params.getOrNull(1))
      }
    }
  }

  /**
   * Puts the benched [newIndex] on [position] of one side: the outgoing monster's leaving effects
   * (Natural Cure, Regenerator, its form and volatiles), Baton Pass's hand-over, the client's
   * switch-in ([kind] [RECALLED] recalls a monster still standing, [DRAGGED_IN] skips that) and the
   * entering effects. Every switch goes through here.
   */
  fun swapIn(
      battle: BattleInstance,
      playerSide: Boolean,
      position: Int,
      newIndex: Int,
      kind: Int,
      batonPass: Boolean,
      events: MutableList<BattleEvent>,
  ) {
    val positions = if (playerSide) battle.playerPositions else battle.opponentPositions
    val team = if (playerSide) battle.party else battle.opponent
    val seen = if (playerSide) battle.seenActive else battle.opponentSeen
    val oldIndex = positions[position]
    val outgoing = team[oldIndex]
    val passed = if (batonPass && !outgoing.fainted) outgoing.batonPass() else null
    if (!outgoing.fainted) {
      if (outgoing.ability == Ability.NATURAL_CURE) outgoing.status = 0
      if (outgoing.ability == Ability.REGENERATOR) outgoing.currentHp = (outgoing.currentHp + outgoing.maxHp / 3).coerceAtMost(outgoing.maxHp)
    }
    switchOut(battle, outgoing)
    outgoing.resetVolatile()
    val fullBlock = newIndex !in seen
    positions[position] = newIndex
    seen.add(newIndex)
    val incoming = team[newIndex]
    prepareIllusion(battle, incoming)
    passed?.let { incoming.receive(it) }
    events += BattleEvent.SwitchedIn(playerSide, position, oldIndex, fullBlock, kind)
    switchIn(battle, incoming, events)
  }

  /** The party indexes of [playerSide]'s side that could come in: standing and not on the field. */
  private fun bench(battle: BattleInstance, playerSide: Boolean): List<Int> {
    val positions = if (playerSide) battle.playerPositions else battle.opponentPositions
    val team = if (playerSide) battle.party else battle.opponent
    return team.indices.filter { !team[it].fainted && it !in positions }
  }

  /**
   * U-turn, Volt Switch, Flip Turn, Parting Shot, Baton Pass, Chilly Reception: [mon] leaves for a
   * benched partner. The player picks it (the turn pauses on [BattleInstance.pendingSelfSwitch]);
   * the opponent sends its next. False when nobody can come in.
   */
  private fun selfSwitch(battle: BattleInstance, mon: BattleMonState, batonPass: Boolean, events: MutableList<BattleEvent>): Boolean {
    if (mon.fainted || battle.opponent.all { it.fainted } || battle.party.all { it.fainted }) return false
    val playerSide = battle.isPlayerSide(mon.entityId)
    val position = battle.positionOf(mon)
    val bench = bench(battle, playerSide)
    if (position < 0 || bench.isEmpty()) return false
    if (playerSide) battle.pendingSelfSwitch = SelfSwitch(position, batonPass)
    else swapIn(battle, false, position, bench.first(), RECALLED, batonPass, events)
    return true
  }

  /**
   * Roar, Whirlwind, Dragon Tail, Circle Throw: [target] is forced out. In a trainer battle a random
   * benched partner is dragged in with no recall; a wild battle simply ends ("fled from battle" for
   * Roar, "blew away" otherwise). Ingrain and Suction Cups hold on.
   */
  private fun dragOut(battle: BattleInstance, user: BattleMonState, target: BattleMonState, move: MoveDef, events: MutableList<BattleEvent>): Boolean {
    if (target.fainted || target.ingrained || target.ability == Ability.SUCTION_CUPS) return false
    if (battle.trainer == null) {
      if (!battle.escapable) return false
      events += BattleEvent.BlownAway(user.entityId, target.entityId, move.id)
      battle.moveEnded = MoveEnding.BLOWN_AWAY
      return true
    }
    val playerSide = battle.isPlayerSide(target.entityId)
    val position = battle.positionOf(target)
    val bench = bench(battle, playerSide)
    if (position < 0 || bench.isEmpty()) return false
    swapIn(battle, playerSide, position, bench[battle.rng.pick(bench.size)], DRAGGED_IN, false, events)
    return true
  }

  /** A monster leaving the field: its switch-out form, else the one it came in with. Silent. */
  fun switchOut(battle: BattleInstance, mon: BattleMonState) {
    revertTransform(mon)
    if (formChange(mon, "FORM_CHANGE_BATTLE_SWITCH_OUT", null) { abilityMatches(mon, it.params.getOrNull(0)) }) return
    restoreForm(mon)
  }

  private fun restoreForm(mon: BattleMonState) {
    if (mon.species.id == mon.originalSpecies.id) return
    mon.species = mon.originalSpecies
    mon.stats = StatCalculator.computeAll(mon.originalSpecies, mon.source).copy(hp = mon.stats.hp)
  }

  /** Every party member goes back to its real species; the client's records are put right. */
  fun endBattle(battle: BattleInstance): List<BattleEvent> {
    val events = mutableListOf<BattleEvent>()
    for (mon in battle.party + battle.opponent) {
      if (mon.mimicked != null) {
        mon.restoreMimic()
        events += BattleEvent.MovesChanged(mon.entityId, mon.moves.map { it.id to it.pp })
      }
      val transformed = mon.transformed
      revertTransform(mon)
      if (mon.species.id == mon.originalSpecies.id && !transformed) continue
      restoreForm(mon)
      events += BattleEvent.SpeciesShown(mon.entityId, mon.wireSpeciesId().toInt())
      if (transformed) events += BattleEvent.MovesChanged(mon.entityId, mon.moves.map { it.id to it.pp })
    }
    return events
  }

  // ---------------------------------------------------------------------------------------------
  // Held items: consumption and the lines that go with it

  /** The monster eats or uses up its item; Unburden and Cheek Pouch react. */
  private fun consumeItem(mon: BattleMonState, events: MutableList<BattleEvent>) {
    val item = items.get(mon.heldItem)
    mon.consumedItem = mon.heldItem
    mon.heldItem = 0
    if (mon.ability == Ability.UNBURDEN) mon.unburdened = true
    events += BattleEvent.ItemChanged(mon.entityId, 0)
    if (items.isBerry(item) && mon.ability == Ability.CHEEK_POUCH && mon.currentHp < mon.maxHp) {
      events += BattleEvent.AbilityShown(mon.entityId, Ability.CHEEK_POUCH)
      healHp(mon, (mon.maxHp / 3).coerceAtLeast(1), events)
    }
  }

  private fun canEatBerry(battle: BattleInstance, mon: BattleMonState): Boolean =
      battle.foesOf(mon).none { it.ability == Ability.UNNERVE }

  private fun ripen(mon: BattleMonState, amount: Int): Int = if (mon.ability == Ability.RIPEN) amount * 2 else amount

  /** Healing and pinch berries fire the moment hp crosses their threshold. */
  private fun checkBerries(battle: BattleInstance, mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.fainted || mon.heldItem == 0) return
    val item = items.of(mon) ?: return
    if (!items.isBerry(item) && item != de.fiereu.openmmo.items.generated.Items.BERRY_JUICE) return
    if (items.isBerry(item) && !canEatBerry(battle, mon)) return
    val pinch = mon.ability == Ability.GLUTTONY && mon.currentHp * 2 <= mon.maxHp || mon.currentHp * 4 <= mon.maxHp
    val half = mon.currentHp * 2 <= mon.maxHp
    val itemId = mon.heldItem
    val halfHeal = items.halfHpHeal(item, mon.maxHp)
    if (halfHeal > 0 && half) {
      consumeItem(mon, events)
      mon.currentHp = (mon.currentHp + ripen(mon, halfHeal)).coerceAtMost(mon.maxHp)
      events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL, listOf(itemId, mon.currentHp))
      return
    }
    val quarterHeal = items.quarterHpHeal(item, mon.maxHp)
    if (quarterHeal > 0 && pinch) {
      consumeItem(mon, events)
      mon.currentHp = (mon.currentHp + ripen(mon, quarterHeal)).coerceAtMost(mon.maxHp)
      events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL, listOf(itemId, mon.currentHp))
      return
    }
    if (!pinch) return
    items.pinchStat(item)?.let { stat ->
      consumeItem(mon, events)
      ownStage(mon, stat, if (mon.ability == Ability.RIPEN) 2 else 1, events)
      return
    }
    when (item) {
      de.fiereu.openmmo.items.generated.Items.LANSAT_BERRY -> { consumeItem(mon, events); mon.focusEnergy = true }
      de.fiereu.openmmo.items.generated.Items.STARF_BERRY -> {
        consumeItem(mon, events)
        val stats = listOf(BattleStat.ATTACK, BattleStat.DEFENSE, BattleStat.SP_ATTACK, BattleStat.SP_DEFENSE, BattleStat.SPEED)
        ownStage(mon, stats[battle.rng.pick(stats.size)], 2, events)
      }
      de.fiereu.openmmo.items.generated.Items.CUSTAP_BERRY -> { consumeItem(mon, events); mon.custapReady = true }
      de.fiereu.openmmo.items.generated.Items.MICLE_BERRY -> { consumeItem(mon, events); mon.micleBoost = true }
      else -> Unit
    }
  }

  /** Status berries and Lum fire as soon as the status lands. */
  private fun checkStatusBerry(battle: BattleInstance, mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.fainted || !StatusCondition.hasAny(mon.status)) return
    val item = items.of(mon) ?: return
    if (!canEatBerry(battle, mon)) return
    val cures = items.curedStatus(item)
    if (mon.status and cures == 0) return
    val itemId = mon.heldItem
    consumeItem(mon, events)
    mon.status = StatusCondition.NONE
    mon.toxicCounter = 0
    mon.nightmare = false
    // The client clears the status and prints "cured its ..." from this line on its own.
    events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_CURED_STATUS, listOf(itemId))
  }

  private fun checkConfusionBerry(battle: BattleInstance, mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.fainted || mon.confusionTurns == 0) return
    val item = items.of(mon) ?: return
    if (!items.curesConfusion(item) || !canEatBerry(battle, mon)) return
    val itemId = mon.heldItem
    consumeItem(mon, events)
    mon.confusionTurns = 0
    events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_RESTORED_STATUS, listOf(itemId))
    events += BattleEvent.Line(mon.entityId, BattleLine.CONFUSION, listOf(1))
  }

  /** Thief, Covet, Pickpocket, Magician: [thief] takes [victim]'s item when it holds none. */
  private fun stealItem(thief: BattleMonState, victim: BattleMonState, events: MutableList<BattleEvent>): Boolean {
    if (thief.heldItem != 0 || !items.canBeTaken(victim)) return false
    val taken = victim.heldItem
    victim.heldItem = 0
    thief.heldItem = taken
    events += BattleEvent.ItemChanged(victim.entityId, 0)
    events += BattleEvent.ItemChanged(thief.entityId, taken)
    events += BattleEvent.Line(thief.entityId, BattleLine.ITEM_RECEIVED, listOf(taken))
    return true
  }

  /** Singles: the player's one monster uses [playerMoveId] on the one foe. */
  fun resolveTurn(battle: BattleInstance, playerMoveId: Short): List<BattleEvent> =
      resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, playerMoveId, 1, 0)))

  /**
   * One turn over the whole field: every move the player chose for their positions plus one move
   * per opposing monster, ordered by priority and speed together. A spread move becomes one action
   * per target, the follow-ups riding under the first one's announcement. A single-target move
   * whose target fell before it goes out is redirected to another foe on that side.
   */
  fun resolveTurn(battle: BattleInstance, chosen: List<ChosenAction>): List<BattleEvent> {
    val events = mutableListOf<BattleEvent>()
    val faintedBefore = battle.party.count { it.fainted } to battle.opponent.count { it.fainted }
    battle.field.lastMoveThisTurn = 0
    battle.playerSide.roundUsedThisTurn = false
    battle.opponentSide.roundUsedThisTurn = false
    battle.field.ionDeluge = false
    battle.raid?.let { raidTurnStart(battle, it, events) }
    val actions = mutableListOf<TurnAction>()
    for (choice in chosen) {
      if (choice.kind != ChosenAction.Kind.MOVE) continue
      val mon = battle.monAt(0, choice.position) ?: continue
      if (mon.fainted) continue
      val move = chooseMove(battle, mon, moves.get(choice.moveId.toInt()))
      // The client packs the target as (position << 4) | side and lets the player point any
      // single-target move at a partner (captured: position 1 aiming at position 0 sent 0x00). The
      // user's own cell is what an attack with nothing chosen sends in singles, so that one only
      // counts for a move that can aim at its user.
      val chosen =
          battle.monAt(choice.targetSide, choice.targetPosition)?.takeIf {
            choice.targetSide != 0 || it !== mon || move?.target in OWN_SIDE_TARGETS
          }
      actions += targetsFor(battle, mon, move, chosen)
    }
    for (enemy in battle.opponentActives()) {
      // One that already acted this turn (Pursuit on a switch) has nothing left to do.
      if (enemy.fainted || enemy.movedThisTurn) continue
      val move = chooseMove(battle, enemy, pickEnemyMove(battle, enemy))
      // A foe out of reach (the far corner of a triple battle) is not a target.
      val victims = battle.foesOf(enemy).filter { battle.reaches(enemy, it) }.ifEmpty { battle.foesOf(enemy) }
      val target = if (victims.isEmpty()) null else victims[battle.rng.pick(victims.size)]
      actions += targetsFor(battle, enemy, move, target)
    }

    val ordered = order(battle, actions)
    battle.plannedMoves.clear()
    for (planned in actions) {
      if (planned.followUp) continue
      val plannedMove = planned.move ?: continue
      battle.plannedMoves[planned.attacker] = plannedMove
      // Beak Blast heats up before anyone moves: contact from then on burns.
      if (plannedMove.effect == MoveEffect.BEAK_BLAST) planned.attacker.beakBlast = true
    }
    return runActions(battle, ordered, mutableSetOf(), faintedBefore, events)
  }

  /**
   * Picks a paused turn back up once the player chose who comes in for U-turn or Baton Pass (the
   * service answered [BattleInstance.pendingSelfSwitch]): the remaining actions, then the turn's end.
   */
  fun resumeTurn(battle: BattleInstance): List<BattleEvent> {
    val resume = battle.turnResume ?: return emptyList()
    battle.turnResume = null
    return runActions(battle, resume.remaining, resume.charging, resume.faintedBefore, mutableListOf())
  }

  private fun runActions(
      battle: BattleInstance,
      queue: List<TurnAction>,
      charging: MutableSet<BattleMonState>,
      faintedBefore: Pair<Int, Int>,
      events: MutableList<BattleEvent>,
  ): List<BattleEvent> {
    // A mutable queue: After You and Quash move actions within it.
    val pending = queue.toMutableList()
    var index = -1
    while (++index < pending.size) {
      val planned = pending[index]
      if (planned.attacker.fainted) continue
      // Switched out before its turn came (dragged out, or U-turned away).
      if (battle.positionOf(planned.attacker) < 0) continue
      // A spread move that only charged this turn has no further targets to hit.
      if (planned.followUp && planned.attacker in charging) continue
      val action = redirect(battle, planned)?.let { attention(battle, it) } ?: continue
      val wasCharging = action.attacker.chargingMoveId != 0
      val before = events.size
      execute(battle, action, movesLast = index == pending.lastIndex, events)
      if (!action.followUp) recordMove(battle, action, events.subList(before, events.size))
      if (!wasCharging && action.attacker.chargingMoveId != 0) charging += action.attacker
      battle.reorder?.let { (mon, next) ->
        battle.reorder = null
        reorderQueue(pending, index, mon, next)
      }
      // A move ended the battle, or the player owes a pick before the rest of the turn.
      if (battle.moveEnded != null) return events
      if (battle.pendingSelfSwitch != null) {
        battle.turnResume = TurnResume(pending.drop(index + 1), charging, faintedBefore)
        return events
      }
      if (battle.opponentActives().all { it.fainted } || battle.playerActives().all { it.fainted }) break
    }
    // End-of-turn effects run whenever the battle goes on, replacement pending or not - the
    // cartridges tick poison on the survivor before the next monster comes out. Gating on both
    // sides' ACTIVES standing skipped the whole phase every time a foe fell, so a poisoned
    // player who one-shot each opponent never took poison damage at all (2026-09-08).
    if (battle.party.any { !it.fainted } && battle.opponent.any { !it.fainted }) endOfTurn(battle, events)
    battle.playerSide.faintedLastTurn = battle.party.count { it.fainted } > faintedBefore.first
    battle.opponentSide.faintedLastTurn = battle.opponent.count { it.fainted } > faintedBefore.second
    for (mon in battle.actives()) mon.endTurn()
    return events
  }

  /** A voluntary switch spends the player's turn, so the enemy attacks the incoming monster. */
  fun resolveSwitchTurn(battle: BattleInstance): List<BattleEvent> = resolveTurn(battle, emptyList())

  /**
   * The actions one use of [move] by [mon] turns into: several for a spread move (every standing
   * foe, plus the partner for the moves that hit it too), one otherwise. [chosen] is the target the
   * player pointed at; a self or field move keeps a foe as its nominal defender, as the effect
   * handlers already read the user off the action.
   */
  private fun targetsFor(battle: BattleInstance, mon: BattleMonState, move: MoveDef?, chosen: BattleMonState?): List<TurnAction> {
    val fallback = chosen?.takeIf { !it.fainted } ?: battle.opponentOf(mon)
    if (move == null) return listOf(TurnAction(mon, fallback, null))
    // In a triple battle a spread only reaches neighbours, and the raid's side never hits its own.
    val raidSide = battle.raid != null && !battle.isPlayerSide(mon.entityId)
    val targets =
        when (move.target) {
          // Spikes, Toxic Spikes, Stealth Rock and Sticky Web (the only OPPONENTS_FIELD moves, all
          // status) lay one hazard on the foe's side: one action, or every further target failed.
          MoveTarget.OPPONENTS_FIELD -> return listOf(TurnAction(mon, fallback, move))
          MoveTarget.BOTH -> battle.foesOf(mon).filter { battle.reaches(mon, it) }
          MoveTarget.FOES_AND_ALLY, MoveTarget.ALL_BATTLERS ->
              battle.foesOf(mon).filter { battle.reaches(mon, it) } +
                  (if (raidSide) emptyList() else battle.alliesOf(mon).filter { battle.reaches(mon, it) })
          // Coaching and Aromatic Mist go to the partner, never to a foe.
          MoveTarget.ALLY ->
              return listOf(
                  TurnAction(mon, chosen?.takeIf { c -> battle.alliesOf(mon).any { it === c } } ?: battle.alliesOf(mon).firstOrNull() ?: fallback, move))
          else -> return listOf(TurnAction(mon, fallback, move))
        }
    if (targets.size <= 1) return listOf(TurnAction(mon, targets.firstOrNull() ?: fallback, move))
    return targets.mapIndexed { i, target -> TurnAction(mon, target, move, spread = true, followUp = i > 0) }
  }

  /** A single-target action whose target already fell goes to another foe on that side, or nowhere. */
  private fun redirect(battle: BattleInstance, action: TurnAction): TurnAction? {
    // A target that fell, or left the field in the meantime (U-turn, Roar), is replaced.
    if ((!action.defender.fainted && battle.positionOf(action.defender) >= 0) || action.defender === action.attacker) return action
    if (action.spread) return null
    val replacement =
        battle.foesOf(action.attacker).firstOrNull { battle.reaches(action.attacker, it) }
            ?: battle.foesOf(action.attacker).firstOrNull()
            ?: return null
    return action.copy(defender = replacement)
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
    // Encore holds it to the encored move while that move has pp.
    if (mon.encoreTurns > 0) {
      moves.get(mon.encoreMoveId)?.let { encored ->
        if (mon.moves.any { it.id.toInt() == encored.id && it.pp > 0 }) return encored
      }
    }
    // A Choice item holds the monster to the first move it picked.
    if (items.isChoice(items.of(mon)) && mon.choiceLockedMove != 0) {
      moves.get(mon.choiceLockedMove)?.let { locked ->
        if (mon.moves.any { it.id.toInt() == locked.id && it.pp > 0 }) return locked
      }
    } else if (!items.isChoice(items.of(mon))) {
      mon.choiceLockedMove = 0
    }
    if (chosen == null) return null
    val slot = mon.moves.indexOfFirst { it.id.toInt() == chosen.id }
    if (slot >= 0 && mon.moves[slot].pp <= 0) return moves.get(STRUGGLE_ID) ?: chosen
    return chosen
  }

  /** Every action of the turn, fastest first; a spread move's follow-ups stay behind their opener. */
  private fun order(battle: BattleInstance, actions: List<TurnAction>): List<TurnAction> {
    val groups = mutableListOf<MutableList<TurnAction>>()
    for (action in actions) {
      val group = groups.lastOrNull()?.takeIf { it.first().attacker === action.attacker && action.followUp }
      if (group != null) group += action else groups += mutableListOf(action)
    }
    val keyed = groups.map { group -> Triple(group, bracket(battle, group.first()), speedOf(battle, group.first().attacker)) }
    val tieBreak = keyed.associate { it.first to battle.rng.pick(1 shl 16) }
    return keyed
        .sortedWith(
            compareByDescending<Triple<List<TurnAction>, Int, Int>> { it.second }
                // Trick Room lets the slowest go first within a priority bracket.
                .thenByDescending { if (battle.field.trickRoomTurns > 0) -it.third else it.third }
                .thenByDescending { tieBreak[it.first] })
        .flatMap { it.first }
  }

  private fun bracket(battle: BattleInstance, action: TurnAction): Int {
    fun priority(action: TurnAction): Int {
      val move = action.move ?: return 0
      // Grassy Glide goes first on Grassy Terrain.
      val glide = move.effect == MoveEffect.GRASSY_GLIDE && battle.field.terrain == Terrain.GRASSY && isGrounded(battle, action.attacker)
      return move.priority + Abilities.priorityBonus(action.attacker, move) + (if (glide) 1 else 0)
    }
    // Within a priority bracket Quick Claw (20%) and a ready Custap Berry go first, Lagging Tail
    // and Iron Ball last.
    run {
      val mon = action.attacker
      val item = items.of(mon)
      val first =
          mon.custapReady ||
              (item == de.fiereu.openmmo.items.generated.Items.QUICK_CLAW && battle.rng.accuracyRoll() <= 20)
      if (first) mon.movedFirstByItem = mon.heldItem.takeIf { item == de.fiereu.openmmo.items.generated.Items.QUICK_CLAW } ?: mon.consumedItem
      val last = item == de.fiereu.openmmo.items.generated.Items.LAGGING_TAIL || item == de.fiereu.openmmo.items.generated.Items.IRON_BALL
      return priority(action) * 10 + (if (first) 1 else 0) - (if (last) 1 else 0)
    }
  }

  private fun speedOf(battle: BattleInstance, mon: BattleMonState): Int {
    var speed = mon.effective(BattleStat.SPEED) * Abilities.speedMultiplierPercent(mon, weather(battle)) / 100
    speed = speed * items.speedPercent(mon) / 100
    if (StatusCondition.isParalyzed(mon.status) && Abilities.paralysisSlows(mon)) speed /= 4
    if (battle.sideOf(mon).tailwindTurns > 0) speed *= 2
    return speed
  }

  /** The weather as it acts on the field: Air Lock and Cloud Nine switch its effects off. */
  private fun weather(battle: BattleInstance): Weather? =
      if (battle.actives().any { Abilities.weatherNegated(it, it) }) null else battle.weather

  /** A monster's own stat stage change (an ability's doing), reported like any other. */
  private fun ownStage(mon: BattleMonState, stat: BattleStat, delta: Int, events: MutableList<BattleEvent>): Boolean {
    if (mon.fainted) return false
    val applied = mon.changeStage(stat, if (mon.ability == Ability.CONTRARY) -delta else if (mon.ability == Ability.SIMPLE) delta * 2 else delta)
    val value = if (stat == BattleStat.ACCURACY || stat == BattleStat.EVASION) 0 else mon.effective(stat)
    events += BattleEvent.StageChanged(mon.entityId, stat, mon.stage(stat), value, applied, applied == 0)
    return applied != 0
  }

  // ---------------------------------------------------------------------------------------------
  // Abilities that fire on entering the field

  /**
   * Illusion: a Zoroark entering behind a teammate takes that teammate's look until it is hit.
   * Decided before the block is sent, so the client draws the disguise from the start.
   */
  fun prepareIllusion(battle: BattleInstance, mon: BattleMonState) {
    mon.illusionOf = null
    if (mon.ability != Ability.ILLUSION) return
    val team = if (battle.isPlayerSide(mon.entityId)) battle.party else battle.opponent
    val last = team.lastOrNull { !it.fainted && it !== mon } ?: return
    if (last.species.id == mon.species.id) return
    mon.illusionOf = last
  }

  /** Runs the switch-in abilities of [mon] against the monster facing it. */
  fun switchIn(battle: BattleInstance, mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (mon.fainted) return
    arrive(battle, mon, events)
    if (mon.fainted) return
    val foe = battle.opponentOf(mon)
    fun shown(other: Long = 0, moveId: Int = 0) {
      events += BattleEvent.TurnEffect(mon.entityId)
      events += BattleEvent.AbilityShown(mon.entityId, mon.ability, other, moveId)
    }
    formChange(mon, "FORM_CHANGE_BATTLE_SWITCH_IN", events) { abilityMatches(mon, it.params.getOrNull(0)) }
    formChange(mon, "FORM_CHANGE_BATTLE_HP_PERCENT_SEND_OUT", events) {
      hpPercentMatches(mon, it) && (it.params.getOrNull(3)?.toIntOrNull()?.let { min -> mon.level >= min } ?: true)
    }
    formChange(mon, "FORM_CHANGE_BATTLE_WEATHER", events) {
      weatherMatches(weather(battle), it.params.getOrNull(0)) && abilityMatches(mon, it.params.getOrNull(1))
    }
    if (mon.ability == Ability.FRISK && foe.heldItem != 0) {
      events += BattleEvent.TurnEffect(mon.entityId)
      events += BattleEvent.AbilityShown(mon.entityId, Ability.FRISK, foe.entityId, 0, foe.heldItem)
    }
    when (mon.ability) {
      Ability.INTIMIDATE -> for (foe in battle.foesOf(mon).ifEmpty { listOf(foe) }) {
        shown(foe.entityId)
        if (!foe.fainted) {
          when {
            foe.ability == Ability.GUARD_DOG -> ownStage(foe, BattleStat.ATTACK, 1, events)
            Abilities.blocksStatDrop(foe, BattleStat.ATTACK) || foe.ability == Ability.INNER_FOCUS ||
                foe.ability == Ability.OBLIVIOUS || foe.ability == Ability.OWN_TEMPO || foe.ability == Ability.SCRAPPY -> {
              events += BattleEvent.AbilityShown(foe.entityId, foe.ability)
            }
            else -> {
              ownStage(foe, BattleStat.ATTACK, -1, events)
              if (foe.ability == Ability.RATTLED) ownStage(foe, BattleStat.SPEED, 1, events)
              if (foe.ability == Ability.DEFIANT) ownStage(foe, BattleStat.ATTACK, 2, events)
              if (foe.ability == Ability.COMPETITIVE) ownStage(foe, BattleStat.SP_ATTACK, 2, events)
            }
          }
        }
      }
      Ability.DRIZZLE, Ability.PRIMORDIAL_SEA -> if (battle.weather != Weather.RAIN) { shown(); setWeather(battle, Weather.RAIN, events) }
      Ability.DROUGHT, Ability.DESOLATE_LAND, Ability.ORICHALCUM_PULSE ->
          if (battle.weather != Weather.SUN) { shown(); setWeather(battle, Weather.SUN, events) }
      Ability.SAND_STREAM, Ability.SAND_SPIT -> if (battle.weather != Weather.SANDSTORM) { shown(); setWeather(battle, Weather.SANDSTORM, events) }
      Ability.SNOW_WARNING -> if (battle.weather != Weather.HAIL) { shown(); setWeather(battle, Weather.HAIL, events) }
      Ability.PRESSURE, Ability.MOLD_BREAKER, Ability.TURBOBLAZE, Ability.TERAVOLT, Ability.UNNERVE,
      Ability.AIR_LOCK, Ability.CLOUD_NINE, Ability.COMATOSE, Ability.NEUTRALIZING_GAS -> shown()
      Ability.SLOW_START -> { mon.slowStartTurns = 5; shown() }
      Ability.INTREPID_SWORD -> { shown(); ownStage(mon, BattleStat.ATTACK, 1, events) }
      Ability.DAUNTLESS_SHIELD -> { shown(); ownStage(mon, BattleStat.DEFENSE, 1, events) }
      Ability.DOWNLOAD -> {
        shown()
        val stat = if (foe.effective(BattleStat.DEFENSE) < foe.effective(BattleStat.SP_DEFENSE)) BattleStat.ATTACK else BattleStat.SP_ATTACK
        ownStage(mon, stat, 1, events)
      }
      // Imposter copies the monster diagonally across: the facing one in singles, the other
      // position in doubles (the Expansion's BATTLE_PARTNER(BATTLE_OPPOSITE)).
      Ability.IMPOSTER -> {
        val position = battle.positionOf(mon)
        val across =
            if (battle.format == de.fiereu.openmmo.net.game.packets.battle.BattleFormat.DOUBLES && position >= 0) 1 - position
            else position
        val target =
            battle.monAt(if (battle.isPlayerSide(mon.entityId)) 1 else 0, across)?.takeIf { !it.fainted } ?: foe
        val copied = mutableListOf<BattleEvent>()
        if (transform(mon, target, copied)) {
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.AbilityShown(mon.entityId, Ability.IMPOSTER, target.entityId)
          events += copied
        }
      }
      Ability.TRACE -> {
        if (foe.ability != Ability.NONE && foe.ability !in UNTRACEABLE) {
          shown(foe.entityId)
          mon.ability = foe.ability
          switchIn(battle, mon, events)
        }
      }
      Ability.ANTICIPATION -> {
        val dangerous = foe.moves.any { m ->
          val def = moves.get(m.id.toInt()) ?: return@any false
          def.power > 0 && (def.effect == MoveEffect.OHKO || effectivenessAgainst(def.type, mon, foe) > TypeChart.NEUTRAL)
        }
        if (dangerous) shown()
      }
      Ability.FOREWARN -> {
        val strongest = foe.moves.mapNotNull { moves.get(it.id.toInt()) }.maxByOrNull { it.power }
        if (strongest != null) {
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.Line(foe.entityId, BattleLine.IDENTIFIED, listOf(strongest.id))
        }
      }
      Ability.SCREEN_CLEANER -> {
        shown()
        for (side in listOf(battle.playerSide, battle.opponentSide)) { side.reflectTurns = 0; side.lightScreenTurns = 0 }
      }
      Ability.SUPERSWEET_SYRUP -> { shown(); if (!foe.fainted) ownStage(foe, BattleStat.EVASION, -1, events) }
      else -> Unit
    }
  }

  /** Whether the player's active monster may run from the wild monster in front of it. */
  fun canFlee(battle: BattleInstance): Boolean {
    val runner = battle.activeMon()
    if (battle.field.fairyLockTurns > 0) return false
    if (runner.ability == Ability.RUN_AWAY) return true
    // Mean Look, Block, Octolock, No Retreat and the trapping moves hold it in.
    if (runner.trappedTurns > 0) return false
    return battle.foesOf(runner).all { blocker ->
      when (blocker.ability) {
        Ability.SHADOW_TAG -> runner.ability == Ability.SHADOW_TAG
        Ability.ARENA_TRAP -> runner.hasType(PokemonType.FLYING) || runner.ability == Ability.LEVITATE
        Ability.MAGNET_PULL -> !runner.hasType(PokemonType.STEEL)
        else -> true
      }
    }
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
    // Destiny Bond holds only until the user moves again.
    if (move?.effect != MoveEffect.DESTINY_BOND) attacker.destinyBond = false
    if (move?.effect != MoveEffect.GRUDGE) attacker.grudge = false
    if (move == null) {
      events += BattleEvent.MoveFailed(attacker.entityId, 0)
      return
    }
    // A spread move's later targets ride on the first target's checks (sleep, paralysis, flinch,
    // confusion...): rolled once per use, not once per target. Rolling them again let a paralysed
    // boss "fail to move" twice after its Earthquake had already gone off (2026-09-14).
    if (action.followUp && attacker.leadChecked) {
      if (attacker.leadLost) return
    } else {
      attacker.leadChecked = true
      attacker.leadLost = !canMove(battle, attacker, move, events)
      if (attacker.leadLost) return
    }

    if (attacker.movedFirstByItem != 0) {
      events += BattleEvent.TurnEffect(attacker.entityId)
      events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_MOVED_FIRST, listOf(attacker.movedFirstByItem))
      attacker.movedFirstByItem = 0
    }
    val continuing = attacker.chargingMoveId == move.id
    // Stance Change and the like: the form shifts before the move goes out.
    formChange(attacker, "FORM_CHANGE_BATTLE_BEFORE_MOVE", events) {
      moveMatches(move, it.params.getOrNull(0)) && abilityMatches(attacker, it.params.getOrNull(1))
    } ||
        formChange(attacker, "FORM_CHANGE_BATTLE_BEFORE_MOVE_CATEGORY", events) {
          categoryMatches(move, it.params.getOrNull(0)) && abilityMatches(attacker, it.params.getOrNull(1))
        }
    if (action.followUp) Unit
    else if (!continuing) spendPp(attacker, action.defender, move, events)
    else events += BattleEvent.MoveUsed(attacker.entityId, move.id.toShort(), slotOf(attacker, move), ppOf(attacker, move))
    if (items.isChoice(items.of(attacker)) && attacker.choiceLockedMove == 0) attacker.choiceLockedMove = move.id

    // Two-turn moves: the first use charges (or hides), the second one strikes. A Power Herb
    // skips the charge.
    val powerHerb = isTwoTurn(battle, move) && !continuing && items.of(attacker) == de.fiereu.openmmo.items.generated.Items.POWER_HERB
    if (powerHerb) consumeItem(attacker, events)
    if (isTwoTurn(battle, move) && !continuing && !powerHerb) {
      attacker.chargingMoveId = move.id
      attacker.semiInvulnerable = move.effect == MoveEffect.SEMI_INVULNERABLE || move.effect == MoveEffect.SKY_DROP
      if (move.effect == MoveEffect.SKULL_BASH) applyStage(action, StageEffect(BattleStat.DEFENSE, 1, true), events)
      events += BattleEvent.Charging(attacker.entityId, move.id.toShort())
      if (attacker.semiInvulnerable) events += BattleEvent.Hidden(attacker.entityId, true)
      return
    }
    attacker.chargingMoveId = 0
    if (attacker.semiInvulnerable) {
      attacker.semiInvulnerable = false
      events += BattleEvent.Hidden(attacker.entityId, false)
    }

    // Powder: a Fire move from a powdered monster blows up in its face.
    if (attacker.powdered && moveType(battle, attacker, move) == PokemonType.FIRE) {
      loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events)
      events += BattleEvent.MoveFailed(attacker.entityId, move.id.toShort())
      return
    }
    // Snatch: a snatcher on the field takes the move its user meant for itself.
    if (move.hasFlag(MoveFlag.SNATCH_AFFECTED)) {
      battle.actives().firstOrNull { it !== attacker && it.snatching && !it.fainted }?.let { thief ->
        thief.snatching = false
        statusMove(battle, TurnAction(thief, battle.opponentOf(thief), move), move, movesLast, events)
        return
      }
    }
    // Psychic Terrain shields grounded monsters from the other side's priority moves.
    if (battle.field.terrain == Terrain.PSYCHIC && move.priority > 0 && isGrounded(battle, action.defender) &&
        battle.isPlayerSide(action.defender.entityId) != battle.isPlayerSide(attacker.entityId)) {
      events += BattleEvent.MoveFailed(attacker.entityId, move.id.toShort())
      return
    }
    if (move.power > 0 || isDamagingEffect(move)) {
      attack(battle, action, move, events, movesLast)
    } else {
      statusMove(battle, action, move, movesLast, events)
    }
  }

  private fun spendPp(attacker: BattleMonState, defender: BattleMonState, move: MoveDef, events: MutableList<BattleEvent>) {
    val slot = slotOf(attacker, move)
    if (slot >= 0 && attacker.moves[slot].pp > 0) {
      // Pressure makes every move aimed at its holder cost one more.
      val cost = if (defender.ability == Ability.PRESSURE && move.target != MoveTarget.USER && move.target != MoveTarget.FIELD) 2 else 1
      attacker.moves[slot].pp = (attacker.moves[slot].pp - cost).coerceAtLeast(0).toByte()
      // A Leppa Berry refills the first move that runs dry.
      if (attacker.moves[slot].pp.toInt() == 0 && items.of(attacker) == de.fiereu.openmmo.items.generated.Items.LEPPA_BERRY) {
        val itemId = attacker.heldItem
        consumeItem(attacker, events)
        attacker.moves[slot].pp = ripen(attacker, 10).coerceAtMost(move.pp).toByte()
        events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_RESTORED_STATUS, listOf(itemId))
      }
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
    // Lines the client prints for a lost action ride under the monster's own entry, so a packet is
    // opened for it here; the MoveUsed that would normally open one never comes.
    fun own() {
      events += BattleEvent.TurnEffect(mon.entityId)
    }
    if (mon.mustRecharge) {
      mon.mustRecharge = false
      own()
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.RECHARGING)
      return false
    }
    if (mon.ability == Ability.TRUANT) {
      if (mon.truantLoafs) {
        mon.truantLoafs = false
        own()
        events += BattleEvent.AbilityShown(mon.entityId, Ability.TRUANT)
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.RECHARGING)
        return false
      }
      mon.truantLoafs = true
    }
    if (StatusCondition.isAsleep(mon.status)) {
      val left = StatusCondition.sleepTurns(mon.status) - (if (mon.ability == Ability.EARLY_BIRD) 2 else 1)
      mon.status = (mon.status and StatusCondition.SLEEP_MASK.inv()) or left.coerceAtLeast(0)
      if (left > 0) {
        own()
        events += BattleEvent.Line(mon.entityId, BattleLine.SLEEP, listOf(1)) // 1 = "is fast asleep" (f/Kz: true -> entry 309; false -> 312 "woke up")
        // Sleep Talk and Snore are the moves a sleeping monster can still use.
        if (move.effect != MoveEffect.SLEEP_TALK && move.effect != MoveEffect.SNORE) {
          events += BattleEvent.CantMove(mon.entityId, CantMoveReason.ASLEEP)
          return false
        }
      }
      mon.nightmare = false
      own()
      events += BattleEvent.StatusChanged(mon.entityId, mon.status)
    }
    if (StatusCondition.isFrozen(mon.status)) {
      if (move.hasFlag(MoveFlag.THAWS_USER) || battle.rng.accuracyRoll() <= 20) {
        mon.status = mon.status and StatusCondition.FREEZE.inv()
        own()
        events += BattleEvent.StatusChanged(mon.entityId, mon.status)
      } else {
        own()
        events += BattleEvent.Line(mon.entityId, BattleLine.FREEZE, listOf(1))
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.FROZEN)
        return false
      }
    }
    if (mon.flinched) {
      own()
      events += BattleEvent.Line(mon.entityId, BattleLine.FLINCHED)
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.FLINCHED)
      if (mon.ability == Ability.STEADFAST) {
        events += BattleEvent.AbilityShown(mon.entityId, Ability.STEADFAST)
        ownStage(mon, BattleStat.SPEED, 1, events)
      }
      return false
    }
    // Disable, Torment, Taunt, Heal Block and Gravity keep the move itself from being used.
    val blocked =
        (mon.disableTurns > 0 && move.id == mon.disabledMoveId) ||
            (mon.tormented && move.id == mon.lastMoveId && move.id != STRUGGLE_ID) ||
            (mon.tauntTurns > 0 && move.power == 0) ||
            (mon.healBlockTurns > 0 && move.hasFlag(MoveFlag.HEALING)) ||
            (battle.field.gravityTurns > 0 && move.id in GRAVITY_BANNED) ||
            battle.foesOf(mon).any { foe -> foe.imprisoning && foe.moves.any { it.id.toInt() == move.id } }
    if (blocked) {
      own()
      events += BattleEvent.MoveFailed(mon.entityId, move.id.toShort())
      return false
    }
    if (mon.confusionTurns > 0) {
      mon.confusionTurns--
      own()
      if (mon.confusionTurns == 0) {
        events += BattleEvent.Line(mon.entityId, BattleLine.CONFUSION, listOf(1))
      } else if (battle.rng.coinFlip()) {
        // Hits itself: a typeless 40-power physical hit with its own attack and defense.
        val damage = baseDamage(mon, mon, CONFUSION_SELF_HIT_POWER, physical = true, crit = false, battle.rng)
        mon.currentHp = (mon.currentHp - damage.coerceAtLeast(1)).coerceAtLeast(0)
        events += BattleEvent.Line(mon.entityId, BattleLine.CONFUSION, listOf(2))
        events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.CONFUSED)
        if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
        return false
      } else {
        events += BattleEvent.Line(mon.entityId, BattleLine.CONFUSION, listOf(3))
      }
    }
    if (StatusCondition.isParalyzed(mon.status) && battle.rng.accuracyRoll() <= 25) {
      own()
      events += BattleEvent.Line(mon.entityId, BattleLine.PARALYZED, listOf(0))
      events += BattleEvent.CantMove(mon.entityId, CantMoveReason.PARALYZED)
      return false
    }
    // Attract: half the time love wins, while the one it fell for is still on the field.
    mon.infatuatedWith?.let { crush ->
      if (crush.fainted || crush !in battle.actives()) {
        mon.infatuatedWith = null
      } else if (battle.rng.coinFlip()) {
        own()
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.INFATUATED)
        return false
      }
    }
    return true
  }

  private fun isTwoTurn(battle: BattleInstance, move: MoveDef): Boolean =
      when (move.effect) {
        MoveEffect.SEMI_INVULNERABLE,
        MoveEffect.TWO_TURNS_ATTACK,
        MoveEffect.SKULL_BASH,
        MoveEffect.RAZOR_WIND,
        MoveEffect.SKY_ATTACK,
        MoveEffect.GEOMANCY,
        MoveEffect.SKY_DROP -> true
        MoveEffect.SOLAR_BEAM -> weather(battle) != Weather.SUN
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
      movesLast: Boolean = false,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val moveId = move.id.toShort()

    // Future Sight and Doom Desire only take aim now; the blow lands later (delayedAttacks).
    if (move.effect == MoveEffect.FUTURE_SIGHT) {
      val side = if (battle.isPlayerSide(defender.entityId)) 0 else 1
      val position = battle.positionOf(defender)
      if (position < 0 || battle.delayedAttacks.any { it.side == side && it.position == position }) {
        events += BattleEvent.MoveFailed(attacker.entityId, moveId)
        return
      }
      battle.delayedAttacks += DelayedAttack(attacker, side, position, move, FUTURE_SIGHT_TURNS)
      return
    }
    // Feint and Hyperspace Fury lift the target's protection before hitting.
    if (move.additionalEffects.any { it.effect == MoveAdditionalEffect.FEINT }) defender.protectedThisTurn = false
    if (defender.protectedThisTurn && move.hasFlag(MoveFlag.PROTECT_AFFECTED)) {
      events += BattleEvent.Protected(defender.entityId)
      if (move.hasFlag(MoveFlag.EXPLOSION)) explode(attacker, events)
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
    val unusable =
        when (move.effect) {
          MoveEffect.SNORE -> !StatusCondition.isAsleep(attacker.status)
          MoveEffect.FIRST_TURN_ONLY -> attacker.turnsOnField > 0
          MoveEffect.FOCUS_PUNCH -> attacker.lastDamageTaken > 0
          MoveEffect.BELCH -> attacker.consumedItem == 0 || !items.isBerry(items.get(attacker.consumedItem))
          MoveEffect.LAST_RESORT ->
              attacker.moves.count { it.id.toInt() != 0 } < 2 ||
                  attacker.moves.any { it.id.toInt() != 0 && it.id.toInt() != move.id && it.id.toInt() !in attacker.usedMoves }
          MoveEffect.SYNCHRONOISE -> PokemonType.entries.none { attacker.hasType(it) && defender.hasType(it) }
          MoveEffect.SPIT_UP -> attacker.stockpile == 0
          // Burn Up and Double Shock need the type they burn away.
          MoveEffect.FAIL_IF_NOT_ARG_TYPE -> argType(move)?.let { !attacker.hasType(it) } ?: false
          MoveEffect.FLING -> items.flingPower(items.of(attacker)) == 0
          MoveEffect.NATURAL_GIFT -> items.naturalGift(items.of(attacker)) == null
          MoveEffect.POLTERGEIST -> defender.heldItem == 0
          MoveEffect.STEEL_ROLLER -> battle.field.terrain == null
          MoveEffect.SHELL_TRAP -> attacker.lastDamageTaken <= 0 || !attacker.lastDamagePhysical
          MoveEffect.UPPER_HAND -> defender.movedThisTurn || (battle.plannedMoves[defender]?.priority ?: 0) <= 0
          else -> false
        }
    if (unusable) {
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    // Pollen Puff aimed at a partner heals it instead of hurting it.
    if (move.effect == MoveEffect.HIT_ENEMY_HEAL_ALLY && battle.alliesOf(attacker).any { it === defender }) {
      if (defender.currentHp >= defender.maxHp || defender.healBlockTurns > 0) events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      else healHp(defender, (defender.maxHp + 1) / 2, events)
      return
    }
    // Nature Power becomes the move its surroundings call for (Gen 5+ table).
    if (move.effect == MoveEffect.NATURE_POWER) {
      val calledId =
          when (battle.field.terrain) {
            Terrain.ELECTRIC -> THUNDERBOLT
            Terrain.GRASSY -> ENERGY_BALL
            Terrain.MISTY -> MOONBLAST
            Terrain.PSYCHIC -> PSYCHIC_MOVE
            null -> if (battle.encounter.surfing) HYDRO_PUMP else if (battle.encounter.cave) EARTHQUAKE_MOVE else TRI_ATTACK
          }
      val called = moves.get(calledId)
      if (called == null) events += BattleEvent.MoveFailed(attacker.entityId, moveId) else callMove(battle, action, called, movesLast, events)
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
      val physical = move.category == DamageCategory.PHYSICAL
      if (attacker.lastDamageTaken <= 0 || attacker.lastDamagePhysical != physical) {
        events += BattleEvent.MoveFailed(attacker.entityId, moveId)
        return
      }
      dealFixed(action, attacker.lastDamageTaken * 2, events)
      return
    }

    val explodes = move.hasFlag(MoveFlag.EXPLOSION) || move.effect == MoveEffect.EXPLOSION
    if (explodes && (attacker.ability == Ability.DAMP || defender.ability == Ability.DAMP)) {
      events += BattleEvent.AbilityShown(if (attacker.ability == Ability.DAMP) attacker.entityId else defender.entityId, Ability.DAMP)
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    val effectiveType = moveType(battle, attacker, move)
    var effectiveness = effectivenessAgainst(effectiveType, defender, attacker)
    // Flying Press adds its second type's matchup; Freeze-Dry hits its named type super effectively.
    argType(move)?.let { second ->
      if (move.effect == MoveEffect.TWO_TYPED_MOVE) effectiveness = effectiveness * effectivenessAgainst(second, defender, attacker) / TypeChart.NEUTRAL
      if (move.effect == MoveEffect.SUPER_EFFECTIVE_ON_ARG && defender.hasType(second))
          effectiveness = effectiveness * 2 * TypeChart.NEUTRAL / typeChart.multiplier(effectiveType, second).coerceAtLeast(1)
    }
    if (effectiveness == 0 && move.effect != MoveEffect.OHKO) {
      events += BattleEvent.Immune(defender.entityId)
      if (explodes) explode(attacker, events)
      return
    }
    if (absorbedByAbility(battle, action, move, effectiveType, events)) {
      if (explodes) explode(attacker, events)
      return
    }
    if (effectiveType == PokemonType.GROUND && items.of(defender) == de.fiereu.openmmo.items.generated.Items.AIR_BALLOON &&
        !defender.airBalloonPopped) {
      events += BattleEvent.Immune(defender.entityId)
      return
    }
    if (Abilities.wonderGuard(defender, attacker, effectiveness, move.power) ||
        (move.effect == MoveEffect.OHKO && Abilities.sturdy(defender, attacker))) {
      events += BattleEvent.AbilityShown(defender.entityId, defender.ability)
      events += BattleEvent.Immune(defender.entityId)
      return
    }
    if (!accuracyCheck(battle, action, move, events)) {
      events += BattleEvent.MoveMissed(attacker.entityId, moveId)
      if (move.effect == MoveEffect.RECOIL_IF_MISS) {
        val wouldDeal = hit(battle, action, move, effectiveType, effectiveness, powerOf(battle, action, move)).damage
        loseHp(attacker, (wouldDeal / 2).coerceAtLeast(1), events)
      }
      if (explodes) explode(attacker, events)
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
      MoveEffect.FINAL_GAMBIT -> {
        dealFixed(action, attacker.currentHp, events)
        explode(attacker, events)
        return
      }
      else -> Unit
    }

    // Brick Break and Psychic Fangs shatter the screens before their damage is worked out.
    if (move.additionalEffects.any { it.effect == MoveAdditionalEffect.BREAK_SCREEN }) {
      battle.sideOf(defender).reflectTurns = 0
      battle.sideOf(defender).lightScreenTurns = 0
    }
    val hits = if (move.effect == MoveEffect.BEAT_UP) beatUpCrew(battle, attacker).size.coerceAtLeast(1) else hitCount(battle, move)
    var totalDamage = 0
    var lastCrit = false
    var struck = 0
    var hitSubstitute = false
    for (i in 0 until hits) {
      if (defender.fainted) break
      if (i > 0 && rerollsPerHit(move) && !accuracyCheck(battle, action, move, events)) break
      attacker.beatUpIndex = i
      val power = powerOf(battle, action, move) * (if (move.effect == MoveEffect.TRIPLE_KICK) i + 1 else 1)
      if (move.effect == MoveEffect.PRESENT && power == 0) {
        // The present healed instead.
        healHp(defender, defender.maxHp / 4, events)
        return
      }
      // Disguise and Ice Face: the form takes the hit instead of the monster.
      if (formChange(defender, "FORM_CHANGE_BATTLE_HIT_BY_MOVE_CATEGORY", events) {
        abilityMatches(defender, it.params.getOrNull(0)) && categoryMatches(move, it.params.getOrNull(1))
      }) {
        events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(0))
        break
      }
      val result = hit(battle, action, move, effectiveType, effectiveness, power, movesLast)
      var damage = result.damage
      // A resist berry halves the one super-effective hit of its type, then is gone.
      val resistBerry = items.of(defender)
      if (items.resistBerryType(resistBerry) == effectiveType &&
          (effectiveness > 10 || resistBerry == de.fiereu.openmmo.items.generated.Items.CHILAN_BERRY) &&
          canEatBerry(battle, defender)) {
        damage = if (defender.ability == Ability.RIPEN) damage / 4 else damage / 2
        consumeItem(defender, events)
      }
      // A Substitute takes the hit in the monster's place until it breaks.
      if (defender.substituteHp > 0 && defender !== attacker && !bypassesSubstitute(attacker, move)) {
        defender.substituteHp = (defender.substituteHp - damage).coerceAtLeast(0)
        totalDamage += damage
        lastCrit = result.crit
        struck++
        hitSubstitute = true
        continue
      }
      var endured = false
      var sturdy = false
      var sashKind = 0
      var crystalShell = false
      if (damage >= defender.currentHp) {
        sturdy = defender.currentHp == defender.maxHp && Abilities.sturdy(defender, attacker)
        val item = items.of(defender)
        if (!sturdy && !defender.enduring && move.effect != MoveEffect.FALSE_SWIPE) {
          if (item == de.fiereu.openmmo.items.generated.Items.FOCUS_SASH && defender.currentHp == defender.maxHp) {
            sashKind = 3
            consumeItem(defender, events)
          } else if (item == de.fiereu.openmmo.items.generated.Items.FOCUS_BAND && battle.rng.accuracyRoll() <= 10) {
            sashKind = 1
          }
        }
        val heldByItsOwn = move.effect == MoveEffect.FALSE_SWIPE || defender.enduring || sturdy || sashKind != 0
        // Only the crystal kept it standing: that is the one the banner names.
        crystalShell = !heldByItsOwn && battle.raid?.endures(defender) == true
        val survive = heldByItsOwn || crystalShell
        endured = survive && defender.enduring
        damage = if (survive) (defender.currentHp - 1).coerceAtLeast(0) else defender.currentHp
      }
      val hpBefore = defender.currentHp
      defender.currentHp -= damage
      defender.lastDamageTaken = damage
      defender.lastDamagePhysical = isPhysical(attacker, defender, move)
      defender.timesHit++
      totalDamage += damage
      lastCrit = result.crit
      struck++
      events += BattleEvent.DamageDealt(defender.entityId, defender.currentHp, result.crit, effectiveness)
      // Any landed hit counts, even one the crystal held to nothing (a boss already at 1 hp).
      battle.raid?.takeIf { it.isBoss(defender) }?.hurtThisTurn = true
      if (endured) events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(0))
      if (sturdy) {
        events += BattleEvent.AbilityShown(defender.entityId, Ability.STURDY)
        events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(2))
      }
      if (sashKind != 0) events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(sashKind))
      if (crystalShell) raidBanner(defender, CrystalOnixRaid.CRYSTAL_SHELL, CrystalOnixRaid.SHELL_LINE, events)
      if (defender.illusionOf != null) {
        defender.illusionOf = null
        events += BattleEvent.SpeciesShown(defender.entityId, defender.wireSpeciesId().toInt())
      }
      hitReactions(battle, action, move, effectiveType, result.crit, hpBefore, events)
      itemHitReactions(battle, action, move, effectiveType, effectiveness, damage, events)
      checkBerries(battle, defender, events)
    }
    if (struck > 1) events += BattleEvent.MultiHit(attacker.entityId, struck)
    if (struck == 0) return
    if (hitSubstitute) {
      // Nothing reaches the monster behind the doll; U-turn still takes its user out.
      if (explodes) explode(attacker, events)
      if (move.effect == MoveEffect.HIT_ESCAPE) selfSwitch(battle, attacker, false, events)
      return
    }
    if (defender.fainted) {
      koReactions(attacker, events)
      if (move.effect == MoveEffect.FELL_STINGER) ownStage(attacker, BattleStat.ATTACK, 3, events)
      if (defender.destinyBond && attacker !== defender) explode(attacker, events)
      if (defender.grudge) attacker.moves.firstOrNull { it.id.toInt() == move.id }?.let { it.pp = 0 }
    }
    if (move.effect == MoveEffect.SMACK_DOWN && !defender.fainted) {
      defender.grounded = true
      defender.magnetRiseTurns = 0
      if (defender.chargingMoveId in HIDDEN_IN_AIR) {
        defender.chargingMoveId = 0
        defender.semiInvulnerable = false
        events += BattleEvent.Hidden(defender.entityId, false)
      }
    }
    if (move.effect == MoveEffect.SPIT_UP) releaseStockpile(attacker, events)
    when (move.effect) {
      MoveEffect.FLING, MoveEffect.NATURAL_GIFT -> if (attacker.heldItem != 0) consumeItem(attacker, events)
      MoveEffect.STONE_AXE -> battle.sideOf(defender).stealthRock = true
      MoveEffect.CEASELESS_EDGE -> battle.sideOf(defender).let { if (it.spikes < 3) it.spikes++ }
      MoveEffect.ICE_SPINNER, MoveEffect.STEEL_ROLLER -> {
        battle.field.terrain = null
        battle.field.terrainTurns = 0
      }
      MoveEffect.SCALE_SHOT -> {
        ownStage(attacker, BattleStat.SPEED, 1, events)
        ownStage(attacker, BattleStat.DEFENSE, -1, events)
      }
      else -> Unit
    }

    // A Fire hit thaws; Smelling Salt cures the paralysis it doubled on.
    if (effectiveType == PokemonType.FIRE && StatusCondition.isFrozen(defender.status)) {
      defender.status = defender.status and StatusCondition.FREEZE.inv()
      events += BattleEvent.StatusChanged(defender.entityId, defender.status)
    }
    if (move.effect == MoveEffect.RAPID_SPIN) {
      attacker.leechSeeded = false
      attacker.trappedTurns = 0
    }

    if (defender.fainted) events += BattleEvent.Fainted(defender.entityId)

    // After-hit effects on the user.
    val recoils = !Abilities.noRecoil(attacker)
    when (move.effect) {
      MoveEffect.RECOIL, MoveEffect.DOUBLE_EDGE ->
          if (recoils) loseHp(attacker, (totalDamage * (move.argumentValue ?: 25) / 100).coerceAtLeast(1), events)
      MoveEffect.MAX_HP_50_RECOIL, MoveEffect.CHLOROBLAST -> if (recoils) loseHp(attacker, (attacker.maxHp / 2).coerceAtLeast(1), events)
      MoveEffect.STRUGGLE -> if (attacker.ability != Ability.MAGIC_GUARD) loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events)
      MoveEffect.ABSORB, MoveEffect.DREAM_EATER ->
          healHp(attacker, (totalDamage * (move.argumentValue ?: 50) / 100 * items.drainPercent(attacker) / 100).coerceAtLeast(1), events)
      MoveEffect.STEAL_ITEM -> if (!defender.fainted || defender.heldItem != 0) stealItem(attacker, defender, events)
      MoveEffect.KNOCK_OFF ->
          if (items.canBeTaken(defender)) {
            val taken = defender.heldItem
            defender.heldItem = 0
            events += BattleEvent.ItemChanged(defender.entityId, 0)
            events += BattleEvent.Line(defender.entityId, BattleLine.ITEM_KNOCKED_OFF, listOf(taken))
          }
      else -> Unit
    }
    // Bug Bite and Incinerate work on the target's berry; Recycle-style item moves are below.
    for (extra in move.additionalEffects) {
      when (extra.effect) {
        MoveAdditionalEffect.BUG_BITE ->
            if (items.isBerry(items.get(defender.heldItem)) && items.canBeTaken(defender) && !attacker.fainted) {
              val id = defender.heldItem
              defender.heldItem = 0
              events += BattleEvent.ItemChanged(defender.entityId, 0)
              events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_STOLE_ATE, listOf(id))
              val eaten = items.get(id)
              val heal = items.halfHpHeal(eaten, attacker.maxHp) + items.quarterHpHeal(eaten, attacker.maxHp)
              if (heal > 0) healHp(attacker, heal, events)
              items.pinchStat(eaten)?.let { ownStage(attacker, it, 1, events) }
            }
        MoveAdditionalEffect.INCINERATE ->
            if (items.isBerry(items.get(defender.heldItem)) && items.canBeTaken(defender)) {
              val id = defender.heldItem
              defender.heldItem = 0
              events += BattleEvent.ItemChanged(defender.entityId, 0)
              events += BattleEvent.Line(defender.entityId, BattleLine.ITEM_BURNT, listOf(id))
            }
        else -> Unit
      }
    }
    when (move.effect) {
      else -> Unit
    }
    if (explodes) explode(attacker, events)
    secondaryEffects(battle, action, move, events)
    // Once the hit and its effects are done: U-turn takes its user out, Dragon Tail its target.
    when (move.effect) {
      MoveEffect.HIT_ESCAPE -> selfSwitch(battle, attacker, false, events)
      MoveEffect.HIT_SWITCH_TARGET -> if (!defender.fainted) dragOut(battle, attacker, defender, move, events)
      else -> Unit
    }
  }

  /**
   * What the defender's (and attacker's) abilities do once a hit has landed: contact statuses,
   * Rough Skin, the stat reactions, Aftermath and Innards Out on a faint.
   */
  private fun hitReactions(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      type: PokemonType,
      crit: Boolean,
      hpBefore: Int,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val contact = move.hasFlag(MoveFlag.MAKES_CONTACT) && attacker.ability != Ability.LONG_REACH
    val physical = isPhysical(attacker, defender, move)
    fun shown(mon: BattleMonState) = events.add(BattleEvent.AbilityShown(mon.entityId, mon.ability))
    if (defender.beakBlast && contact && !attacker.fainted && canReceiveStatus(battle, defender, attacker, StatusCondition.BURN))
        inflictStatus(battle, defender, attacker, StatusCondition.BURN, events)
    fun roll(chance: Int) = battle.rng.accuracyRoll() <= chance
    // The defender's abilities, felt by the attacker.
    if (contact && !attacker.fainted) {
      when (defender.ability) {
        Ability.STATIC -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.PARALYSIS)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.PARALYSIS, events) }
        Ability.POISON_POINT -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.POISON)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.POISON, events) }
        Ability.FLAME_BODY -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.BURN)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.BURN, events) }
        Ability.EFFECT_SPORE ->
            if (roll(30)) {
              val status = listOf(StatusCondition.POISON, StatusCondition.PARALYSIS, StatusCondition.asleep(2 + battle.rng.pick(3)))[battle.rng.pick(3)]
              if (canReceiveStatus(battle, defender, attacker, status)) { shown(defender); inflictStatus(battle, defender, attacker, status, events) }
            }
        Ability.ROUGH_SKIN, Ability.IRON_BARBS ->
            if (attacker.ability != Ability.MAGIC_GUARD) { shown(defender); loseHp(attacker, (attacker.maxHp / 8).coerceAtLeast(1), events) }
        Ability.GOOEY, Ability.TANGLING_HAIR -> { shown(defender); ownStage(attacker, BattleStat.SPEED, -1, events) }
        Ability.AFTERMATH ->
            if (defender.fainted && attacker.ability != Ability.MAGIC_GUARD) { shown(defender); loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events) }
        else -> Unit
      }
      when (attacker.ability) {
        Ability.POISON_TOUCH -> if (roll(30) && canReceiveStatus(battle, attacker, defender, StatusCondition.POISON)) { shown(attacker); inflictStatus(battle, attacker, defender, StatusCondition.POISON, events) }
        Ability.STENCH -> if (roll(10) && !defender.movedThisTurn && !Abilities.blocksFlinch(defender)) defender.flinched = true
        else -> Unit
      }
    }
    if (defender.fainted) {
      if (defender.ability == Ability.INNARDS_OUT && attacker.ability != Ability.MAGIC_GUARD && !attacker.fainted) {
        shown(defender)
        loseHp(attacker, hpBefore.coerceAtLeast(1), events)
      }
      return
    }
    // The defender's abilities, felt by itself.
    val crossedHalf = hpBefore * 2 > defender.maxHp && defender.currentHp * 2 <= defender.maxHp
    when (defender.ability) {
      Ability.WEAK_ARMOR -> if (physical) { shown(defender); ownStage(defender, BattleStat.DEFENSE, -1, events); ownStage(defender, BattleStat.SPEED, 2, events) }
      Ability.STAMINA -> { shown(defender); ownStage(defender, BattleStat.DEFENSE, 1, events) }
      Ability.WATER_COMPACTION -> if (type == PokemonType.WATER) { shown(defender); ownStage(defender, BattleStat.DEFENSE, 2, events) }
      Ability.STEAM_ENGINE -> if (type == PokemonType.FIRE || type == PokemonType.WATER) { shown(defender); ownStage(defender, BattleStat.SPEED, 6, events) }
      Ability.JUSTIFIED -> if (type == PokemonType.DARK) { shown(defender); ownStage(defender, BattleStat.ATTACK, 1, events) }
      Ability.RATTLED -> if (type == PokemonType.BUG || type == PokemonType.GHOST || type == PokemonType.DARK) { shown(defender); ownStage(defender, BattleStat.SPEED, 1, events) }
      Ability.ANGER_POINT -> if (crit) { shown(defender); ownStage(defender, BattleStat.ATTACK, 12, events) }
      Ability.BERSERK -> if (crossedHalf) { shown(defender); ownStage(defender, BattleStat.SP_ATTACK, 1, events) }
      Ability.ANGER_SHELL ->
          if (crossedHalf) {
            shown(defender)
            ownStage(defender, BattleStat.ATTACK, 1, events); ownStage(defender, BattleStat.SP_ATTACK, 1, events); ownStage(defender, BattleStat.SPEED, 1, events)
            ownStage(defender, BattleStat.DEFENSE, -1, events); ownStage(defender, BattleStat.SP_DEFENSE, -1, events)
          }
      Ability.COTTON_DOWN -> { shown(defender); ownStage(attacker, BattleStat.SPEED, -1, events) }
      Ability.SAND_SPIT -> if (battle.weather != Weather.SANDSTORM) { shown(defender); setWeather(battle, Weather.SANDSTORM, events) }
      Ability.THERMAL_EXCHANGE -> if (type == PokemonType.FIRE) { shown(defender); ownStage(defender, BattleStat.ATTACK, 1, events) }
      else -> Unit
    }
  }

  /** Held items on both sides once a hit has landed. */
  private fun itemHitReactions(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      type: PokemonType,
      effectiveness: Int,
      damage: Int,
      events: MutableList<BattleEvent>,
  ) {
    val attacker = action.attacker
    val defender = action.defender
    val contact = items.moveIsContact(move, attacker)
    val I = de.fiereu.openmmo.items.generated.Items
    // The defender's item.
    when (val item = items.of(defender)) {
      I.ROCKY_HELMET ->
          if (contact && attacker.ability != Ability.MAGIC_GUARD && !attacker.fainted) {
            events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_HURT, listOf(1, defender.heldItem))
            loseHp(attacker, (attacker.maxHp / 6).coerceAtLeast(1), events)
          }
      I.STICKY_BARB ->
          if (contact && attacker.heldItem == 0 && !attacker.fainted) {
            attacker.heldItem = defender.heldItem
            defender.heldItem = 0
            events += BattleEvent.ItemChanged(defender.entityId, 0)
            events += BattleEvent.ItemChanged(attacker.entityId, attacker.heldItem)
          }
      I.JABOCA_BERRY ->
          if (isPhysical(attacker, defender, move) && attacker.ability != Ability.MAGIC_GUARD && canEatBerry(battle, defender)) {
            val id = defender.heldItem
            consumeItem(defender, events)
            events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_HURT, listOf(1, id))
            loseHp(attacker, ripen(defender, attacker.maxHp / 8).coerceAtLeast(1), events)
          }
      I.ROWAP_BERRY ->
          if (!isPhysical(attacker, defender, move) && attacker.ability != Ability.MAGIC_GUARD && canEatBerry(battle, defender)) {
            val id = defender.heldItem
            consumeItem(defender, events)
            events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_HURT, listOf(1, id))
            loseHp(attacker, ripen(defender, attacker.maxHp / 8).coerceAtLeast(1), events)
          }
      I.ENIGMA_BERRY ->
          if (effectiveness > 10 && !defender.fainted && canEatBerry(battle, defender)) {
            val id = defender.heldItem
            consumeItem(defender, events)
            defender.currentHp = (defender.currentHp + ripen(defender, defender.maxHp / 4)).coerceAtMost(defender.maxHp)
            events += BattleEvent.Line(defender.entityId, BattleLine.ITEM_HEAL, listOf(id, defender.currentHp))
          }
      I.ABSORB_BULB -> if (type == PokemonType.WATER && !defender.fainted) { consumeItem(defender, events); ownStage(defender, BattleStat.SP_ATTACK, 1, events) }
      I.CELL_BATTERY -> if (type == PokemonType.ELECTRIC && !defender.fainted) { consumeItem(defender, events); ownStage(defender, BattleStat.ATTACK, 1, events) }
      I.AIR_BALLOON -> { consumeItem(defender, events); defender.airBalloonPopped = true }
      else -> Unit
    }
    if (defender.ability == Ability.PICKPOCKET && contact && !defender.fainted) {
      if (stealItem(defender, attacker, events)) events += BattleEvent.AbilityShown(defender.entityId, Ability.PICKPOCKET)
    }
    // The attacker's item and item abilities.
    if (attacker.fainted) return
    formChange(attacker, "FORM_CHANGE_BATTLE_HP_PERCENT_DURING_MOVE", events) {
      hpPercentMatches(attacker, it) && moveMatches(move, it.params.getOrNull(3))
    }
    if (attacker.ability == Ability.MAGICIAN && !defender.fainted) {
      if (stealItem(attacker, defender, events)) events += BattleEvent.AbilityShown(attacker.entityId, Ability.MAGICIAN)
    }
    when (items.of(attacker)) {
      I.LIFE_ORB ->
          if (attacker.ability != Ability.MAGIC_GUARD &&
              !(attacker.ability == Ability.SHEER_FORCE && move.additionalEffects.any { !it.self })) {
            events += BattleEvent.Line(attacker.entityId, BattleLine.LIFE_ORB_HURT)
            loseHp(attacker, (attacker.maxHp / 10).coerceAtLeast(1), events)
          }
      I.SHELL_BELL ->
          if (attacker.currentHp < attacker.maxHp && damage > 0) {
            attacker.currentHp = (attacker.currentHp + (damage / 8).coerceAtLeast(1)).coerceAtMost(attacker.maxHp)
            events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_HEAL_SMALL, listOf(attacker.currentHp, attacker.heldItem))
          }
      I.KING_S_ROCK, I.RAZOR_FANG ->
          if (!defender.movedThisTurn && !defender.fainted && !Abilities.blocksFlinch(defender) &&
              battle.rng.accuracyRoll() <= items.flinchChance(attacker))
              defender.flinched = true
      else -> Unit
    }
  }

  /** The attacker's reward for a knockout. */
  private fun koReactions(attacker: BattleMonState, events: MutableList<BattleEvent>) {
    if (attacker.fainted) return
    fun boost(stat: BattleStat) {
      events += BattleEvent.AbilityShown(attacker.entityId, attacker.ability)
      ownStage(attacker, stat, 1, events)
    }
    when (attacker.ability) {
      Ability.MOXIE, Ability.CHILLING_NEIGH, Ability.AS_ONE_ICE_RIDER -> boost(BattleStat.ATTACK)
      Ability.GRIM_NEIGH, Ability.AS_ONE_SHADOW_RIDER, Ability.SOUL_HEART -> boost(BattleStat.SP_ATTACK)
      Ability.BEAST_BOOST -> {
        val best =
            listOf(BattleStat.ATTACK, BattleStat.DEFENSE, BattleStat.SP_ATTACK, BattleStat.SP_DEFENSE, BattleStat.SPEED)
                .maxByOrNull { attacker.unstaged(it) } ?: BattleStat.ATTACK
        boost(best)
      }
      else -> Unit
    }
  }

  /**
   * A defender ability that swallows the move: Levitate, the absorbers, Flash Fire, Soundproof...
   * True when the move ends here (the ability's own effect has been applied and announced).
   */
  private fun absorbedByAbility(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      type: PokemonType,
      events: MutableList<BattleEvent>,
  ): Boolean {
    val attacker = action.attacker
    val defender = action.defender
    if (Abilities.ignoresTargetAbilities(attacker)) return false
    val absorb = Abilities.absorb(defender, type, move) ?: return false
    events += BattleEvent.AbilityShown(defender.entityId, defender.ability)
    when (absorb) {
      Abilities.Absorb.IMMUNE -> events += BattleEvent.Immune(defender.entityId)
      Abilities.Absorb.HEAL_QUARTER ->
          if (defender.currentHp < defender.maxHp) healHp(defender, (defender.maxHp / 4).coerceAtLeast(1), events)
      Abilities.Absorb.SP_ATTACK_UP -> ownStage(defender, BattleStat.SP_ATTACK, 1, events)
      Abilities.Absorb.SPEED_UP -> ownStage(defender, BattleStat.SPEED, 1, events)
      Abilities.Absorb.ATTACK_UP -> ownStage(defender, BattleStat.ATTACK, 1, events)
      Abilities.Absorb.DEFENSE_UP_2 -> ownStage(defender, BattleStat.DEFENSE, 2, events)
      Abilities.Absorb.FLASH_FIRE -> defender.flashFire = true
    }
    return true
  }

  /** Explosion and Self-Destruct: the user faints whatever happened to the target. */
  private fun explode(attacker: BattleMonState, events: MutableList<BattleEvent>) {
    if (attacker.fainted) return
    attacker.currentHp = 0
    events += BattleEvent.TurnEffect(attacker.entityId)
    events += BattleEvent.HpChanged(attacker.entityId, 0)
    events += BattleEvent.Fainted(attacker.entityId)
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

  /**
   * Physical or special by the move itself (the Gen 4+ split). Photon Geyser uses whichever of the
   * user's attacks is higher, Shell Side Arm whichever side would hurt the target more.
   */
  private fun isPhysical(attacker: BattleMonState, defender: BattleMonState, move: MoveDef): Boolean =
      when (move.effect) {
        MoveEffect.PHOTON_GEYSER -> attacker.effective(BattleStat.ATTACK) > attacker.effective(BattleStat.SP_ATTACK)
        MoveEffect.SHELL_SIDE_ARM ->
            attacker.effective(BattleStat.ATTACK) * defender.effective(BattleStat.SP_DEFENSE) >
                attacker.effective(BattleStat.SP_ATTACK) * defender.effective(BattleStat.DEFENSE)
        else -> move.category == DamageCategory.PHYSICAL
      }

  private fun moveType(battle: BattleInstance, attacker: BattleMonState, move: MoveDef): PokemonType {
    if (attacker.electrified) return PokemonType.ELECTRIC
    if (attacker.ability == Ability.NORMALIZE) return PokemonType.NORMAL
    val base =
        when (move.effect) {
          MoveEffect.HIDDEN_POWER -> hiddenPowerType(attacker)
          MoveEffect.WEATHER_BALL ->
              when (weather(battle)) {
                Weather.RAIN -> PokemonType.WATER
                Weather.SUN -> PokemonType.FIRE
                Weather.SANDSTORM -> PokemonType.ROCK
                Weather.HAIL -> PokemonType.ICE
                null -> PokemonType.NORMAL
              }
          MoveEffect.CHANGE_TYPE_ON_ITEM ->
              when (move.argument?.value) {
                "HOLD_EFFECT_PLATE" -> items.plateType(items.of(attacker))
                "HOLD_EFFECT_DRIVE" -> items.driveType(items.of(attacker))
                else -> null
              } ?: move.type
          MoveEffect.REVELATION_DANCE -> attacker.type1.takeIf { it != PokemonType.QUESTIONQUESTIONQUESTION } ?: move.type
          MoveEffect.TERRAIN_PULSE -> terrainType(battle)?.takeIf { isGrounded(battle, attacker) } ?: move.type
          MoveEffect.NATURAL_GIFT -> items.naturalGift(items.of(attacker))?.first ?: move.type
          else -> move.type
        }
    if (base == PokemonType.NORMAL) Abilities.retypedNormal(attacker)?.let { return it }
    if (base == PokemonType.NORMAL && battle.field.ionDeluge) return PokemonType.ELECTRIC
    return base
  }

  private fun effectivenessAgainst(type: PokemonType, defender: BattleMonState, attacker: BattleMonState? = null): Int {
    // Magnet Rise floats over Ground moves until Smack Down brings it down.
    if (type == PokemonType.GROUND && (defender.magnetRiseTurns > 0 || defender.telekinesisTurns > 0) && !defender.grounded) return 0
    var eff = typeChart.effectiveness(type, defender.type1, defender.type2)
    defender.thirdType?.let { third ->
      if (third != defender.type1 && third != defender.type2) eff = eff * typeChart.multiplier(type, third) / TypeChart.NEUTRAL
    }
    // Foresight (or Scrappy) lets Normal and Fighting hit a Ghost.
    val seesGhosts = defender.identified || (attacker != null && Abilities.hitsGhosts(attacker))
    if (eff == 0 && seesGhosts && defender.hasType(PokemonType.GHOST) &&
        (type == PokemonType.NORMAL || type == PokemonType.FIGHTING)) {
      val other = if (defender.type1 == PokemonType.GHOST) defender.type2 else defender.type1
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
          // Low Kick and Grass Knot share LOW_KICK; Heavy Slam and Heat Crash share HEAT_CRASH.
          MoveEffect.LOW_KICK -> WeightMechanics.lowKickPower(defender.species.weight)
          MoveEffect.HEAT_CRASH ->
              WeightMechanics.heavySlamPower(attacker.species.weight, defender.species.weight)
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
          MoveEffect.WEATHER_BALL -> if (weather(battle) != null) 100 else 50
          MoveEffect.SOLAR_BEAM ->
              if (weather(battle) != null && weather(battle) != Weather.SUN) move.power / 2 else move.power
          // The Expansion's CalcMoveBasePower tables.
          MoveEffect.GYRO_BALL ->
              (25 * speedOf(battle, defender) / speedOf(battle, attacker).coerceAtLeast(1) + 1).coerceAtMost(150)
          MoveEffect.ELECTRO_BALL ->
              when (speedOf(battle, attacker) / speedOf(battle, defender).coerceAtLeast(1)) {
                0 -> 40
                1 -> 60
                2 -> 80
                3 -> 120
                else -> 150
              }
          MoveEffect.STORED_POWER -> move.power + 20 * positiveStages(attacker)
          MoveEffect.PUNISHMENT -> (60 + 20 * positiveStages(defender)).coerceAtMost(200)
          MoveEffect.TRUMP_CARD ->
              when (attacker.moves.firstOrNull { it.id.toInt() == move.id }?.pp?.toInt() ?: 0) {
                0 -> 200
                1 -> 80
                2 -> 60
                3 -> 50
                else -> 40
              }
          MoveEffect.SPIT_UP -> 100 * attacker.stockpile
          MoveEffect.FURY_CUTTER -> (move.power shl priorStreak(attacker, move).coerceAtMost(3)).coerceAtMost(160)
          MoveEffect.ROLLOUT -> (move.power shl (priorStreak(attacker, move) % 5)) * (if (attacker.defenseCurled) 2 else 1)
          MoveEffect.ECHOED_VOICE -> (move.power * (1 + battle.field.echoedVoice)).coerceAtMost(200)
          MoveEffect.LAST_RESPECTS -> move.power + move.power * team(battle, attacker).count { it.fainted }.coerceAtMost(100)
          MoveEffect.RAGE_FIST -> (move.power + 50 * attacker.timesHit).coerceAtMost(350)
          MoveEffect.FLING -> items.flingPower(items.of(attacker)).coerceAtLeast(1)
          MoveEffect.NATURAL_GIFT -> items.naturalGift(items.of(attacker))?.second ?: 1
          // Gen 5+ Beat Up: one strike per able party member, 5 + that member's base Attack / 10.
          MoveEffect.BEAT_UP -> 5 + (beatUpCrew(battle, attacker).getOrNull(attacker.beatUpIndex)?.species?.baseAttack ?: 0) / 10
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
      MoveEffect.ACROBATICS -> if (attacker.heldItem == 0) power *= 2
      MoveEffect.BRINE -> if (defender.currentHp * 2 <= defender.maxHp) power *= 2
      MoveEffect.RETALIATE -> if (battle.sideOf(attacker).faintedLastTurn) power *= 2
      MoveEffect.STOMPING_TANTRUM -> if (attacker.lastMoveFailed) power *= 2
      MoveEffect.BOLT_BEAK -> if (!defender.movedThisTurn) power *= 2
      MoveEffect.TERRAIN_PULSE -> if (battle.field.terrain != null && isGrounded(battle, attacker)) power *= 2
      MoveEffect.TERRAIN_BOOST ->
          when (move.id) {
            EXPANDING_FORCE -> if (battle.field.terrain == Terrain.PSYCHIC && isGrounded(battle, attacker)) power = power * 3 / 2
            RISING_VOLTAGE -> if (battle.field.terrain == Terrain.ELECTRIC && isGrounded(battle, defender)) power *= 2
            MISTY_EXPLOSION -> if (battle.field.terrain == Terrain.MISTY && isGrounded(battle, attacker)) power = power * 3 / 2
            PSYBLADE -> if (battle.field.terrain == Terrain.ELECTRIC) power = power * 3 / 2
          }
      MoveEffect.GRAV_APPLE -> if (battle.field.gravityTurns > 0) power = power * 3 / 2
      MoveEffect.FICKLE_BEAM -> if (battle.rng.accuracyRoll() <= 30) power *= 2
      MoveEffect.ROUND -> if (battle.sideOf(attacker).roundUsedThisTurn) power *= 2
      MoveEffect.FUSION_COMBO ->
          if (battle.field.lastMoveThisTurn.let { it != 0 && it != move.id && moves.get(it)?.effect == MoveEffect.FUSION_COMBO }) power *= 2
      MoveEffect.LASH_OUT -> if (attacker.statLoweredThisTurn) power *= 2
      MoveEffect.PURSUIT -> if (defender.pursued) power *= 2
      else -> Unit
    }
    if (attacker.charged && move.type == PokemonType.ELECTRIC) power *= 2
    if (move.type == PokemonType.ELECTRIC && battle.field.mudSportTurns > 0) power /= 3
    if (move.type == PokemonType.FIRE && battle.field.waterSportTurns > 0) power /= 3
    // Earthquake into Dig, Surf into Dive, Gust into Fly: the moves that reach a hidden target hit it twice as hard.
    if (defender.semiInvulnerable && reachesHidden(defender, move) &&
        (move.hasFlag(MoveFlag.DAMAGES_UNDERGROUND) || move.hasFlag(MoveFlag.DAMAGES_UNDERWATER) ||
            move.hasFlag(MoveFlag.DAMAGES_AIRBORNE_DOUBLE))) power *= 2
    return power
  }

  private fun hit(
      battle: BattleInstance,
      action: TurnAction,
      move: MoveDef,
      type: PokemonType,
      effectiveness: Int,
      power: Int,
      movesLast: Boolean = false,
  ): HitResult {
    val attacker = action.attacker
    val defender = action.defender
    val physical = isPhysical(attacker, defender, move)
    val critStage =
        (if (attacker.focusEnergy) 2 else 0) +
            (if (move.effect == MoveEffect.HIGH_CRITICAL) 1 else 0) +
            move.criticalHitStage +
            attacker.critBoost +
            Abilities.critStages(attacker) +
            items.critStages(attacker)
    val critBlocked =
        (Abilities.noCrits(defender) && !Abilities.ignoresTargetAbilities(attacker)) || battle.sideOf(defender).luckyChantTurns > 0
    // Psyshock hits the physical Defense, Foul Play uses the target's Attack, Body Press the user's
    // Defense; Wonder Room swaps the two defenses for everyone.
    var defenseStat: BattleStat? = if (move.effect == MoveEffect.PSYSHOCK) BattleStat.DEFENSE else null
    if (battle.field.wonderRoomTurns > 0) {
      val base = defenseStat ?: if (physical) BattleStat.DEFENSE else BattleStat.SP_DEFENSE
      defenseStat = if (base == BattleStat.DEFENSE) BattleStat.SP_DEFENSE else BattleStat.DEFENSE
    }
    val crit =
        !critBlocked &&
            (move.hasFlag(MoveFlag.ALWAYS_CRIT) ||
                (attacker.ability == Ability.MERCILESS && StatusCondition.isPoisoned(defender.status)) ||
                battle.rng.critRoll(CRIT_DENOMINATORS[critStage.coerceIn(0, CRIT_DENOMINATORS.lastIndex)]))
    var dmg =
        baseDamage(
            attacker, defender, power, physical, crit, battle.rng,
            explosion = move.hasFlag(MoveFlag.EXPLOSION) || move.effect == MoveEffect.EXPLOSION,
            defenseStatPercent = Abilities.defenseStatPercent(defender, attacker, physical),
            attackSource = if (move.effect == MoveEffect.FOUL_PLAY) defender else attacker,
            attackStat = if (move.effect == MoveEffect.BODY_PRESS) BattleStat.DEFENSE else null,
            defenseStat = defenseStat)
    if (physical && StatusCondition.isBurned(attacker.status) && attacker.ability != Ability.GUTS) dmg /= 2
    val side = battle.sideOf(defender)
    if (!crit && ((physical && side.reflectTurns > 0) || (!physical && side.lightScreenTurns > 0))) dmg /= 2
    val weather = weather(battle)
    when (weather) {
      Weather.RAIN -> if (type == PokemonType.WATER) dmg = dmg * 3 / 2 else if (type == PokemonType.FIRE) dmg /= 2
      // Hydro Steam is the Water move the sun strengthens.
      Weather.SUN ->
          if (type == PokemonType.FIRE || move.effect == MoveEffect.HYDRO_STEAM) dmg = dmg * 3 / 2
          else if (type == PokemonType.WATER) dmg /= 2
      else -> Unit
    }
    if (crit) dmg = if (attacker.ability == Ability.SNIPER) dmg * 3 else dmg * 2
    val stab = attacker.hasType(type)
    if (stab) dmg = if (attacker.ability == Ability.ADAPTABILITY) dmg * 2 else dmg * 3 / 2
    dmg = dmg * effectiveness / TypeChart.NEUTRAL
    dmg = dmg * Abilities.offensePercent(attacker, move, type, power, physical, weather, effectiveness, movesLast, defender) / 100
    dmg = dmg * Abilities.defensePercent(defender, attacker, move, type, physical, effectiveness) / 100
    dmg = dmg * items.offensePercent(attacker, move, type, physical, effectiveness) / 100
    if (attacker.helpingHand) dmg = dmg * 3 / 2
    if (attacker.meFirst) dmg = dmg * 3 / 2
    // Terrains boost grounded attackers' moves of their type and soften some hits on grounded targets.
    when (battle.field.terrain) {
      Terrain.ELECTRIC -> if (type == PokemonType.ELECTRIC && isGrounded(battle, attacker)) dmg = dmg * 13 / 10
      Terrain.GRASSY -> {
        if (type == PokemonType.GRASS && isGrounded(battle, attacker)) dmg = dmg * 13 / 10
        if ((move.effect == MoveEffect.EARTHQUAKE || move.effect == MoveEffect.MAGNITUDE) && isGrounded(battle, defender)) dmg /= 2
      }
      Terrain.PSYCHIC -> if (type == PokemonType.PSYCHIC && isGrounded(battle, attacker)) dmg = dmg * 13 / 10
      Terrain.MISTY -> if (type == PokemonType.DRAGON && isGrounded(battle, defender)) dmg /= 2
      null -> Unit
    }
    if (move.effect == MoveEffect.COLLISION_COURSE && effectiveness > TypeChart.NEUTRAL) dmg = dmg * 4 / 3
    dmg =
        dmg *
            items.defensePercent(
                defender,
                physical,
                de.fiereu.openmmo.server.game.services.EvolutionTable.canEvolve(defender.wireSpeciesId().toInt())) / 100
    dmg = dmg * battle.rng.damageRoll() / 100
    if (action.spread) dmg = dmg * 3 / 4
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
      defenseStatPercent: Int = 100,
      attackSource: BattleMonState = attacker,
      attackStat: BattleStat? = null,
      defenseStat: BattleStat? = null,
  ): Int {
    val atkStat = attackStat ?: if (physical) BattleStat.ATTACK else BattleStat.SP_ATTACK
    val defStat = defenseStat ?: if (physical) BattleStat.DEFENSE else BattleStat.SP_DEFENSE
    // A crit ignores the attacker's negative stages and the defender's positive stages; Unaware
    // ignores the other side's stages altogether.
    val atk =
        if ((crit && attackSource.stage(atkStat) < 0) || defender.ability == Ability.UNAWARE) attackSource.unstaged(atkStat)
        else attackSource.effective(atkStat)
    var def =
        if ((crit && defender.stage(defStat) > 0) || attacker.ability == Ability.UNAWARE) defender.unstaged(defStat)
        else defender.effective(defStat)
    def = def * defenseStatPercent / 100
    if (explosion) def = (def / 2).coerceAtLeast(1)
    return (2 * attacker.level / 5 + 2) * power * atk / def.coerceAtLeast(1) / 50 + 2
  }

  /** Whether [move] reaches a [defender] hidden by Fly, Dig or Dive; Shadow Force and the like hide it from everything. */
  private fun reachesHidden(defender: BattleMonState, move: MoveDef): Boolean =
      when (defender.chargingMoveId) {
        in HIDDEN_IN_AIR -> move.hasFlag(MoveFlag.DAMAGES_AIRBORNE) || move.hasFlag(MoveFlag.DAMAGES_AIRBORNE_DOUBLE)
        HIDDEN_UNDERGROUND -> move.hasFlag(MoveFlag.DAMAGES_UNDERGROUND)
        HIDDEN_UNDERWATER -> move.hasFlag(MoveFlag.DAMAGES_UNDERWATER)
        else -> false
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
    if (attacker.ability == Ability.NO_GUARD || defender.ability == Ability.NO_GUARD) return true
    if (defender.semiInvulnerable && !reachesHidden(defender, move)) return false
    // Telekinesis leaves its target unable to dodge anything but a one-hit KO.
    if (defender.telekinesisTurns > 0 && move.effect != MoveEffect.OHKO) return true
    if (move.effect == MoveEffect.OHKO) {
      if (defender.level > attacker.level) return false
      return battle.rng.accuracyRoll() <= 30 + (attacker.level - defender.level)
    }
    var accuracy = move.accuracy
    val weather = weather(battle)
    if (move.hasFlag(MoveFlag.ALWAYS_HITS_IN_RAIN)) {
      when (weather) {
        Weather.RAIN -> return true
        Weather.SUN -> accuracy = 50
        else -> Unit
      }
    }
    if (move.hasFlag(MoveFlag.ALWAYS_HITS_IN_HAIL) && weather == Weather.HAIL) return true
    if (accuracy == 0) return true
    // Keen Eye and Mind's Eye look past evasion boosts; Unaware on either side drops the other's stages.
    val evasionStage =
        if (attacker.ability == Ability.KEEN_EYE || attacker.ability == Ability.MINDS_EYE || attacker.ability == Ability.UNAWARE)
            defender.stage(BattleStat.EVASION).coerceAtMost(0)
        else defender.stage(BattleStat.EVASION)
    val accuracyStage = if (defender.ability == Ability.UNAWARE) 0 else attacker.stage(BattleStat.ACCURACY)
    val stage = accuracyStage - evasionStage
    var threshold = StatStages.scaleAccuracy(accuracy, stage)
    if (battle.field.gravityTurns > 0) threshold = threshold * 5 / 3
    threshold = threshold * Abilities.accuracyPercent(attacker, move, move.category == DamageCategory.PHYSICAL) / 100
    threshold = threshold * items.accuracyPercent(attacker, defender, speedOf(battle, attacker) < speedOf(battle, defender)) / 100
    if (!Abilities.ignoresTargetAbilities(attacker)) threshold = threshold * Abilities.evasionPercent(defender, move, weather) / 100
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
    val sheerForce = attacker.ability == Ability.SHEER_FORCE
    val shieldDust = Abilities.blocksSecondaryEffects(defender) && !Abilities.ignoresTargetAbilities(attacker)
    for (extra in move.additionalEffects) {
      if (!extra.self && (sheerForce || shieldDust)) continue
      val chance = (extra.chance * Abilities.secondaryChanceMultiplier(attacker)).coerceAtMost(100)
      if (extra.chance < 100 && battle.rng.accuracyRoll() > chance) continue
      if (!extra.self && defender.fainted) continue
      applyAdditional(battle, action, move, extra, events)
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
      MoveAdditionalEffect.SLEEP -> inflictStatus(battle, attacker, target, StatusCondition.asleep(2 + battle.rng.pick(3)), events)
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
      MoveAdditionalEffect.FLINCH ->
          if (!target.movedThisTurn) {
            if (Abilities.blocksFlinch(target) && !Abilities.ignoresTargetAbilities(attacker)) Unit
            else target.flinched = true
          }
      MoveAdditionalEffect.STAT_PLUS, MoveAdditionalEffect.STAT_MINUS -> {
        val sign = if (extra.effect == MoveAdditionalEffect.STAT_PLUS) 1 else -1
        for ((name, stages) in extra.stats) {
          val stat = battleStat(name) ?: continue
          applyStage(action, StageEffect(stat, sign * stages, extra.self), events)
        }
      }
      MoveAdditionalEffect.WRAP ->
          if (target.trappedTurns == 0) {
            target.trappedTurns = items.trapTurns(attacker, 2 + battle.rng.pick(4))
            target.trappingMoveId = move.id
            target.trapDamageDivisor = items.trapDamageDivisor(attacker)
          }
      MoveAdditionalEffect.PREVENT_ESCAPE, MoveAdditionalEffect.TRAP_BOTH ->
          if (target.trappedTurns == 0) {
            target.trappedTurns = 99
            events += BattleEvent.Line(target.entityId, BattleLine.NO_ESCAPE)
          }
      MoveAdditionalEffect.RECHARGE -> attacker.mustRecharge = true
      MoveAdditionalEffect.RECOIL_HP_25 -> loseHp(attacker, (attacker.maxHp / 4).coerceAtLeast(1), events)
      // Burn Up and Double Shock: the type in the move's argument leaves the user; a pure type leaves it typeless.
      MoveAdditionalEffect.REMOVE_ARG_TYPE ->
          argType(move)?.let { lost ->
            val kept = listOf(target.type1, target.type2).filter { it != lost }
            val none = PokemonType.QUESTIONQUESTIONQUESTION
            target.typeOverride = (kept.firstOrNull() ?: none) to (kept.lastOrNull() ?: none)
          }
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
      MoveAdditionalEffect.HAZE -> haze(attacker, defender, events)
      MoveAdditionalEffect.AROMATHERAPY, MoveAdditionalEffect.HEAL_TEAM -> cureStatus(attacker, events)
      else -> Unit
    }
  }

  private fun haze(attacker: BattleMonState, defender: BattleMonState, events: MutableList<BattleEvent>) {
    for (mon in listOf(attacker, defender)) {
      var any = false
      for (stat in BattleStat.entries) if (mon.changeStage(stat, -mon.stage(stat)) != 0) any = true
      if (any) events += BattleEvent.Line(mon.entityId, BattleLine.STATS_CLEARED)
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
      return
    }
    if (targetsFoe && defender.semiInvulnerable && move.accuracy > 0) {
      events += BattleEvent.MoveMissed(attacker.entityId, moveId)
      return
    }
    if (targetsFoe && move.effect != MoveEffect.NON_VOLATILE_STATUS && absorbedByAbility(battle, action, move, move.type, events)) return
    // Magic Coat and Magic Bounce send a reflectable status move back at its user.
    if (move.hasFlag(MoveFlag.MAGIC_COAT_AFFECTED) && defender !== attacker && !action.reflected &&
        (defender.magicCoat || (defender.ability == Ability.MAGIC_BOUNCE && !Abilities.ignoresTargetAbilities(attacker)))) {
      if (!defender.magicCoat) events += BattleEvent.AbilityShown(defender.entityId, Ability.MAGIC_BOUNCE)
      statusMove(battle, TurnAction(defender, attacker, move, reflected = true), move, movesLast, events)
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

    // A Substitute shuts out the status moves aimed at the monster behind it.
    val aimedAtMon = targetsFoe && move.target != MoveTarget.OPPONENTS_FIELD && move.target != MoveTarget.ALL_BATTLERS
    if (aimedAtMon && defender !== attacker && defender.substituteHp > 0 && !bypassesSubstitute(attacker, move) &&
        battle.alliesOf(attacker).none { it === defender }) return fail()

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
        // The Expansion's stat move changes whoever the move targets; Dragon Dance and Shell Smash
        // aim at their user without marking each change as self.
        val onUser = move.target == MoveTarget.USER || move.target == MoveTarget.USER_AND_ALLY
        val hostile = move.additionalEffects.any { !it.self && !onUser }
        if (hostile && !accurate()) return
        var applied = false
        for (extra in move.additionalEffects) {
          val sign = if (extra.effect == MoveAdditionalEffect.STAT_MINUS) -1 else 1
          for ((name, stages) in extra.stats) {
            val stat = battleStat(name) ?: continue
            if (applyStage(action, StageEffect(stat, sign * stages, extra.self || onUser), events)) applied = true
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
              MoveEffect.SLEEP, MoveEffect.DARK_VOID -> StatusCondition.asleep(2 + battle.rng.pick(3))
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
          events += BattleEvent.Line(defender.entityId, BattleLine.DROWSY)
          return
        }
        if (status == 0) return fail()
        if (effectivenessAgainst(move.type, defender, attacker) == 0) {
          events += BattleEvent.Immune(defender.entityId)
          return
        }
        if (absorbedByAbility(battle, action, move, move.type, events)) return
        if (!canReceiveStatus(battle, attacker, defender, status)) {
          if (Abilities.blocksStatus(defender, status, weather(battle), fromMove = true) && !Abilities.ignoresTargetAbilities(attacker))
              events += BattleEvent.AbilityShown(defender.entityId, defender.ability)
          return fail()
        }
        if (!accurate()) return
        inflictStatus(battle, attacker, defender, status, events)
      }
      MoveEffect.CONFUSE, MoveEffect.TEETER_DANCE -> {
        if (defender.confusionTurns > 0 || battle.sideOf(defender).safeguardTurns > 0) return fail()
        if (!accurate()) return
        confuse(battle, attacker, defender, events)
      }
      // Swagger and Flatter share the Expansion's EFFECT_SWAGGER; the stat they raise is in the data.
      MoveEffect.SWAGGER, MoveEffect.FLATTER -> {
        if (!accurate()) return
        for (extra in move.additionalEffects) {
          for ((name, stages) in extra.stats) {
            val stat = battleStat(name) ?: continue
            applyStage(action, StageEffect(stat, stages, false), events)
          }
        }
        confuse(battle, attacker, defender, events)
      }
      MoveEffect.TRANSFORM -> if (!transform(attacker, defender, events)) fail()
      MoveEffect.DO_NOTHING -> Unit
      MoveEffect.SUBSTITUTE -> {
        val cost = attacker.maxHp / 4
        if (attacker.substituteHp > 0 || cost == 0 || attacker.currentHp <= cost) return fail()
        loseHp(attacker, cost, events)
        attacker.substituteHp = cost
      }
      MoveEffect.TAUNT -> {
        if (defender.tauntTurns > 0 || defender.ability == Ability.OBLIVIOUS) return fail()
        if (!accurate()) return
        defender.tauntTurns = if (defender.movedThisTurn) 4 else 3
      }
      MoveEffect.ENCORE -> {
        val last = defender.lastMoveId
        if (last == 0 || defender.encoreTurns > 0 || moves.get(last)?.let { it.hasFlag(MoveFlag.ENCORE_BANNED) || it.effect in ENCORE_FAILS } == true ||
            defender.moves.none { it.id.toInt() == last && it.pp > 0 }) return fail()
        if (!accurate()) return
        defender.encoreMoveId = last
        defender.encoreTurns = if (defender.movedThisTurn) 4 else 3
      }
      MoveEffect.DISABLE -> {
        val last = defender.lastMoveId
        if (last == 0 || last == STRUGGLE_ID || defender.disableTurns > 0 || defender.moves.none { it.id.toInt() == last && it.pp > 0 }) return fail()
        if (!accurate()) return
        defender.disabledMoveId = last
        defender.disableTurns = 4
      }
      MoveEffect.TORMENT -> {
        if (defender.tormented) return fail()
        if (!accurate()) return
        defender.tormented = true
      }
      MoveEffect.ATTRACT -> {
        if (attacker.gender < 0 || defender.gender < 0 || attacker.gender == defender.gender ||
            defender.infatuatedWith != null || defender.ability == Ability.OBLIVIOUS) return fail()
        if (!accurate()) return
        defender.infatuatedWith = attacker
      }
      MoveEffect.DESTINY_BOND -> attacker.destinyBond = true
      MoveEffect.PERISH_SONG -> {
        val hearing = battle.actives().filter { !it.fainted && it.perishCount == 0 && it.ability != Ability.SOUNDPROOF }
        if (hearing.isEmpty()) return fail()
        for (mon in hearing) mon.perishCount = PERISH_COUNT
      }
      MoveEffect.AQUA_RING -> {
        if (attacker.aquaRing) return fail()
        attacker.aquaRing = true
      }
      MoveEffect.MAGNET_RISE -> {
        if (attacker.magnetRiseTurns > 0 || attacker.grounded || battle.field.gravityTurns > 0) return fail()
        attacker.magnetRiseTurns = 5
      }
      MoveEffect.HEAL_BLOCK -> {
        if (defender.healBlockTurns > 0) return fail()
        if (!accurate()) return
        defender.healBlockTurns = 5
      }
      MoveEffect.TAILWIND -> {
        val side = battle.sideOf(attacker)
        if (side.tailwindTurns > 0) return fail()
        side.tailwindTurns = 4
      }
      MoveEffect.LUCKY_CHANT -> {
        val side = battle.sideOf(attacker)
        if (side.luckyChantTurns > 0) return fail()
        side.luckyChantTurns = 5
      }
      // The rooms toggle: used again while up, they end.
      MoveEffect.TRICK_ROOM -> battle.field.trickRoomTurns = if (battle.field.trickRoomTurns > 0) 0 else 5
      MoveEffect.WONDER_ROOM -> battle.field.wonderRoomTurns = if (battle.field.wonderRoomTurns > 0) 0 else 5
      MoveEffect.GRAVITY -> {
        if (battle.field.gravityTurns > 0) return fail()
        battle.field.gravityTurns = 5
        for (mon in battle.actives()) {
          mon.magnetRiseTurns = 0
          if (mon.chargingMoveId in HIDDEN_IN_AIR) {
            mon.chargingMoveId = 0
            mon.semiInvulnerable = false
            events += BattleEvent.Hidden(mon.entityId, false)
          }
        }
      }
      MoveEffect.SPIKES, MoveEffect.TOXIC_SPIKES, MoveEffect.STEALTH_ROCK, MoveEffect.STICKY_WEB -> {
        val side = if (battle.isPlayerSide(attacker.entityId)) battle.opponentSide else battle.playerSide
        when (move.effect) {
          MoveEffect.SPIKES -> if (side.spikes >= 3) return fail() else side.spikes++
          MoveEffect.TOXIC_SPIKES -> if (side.toxicSpikes >= 2) return fail() else side.toxicSpikes++
          MoveEffect.STEALTH_ROCK -> if (side.stealthRock) return fail() else side.stealthRock = true
          else -> if (side.stickyWeb) return fail() else side.stickyWeb = true
        }
      }
      MoveEffect.FOLLOW_ME -> attacker.centerOfAttention = true
      MoveEffect.HELPING_HAND -> {
        val ally = battle.alliesOf(attacker).firstOrNull() ?: return fail()
        ally.helpingHand = true
      }
      MoveEffect.HEAL_PULSE -> {
        if (defender.currentHp >= defender.maxHp || defender.healBlockTurns > 0) return fail()
        healHp(defender, (defender.maxHp + 1) / 2, events)
      }
      MoveEffect.LIFE_DEW, MoveEffect.JUNGLE_HEALING -> {
        val cures = move.effect == MoveEffect.JUNGLE_HEALING
        val team = (listOf(attacker) + battle.alliesOf(attacker)).filter { it.currentHp < it.maxHp || (cures && StatusCondition.hasAny(it.status)) }
        if (team.isEmpty()) return fail()
        for (mon in team) {
          healHp(mon, (mon.maxHp / 4).coerceAtLeast(1), events)
          if (cures) cureStatus(mon, events)
        }
      }
      MoveEffect.OVERWRITE_ABILITY -> {
        val ability = move.argument?.value?.removePrefix("ABILITY_")?.let { runCatching { Ability.valueOf(it) }.getOrNull() } ?: return fail()
        if (defender.ability == ability || defender.ability == Ability.MULTITYPE || defender.ability == Ability.TRUANT) return fail()
        if (!accurate()) return
        defender.ability = ability
        events += BattleEvent.AbilityShown(defender.entityId, ability)
        if (ability == Ability.INSOMNIA && StatusCondition.isAsleep(defender.status)) cureStatus(defender, events)
      }
      MoveEffect.ENTRAINMENT -> {
        if (attacker.ability == defender.ability || attacker.ability in UNTRACEABLE ||
            defender.ability == Ability.TRUANT || defender.ability == Ability.MULTITYPE) return fail()
        if (!accurate()) return
        defender.ability = attacker.ability
        events += BattleEvent.AbilityShown(defender.entityId, defender.ability)
      }
      MoveEffect.ROLE_PLAY -> {
        if (attacker.ability == defender.ability || defender.ability in UNTRACEABLE || defender.ability == Ability.WONDER_GUARD) return fail()
        attacker.ability = defender.ability
        events += BattleEvent.AbilityShown(attacker.entityId, attacker.ability)
      }
      MoveEffect.SKILL_SWAP -> {
        val locked = setOf(Ability.WONDER_GUARD, Ability.MULTITYPE, Ability.ILLUSION)
        if (attacker.ability in locked || defender.ability in locked || attacker.ability == defender.ability) return fail()
        if (!accurate()) return
        val mine = attacker.ability
        attacker.ability = defender.ability
        defender.ability = mine
        events += BattleEvent.AbilityShown(attacker.entityId, attacker.ability)
        events += BattleEvent.AbilityShown(defender.entityId, defender.ability)
      }
      MoveEffect.GASTRO_ACID -> {
        if (defender.ability == Ability.NONE || defender.ability == Ability.MULTITYPE) return fail()
        if (!accurate()) return
        defender.ability = Ability.NONE
      }
      MoveEffect.POWER_SWAP, MoveEffect.GUARD_SWAP, MoveEffect.HEART_SWAP -> {
        val swapped =
            when (move.effect) {
              MoveEffect.POWER_SWAP -> listOf(BattleStat.ATTACK, BattleStat.SP_ATTACK)
              MoveEffect.GUARD_SWAP -> listOf(BattleStat.DEFENSE, BattleStat.SP_DEFENSE)
              else -> BattleStat.entries
            }
        for (stat in swapped) {
          val mine = attacker.stage(stat)
          val theirs = defender.stage(stat)
          attacker.changeStage(stat, theirs - mine)
          defender.changeStage(stat, mine - theirs)
        }
      }
      MoveEffect.SPEED_SWAP -> {
        val mine = attacker.stats.spd
        attacker.stats = attacker.stats.copy(spd = defender.stats.spd)
        defender.stats = defender.stats.copy(spd = mine)
      }
      MoveEffect.POWER_SPLIT -> {
        val atk = (attacker.stats.atk + defender.stats.atk) / 2
        val spAtk = (attacker.stats.spAtk + defender.stats.spAtk) / 2
        attacker.stats = attacker.stats.copy(atk = atk, spAtk = spAtk)
        defender.stats = defender.stats.copy(atk = atk, spAtk = spAtk)
      }
      MoveEffect.GUARD_SPLIT -> {
        val def = (attacker.stats.def + defender.stats.def) / 2
        val spDef = (attacker.stats.spDef + defender.stats.spDef) / 2
        attacker.stats = attacker.stats.copy(def = def, spDef = spDef)
        defender.stats = defender.stats.copy(def = def, spDef = spDef)
      }
      MoveEffect.POWER_TRICK -> attacker.stats = attacker.stats.copy(atk = attacker.stats.def, def = attacker.stats.atk)
      MoveEffect.PSYCHO_SHIFT -> {
        val status = attacker.status
        if (!StatusCondition.hasAny(status) || !canReceiveStatus(battle, attacker, defender, status)) return fail()
        if (!accurate()) return
        inflictStatus(battle, attacker, defender, status, events)
        cureStatus(attacker, events)
      }
      MoveEffect.ACUPRESSURE -> {
        val target = if (battle.alliesOf(attacker).any { it === defender }) defender else attacker
        val open = BattleStat.entries.filter { target.stage(it) < StatStages.MAX }
        if (open.isEmpty()) return fail()
        ownStage(target, open[battle.rng.pick(open.size)], 2, events)
      }
      MoveEffect.TOPSY_TURVY -> {
        if (BattleStat.entries.all { defender.stage(it) == 0 }) return fail()
        if (!accurate()) return
        for (stat in BattleStat.entries) defender.changeStage(stat, -2 * defender.stage(stat))
      }
      MoveEffect.SPITE -> {
        val slot = defender.moves.indexOfFirst { defender.lastMoveId != 0 && it.id.toInt() == defender.lastMoveId && it.pp > 0 }
        if (slot < 0) return fail()
        defender.moves[slot].pp = (defender.moves[slot].pp - 4).coerceAtLeast(0).toByte()
      }
      MoveEffect.HEALING_WISH, MoveEffect.LUNAR_DANCE -> {
        val team = if (battle.isPlayerSide(attacker.entityId)) battle.party else battle.opponent
        if (team.none { !it.fainted && it !in battle.actives() }) return fail()
        battle.sideOf(attacker).healingWish = if (move.effect == MoveEffect.LUNAR_DANCE) 2 else 1
        explode(attacker, events)
      }
      MoveEffect.MIRROR_MOVE -> {
        val picked = moves.get(defender.lastMoveId)?.takeIf { it.hasFlag(MoveFlag.MIRROR_MOVE_AFFECTED) } ?: return fail()
        callMove(battle, action, picked, movesLast, events)
      }
      MoveEffect.COPYCAT -> {
        val picked =
            moves.get(battle.field.lastMoveId)?.takeIf { !it.hasFlag(MoveFlag.COPYCAT_BANNED) && it.effect !in CALLS_OTHER_MOVES } ?: return fail()
        callMove(battle, action, picked, movesLast, events)
      }
      MoveEffect.STOCKPILE -> {
        if (attacker.stockpile >= 3) return fail()
        attacker.stockpile++
        if (ownStage(attacker, BattleStat.DEFENSE, 1, events)) attacker.stockpileDef++
        if (ownStage(attacker, BattleStat.SP_DEFENSE, 1, events)) attacker.stockpileSpDef++
      }
      MoveEffect.SWALLOW -> {
        if (attacker.stockpile == 0 || attacker.currentHp >= attacker.maxHp) return fail()
        val heal =
            when (attacker.stockpile) {
              1 -> attacker.maxHp / 4
              2 -> attacker.maxHp / 2
              else -> attacker.maxHp
            }
        healHp(attacker, heal.coerceAtLeast(1), events)
        releaseStockpile(attacker, events)
      }
      // The Expansion's own stat moves: a condition or a side effect on top of the stat changes in the data.
      MoveEffect.CAPTIVATE -> {
        if (attacker.gender < 0 || defender.gender < 0 || attacker.gender == defender.gender || defender.ability == Ability.OBLIVIOUS) return fail()
        if (!accurate()) return
        if (!moveStats(action, move, defender, events)) fail()
      }
      MoveEffect.STAT_CHANGE_ON_STATUS -> {
        if (!StatusCondition.isPoisoned(defender.status)) return fail()
        if (!accurate()) return
        if (!moveStats(action, move, defender, events)) fail()
      }
      MoveEffect.CHARGE -> {
        attacker.charged = true
        moveStats(action, move, attacker, events)
      }
      MoveEffect.DEFOG -> {
        if (!accurate()) return
        moveStats(action, move, defender, events)
        val foeSide = battle.sideOf(defender)
        foeSide.reflectTurns = 0
        foeSide.lightScreenTurns = 0
        foeSide.safeguardTurns = 0
        foeSide.mistTurns = 0
        clearHazards(battle)
      }
      MoveEffect.MEMENTO -> {
        if (accurate()) moveStats(action, move, defender, events)
        explode(attacker, events)
      }
      MoveEffect.STRENGTH_SAP -> {
        if (defender.stage(BattleStat.ATTACK) <= StatStages.MIN) return fail()
        if (!accurate()) return
        val drained = defender.effective(BattleStat.ATTACK)
        moveStats(action, move, defender, events)
        healHp(attacker, drained, events)
      }
      // Parting Shot switches out only once a stat really dropped.
      MoveEffect.PARTING_SHOT -> {
        if (!accurate()) return
        if (moveStats(action, move, defender, events)) selfSwitch(battle, attacker, false, events) else fail()
      }
      MoveEffect.TAR_SHOT -> {
        if (!accurate()) return
        if (!moveStats(action, move, defender, events)) fail()
      }
      MoveEffect.BATON_PASS -> if (!selfSwitch(battle, attacker, true, events)) fail()
      MoveEffect.WEATHER_AND_SWITCH -> {
        setWeather(battle, Weather.HAIL, events, attacker)
        selfSwitch(battle, attacker, false, events)
      }
      MoveEffect.ROAR -> if (!dragOut(battle, attacker, defender, move, events)) fail()
      // Teleport (Gen 5): an escape from a wild battle, nothing in a trainer's.
      MoveEffect.TELEPORT -> {
        if (battle.trainer != null || !battle.escapable) return fail()
        battle.moveEnded = if (battle.isPlayerSide(attacker.entityId)) MoveEnding.PLAYER_FLED else MoveEnding.WILD_FLED
      }
      MoveEffect.NO_RETREAT -> {
        if (attacker.noRetreat) return fail()
        attacker.noRetreat = true
        attacker.trappedTurns = 99
        moveStats(action, move, attacker, events)
      }
      MoveEffect.CLANGOROUS_SOUL -> {
        val cost = attacker.maxHp / 3
        if (attacker.currentHp <= cost) return fail()
        loseHp(attacker, cost, events)
        moveStats(action, move, attacker, events)
      }
      MoveEffect.GEOMANCY -> moveStats(action, move, attacker, events)
      MoveEffect.STAT_CHANGE_MAGNETIC -> {
        val team = (listOf(attacker) + battle.alliesOf(attacker)).filter { it.ability == Ability.PLUS || it.ability == Ability.MINUS }
        if (team.isEmpty()) return fail()
        for (mon in team) moveStats(action, move, mon, events)
      }
      MoveEffect.ROTOTILLER, MoveEffect.FLOWER_SHIELD -> {
        val groundOnly = move.effect == MoveEffect.ROTOTILLER
        val grass =
            battle.actives().filter {
              !it.fainted && it.hasType(PokemonType.GRASS) && !(groundOnly && it.hasType(PokemonType.FLYING))
            }
        if (grass.isEmpty()) return fail()
        for (mon in grass) moveStats(action, move, mon, events)
      }
      MoveEffect.TIDY_UP -> {
        clearHazards(battle)
        for (mon in battle.actives()) mon.substituteHp = 0
        moveStats(action, move, attacker, events)
      }
      MoveEffect.STUFF_CHEEKS -> {
        if (!items.isBerry(items.get(attacker.heldItem))) return fail()
        consumeItem(attacker, events)
        moveStats(action, move, attacker, events)
      }
      // Aurora Veil halves both kinds of damage for five turns, which the two screens already do.
      MoveEffect.AURORA_VEIL -> {
        val side = battle.sideOf(attacker)
        if (weather(battle) != Weather.HAIL || (side.reflectTurns > 0 && side.lightScreenTurns > 0)) return fail()
        val turns = items.screenTurns(attacker)
        if (side.reflectTurns == 0) side.reflectTurns = turns
        if (side.lightScreenTurns == 0) side.lightScreenTurns = turns
      }
      MoveEffect.MUD_SPORT -> {
        if (battle.field.mudSportTurns > 0) return fail()
        battle.field.mudSportTurns = 5
      }
      MoveEffect.WATER_SPORT -> {
        if (battle.field.waterSportTurns > 0) return fail()
        battle.field.waterSportTurns = 5
      }
      MoveEffect.SOAK -> {
        val type = argType(move) ?: return fail()
        if (defender.type1 == type && defender.type2 == type) return fail()
        if (!accurate()) return
        defender.typeOverride = type to type
      }
      // Conversion takes the type of the user's first move (Gen 6+).
      MoveEffect.CONVERSION -> {
        val type = attacker.moves.firstNotNullOfOrNull { moves.get(it.id.toInt())?.type } ?: return fail()
        if (attacker.type1 == type && attacker.type2 == type) return fail()
        attacker.typeOverride = type to type
      }
      // Conversion 2 picks a type that resists the target's last move.
      MoveEffect.CONVERSION_2 -> {
        val hit = moves.get(defender.lastMoveId)?.type ?: return fail()
        val resisting =
            PokemonType.entries.filter {
              it != PokemonType.QUESTIONQUESTIONQUESTION && typeChart.multiplier(hit, it) < TypeChart.NEUTRAL && !attacker.hasType(it)
            }
        if (resisting.isEmpty()) return fail()
        val type = resisting[battle.rng.pick(resisting.size)]
        attacker.typeOverride = type to type
      }
      MoveEffect.REFLECT_TYPE -> {
        if (attacker.type1 == defender.type1 && attacker.type2 == defender.type2) return fail()
        attacker.typeOverride = defender.type1 to defender.type2
      }
      MoveEffect.ELECTRIC_TERRAIN, MoveEffect.GRASSY_TERRAIN, MoveEffect.MISTY_TERRAIN, MoveEffect.PSYCHIC_TERRAIN -> {
        val terrain =
            when (move.effect) {
              MoveEffect.ELECTRIC_TERRAIN -> Terrain.ELECTRIC
              MoveEffect.GRASSY_TERRAIN -> Terrain.GRASSY
              MoveEffect.MISTY_TERRAIN -> Terrain.MISTY
              else -> Terrain.PSYCHIC
            }
        if (battle.field.terrain == terrain) return fail()
        battle.field.terrain = terrain
        battle.field.terrainTurns = 5
      }
      MoveEffect.EMBARGO -> {
        if (defender.embargoTurns > 0) return fail()
        if (!accurate()) return
        defender.embargoTurns = 5
      }
      MoveEffect.MAGIC_ROOM -> {
        battle.field.magicRoomTurns = if (battle.field.magicRoomTurns > 0) 0 else 5
        for (mon in battle.party + battle.opponent) mon.inMagicRoom = battle.field.magicRoomTurns > 0
      }
      // Camouflage takes the terrain's type, else the surroundings' (water, cave ground, plain Normal).
      MoveEffect.CAMOUFLAGE -> {
        val type =
            terrainType(battle)
                ?: when {
                  battle.encounter.surfing -> PokemonType.WATER
                  battle.encounter.cave -> PokemonType.GROUND
                  else -> PokemonType.NORMAL
                }
        if (attacker.type1 == type && attacker.type2 == type) return fail()
        attacker.typeOverride = type to type
      }
      MoveEffect.THIRD_TYPE -> {
        val type = argType(move) ?: return fail()
        if (defender.hasType(type)) return fail()
        if (!accurate()) return
        defender.thirdType = type
      }
      MoveEffect.HAPPY_HOUR, MoveEffect.CELEBRATE, MoveEffect.HOLD_HANDS -> Unit
      // Sketch keeps the target's last move for good, in Sketch's slot.
      MoveEffect.SKETCH -> {
        val slot = attacker.moves.indexOfFirst { it.id.toInt() == move.id }
        val sketched = moves.get(defender.lastMoveId)
        if (slot < 0 || attacker.transformed || sketched == null || sketched.hasFlag(MoveFlag.SKETCH_BANNED) ||
            attacker.moves.any { it.id.toInt() == sketched.id }) return fail()
        attacker.moves[slot] = PokemonMove(sketched.id.toShort(), sketched.pp.toByte())
        attacker.source = attacker.source.copy(moves = attacker.moves.map { PokemonMove(it.id, it.pp) })
        events += BattleEvent.MovesChanged(attacker.entityId, attacker.moves.map { it.id to it.pp })
      }
      // Mimic borrows it for as long as the monster stays in.
      MoveEffect.MIMIC -> {
        val slot = attacker.moves.indexOfFirst { it.id.toInt() == move.id }
        val copied = moves.get(defender.lastMoveId)
        if (slot < 0 || attacker.mimicked != null || copied == null || copied.hasFlag(MoveFlag.MIMIC_BANNED) ||
            attacker.moves.any { it.id.toInt() == copied.id }) return fail()
        if (!accurate()) return
        attacker.mimicked = slot to PokemonMove(attacker.moves[slot].id, attacker.moves[slot].pp)
        attacker.moves[slot] = PokemonMove(copied.id.toShort(), copied.pp.toByte())
        events += BattleEvent.MovesChanged(attacker.entityId, attacker.moves.map { it.id to it.pp })
      }
      MoveEffect.IMPRISON -> {
        if (attacker.imprisoning) return fail()
        attacker.imprisoning = true
      }
      MoveEffect.GRUDGE -> attacker.grudge = true
      MoveEffect.SNATCH -> attacker.snatching = true
      MoveEffect.MAGIC_COAT -> attacker.magicCoat = true
      MoveEffect.ASSIST -> {
        val pool =
            team(battle, attacker).filter { it !== attacker }.flatMap { it.moves }.mapNotNull { moves.get(it.id.toInt()) }
                .filter { !it.hasFlag(MoveFlag.ASSIST_BANNED) }
        if (pool.isEmpty()) return fail()
        callMove(battle, action, pool[battle.rng.pick(pool.size)], movesLast, events)
      }
      // Me First steals the attack the target is about to use, half again as strong.
      MoveEffect.ME_FIRST -> {
        val planned = battle.plannedMoves[defender]
        if (defender.movedThisTurn || planned == null || planned.power == 0 || planned.hasFlag(MoveFlag.ME_FIRST_BANNED)) return fail()
        attacker.meFirst = true
        callMove(battle, action, planned, movesLast, events)
        attacker.meFirst = false
      }
      MoveEffect.INSTRUCT -> {
        val again = moves.get(defender.lastMoveId)
        val slot = defender.moves.indexOfFirst { it.id.toInt() == defender.lastMoveId && it.pp > 0 }
        if (again == null || slot < 0 || defender === attacker || again.hasFlag(MoveFlag.INSTRUCT_BANNED) || defender.chargingMoveId != 0) return fail()
        defender.moves[slot].pp = (defender.moves[slot].pp - 1).toByte()
        callMove(battle, TurnAction(defender, battle.opponentOf(defender), again), again, movesLast, events)
      }
      MoveEffect.AFTER_YOU, MoveEffect.QUASH -> {
        if (defender === attacker || defender.movedThisTurn || battle.plannedMoves[defender] == null) return fail()
        if (!accurate()) return
        battle.reorder = defender to (move.effect == MoveEffect.AFTER_YOU)
      }
      MoveEffect.COURT_CHANGE -> battle.playerSide.swapWith(battle.opponentSide)
      MoveEffect.OCTOLOCK -> {
        if (defender.octolockedBy != null) return fail()
        if (!accurate()) return
        defender.octolockedBy = attacker
        defender.trappedTurns = 99
      }
      MoveEffect.TEATIME -> {
        val eaters = battle.actives().filter { !it.fainted && items.isBerry(items.of(it)) }
        if (eaters.isEmpty()) return fail()
        for (mon in eaters) eatBerry(mon, events)
      }
      MoveEffect.CORROSIVE_GAS -> {
        val victims = battle.actives().filter { it !== attacker && !it.fainted && it.heldItem != 0 && it.ability != Ability.STICKY_HOLD }
        if (victims.isEmpty()) return fail()
        for (mon in victims) {
          mon.heldItem = 0
          events += BattleEvent.ItemChanged(mon.entityId, 0)
        }
      }
      MoveEffect.PURIFY -> {
        if (!StatusCondition.hasAny(defender.status)) return fail()
        cureStatus(defender, events)
        healHp(attacker, (attacker.maxHp + 1) / 2, events)
      }
      MoveEffect.REVIVAL_BLESSING -> {
        val fallen = team(battle, attacker).firstOrNull { it.fainted && battle.positionOf(it) < 0 } ?: return fail()
        fallen.currentHp = (fallen.maxHp / 2).coerceAtLeast(1)
        events += BattleEvent.RecordHp(fallen.entityId, fallen.currentHp)
      }
      MoveEffect.DOODLE -> {
        val copied = defender.ability
        if (copied in UNTRACEABLE) return fail()
        if (!accurate()) return
        for (mon in listOf(attacker) + battle.alliesOf(attacker)) {
          mon.ability = copied
          events += BattleEvent.AbilityShown(mon.entityId, copied)
        }
      }
      MoveEffect.DRAGON_CHEER -> {
        val ally = battle.alliesOf(attacker).firstOrNull() ?: return fail()
        if (ally.critBoost > 0) return fail()
        ally.critBoost = if (ally.hasType(PokemonType.DRAGON)) 2 else 1
      }
      MoveEffect.TELEKINESIS -> {
        if (defender.telekinesisTurns > 0 || defender.grounded || items.of(defender) == de.fiereu.openmmo.items.generated.Items.IRON_BALL) return fail()
        if (!accurate()) return
        defender.telekinesisTurns = 3
      }
      MoveEffect.ELECTRIFY -> {
        if (defender.movedThisTurn) return fail()
        defender.electrified = true
      }
      MoveEffect.ION_DELUGE -> battle.field.ionDeluge = true
      MoveEffect.FAIRY_LOCK -> {
        if (battle.field.fairyLockTurns > 0) return fail()
        battle.field.fairyLockTurns = 2
      }
      MoveEffect.POWDER -> {
        if (!accurate()) return
        defender.powdered = true
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
            when (weather(battle)) {
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
        setWeather(battle, weather, events, attacker)
      }
      MoveEffect.RECYCLE -> {
        if (attacker.heldItem != 0 || attacker.consumedItem == 0) return fail()
        attacker.heldItem = attacker.consumedItem
        attacker.consumedItem = 0
        events += BattleEvent.ItemChanged(attacker.entityId, attacker.heldItem)
        events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_FOUND, listOf(attacker.heldItem))
      }
      MoveEffect.TRICK -> {
        if (!accurate()) return
        if (attacker.heldItem == 0 && defender.heldItem == 0) return fail()
        if (defender.ability == Ability.STICKY_HOLD && defender.heldItem != 0) return fail()
        val mine = attacker.heldItem
        attacker.heldItem = defender.heldItem
        defender.heldItem = mine
        events += BattleEvent.ItemChanged(attacker.entityId, attacker.heldItem)
        events += BattleEvent.ItemChanged(defender.entityId, defender.heldItem)
        events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_SWAPPED, listOf(attacker.heldItem, defender.heldItem))
      }
      MoveEffect.BESTOW -> {
        if (attacker.heldItem == 0 || defender.heldItem != 0) return fail()
        defender.heldItem = attacker.heldItem
        attacker.heldItem = 0
        events += BattleEvent.ItemChanged(attacker.entityId, 0)
        events += BattleEvent.ItemChanged(defender.entityId, defender.heldItem)
        events += BattleEvent.Line(defender.entityId, BattleLine.ITEM_RECEIVED, listOf(defender.heldItem))
      }
      MoveEffect.LEECH_SEED -> {
        if (defender.leechSeeded || defender.hasType(PokemonType.GRASS)) return fail()
        if (!accurate()) return
        seed(attacker, defender, events)
      }
      MoveEffect.LIGHT_SCREEN -> {
        val side = battle.sideOf(attacker)
        if (side.lightScreenTurns > 0) return fail()
        side.lightScreenTurns = items.screenTurns(attacker)
      }
      MoveEffect.REFLECT -> {
        val side = battle.sideOf(attacker)
        if (side.reflectTurns > 0) return fail()
        side.reflectTurns = items.screenTurns(attacker)
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
      MoveEffect.HAZE -> haze(attacker, defender, events)
      MoveEffect.FOCUS_ENERGY, MoveEffect.LASER_FOCUS -> {
        if (attacker.focusEnergy) return fail()
        attacker.focusEnergy = true
      }
      MoveEffect.CURSE -> {
        if (attacker.hasType(PokemonType.GHOST)) {
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
        events += BattleEvent.Line(attacker.entityId, BattleLine.ROOTED)
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
        events += BattleEvent.Line(defender.entityId, BattleLine.NO_ESCAPE)
      }
      MoveEffect.LOCK_ON -> attacker.lockedOn = true
      MoveEffect.FORESIGHT, MoveEffect.MIRACLE_EYE -> {
        if (defender.identified) return fail()
        defender.identified = true
        events += BattleEvent.Line(defender.entityId, BattleLine.IDENTIFIED, listOf(move.id))
      }
      MoveEffect.NIGHTMARE -> {
        if (!StatusCondition.isAsleep(defender.status) || defender.nightmare) return fail()
        defender.nightmare = true
      }
      MoveEffect.SLEEP_TALK -> {
        if (!StatusCondition.isAsleep(attacker.status)) return fail()
        val options =
            attacker.moves.filter {
              it.id.toInt() != 0 && it.id.toInt() != move.id && moves.get(it.id.toInt())?.hasFlag(MoveFlag.SLEEP_TALK_BANNED) != true
            }
        if (options.isEmpty()) return fail()
        val picked = moves.get(options[battle.rng.pick(options.size)].id.toInt()) ?: return fail()
        callMove(battle, action, picked, movesLast, events)
      }
      MoveEffect.METRONOME -> {
        var picked: MoveDef? = null
        repeat(20) {
          if (picked == null) {
            val candidate = moves.get(1 + battle.rng.pick(354))
            if (candidate != null && !candidate.hasFlag(MoveFlag.METRONOME_BANNED) && candidate.effect != MoveEffect.METRONOME) picked = candidate
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
    // The caller's slot and pp: slot 0 with 0 pp made the owner's client empty its first move.
    val caller = action.move
    events +=
        if (caller == null) BattleEvent.MoveUsed(action.attacker.entityId, move.id.toShort(), 0, 0)
        else BattleEvent.MoveUsed(action.attacker.entityId, move.id.toShort(), slotOf(action.attacker, caller).coerceAtLeast(0), ppOf(action.attacker, caller))
    if (move.power > 0 || isDamagingEffect(move)) attack(battle, action, move, events)
    else statusMove(battle, action, move, movesLast, events)
  }

  private fun statusFor(battle: BattleInstance, effect: MoveAdditionalEffect?): Int =
      when (effect) {
        MoveAdditionalEffect.SLEEP -> StatusCondition.asleep(2 + battle.rng.pick(3))
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
    // Misty Terrain keeps every status off grounded monsters; Electric Terrain keeps them awake.
    if (battle.field.terrain == Terrain.MISTY && isGrounded(battle, target)) return false
    if (battle.field.terrain == Terrain.ELECTRIC && StatusCondition.isAsleep(status) && isGrounded(battle, target)) return false
    if (Abilities.blocksStatus(target, status, weather(battle), fromMove = source !== target) &&
        !(source !== target && Abilities.ignoresTargetAbilities(source)))
        return false
    return when {
      status and StatusCondition.BURN != 0 -> !target.hasType(PokemonType.FIRE)
      status and StatusCondition.FREEZE != 0 -> !target.hasType(PokemonType.ICE)
      status and (StatusCondition.POISON or StatusCondition.TOXIC) != 0 ->
          !target.hasType(PokemonType.POISON) && !target.hasType(PokemonType.STEEL)
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
    checkStatusBerry(battle, target, events)
    // Synchronize hands poison, burn and paralysis back to whoever caused them.
    if (target.ability == Ability.SYNCHRONIZE && source !== target &&
        status and (StatusCondition.POISON or StatusCondition.TOXIC or StatusCondition.BURN or StatusCondition.PARALYSIS) != 0 &&
        canReceiveStatus(battle, target, source, status)) {
      events += BattleEvent.AbilityShown(target.entityId, Ability.SYNCHRONIZE)
      source.status = status
      if (status and StatusCondition.TOXIC != 0) source.toxicCounter = 0
      events += BattleEvent.StatusChanged(source.entityId, source.status)
    }
    return true
  }

  private fun confuse(battle: BattleInstance, source: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>) {
    if (target.fainted || target.confusionTurns > 0) return
    if (source !== target && battle.sideOf(target).safeguardTurns > 0) return
    if (battle.field.terrain == Terrain.MISTY && isGrounded(battle, target)) return
    if (Abilities.blocksConfusion(target) && !(source !== target && Abilities.ignoresTargetAbilities(source))) {
      events += BattleEvent.AbilityShown(target.entityId, target.ability)
      return
    }
    target.confusionTurns = 2 + battle.rng.pick(4)
    events += BattleEvent.Line(target.entityId, BattleLine.CONFUSION, listOf(0))
    checkConfusionBerry(battle, target, events)
  }

  private fun seed(source: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>) {
    if (target.leechSeeded || target.hasType(PokemonType.GRASS)) return
    target.leechSeeded = true
    events += BattleEvent.Line(target.entityId, BattleLine.SEEDED, listOf(0))
  }

  private fun cureStatus(mon: BattleMonState, events: MutableList<BattleEvent>) {
    if (!StatusCondition.hasAny(mon.status)) return
    mon.status = StatusCondition.NONE
    mon.toxicCounter = 0
    mon.nightmare = false
    events += BattleEvent.StatusChanged(mon.entityId, mon.status)
  }

  private fun setWeather(battle: BattleInstance, weather: Weather, events: MutableList<BattleEvent>, user: BattleMonState? = null) {
    if (battle.weather == weather) return
    battle.weather = weather
    battle.weatherTurns = user?.let { items.weatherTurns(it, weather) } ?: WEATHER_TURNS
    weatherForms(battle, events)
    events += BattleEvent.WeatherChanged(weather)
  }

  private fun healHp(mon: BattleMonState, amount: Int, events: MutableList<BattleEvent>) {
    if (mon.fainted || mon.healBlockTurns > 0) return
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
    var delta = effect.delta
    val byFoe = !effect.onSelf && action.attacker !== target
    if (byFoe && delta < 0 && !Abilities.ignoresTargetAbilities(action.attacker)) {
      if (Abilities.blocksStatDrop(target, effect.stat)) {
        events += BattleEvent.AbilityShown(target.entityId, target.ability)
        return false
      }
      if (target.ability == Ability.MIRROR_ARMOR) {
        events += BattleEvent.AbilityShown(target.entityId, target.ability)
        return ownStage(action.attacker, effect.stat, delta, events)
      }
    }
    if (target.ability == Ability.CONTRARY) delta = -delta
    if (target.ability == Ability.SIMPLE) delta *= 2
    val applied = target.changeStage(effect.stat, delta)
    if (byFoe && applied < 0) {
      if (target.ability == Ability.DEFIANT) ownStage(target, BattleStat.ATTACK, 2, events)
      if (target.ability == Ability.COMPETITIVE) ownStage(target, BattleStat.SP_ATTACK, 2, events)
    }
    // A White Herb undoes the drop on the spot.
    if (applied < 0 && items.of(target) == de.fiereu.openmmo.items.generated.Items.WHITE_HERB) {
      val id = target.heldItem
      consumeItem(target, events)
      for (stat in BattleStat.entries) if (target.stage(stat) < 0) target.changeStage(stat, -target.stage(stat))
      events += BattleEvent.Line(target.entityId, BattleLine.ITEM_RESTORED_STATUS, listOf(id))
    }
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

  /** The Crystal Onix raid's turn opening: on [CrystalOnixRaid.CRACK_TURN] the crystal breaks. */
  private fun raidTurnStart(battle: BattleInstance, raid: RaidBossState, events: MutableList<BattleEvent>) {
    raid.hurtThisTurn = false
    if (raid.cracked || battle.turn < CrystalOnixRaid.CRACK_TURN) return
    val boss = battle.opponent.firstOrNull { raid.isBoss(it) }?.takeIf { !it.fainted } ?: return
    raid.cracked = true
    raidPower(boss, CrystalOnixRaid.CRYSTAL_BREAK, CrystalOnixRaid.CRACK_LINE, events)
    ownStage(boss, BattleStat.DEFENSE, 2, events)
    ownStage(boss, BattleStat.SP_DEFENSE, 2, events)
    healHp(boss, boss.maxHp / 2, events)
    raidSandstorm(battle, events)
    boss.moves.clear()
    CrystalOnixRaid.CRACKED_MOVES.forEach { id ->
      boss.moves += PokemonMove(id.toShort(), (moves.get(id)?.pp ?: 0).toByte())
    }
    events += BattleEvent.MovesChanged(boss.entityId, boss.moves.map { it.id to it.pp })
  }

  /** The raid's end of turn, before weather: the mending, the rally, the shards and the cleanse. */
  private fun raidEndOfTurn(battle: BattleInstance, raid: RaidBossState, events: MutableList<BattleEvent>) {
    val boss = battle.opponent.firstOrNull { raid.isBoss(it) }?.takeIf { !it.fainted } ?: return
    if (!raid.cracked && raid.hurtThisTurn) {
      raidPower(boss, CrystalOnixRaid.LIVING_CRYSTAL, CrystalOnixRaid.MEND_LINE, events)
      healHp(boss, boss.maxHp / 2, events)
    }
    if (!raid.rallied && boss.currentHp * 4 < boss.maxHp) {
      raid.rallied = true
      raidPower(boss, CrystalOnixRaid.LAST_LIGHT, CrystalOnixRaid.RALLY_LINE, events)
      healHp(boss, boss.maxHp / 2, events)
      ownStage(boss, BattleStat.DEFENSE, 1, events)
      ownStage(boss, BattleStat.SP_DEFENSE, 1, events)
    }
    if (battle.turn % CrystalOnixRaid.SHARD_EVERY == 0) {
      raidPower(boss, CrystalOnixRaid.SHARD_STORM, CrystalOnixRaid.SHARDS_LINE, events)
      for (mon in battle.playerActives()) {
        if (mon.fainted) continue
        mon.currentHp = (mon.currentHp - (mon.maxHp / 3).coerceAtLeast(1)).coerceAtLeast(0)
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.HpChanged(mon.entityId, mon.currentHp)
        if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
      }
      raidSandstorm(battle, events)
    }
    if (battle.turn % CrystalOnixRaid.CLEANSE_EVERY == 0) {
      raidPower(boss, CrystalOnixRaid.CLEAR_LIGHT, CrystalOnixRaid.CLEANSE_LINE, events)
      for (mon in battle.playerActives()) {
        if (mon.fainted) continue
        for (stat in BattleStat.entries) if (mon.stage(stat) > 0) resetStage(mon, stat, events)
      }
      for (stat in BattleStat.entries) if (boss.stage(stat) < 0) resetStage(boss, stat, events)
      cureStatus(boss, events)
    }
  }

  /** Raid Sandstorm: raised, or already raging and wound back to its full length. */
  private fun raidSandstorm(battle: BattleInstance, events: MutableList<BattleEvent>) {
    if (battle.weather == Weather.SANDSTORM) battle.weatherTurns = WEATHER_TURNS
    else setWeather(battle, Weather.SANDSTORM, events)
  }

  /**
   * A raid power in its own event group on the (standing) boss, so the client always finds the
   * group's attacker: the power's ability banner, its line, then whatever heal or stage changes are
   * queued after it land on the boss's panel.
   */
  private fun raidPower(boss: BattleMonState, abilityId: Int, stringId: Int, events: MutableList<BattleEvent>) {
    events += BattleEvent.TurnEffect(boss.entityId)
    raidBanner(boss, abilityId, stringId, events)
  }

  /** The power's banner and line under the event group already open (a hit's, or the power's own). */
  private fun raidBanner(boss: BattleMonState, abilityId: Int, stringId: Int, events: MutableList<BattleEvent>) {
    events += BattleEvent.RaidAbilityShown(boss.entityId, abilityId)
    events += BattleEvent.ClientLine(boss.entityId, stringId, shape = 0)
  }

  /** Puts one stage back to 0, reported the way any stage change is. */
  private fun resetStage(mon: BattleMonState, stat: BattleStat, events: MutableList<BattleEvent>) {
    val applied = mon.changeStage(stat, -mon.stage(stat))
    val value = if (stat == BattleStat.ACCURACY || stat == BattleStat.EVASION) 0 else mon.effective(stat)
    events += BattleEvent.StageChanged(mon.entityId, stat, mon.stage(stat), value, applied, applied == 0)
  }

  private fun endOfTurn(battle: BattleInstance, events: MutableList<BattleEvent>) {
    battle.raid?.let { raidEndOfTurn(battle, it, events) }
    val order = battle.actives().filter { !it.fainted }.sortedByDescending { speedOf(battle, it) }

    // Weather: counts down, then hurts whoever it hurts.
    if (battle.weather != null) {
      battle.weatherTurns--
      if (battle.weatherTurns <= 0) {
        battle.weather = null
        weatherForms(battle, events)
        events += BattleEvent.WeatherChanged(null)
      } else if (weather(battle) != null) {
        for (mon in order) {
          if (mon.fainted) continue
          val hurt =
              when (battle.weather) {
                Weather.SANDSTORM ->
                    !mon.hasType(PokemonType.ROCK) && !mon.hasType(PokemonType.GROUND) &&
                        !mon.hasType(PokemonType.STEEL) && !Abilities.immuneToSandstorm(mon)
                Weather.HAIL -> !mon.hasType(PokemonType.ICE) && !Abilities.immuneToHail(mon)
                else -> false
              }
          if (hurt && !mon.semiInvulnerable && !Abilities.noIndirectDamage(mon)) {
            mon.currentHp = (mon.currentHp - (mon.maxHp / 16).coerceAtLeast(1)).coerceAtLeast(0)
            events += BattleEvent.TurnEffect(mon.entityId)
            events +=
                BattleEvent.Line(
                    mon.entityId,
                    BattleLine.WEATHER_DAMAGE,
                    listOf(battle.weather?.wireValue ?: 0, mon.currentHp))
            if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
          }
        }
      }
    }

    for (mon in order) {
      if (mon.fainted) continue
      val other = battle.opponentOf(mon)
      // Each line below carries the new hp, so the client moves the bar from the line itself.
      // Magic Guard takes none of this damage.
      fun hurt(amount: Int, line: BattleLine, prefix: List<Int> = emptyList(), suffix: List<Int> = emptyList()) {
        if (Abilities.noIndirectDamage(mon)) return
        mon.currentHp = (mon.currentHp - amount.coerceAtLeast(1)).coerceAtLeast(0)
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.Line(mon.entityId, line, prefix + mon.currentHp + suffix)
        if (mon.fainted) events += BattleEvent.Fainted(mon.entityId)
      }
      fun abilityHeal(amount: Int) {
        if (mon.currentHp >= mon.maxHp) return
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.AbilityShown(mon.entityId, mon.ability)
        healHp(mon, amount.coerceAtLeast(1), events)
      }
      fun abilityHurt(amount: Int) {
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.AbilityShown(mon.entityId, mon.ability)
        loseHp(mon, amount.coerceAtLeast(1), events)
      }
      val weather = weather(battle)
      when (mon.ability) {
        Ability.SPEED_BOOST -> { events += BattleEvent.TurnEffect(mon.entityId); events += BattleEvent.AbilityShown(mon.entityId, mon.ability); ownStage(mon, BattleStat.SPEED, 1, events) }
        Ability.SHED_SKIN -> if (StatusCondition.hasAny(mon.status) && battle.rng.accuracyRoll() <= 30) { events += BattleEvent.TurnEffect(mon.entityId); events += BattleEvent.AbilityShown(mon.entityId, mon.ability); cureStatus(mon, events) }
        Ability.HYDRATION -> if (StatusCondition.hasAny(mon.status) && weather == Weather.RAIN) { events += BattleEvent.TurnEffect(mon.entityId); events += BattleEvent.AbilityShown(mon.entityId, mon.ability); cureStatus(mon, events) }
        Ability.RAIN_DISH -> if (weather == Weather.RAIN) abilityHeal(mon.maxHp / 16)
        Ability.ICE_BODY -> if (weather == Weather.HAIL) abilityHeal(mon.maxHp / 16)
        Ability.DRY_SKIN -> if (weather == Weather.RAIN) abilityHeal(mon.maxHp / 8) else if (weather == Weather.SUN) abilityHurt(mon.maxHp / 8)
        Ability.SOLAR_POWER -> if (weather == Weather.SUN) abilityHurt(mon.maxHp / 8)
        Ability.MOODY -> {
          val stats = listOf(BattleStat.ATTACK, BattleStat.DEFENSE, BattleStat.SP_ATTACK, BattleStat.SP_DEFENSE, BattleStat.SPEED)
          val up = stats[battle.rng.pick(stats.size)]
          val down = stats.filter { it != up }[battle.rng.pick(stats.size - 1)]
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.AbilityShown(mon.entityId, mon.ability)
          ownStage(mon, up, 2, events)
          ownStage(mon, down, -1, events)
        }
        Ability.BAD_DREAMS -> if (StatusCondition.isAsleep(other.status) && !other.fainted && !Abilities.noIndirectDamage(other)) {
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.AbilityShown(mon.entityId, mon.ability)
          loseHp(other, (other.maxHp / 8).coerceAtLeast(1), events)
        }
        Ability.SLOW_START -> if (mon.slowStartTurns > 0) { mon.slowStartTurns--; if (mon.slowStartTurns == 0) { events += BattleEvent.TurnEffect(mon.entityId); events += BattleEvent.AbilityShown(mon.entityId, mon.ability) } }
        else -> Unit
      }
      if (mon.fainted) {
        if (!formChange(mon, "FORM_CHANGE_FAINT", null)) restoreForm(mon)
        continue
      }
      // Held items with an end-of-turn effect.
      val I = de.fiereu.openmmo.items.generated.Items
      when (items.of(mon)) {
        I.LEFTOVERS ->
            if (mon.currentHp < mon.maxHp) {
              mon.currentHp = (mon.currentHp + (mon.maxHp / 16).coerceAtLeast(1)).coerceAtMost(mon.maxHp)
              events += BattleEvent.TurnEffect(mon.entityId)
              events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL_SMALL, listOf(mon.currentHp, mon.heldItem))
            }
        I.BLACK_SLUDGE ->
            if (mon.hasType(PokemonType.POISON)) {
              if (mon.currentHp < mon.maxHp) {
                mon.currentHp = (mon.currentHp + (mon.maxHp / 16).coerceAtLeast(1)).coerceAtMost(mon.maxHp)
                events += BattleEvent.TurnEffect(mon.entityId)
                events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL_SMALL, listOf(mon.currentHp, mon.heldItem))
              }
            } else if (!Abilities.noIndirectDamage(mon)) {
              events += BattleEvent.TurnEffect(mon.entityId)
              events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HURT, listOf(0, mon.heldItem))
              loseHp(mon, (mon.maxHp / 8).coerceAtLeast(1), events)
            }
        I.STICKY_BARB ->
            if (!Abilities.noIndirectDamage(mon)) {
              events += BattleEvent.TurnEffect(mon.entityId)
              events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HURT, listOf(0, mon.heldItem))
              loseHp(mon, (mon.maxHp / 8).coerceAtLeast(1), events)
            }
        I.TOXIC_ORB ->
            if (canReceiveStatus(battle, mon, mon, StatusCondition.TOXIC)) {
              events += BattleEvent.TurnEffect(mon.entityId)
              inflictStatus(battle, mon, mon, StatusCondition.TOXIC, events)
            }
        I.FLAME_ORB ->
            if (canReceiveStatus(battle, mon, mon, StatusCondition.BURN)) {
              events += BattleEvent.TurnEffect(mon.entityId)
              inflictStatus(battle, mon, mon, StatusCondition.BURN, events)
            }
        else -> Unit
      }
      if (mon.ability == Ability.HARVEST && mon.heldItem == 0 && items.isBerry(items.get(mon.consumedItem)) &&
          (weather == Weather.SUN || battle.rng.coinFlip())) {
        mon.heldItem = mon.consumedItem
        mon.consumedItem = 0
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.AbilityShown(mon.entityId, Ability.HARVEST)
        events += BattleEvent.ItemChanged(mon.entityId, mon.heldItem)
        events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_FOUND, listOf(mon.heldItem))
      }
      if (mon.fainted) continue
      // Zen Mode, Schooling, Hunger Switch: the turn-end form checks.
      formChange(mon, "FORM_CHANGE_BATTLE_HP_PERCENT_TURN_END", events) {
        hpPercentMatches(mon, it) && (it.params.getOrNull(3)?.toIntOrNull()?.let { min -> mon.level >= min } ?: true)
      }
      formChange(mon, "FORM_CHANGE_BATTLE_TURN_END", events) { abilityMatches(mon, it.params.getOrNull(0)) }
      if (mon.ingrained && mon.currentHp < mon.maxHp) {
        mon.currentHp = (mon.currentHp + (mon.maxHp / 16).coerceAtLeast(1)).coerceAtMost(mon.maxHp)
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.Line(mon.entityId, BattleLine.ROOT_HEAL, listOf(mon.currentHp))
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
        hurt(drained, BattleLine.LEECH_SEED_DRAIN, suffix = listOf(0xFF, 0))
        healHp(other, drained, events)
      }
      if (mon.fainted) continue
      if (mon.ability == Ability.POISON_HEAL && StatusCondition.isPoisoned(mon.status)) {
        abilityHeal(mon.maxHp / 8)
      } else if (StatusCondition.isBadlyPoisoned(mon.status)) {
        mon.toxicCounter = (mon.toxicCounter + 1).coerceAtMost(15)
        hurt(mon.maxHp * mon.toxicCounter / 16, BattleLine.POISON_DAMAGE)
      } else if (StatusCondition.isPoisoned(mon.status)) {
        hurt(mon.maxHp / 8, BattleLine.POISON_DAMAGE)
      } else if (StatusCondition.isBurned(mon.status)) {
        hurt(mon.maxHp / 8, BattleLine.BURN_DAMAGE)
      }
      if (mon.fainted) continue
      if (mon.nightmare && StatusCondition.isAsleep(mon.status)) {
        hurt(mon.maxHp / 4, BattleLine.NIGHTMARE_DAMAGE)
      }
      if (mon.fainted) continue
      if (mon.cursed) hurt(mon.maxHp / 4, BattleLine.CURSE_DAMAGE, prefix = listOf(0))
      if (mon.fainted) continue
      if (mon.trappedTurns in 1..98) {
        mon.trappedTurns--
        if (mon.trappedTurns == 0) {
          events += BattleEvent.TurnEffect(mon.entityId)
          // f/N00(byte kind, short MOVE, short HP): the move comes first, the hp goes to the base
          // class. Sent the other way round, the client named the hp as a move ("hurt by Defense
          // Curl" at 111 hp) and set the hp to the move id - Bind left a Pokemon at 20 (2026-09-08).
          events += BattleEvent.Line(mon.entityId, BattleLine.TRAP, listOf(2, mon.trappingMoveId))
          mon.trappingMoveId = 0
        } else {
          hurt(mon.maxHp / (mon.trapDamageDivisor * 2), BattleLine.TRAP, prefix = listOf(1, mon.trappingMoveId))
        }
      }
      if (mon.fainted) continue
      if (mon.drowsyTurns > 0) {
        mon.drowsyTurns--
        if (mon.drowsyTurns == 0 && !StatusCondition.hasAny(mon.status)) {
          mon.status = StatusCondition.asleep(2 + battle.rng.pick(3))
          events += BattleEvent.TurnEffect(mon.entityId)
          events += BattleEvent.StatusChanged(mon.entityId, mon.status)
        }
      }
    }
    // Move-made timers, in speed order.
    for (mon in order) {
      if (mon.fainted) continue
      if (mon.aquaRing && mon.currentHp < mon.maxHp && mon.healBlockTurns == 0) {
        events += BattleEvent.TurnEffect(mon.entityId)
        healHp(mon, (mon.maxHp / 16).coerceAtLeast(1), events)
      }
      if (mon.tauntTurns > 0) mon.tauntTurns--
      if (mon.encoreTurns > 0 && --mon.encoreTurns == 0) mon.encoreMoveId = 0
      if (mon.disableTurns > 0 && --mon.disableTurns == 0) mon.disabledMoveId = 0
      if (mon.magnetRiseTurns > 0) mon.magnetRiseTurns--
      if (mon.healBlockTurns > 0) mon.healBlockTurns--
      if (mon.embargoTurns > 0) mon.embargoTurns--
      if (mon.telekinesisTurns > 0) mon.telekinesisTurns--
      // Octolock squeezes both defenses every turn while its holder stays in.
      mon.octolockedBy?.let { holder ->
        if (holder.fainted || battle.positionOf(holder) < 0) {
          mon.octolockedBy = null
        } else {
          events += BattleEvent.TurnEffect(mon.entityId)
          ownStage(mon, BattleStat.DEFENSE, -1, events)
          ownStage(mon, BattleStat.SP_DEFENSE, -1, events)
        }
      }
      if (battle.field.terrain == Terrain.GRASSY && isGrounded(battle, mon) && mon.currentHp < mon.maxHp && mon.healBlockTurns == 0) {
        events += BattleEvent.TurnEffect(mon.entityId)
        healHp(mon, (mon.maxHp / 16).coerceAtLeast(1), events)
      }
      if (mon.perishCount > 0 && --mon.perishCount == 0) {
        mon.currentHp = 0
        events += BattleEvent.TurnEffect(mon.entityId)
        events += BattleEvent.HpChanged(mon.entityId, 0)
        events += BattleEvent.Fainted(mon.entityId)
      }
    }
    delayedAttacks(battle, events)
    battle.playerSide.tick()
    battle.opponentSide.tick()
    battle.field.tick()
    for (mon in battle.party + battle.opponent) mon.inMagicRoom = battle.field.magicRoomTurns > 0
  }

  /** Future Sight and Doom Desire that come due strike whatever now stands on the aimed position. */
  private fun delayedAttacks(battle: BattleInstance, events: MutableList<BattleEvent>) {
    val due = mutableListOf<DelayedAttack>()
    for (pending in battle.delayedAttacks) if (--pending.turns <= 0) due += pending
    battle.delayedAttacks.removeAll(due)
    for (pending in due) {
      val target = battle.monAt(pending.side, pending.position)?.takeIf { !it.fainted } ?: continue
      events += BattleEvent.TurnEffect(target.entityId)
      attack(battle, TurnAction(pending.attacker, target, pending.move), pending.move.copy(effect = MoveEffect.HIT), events)
    }
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

  private companion object {
    /** Transform gives every copied move this much pp, or the move's maximum when that is lower. */
    const val TRANSFORM_PP = 5
    /** Charging moves that hide their user in the air (Fly, Bounce, Sky Drop), underground (Dig), underwater (Dive). */
    val HIDDEN_IN_AIR = setOf(19, 340, 507)
    const val HIDDEN_UNDERGROUND = 91
    const val HIDDEN_UNDERWATER = 291
    /** Switch-in kinds (r32645 q94): 0 recalls a monster still standing, 5 skips the recall. */
    const val RECALLED = 0
    const val DRAGGED_IN = 5
    // The terrain-boosted moves, told apart by id (they share EFFECT_TERRAIN_BOOST).
    const val EXPANDING_FORCE = 725
    const val RISING_VOLTAGE = 732
    const val MISTY_EXPLOSION = 730
    const val PSYBLADE = 827
    // Nature Power's picks.
    const val THUNDERBOLT = 85
    const val ENERGY_BALL = 412
    const val MOONBLAST = 585
    const val PSYCHIC_MOVE = 94
    const val HYDRO_PUMP = 56
    const val EARTHQUAKE_MOVE = 89
    const val TRI_ATTACK = 161
    /** Perish Song counts down from here at each turn's end and faints at 0 ("3 turns" after the use turn). */
    const val PERISH_COUNT = 4
    /** Future Sight lands at the end of the second turn after the one it was used in. */
    const val FUTURE_SIGHT_TURNS = 3
    /** Fly, Jump Kick, High Jump Kick, Splash, Bounce, Magnet Rise, Telekinesis and Sky Drop fail under Gravity. */
    val GRAVITY_BANNED = setOf(19, 26, 136, 150, 340, 393, 477, 507)
    /** Moves Encore cannot lock a monster into. */
    val ENCORE_FAILS = setOf(MoveEffect.ENCORE, MoveEffect.TRANSFORM, MoveEffect.MIMIC, MoveEffect.SKETCH, MoveEffect.MIRROR_MOVE)
    /** Moves that call another move, which Copycat will not copy. */
    val CALLS_OTHER_MOVES =
        setOf(MoveEffect.COPYCAT, MoveEffect.MIRROR_MOVE, MoveEffect.METRONOME, MoveEffect.SLEEP_TALK, MoveEffect.ASSIST, MoveEffect.ME_FIRST)
    /** Targets a move may aim at its own side with. */
    val OWN_SIDE_TARGETS = setOf(MoveTarget.USER, MoveTarget.ALLY, MoveTarget.USER_OR_ALLY, MoveTarget.USER_AND_ALLY, MoveTarget.USER_OR_SELECTED)
    val UNTRACEABLE =
        setOf(
            Ability.TRACE, Ability.MULTITYPE, Ability.ILLUSION, Ability.IMPOSTER, Ability.STANCE_CHANGE,
            Ability.ZEN_MODE, Ability.FLOWER_GIFT, Ability.FORECAST, Ability.SCHOOLING, Ability.DISGUISE,
            Ability.BATTLE_BOND, Ability.POWER_CONSTRUCT, Ability.RKS_SYSTEM, Ability.COMATOSE,
            Ability.SHIELDS_DOWN, Ability.RECEIVER, Ability.POWER_OF_ALCHEMY, Ability.ICE_FACE,
            Ability.GULP_MISSILE, Ability.HUNGER_SWITCH, Ability.ZERO_TO_HERO, Ability.COMMANDER)

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
