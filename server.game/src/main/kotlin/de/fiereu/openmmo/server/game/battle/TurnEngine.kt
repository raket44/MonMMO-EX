package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.Ability
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
        "DAMAGE_CATEGORY_PHYSICAL" -> move.power > 0 && MoveCategory.isPhysical(move.type)
        "DAMAGE_CATEGORY_SPECIAL" -> move.power > 0 && !MoveCategory.isPhysical(move.type)
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
    for (mon in listOf(battle.activeMon(), battle.opponentMon())) {
      formChange(mon, "FORM_CHANGE_BATTLE_WEATHER", events) {
        weatherMatches(weather, it.params.getOrNull(0)) && abilityMatches(mon, it.params.getOrNull(1))
      }
    }
  }

  /** A monster leaving the field: its switch-out form, else the one it came in with. Silent. */
  fun switchOut(battle: BattleInstance, mon: BattleMonState) {
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
      if (mon.species.id == mon.originalSpecies.id) continue
      restoreForm(mon)
      events += BattleEvent.SpeciesShown(mon.entityId, mon.wireSpeciesId().toInt())
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
      battle.opponentOf(mon).ability != Ability.UNNERVE

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
      events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL, listOf(mon.currentHp, itemId))
      return
    }
    val quarterHeal = items.quarterHpHeal(item, mon.maxHp)
    if (quarterHeal > 0 && pinch) {
      consumeItem(mon, events)
      mon.currentHp = (mon.currentHp + ripen(mon, quarterHeal)).coerceAtMost(mon.maxHp)
      events += BattleEvent.Line(mon.entityId, BattleLine.ITEM_HEAL, listOf(mon.currentHp, itemId))
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

  private fun order(battle: BattleInstance, a: TurnAction, b: TurnAction): List<TurnAction> {
    fun priority(action: TurnAction): Int {
      val move = action.move ?: return 0
      return move.priority + Abilities.priorityBonus(action.attacker, move)
    }
    // Within a priority bracket Quick Claw (20%) and a ready Custap Berry go first, Lagging Tail
    // and Iron Ball last.
    fun bracket(action: TurnAction): Int {
      val mon = action.attacker
      val item = items.of(mon)
      val first =
          mon.custapReady ||
              (item == de.fiereu.openmmo.items.generated.Items.QUICK_CLAW && battle.rng.accuracyRoll() <= 20)
      if (first) mon.movedFirstByItem = mon.heldItem.takeIf { item == de.fiereu.openmmo.items.generated.Items.QUICK_CLAW } ?: mon.consumedItem
      val last = item == de.fiereu.openmmo.items.generated.Items.LAGGING_TAIL || item == de.fiereu.openmmo.items.generated.Items.IRON_BALL
      return priority(action) * 10 + (if (first) 1 else 0) - (if (last) 1 else 0)
    }
    val pa = bracket(a)
    val pb = bracket(b)
    if (pa != pb) return if (pa > pb) listOf(a, b) else listOf(b, a)
    val sa = speedOf(battle, a.attacker)
    val sb = speedOf(battle, b.attacker)
    if (sa != sb) return if (sa > sb) listOf(a, b) else listOf(b, a)
    return if (battle.rng.coinFlip()) listOf(a, b) else listOf(b, a)
  }

  private fun speedOf(battle: BattleInstance, mon: BattleMonState): Int {
    var speed = mon.effective(BattleStat.SPEED) * Abilities.speedMultiplierPercent(mon, weather(battle)) / 100
    speed = speed * items.speedPercent(mon) / 100
    if (StatusCondition.isParalyzed(mon.status) && Abilities.paralysisSlows(mon)) speed /= 4
    return speed
  }

  /** The weather as it acts on the field: Air Lock and Cloud Nine switch its effects off. */
  private fun weather(battle: BattleInstance): Weather? =
      if (Abilities.weatherNegated(battle.activeMon(), battle.opponentMon())) null else battle.weather

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
      Ability.INTIMIDATE -> {
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
    val blocker = battle.opponentMon()
    if (runner.ability == Ability.RUN_AWAY || blocker.fainted) return true
    return when (blocker.ability) {
      Ability.SHADOW_TAG -> runner.ability == Ability.SHADOW_TAG
      Ability.ARENA_TRAP -> runner.species.hasType(PokemonType.FLYING) || runner.ability == Ability.LEVITATE
      Ability.MAGNET_PULL -> !runner.species.hasType(PokemonType.STEEL)
      else -> true
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
    if (move == null) {
      events += BattleEvent.MoveFailed(attacker.entityId, 0)
      return
    }
    if (!canMove(battle, attacker, move, events)) return

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
    if (!continuing) spendPp(attacker, action.defender, move, events)
    else events += BattleEvent.MoveUsed(attacker.entityId, move.id.toShort(), slotOf(attacker, move), ppOf(attacker, move))
    if (items.isChoice(items.of(attacker)) && attacker.choiceLockedMove == 0) attacker.choiceLockedMove = move.id

    // Two-turn moves: the first use charges (or hides), the second one strikes. A Power Herb
    // skips the charge.
    val powerHerb = isTwoTurn(battle, move) && !continuing && items.of(attacker) == de.fiereu.openmmo.items.generated.Items.POWER_HERB
    if (powerHerb) consumeItem(attacker, events)
    if (isTwoTurn(battle, move) && !continuing && !powerHerb) {
      attacker.chargingMoveId = move.id
      attacker.semiInvulnerable = move.effect == MoveEffect.SEMI_INVULNERABLE
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
        events += BattleEvent.Line(mon.entityId, BattleLine.SLEEP, listOf(0))
        events += BattleEvent.CantMove(mon.entityId, CantMoveReason.ASLEEP)
        return false
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
    return true
  }

  private fun isTwoTurn(battle: BattleInstance, move: MoveDef): Boolean =
      when (move.effect) {
        MoveEffect.SEMI_INVULNERABLE,
        MoveEffect.TWO_TURNS_ATTACK,
        MoveEffect.SKULL_BASH,
        MoveEffect.RAZOR_WIND,
        MoveEffect.SKY_ATTACK -> true
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

    val explodes = move.hasFlag(MoveFlag.EXPLOSION) || move.effect == MoveEffect.EXPLOSION
    if (explodes && (attacker.ability == Ability.DAMP || defender.ability == Ability.DAMP)) {
      events += BattleEvent.AbilityShown(if (attacker.ability == Ability.DAMP) attacker.entityId else defender.entityId, Ability.DAMP)
      events += BattleEvent.MoveFailed(attacker.entityId, moveId)
      return
    }
    val effectiveType = moveType(battle, attacker, move)
    val effectiveness = effectivenessAgainst(effectiveType, defender, attacker)
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
      var endured = false
      var sturdy = false
      var sashKind = 0
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
        val survive = move.effect == MoveEffect.FALSE_SWIPE || defender.enduring || sturdy || sashKind != 0
        endured = survive && defender.enduring
        damage = if (survive) (defender.currentHp - 1).coerceAtLeast(0) else defender.currentHp
      }
      val hpBefore = defender.currentHp
      defender.currentHp -= damage
      defender.lastDamageTaken = damage
      defender.lastDamagePhysical = MoveCategory.isPhysical(effectiveType)
      totalDamage += damage
      lastCrit = result.crit
      struck++
      events += BattleEvent.DamageDealt(defender.entityId, defender.currentHp, result.crit, effectiveness)
      if (endured) events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(0))
      if (sturdy) {
        events += BattleEvent.AbilityShown(defender.entityId, Ability.STURDY)
        events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(2))
      }
      if (sashKind != 0) events += BattleEvent.Line(defender.entityId, BattleLine.ENDURED, listOf(sashKind))
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
    if (defender.fainted) koReactions(attacker, events)

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
    val recoils = !Abilities.noRecoil(attacker)
    when (move.effect) {
      MoveEffect.RECOIL, MoveEffect.DOUBLE_EDGE ->
          if (recoils) loseHp(attacker, (totalDamage * (move.argumentValue ?: 25) / 100).coerceAtLeast(1), events)
      MoveEffect.MAX_HP_50_RECOIL -> if (recoils) loseHp(attacker, (attacker.maxHp / 2).coerceAtLeast(1), events)
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
    val physical = MoveCategory.isPhysical(type)
    fun shown(mon: BattleMonState) = events.add(BattleEvent.AbilityShown(mon.entityId, mon.ability))
    fun roll(chance: Int) = battle.rng.accuracyRoll() <= chance
    // The defender's abilities, felt by the attacker.
    if (contact && !attacker.fainted) {
      when (defender.ability) {
        Ability.STATIC -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.PARALYSIS)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.PARALYSIS, events) }
        Ability.POISON_POINT -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.POISON)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.POISON, events) }
        Ability.FLAME_BODY -> if (roll(30) && canReceiveStatus(battle, defender, attacker, StatusCondition.BURN)) { shown(defender); inflictStatus(battle, defender, attacker, StatusCondition.BURN, events) }
        Ability.EFFECT_SPORE ->
            if (roll(30)) {
              val status = listOf(StatusCondition.POISON, StatusCondition.PARALYSIS, StatusCondition.asleep(2 + battle.rng.pick(4)))[battle.rng.pick(3)]
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
          if (MoveCategory.isPhysical(type) && attacker.ability != Ability.MAGIC_GUARD && canEatBerry(battle, defender)) {
            val id = defender.heldItem
            consumeItem(defender, events)
            events += BattleEvent.Line(attacker.entityId, BattleLine.ITEM_HURT, listOf(1, id))
            loseHp(attacker, ripen(defender, attacker.maxHp / 8).coerceAtLeast(1), events)
          }
      I.ROWAP_BERRY ->
          if (!MoveCategory.isPhysical(type) && attacker.ability != Ability.MAGIC_GUARD && canEatBerry(battle, defender)) {
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
            events += BattleEvent.Line(defender.entityId, BattleLine.ITEM_HEAL, listOf(defender.currentHp, id))
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

  private fun moveType(battle: BattleInstance, attacker: BattleMonState, move: MoveDef): PokemonType {
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
          else -> move.type
        }
    if (base == PokemonType.NORMAL) Abilities.retypedNormal(attacker)?.let { return it }
    return base
  }

  private fun effectivenessAgainst(type: PokemonType, defender: BattleMonState, attacker: BattleMonState? = null): Int {
    val species = defender.species
    var eff = typeChart.effectiveness(type, species.type1, species.type2)
    // Foresight (or Scrappy) lets Normal and Fighting hit a Ghost.
    val seesGhosts = defender.identified || (attacker != null && Abilities.hitsGhosts(attacker))
    if (eff == 0 && seesGhosts && species.hasType(PokemonType.GHOST) &&
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
          MoveEffect.WEATHER_BALL -> if (weather(battle) != null) 100 else 50
          MoveEffect.SOLAR_BEAM ->
              if (weather(battle) != null && weather(battle) != Weather.SUN) move.power / 2 else move.power
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
      movesLast: Boolean = false,
  ): HitResult {
    val attacker = action.attacker
    val defender = action.defender
    val physical = MoveCategory.isPhysical(type)
    val critStage =
        (if (attacker.focusEnergy) 2 else 0) +
            (if (move.effect == MoveEffect.HIGH_CRITICAL) 1 else 0) +
            move.criticalHitStage +
            Abilities.critStages(attacker) +
            items.critStages(attacker)
    val critBlocked = Abilities.noCrits(defender) && !Abilities.ignoresTargetAbilities(attacker)
    val crit =
        !critBlocked &&
            (move.hasFlag(MoveFlag.ALWAYS_CRIT) ||
                (attacker.ability == Ability.MERCILESS && StatusCondition.isPoisoned(defender.status)) ||
                battle.rng.critRoll(CRIT_DENOMINATORS[critStage.coerceIn(0, CRIT_DENOMINATORS.lastIndex)]))
    var dmg =
        baseDamage(
            attacker, defender, power, physical, crit, battle.rng,
            explosion = move.hasFlag(MoveFlag.EXPLOSION) || move.effect == MoveEffect.EXPLOSION,
            defenseStatPercent = Abilities.defenseStatPercent(defender, attacker, physical))
    if (physical && StatusCondition.isBurned(attacker.status) && attacker.ability != Ability.GUTS) dmg /= 2
    val side = battle.sideOf(defender)
    if (!crit && ((physical && side.reflectTurns > 0) || (!physical && side.lightScreenTurns > 0))) dmg /= 2
    val weather = weather(battle)
    when (weather) {
      Weather.RAIN -> if (type == PokemonType.WATER) dmg = dmg * 3 / 2 else if (type == PokemonType.FIRE) dmg /= 2
      Weather.SUN -> if (type == PokemonType.FIRE) dmg = dmg * 3 / 2 else if (type == PokemonType.WATER) dmg /= 2
      else -> Unit
    }
    if (crit) dmg = if (attacker.ability == Ability.SNIPER) dmg * 3 else dmg * 2
    val stab = attacker.species.hasType(type)
    if (stab) dmg = if (attacker.ability == Ability.ADAPTABILITY) dmg * 2 else dmg * 3 / 2
    dmg = dmg * effectiveness / TypeChart.NEUTRAL
    dmg = dmg * Abilities.offensePercent(attacker, move, type, power, physical, weather, effectiveness, movesLast, defender) / 100
    dmg = dmg * Abilities.defensePercent(defender, attacker, move, type, physical, effectiveness) / 100
    dmg = dmg * items.offensePercent(attacker, move, type, physical, effectiveness) / 100
    dmg =
        dmg *
            items.defensePercent(
                defender,
                physical,
                de.fiereu.openmmo.server.game.services.EvolutionTable.canEvolve(defender.wireSpeciesId().toInt())) / 100
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
      defenseStatPercent: Int = 100,
  ): Int {
    val atkStat = if (physical) BattleStat.ATTACK else BattleStat.SP_ATTACK
    val defStat = if (physical) BattleStat.DEFENSE else BattleStat.SP_DEFENSE
    // A crit ignores the attacker's negative stages and the defender's positive stages; Unaware
    // ignores the other side's stages altogether.
    val atk =
        if ((crit && attacker.stage(atkStat) < 0) || defender.ability == Ability.UNAWARE) attacker.unstaged(atkStat)
        else attacker.effective(atkStat)
    var def =
        if ((crit && defender.stage(defStat) > 0) || attacker.ability == Ability.UNAWARE) defender.unstaged(defStat)
        else defender.effective(defStat)
    def = def * defenseStatPercent / 100
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
    if (attacker.ability == Ability.NO_GUARD || defender.ability == Ability.NO_GUARD) return true
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
    val weather = weather(battle)
    if (move.effect == MoveEffect.THUNDER) {
      when (weather) {
        Weather.RAIN -> return true
        Weather.SUN -> accuracy = 50
        else -> Unit
      }
    }
    if (accuracy == 0) return true
    // Keen Eye and Mind's Eye look past evasion boosts; Unaware on either side drops the other's stages.
    val evasionStage =
        if (attacker.ability == Ability.KEEN_EYE || attacker.ability == Ability.MINDS_EYE || attacker.ability == Ability.UNAWARE)
            defender.stage(BattleStat.EVASION).coerceAtMost(0)
        else defender.stage(BattleStat.EVASION)
    val accuracyStage = if (defender.ability == Ability.UNAWARE) 0 else attacker.stage(BattleStat.ACCURACY)
    val stage = accuracyStage - evasionStage
    var threshold = StatStages.scaleAccuracy(accuracy, stage)
    threshold = threshold * Abilities.accuracyPercent(attacker, move, MoveCategory.isPhysical(move.type)) / 100
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
        if (defender.leechSeeded || defender.species.hasType(PokemonType.GRASS)) return fail()
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
    if (Abilities.blocksStatus(target, status, weather(battle), fromMove = source !== target) &&
        !(source !== target && Abilities.ignoresTargetAbilities(source)))
        return false
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
    if (Abilities.blocksConfusion(target) && !(source !== target && Abilities.ignoresTargetAbilities(source))) {
      events += BattleEvent.AbilityShown(target.entityId, target.ability)
      return
    }
    target.confusionTurns = 2 + battle.rng.pick(4)
    events += BattleEvent.Line(target.entityId, BattleLine.CONFUSION, listOf(0))
    checkConfusionBerry(battle, target, events)
  }

  private fun seed(source: BattleMonState, target: BattleMonState, events: MutableList<BattleEvent>) {
    if (target.leechSeeded || target.species.hasType(PokemonType.GRASS)) return
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

  private fun endOfTurn(battle: BattleInstance, events: MutableList<BattleEvent>) {
    val player = battle.activeMon()
    val enemy = battle.opponentMon()
    val order = if (speedOf(battle, player) >= speedOf(battle, enemy)) listOf(player, enemy) else listOf(enemy, player)

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
                    !mon.species.hasType(PokemonType.ROCK) && !mon.species.hasType(PokemonType.GROUND) &&
                        !mon.species.hasType(PokemonType.STEEL) && !Abilities.immuneToSandstorm(mon)
                Weather.HAIL -> !mon.species.hasType(PokemonType.ICE) && !Abilities.immuneToHail(mon)
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
            if (mon.species.hasType(PokemonType.POISON)) {
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
          events += BattleEvent.Line(mon.entityId, BattleLine.TRAP, listOf(2, mon.currentHp, mon.trappingMoveId))
          mon.trappingMoveId = 0
        } else {
          hurt(mon.maxHp / (mon.trapDamageDivisor * 2), BattleLine.TRAP, prefix = listOf(1), suffix = listOf(mon.trappingMoveId))
        }
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
