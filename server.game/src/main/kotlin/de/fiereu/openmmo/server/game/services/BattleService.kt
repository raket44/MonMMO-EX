package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.BattleAction
import de.fiereu.openmmo.common.enums.GameMode
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.items.generated.Items
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.MapLoadedAckPacket
import de.fiereu.openmmo.net.game.packets.SocialListEntryAddPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleActionSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleFormat
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventDetail
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnReplyPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.Gender
import de.fiereu.openmmo.server.game.battle.BattleInstance
import de.fiereu.openmmo.server.game.battle.BattleMonState
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.server.game.battle.BattleRegistry
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.battle.BattleRewards
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.BattleRules
import de.fiereu.openmmo.server.game.battle.ChosenAction
import de.fiereu.openmmo.server.game.battle.Gen1StatCalculator
import de.fiereu.openmmo.server.game.battle.MoveLearner
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.battle.TurnEngine
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.battle.acquiredMonsterDelta
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.world.interest.InterestManager
import de.fiereu.openmmo.trainer.TrainerDef
import de.fiereu.openmmo.trainer.TrainerRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The wild shiny odds, 1 in this many per wild monster (retail's base rate is 1 in 30000).
 * Override with -Dmonmmo.wildShinyRate=<n>; 0 turns wild shinies off.
 */
private val wildShinyDenominator: Int =
    System.getProperty("monmmo.wildShinyRate")?.toIntOrNull()?.coerceAtLeast(0) ?: 30_000

/**
 * A prompt waiting for its answer, kept after the battle ends. A trainer battle can raise one
 * monster past a level more than once, so these are held per monster rather than per player.
 */
private data class PendingMoveLearn(
    val charId: Long,
    val entityId: Long,
    val offered: List<Short>,
)

/**
 * Orchestrates battles: builds the battle state from the party and the opposing side, routes client
 * actions through the [TurnEngine], and persists the outcome. Packets go out through the
 * [BattlePacketEmitter] over the battle's interest key.
 */
@Singleton
class BattleService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val battles: BattleRegistry,
    private val engine: TurnEngine,
    private val wildMons: WildMonFactory,
    private val emitter: BattlePacketEmitter,
    private val rewards: BattleRewards,
    private val moveLearner: MoveLearner,
    private val interestManager: InterestManager,
    private val speciesRegistry: SpeciesRegistry,
    private val moveRegistry: MoveRegistry,
    private val dexProgress: DexProgressService,
    private val trainers: TrainerRegistry,
    private val items: ItemRegistry,
    private val classicMode: ClassicModeService,
    private val trainerSight: javax.inject.Provider<TrainerSightService>? = null,
    private val mapManager: de.fiereu.openmmo.maps.MapManager? = null,
) {

  private val pokeBallItemId: Short by lazy { items.idOf(Items.POKE_BALL).toShort() }

  private val pendingLearns = ConcurrentHashMap<Long, PendingMoveLearn>()

  private fun computeStats(
      charId: Long,
      species: de.fiereu.openmmo.pokemon.SpeciesDef,
      mon: de.fiereu.openmmo.common.Pokemon
  ) =
      when (classicMode.getMode(charId)) {
        GameMode.CLASSIC_RB,
        GameMode.CLASSIC_YELLOW -> Gen1StatCalculator.computeAll(species, mon, splitSpecial = false)
        GameMode.CLASSIC_GS,
        GameMode.CLASSIC_CRYSTAL -> Gen1StatCalculator.computeAll(species, mon, splitSpecial = true)
        else -> StatCalculator.computeAll(species, mon)
      }

  private fun computeWildStats(
      species: de.fiereu.openmmo.pokemon.SpeciesDef,
      mon: de.fiereu.openmmo.common.Pokemon
  ) = StatCalculator.computeAll(species, mon)

  fun onBattlePacket(event: PacketEvent<*>) {
    log.info { "Battle packet ${event.packet::class.simpleName} received: ${event.packet}" }
  }

  suspend fun onBattleAction(event: PacketEvent<BattleActionSelectPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val battle = battles.byChar(charId) ?: return
    if (battle.pendingResult != null) return
    val action = event.packet
    // The client packs a slot reference the way f/fd1 does (and BattleSwitchInPacket writes it):
    // POSITION in the high nibble, side in the low one. A doubles second position arrives as
    // 0x10; reading the low nibble collapsed both choices onto position 0 and the turn never
    // resolved. A move's extra byte is the chosen target packed the same way.
    val position = (action.slotRefPacked.toInt() ushr 4) and 0x0F
    log.info { "Battle action char=$charId position=$position: $action" }
    // A position whose monster fainted owes a replacement and may only switch.
    if (position in battle.forcedSwitchPositions) {
      if (action.action == BattleAction.SWITCH) forcedSwitch(battle, position, action.moveOrItemId.toInt())
      return
    }
    if (battle.forcedSwitchPositions.isNotEmpty() || position !in battle.awaitingPositions) return
    val chosen =
        when (action.action) {
          BattleAction.MOVE ->
              ChosenAction(
                  position,
                  ChosenAction.Kind.MOVE,
                  action.moveOrItemId,
                  targetSide = action.extraFlag.toInt() and 0x0F,
                  targetPosition = (action.extraFlag.toInt() ushr 4) and 0x0F)
          BattleAction.ITEM -> ChosenAction(position, ChosenAction.Kind.ITEM)
          BattleAction.SWITCH -> ChosenAction(position, ChosenAction.Kind.SWITCH, partyIndex = action.moveOrItemId.toInt())
          BattleAction.RUN -> ChosenAction(position, ChosenAction.Kind.RUN)
        }
    battle.pendingActions[position] = chosen
    battle.awaitingPositions -= position
    if (battle.awaitingPositions.isEmpty()) resolvePending(battle)
  }

  /**
   * Every prompted position has answered. Running and a ball throw settle the turn on their own,
   * as they do in singles; voluntary switches go first, then the moves of the whole field.
   */
  private suspend fun resolvePending(battle: BattleInstance) {
    val actions = battle.pendingActions.values.toList()
    battle.pendingActions.clear()
    if (actions.any { it.kind == ChosenAction.Kind.RUN }) {
      flee(battle)
      return
    }
    if (actions.any { it.kind == ChosenAction.Kind.ITEM }) {
      catchWild(battle)
      return
    }
    for (switch in actions.filter { it.kind == ChosenAction.Kind.SWITCH }) {
      val target = switch.partyIndex
      val mon = battle.party.getOrNull(target)
      if (mon == null || mon.fainted || target in battle.playerPositions) continue
      performSwitch(battle, switch.position, target)
    }
    val events = engine.resolveTurn(battle, actions.filter { it.kind == ChosenAction.Kind.MOVE })
    emitter.sendEvents(battle, events)
    afterTurn(battle)
  }

  /**
   * Opens the next turn: every standing position gets a prompt, except one locked into a two-turn
   * move or a recharge, which acts on its own the way the cartridges keep the player out of the
   * menu. With nothing to ask, the turn runs straight away.
   */
  private suspend fun prompt(battle: BattleInstance) {
    if (!openTurn(battle)) resolvePending(battle)
  }

  /** Fills the locked actions and prompts the rest; false when nothing is left to ask. */
  private fun openTurn(battle: BattleInstance): Boolean {
    battle.pendingActions.clear()
    battle.awaitingPositions.clear()
    for ((position, slot) in battle.playerPositions.withIndex()) {
      if (slot < 0) continue
      val mon = battle.party[slot]
      if (mon.fainted) continue
      val locked =
          when {
            mon.chargingMoveId != 0 -> mon.chargingMoveId
            mon.mustRecharge -> mon.moves.firstOrNull { it.id.toInt() != 0 }?.id?.toInt() ?: 0
            else -> 0
          }
      if (locked != 0) battle.pendingActions[position] = ChosenAction(position, ChosenAction.Kind.MOVE, locked.toShort(), 1, position)
      else battle.awaitingPositions += position
    }
    if (battle.awaitingPositions.isEmpty()) return false
    emitter.sendPrompt(battle, battle.awaitingPositions)
    return true
  }

  /** Applies the moveset the player picked after a level up. */
  fun onMoveLearnReply(event: PacketEvent<MoveLearnReplyPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val reply = event.packet
    val pending = pendingLearns[reply.entityId] ?: return
    if (pending.charId != charId || reply.moveId !in pending.offered) return
    // One answer per offered move; the entry lives until every prompt is answered.
    val remaining = pending.offered - reply.moveId
    if (remaining.isEmpty()) pendingLearns.remove(reply.entityId)
    else pendingLearns[reply.entityId] = pending.copy(offered = remaining)
    if (reply.slot < 0) {
      log.info { "char=$charId declined move ${reply.moveId} for ${reply.entityId}" }
      return
    }
    val stored =
        characterStore.getCharacter(charId)?.pokemon?.firstOrNull { it.id == reply.entityId }
            ?: return
    val moves = stored.moves.toMutableList()
    if (!moveLearner.replace(moves, reply.slot.toInt(), reply.moveId)) {
      log.warn {
        "char=$charId picked an invalid slot ${reply.slot} for move ${reply.moveId} on ${reply.entityId}"
      }
      return
    }
    if (moves == stored.moves) return
    characterStore.updatePokemon(charId, stored.copy(moves = moves))
    characterStore.flushCharacterAsync(charId)
    // A battle still running holds its own copy, and the next reward writes that copy back over
    // the store. Move the live one across so the pick survives the rest of the battle.
    battles
        .byChar(charId)
        ?.party
        ?.firstOrNull { it.entityId == reply.entityId }
        ?.let { live ->
          live.moves.clear()
          live.moves.addAll(moves.map { PokemonMove(it.id, it.pp) })
          live.source = live.source.copy(moves = moves)
        }
    event.session.send(emitter.moveSlotsDelta(reply.entityId, moves.map { it.id to it.pp }, 0))
  }

  /** Throws a ball at the monster. False when the character is not in a battle. */
  suspend fun catchActiveWild(charId: Long): Boolean {
    val battle = battles.byChar(charId) ?: return false
    catchWild(battle)
    return true
  }

  /** Ends a running battle when the player disconnects, keeping the last hp and pp state. */
  fun onDisconnect(session: SessionContext) {
    val charId = session.attributes[PLAYER_STATE]?.characterId ?: return
    pendingLearns.values.removeIf { it.charId == charId }
    val battle = battles.byChar(charId) ?: return
    persistParty(battle)
    finishBattle(battle, BattleResult.DISCONNECTED)
  }

  /** Resumes scripts after returning to the overworld; the finished result for whiteouts. */
  fun onClientReady(event: PacketEvent<MapLoadedAckPacket>): BattleResult? {
    if (event.packet.data.isNotEmpty()) return null
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return null
    val battle = battles.byChar(charId) ?: return null
    val result = battle.pendingResult ?: return null
    finishBattle(battle, result)
    afterWildBattle(event.session, battle, result)
    // A scripted loss without whiteout (early rival) is the script's to handle.
    return if (result == BattleResult.DEFEAT && !battle.whiteoutOnDefeat) null else result
  }

  /** True while the character has a battle running, so callers can skip starting another. */
  fun inBattle(charId: Long): Boolean = battles.byChar(charId) != null

  fun startWildBattle(session: SessionContext, dexId: Int, level: Int) {
    createWildBattle(session, dexId, level, catchable = true, escapable = true)
  }

  /** Runs a story battle and waits for its scene. */
  suspend fun startScriptedBattle(
      session: SessionContext,
      dexId: Int,
      level: Int,
      moveIds: List<Int> = emptyList(),
  ): BattleResult {
    val battle =
        createWildBattle(
            session,
            dexId,
            level,
            catchable = false,
            escapable = false,
            moveIds = moveIds,
        ) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /** Runs a battle against the decomp trainer with this id and waits for its scene. */
  suspend fun startTrainerBattle(
      session: SessionContext,
      region: Region,
      trainerId: Int,
  ): BattleResult {
    val trainer = trainers.get(region, trainerId)
    if (trainer == null) {
      log.warn { "No $region trainer with id $trainerId" }
      return BattleResult.FAILED
    }
    return startTrainerBattle(session, trainer)
  }

  /**
   * Runs a battle against a trainer's whole team and waits for its scene. [whiteoutOnDefeat]
   * false is the early rival: losing just hands the outcome back to the script.
   */
  suspend fun startTrainerBattle(
      session: SessionContext,
      trainer: TrainerDef,
      defeatTextId: Int? = null,
      whiteoutOnDefeat: Boolean = true,
      partner: TrainerDef? = null,
      partnerDefeatTextId: Int? = null,
  ): BattleResult {
    // A double sighting: the partner's team lines up behind the first trainer's and the two
    // field one monster each, the way Emerald and FireRed run a simultaneous spot.
    val opponents = (trainer.party + (partner?.party ?: emptyList())).map { OpponentSpec(it.dexId, it.level, it.moveIds, it.iv) }
    val battle =
        createBattle(
            session,
            opponents,
            catchable = false,
            escapable = false,
            trainer = trainer,
            defeatTextId = defeatTextId,
            whiteoutOnDefeat = whiteoutOnDefeat,
            partner = partner,
            partnerDefeatTextId = partnerDefeatTextId,
        ) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /** A horde: several wild monsters on the opposing field at once (Sweet Scent). */
  fun startHordeBattle(session: SessionContext, specs: List<OpponentSpec>): BattleInstance? =
      createBattle(session, specs, catchable = true, escapable = true)

  /** A scripted wild battle (setwildbattle/dowildbattle: legendaries, Snorlax) awaited by the script. */
  suspend fun startScriptedWildBattle(session: SessionContext, dexId: Int, level: Int): BattleResult {
    val battle = createWildBattle(session, dexId, level, catchable = true, escapable = true) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /** Resolves the original pret TRAINER_* constant inside one region's generated trainer table. */
  fun resolveTrainer(region: Region, constant: String): TrainerDef? = trainers.get(region, constant)

  fun resolveTrainer(region: Region, id: Int): TrainerDef? = trainers.get(region, id)

  /** Empty [moveIds] keeps the level up moveset, a null [iv] rolls one like a wild encounter. */
  data class OpponentSpec(
      val dexId: Int,
      val level: Int,
      val moveIds: List<Int>,
      val iv: Int? = null,
  )

  private fun createWildBattle(
      session: SessionContext,
      dexId: Int,
      level: Int,
      catchable: Boolean,
      escapable: Boolean,
      moveIds: List<Int> = emptyList(),
  ): BattleInstance? =
      createBattle(session, listOf(OpponentSpec(dexId, level, moveIds)), catchable, escapable)

  private fun createBattle(
      session: SessionContext,
      opponents: List<OpponentSpec>,
      catchable: Boolean,
      escapable: Boolean,
      trainer: TrainerDef? = null,
      defeatTextId: Int? = null,
      whiteoutOnDefeat: Boolean = true,
      partner: TrainerDef? = null,
      partnerDefeatTextId: Int? = null,
  ): BattleInstance? {
    val charId = session.attributes[PLAYER_STATE]?.characterId ?: return null
    if (battles.byChar(charId) != null) {
      session.send(notice("You are already in a battle."))
      return null
    }
    val stored = characterStore.getCharacter(charId) ?: return null
    if (stored.pokemon.isEmpty()) {
      session.send(notice("You need a monster in your party to battle."))
      return null
    }
    val party = mutableListOf<BattleMonState>()
    for ((index, mon) in stored.pokemon.withIndex()) {
      val def = speciesRegistry.get(mon.dexId)
      if (def == null) {
        session.send(notice("Your party has a species the battle data does not cover yet."))
        return null
      }
      if (!def.abilityMechanicsSupported) {
        // Refusing here made every encounter silently vanish the moment one imported species
        // joined the party. An ability without mechanics simply never triggers, which is a far
        // smaller lie than a world with no battles.
        log.info { "Battle proceeds with inert ability on ${def.name} (char=$charId)" }
      }
      party += BattleMonState(mon.id, def, index, mon, computeStats(charId, def, mon))
    }
    if (party.all { it.fainted }) {
      session.send(notice("All of your monsters have fainted."))
      return null
    }
    val rng = BattleRng()
    val enemies = mutableListOf<BattleMonState>()
    // Only the wild roll for shiny; a trainer's monsters never do.
    val shinyDenominator = if (trainer == null) wildShinyDenominator else 0
    for (spec in opponents) {
      var rolled = wildMons.create(spec.dexId, spec.level, rng, shinyDenominator)
      if (rolled == null) {
        session.send(notice("Unknown species ${spec.dexId}."))
        return null
      }
      if (rolled.isShiny) log.info { "Shiny wild ${spec.dexId} L${spec.level} rolled for char=$charId (1 in $shinyDenominator)" }
      if (spec.moveIds.isNotEmpty()) {
        rolled =
            rolled.copy(
                moves =
                    spec.moveIds.take(4).map { id ->
                      PokemonMove(id.toShort(), (moveRegistry.get(id)?.pp ?: 0).toByte())
                    } + List((4 - spec.moveIds.size).coerceAtLeast(0)) { PokemonMove(0, 0) })
      }
      val def = speciesRegistry.get(spec.dexId)!!
      if (!def.abilityMechanicsSupported) {
        // Refusing here made every encounter silently vanish the moment one imported species
        // joined the party. An ability without mechanics simply never triggers, which is a far
        // smaller lie than a world with no battles.
        log.info { "Battle proceeds with inert ability on ${def.name} (char=$charId)" }
      }
      // A trainer's monsters are built to a fixed difficulty, so they must not keep the rolled
      // IVs. Max hp moves with them, and the monster comes out full.
      if (spec.iv != null) {
        val ivs =
            IVs().apply {
              hp = spec.iv
              atk = spec.iv
              this.def = spec.iv
              spAtk = spec.iv
              spDef = spec.iv
              spd = spec.iv
            }
        val fixed = rolled.copy(iVs = ivs)
        rolled = fixed.copy(hp = computeWildStats(def, fixed).hp.toShort())
      }
      val enemy = BattleMonState(rolled.id, def, null, rolled, computeWildStats(def, rolled))
      log.info {
        "Wild ${def.name} seed=${rolled.seed} slots=${def.ability1}/${def.ability2} -> ${enemy.ability} (char=$charId)"
      }
      enemies += enemy
    }
    log.info {
      "Starting battle for char=$charId (${stored.info.name}) against " +
          enemies.joinToString { "${it.species.name} level ${it.level}" }
    }
    // A trainer flagged for doubles fields two when the player can match it; several wild monsters
    // are a horde (up to five cells, the rest empty).
    val alive = party.indices.filter { !party[it].fainted }
    val format =
        when {
          (trainer?.doubleBattle == true || partner != null) && alive.size >= 2 && enemies.size >= 2 -> BattleFormat.DOUBLES
          trainer == null && enemies.size >= 2 -> BattleFormat.HORDE
          else -> BattleFormat.SINGLES
        }
    val battle =
        battles.create(
            charId,
            session,
            party,
            enemies,
            rng,
            BattleRules(catchable, escapable, trainer, defeatTextId, whiteoutOnDefeat, session.attributes[PLAYER_STATE]?.regionId ?: 0, partner, partnerDefeatTextId),
            format)
    for (position in 0 until format.playerSlots) battle.playerPositions[position] = alive.getOrElse(position) { -1 }
    for (position in 0 until format.opponentSlots) battle.opponentPositions[position] = if (position < enemies.size) position else -1
    battle.seenActive.clear()
    battle.seenActive.addAll(battle.playerPositions.filter { it >= 0 })
    battle.opponentSeen.clear()
    battle.opponentSeen.addAll(battle.opponentPositions.filter { it >= 0 })
    for (mon in battle.actives()) engine.prepareIllusion(battle, mon)
    interestManager.join(session, battle.key)
    emitter.sendStart(battle, stored.info.name)
    openTurn(battle)
    // Every lead's switch-in ability fires as the battle opens, the faster ones first.
    val opening = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    for (mon in battle.actives().sortedByDescending { it.effective(de.fiereu.openmmo.server.game.battle.BattleStat.SPEED) }) {
      engine.switchIn(battle, mon, opening)
    }
    emitter.sendEvents(battle, opening)
    // Facing a species is seeing it: the Pokedex tiers update the moment the battle opens.
    dexProgress.markSeen(session, charId, enemies.map { clientSpeciesId(it.source.dexId) })
    return battle
  }

  private suspend fun afterTurn(battle: BattleInstance) {
    // Every opponent that fell this turn pays out once, before the field is tidied.
    for (foe in battle.opponentActives()) {
      if (foe.fainted && battle.rewardedFaints.add(foe.entityId)) awardXp(battle, foe)
    }
    when {
      battle.opponent.all { it.fainted } -> endVictory(battle)
      battle.party.all { it.fainted } -> endDefeat(battle)
      else -> {
        // A trainer refills an emptied position from the bench; a wild horde just thins out.
        for ((position, index) in battle.opponentPositions.withIndex()) {
          if (index < 0 || !battle.opponent[index].fainted) continue
          if (battle.trainer != null) sendOutNextOpponent(battle, position) else battle.opponentPositions[position] = -1
        }
        // Fainted positions owe a replacement while the bench has one; the switch screen opens
        // for each instead of the action prompt, and the replacements arrive as SWITCH actions.
        val owed = battle.playerPositions.indices.filter { battle.playerPositions[it] >= 0 && battle.party[battle.playerPositions[it]].fainted }
        val benched = battle.party.indices.count { it !in battle.playerPositions && !battle.party[it].fainted }
        battle.forcedSwitchPositions.clear()
        for ((i, position) in owed.withIndex()) {
          if (i < benched) battle.forcedSwitchPositions += position else battle.playerPositions[position] = -1
        }
        if (battle.forcedSwitchPositions.isNotEmpty()) {
          for (position in battle.forcedSwitchPositions) emitter.sendSwitchPrompt(battle, position)
        } else {
          battle.turn += 1
          prompt(battle)
        }
      }
    }
  }

  /** The replacement for a fainted position. Replacing does not spend a turn. */
  private suspend fun forcedSwitch(battle: BattleInstance, position: Int, target: Int) {
    val mon = battle.party.getOrNull(target)
    if (mon == null || mon.fainted || target in battle.playerPositions) {
      // Reopen the switch screen on an invalid choice.
      emitter.sendSwitchPrompt(battle, position)
      return
    }
    // A forced switch confirms the choice before the switch-in. The captures pair the confirm with
    // a full block for a new mon and with a return block for a mon that was already active.
    emitter.sendSwitchConfirm(battle, position)
    performSwitch(battle, position, target)
    battle.forcedSwitchPositions -= position
    if (battle.forcedSwitchPositions.isEmpty()) {
      battle.turn += 1
      prompt(battle)
    }
  }

  /** A benched opponent takes over [position]; an empty bench leaves the cell empty. */
  private fun sendOutNextOpponent(battle: BattleInstance, position: Int) {
    val next = battle.opponent.indices.firstOrNull { !battle.opponent[it].fainted && it !in battle.opponentPositions }
    val oldIndex = battle.opponentPositions[position]
    engine.switchOut(battle, battle.opponent[oldIndex])
    battle.opponent[oldIndex].resetVolatile()
    if (next == null) {
      battle.opponentPositions[position] = -1
      return
    }
    val fullBlock = next !in battle.opponentSeen
    battle.opponentPositions[position] = next
    battle.opponentSeen.add(next)
    engine.prepareIllusion(battle, battle.opponent[next])
    log.info { "Opponent sends out slot $next on position $position for char=${battle.charId}" }
    emitter.sendOpponentSwitchIn(battle, position, oldIndex, fullBlock)
    val entering = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    engine.switchIn(battle, battle.opponent[next], entering)
    emitter.sendEvents(battle, entering)
  }

  private fun performSwitch(battle: BattleInstance, position: Int, target: Int) {
    val oldSlot = battle.playerPositions[position]
    val fullBlock = target !in battle.seenActive
    // Stages, confusion, Leech Seed and the rest stay on the field, not on the monster.
    val outgoing = battle.party[oldSlot]
    if (!outgoing.fainted) {
      if (outgoing.ability == de.fiereu.openmmo.common.enums.Ability.NATURAL_CURE) outgoing.status = 0
      if (outgoing.ability == de.fiereu.openmmo.common.enums.Ability.REGENERATOR)
          outgoing.currentHp = (outgoing.currentHp + outgoing.maxHp / 3).coerceAtMost(outgoing.maxHp)
    }
    engine.switchOut(battle, outgoing)
    outgoing.resetVolatile()
    battle.playerPositions[position] = target
    battle.seenActive.add(target)
    engine.prepareIllusion(battle, battle.party[target])
    log.info { "Switch char=${battle.charId} position $position slot $oldSlot -> $target (fullBlock=$fullBlock)" }
    emitter.sendSwitchIn(battle, position, oldSlot, fullBlock)
    val entering = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    engine.switchIn(battle, battle.party[target], entering)
    emitter.sendEvents(battle, entering)
  }

  private suspend fun flee(battle: BattleInstance) {
    if (!battle.escapable) {
      emitter.sendNotice(battle, "You can't run from this battle.")
      prompt(battle)
      return
    }
    log.info {
      "Flee attempt char=${battle.charId}: ${battle.activeMon().species.name}/${battle.activeMon().ability} vs " +
          "${battle.opponentMon().species.name}/${battle.opponentMon().ability} -> canFlee=${engine.canFlee(battle)}"
    }
    if (!engine.canFlee(battle)) {
      // Shadow Tag, Arena Trap or Magnet Pull on the wild side: the turn is lost, the wild attacks.
      val blocker = battle.opponentMon()
      val events = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
      events += de.fiereu.openmmo.server.game.battle.BattleEvent.TurnEffect(blocker.entityId)
      events += de.fiereu.openmmo.server.game.battle.BattleEvent.AbilityShown(blocker.entityId, blocker.ability)
      events += de.fiereu.openmmo.server.game.battle.BattleEvent.Line(
          battle.activeMon().entityId, de.fiereu.openmmo.net.game.packets.battle.BattleLine.NO_ESCAPE)
      emitter.sendEvents(battle, events)
      emitter.sendEvents(battle, engine.resolveSwitchTurn(battle))
      afterTurn(battle)
      return
    }
    emitter.sendFled(battle)
    persistParty(battle)
    battle.pendingResult = BattleResult.FLED
  }

  private suspend fun catchWild(battle: BattleInstance) {
    if (!battle.catchable) {
      emitter.sendNotice(battle, "You can't catch this monster.")
      prompt(battle)
      return
    }
    // A ball only flies at a lone monster: a horde has to be thinned to one first.
    if (battle.opponentActives().count { !it.fainted } > 1) {
      emitter.sendNotice(battle, "You can't throw a ball while more than one wild monster is out.")
      prompt(battle)
      return
    }
    val stored = characterStore.getCharacter(battle.charId) ?: return
    // A seventh party member corrupts the character - the client refuses the whole character
    // list. With a full party the catch goes to PC storage instead, like the real games.
    val partyFull = stored.pokemon.size >= de.fiereu.openmmo.common.MAX_PARTY_SIZE
    val container = if (partyFull) PokemonContainer.PC else PokemonContainer.PARTY
    val nextSlot =
        if (partyFull) ((stored.pcStorage.maxOfOrNull { it.containerSlot } ?: -1) + 1).toShort()
        else ((stored.pokemon.maxOfOrNull { it.containerSlot } ?: -1) + 1).toShort()
    val caught =
        battle
            .opponentMon()
            .source
            .copy(
                ownerId = battle.charId,
                container = container,
                containerSlot = nextSlot,
                ot = stored.info.name,
                hp = battle.opponentMon().currentHp.toShort(),
                moves = battle.opponentMon().moves.map { PokemonMove(it.id, it.pp) },
                caughtAt = LocalDateTime.now(),
            )
    log.info { "Caught wild ${battle.opponentMon().species.name} for char=${battle.charId}" }
    // The caught monster is sent as a full 148-byte record on opcode 0x14 before the ball-throw
    // event, so the client can resolve the monster when the throw lands.
    battle.session.send(SocialListEntryAddPacket(caught))
    battle.session.send(acquiredMonsterDelta(caught, battle.opponentMon().species))
    // "Player threw a Poke Ball" event.
    battle.session.send(
        BattleListEventPacket(
            kind = 0,
            value = pokeBallItemId,
            subKind = 4,
            detail = BattleListEventDetail(listType = 1, value = 1),
        ),
    )
    if (!characterStore.addPokemon(battle.charId, caught)) {
      log.error { "Could not persist the monster char=${battle.charId} just caught" }
    }
    // Owning is derived from holdings, so a catch just needs the tiers pushed again.
    dexProgress.refresh(battle.session, battle.charId)
    endBattle(battle, BattleResult.CAUGHT)
  }

  private suspend fun endVictory(battle: BattleInstance) {
    evolveEligible(battle)
    pickup(battle)
    var prize = battle.trainer?.let { rewards.trainerPrize(it, battle.trainer.party.lastOrNull()?.level ?: battle.opponent.last().level) } ?: 0
    // The second trainer of a double sighting pays too, off its own last monster.
    prize += battle.partner?.let { rewards.trainerPrize(it, it.party.lastOrNull()?.level ?: 1) } ?: 0
    // An Amulet Coin anywhere in the party doubles the prize money.
    if (prize > 0 && battle.party.any { items.get(it.heldItem) == de.fiereu.openmmo.items.generated.Items.AMULET_COIN })
        prize *= 2
    val paid = prize > 0 && characterStore.addMoney(battle.charId, prize)
    if (prize > 0 && !paid) {
      log.error { "Could not pay char=${battle.charId} the $prize prize" }
    }
    endBattle(battle, BattleResult.VICTORY, battle.rewardedWinners, if (paid) prize else 0)
  }

  /**
   * Pays the active monster for knocking [defeated] out. A trainer's team is paid for one at a
   * time, as each faints, which is when the captures show the delta going out.
   */
  private fun awardXp(battle: BattleInstance, defeated: BattleMonState) {
    // Everyone standing on the player's side took part; a field with nobody standing (a
    // mutual knockout) still pays the lead.
    val winners = battle.playerActives().filter { !it.fainted }.ifEmpty { listOf(battle.activeMon()) }
    for (winner in winners) awardXpTo(battle, winner, defeated)
  }

  private fun awardXpTo(battle: BattleInstance, winner: BattleMonState, defeated: BattleMonState) {
    battle.rewardedWinners += winner.entityId
    val reward = rewards.apply(winner, defeated.species, defeated.level, battle.trainer != null)
    log.info {
      "char=${battle.charId} won: +${reward.xpGained} xp, level ${winner.level} -> ${reward.newLevel}"
    }
    winner.currentHp = reward.newCurrentHp
    if (reward.leveled) winner.leveledThisBattle = true
    val outcome =
        moveLearner.learn(winner.moves, winner.source.dexId, winner.level, reward.newLevel)
    emitter.sendVictoryDelta(battle, winner.entityId, reward)
    // The client prints "{mon} learned {move}!" itself for a move that took a free slot, and
    // opens its forget dialog for a slot of -1; one packet per move either way.
    for (move in outcome.learned) {
      val slot = winner.moves.indexOfFirst { it.id.toInt() == move.moveId }
      battle.session.send(
          MoveLearnPromptPacket(winner.entityId, slot.toByte(), move.moveId.toShort()))
    }
    if (outcome.offered.isNotEmpty()) {
      val offered = outcome.offered.map { it.moveId.toShort() }
      pendingLearns[winner.entityId] = PendingMoveLearn(battle.charId, winner.entityId, offered)
      for (moveId in offered) {
        battle.session.send(MoveLearnPromptPacket(winner.entityId, MoveLearnPromptPacket.ASK, moveId))
      }
    }
    // Happiness grows with every earned victory (operator-directed): the cartridge bands - a
    // less-happy monster warms up faster - doubled by a held Soothe Bell, capped at 255.
    val friendshipGain =
        (if (winner.source.friendship < 100) 5 else if (winner.source.friendship < 200) 4 else 3)
            .let { if (winner.source.heldItem in SOOTHE_BELLS) it * 2 else it }
    val grown =
        winner.source.copy(
            friendship = minOf(255, winner.source.friendship + friendshipGain),
            level = reward.newLevel.toByte(),
            xp = reward.newXp,
            hp = reward.newCurrentHp.toShort(),
            eVs = reward.newEvs,
            moves = winner.moves.map { PokemonMove(it.id, it.pp) },
        )
    winner.source = grown
    winner.stats = reward.newStats
    characterStore.updatePokemon(battle.charId, grown)
  }

  private fun endDefeat(battle: BattleInstance) {
    endBattle(battle, BattleResult.DEFEAT)
  }

  // Known issue: the caught monster does not show up in the party until the client reopens it.
  private fun endBattle(
      battle: BattleInstance,
      result: BattleResult,
      skip: Set<Long> = emptySet(),
      prizeMoney: Int = 0,
  ) {
    emitter.sendEvents(battle, engine.endBattle(battle))
    persistParty(battle, skip)
    val party = characterStore.getCharacter(battle.charId)?.pokemon ?: emptyList()
    emitter.sendBattleEnd(battle, party, prizeMoney)
    battle.pendingResult = result
  }

  /**
   * Evolution is offered when a victorious battle ends, the way the cartridges stage it: only a
   * monster that gained a level in this battle is checked (a level-20 spawn does not evolve off a
   * win that left it at 20 - it evolves on reaching 21, as the operator expects). An Everstone holds
   * a monster back; trade and location methods never match until those systems exist.
   *
   * Nothing evolves here: each eligible monster gets the s2c 0x18 prompt, which the client queues
   * behind this battle's remaining events and plays as its own evolution cinematic once the fight
   * is over. The species changes when the client confirms (c2s 0x0B), in [EvolutionService].
   */
  private fun evolveEligible(battle: BattleInstance) {
    val playerState = battle.session.attributes[PLAYER_STATE] ?: return
    for (state in battle.party) {
      if (!state.leveledThisBattle) continue
      if (state.source.id in playerState.pendingEvolutions) continue
      val mon = state.source
      if (mon.heldItem in BreedingService.EVERSTONES) continue
      val wire = clientSpeciesId(mon.dexId)
      val def = speciesRegistry.get(mon.dexId) ?: continue
      val female = Gender.of(def.genderRatio, mon.seed) == Gender.FEMALE
      val target =
          EvolutionTable.levelEvolution(
              wire,
              EvolutionTable.LevelContext(
                  level = mon.level.toInt(),
                  attack = state.stats.atk,
                  defense = state.stats.def,
                  seed = mon.seed.toLong() and 0xFFFFFFFFL,
                  female = female,
                  heldItem = mon.heldItem,
                  friendship = mon.friendship,
                  daytime = java.time.LocalTime.now().hour in 6..17,
              ),
          ) ?: continue
      val evolvedDef = speciesRegistry.get(target) ?: continue
      promptEvolution(battle.session, playerState, mon, target)
      log.info {
        "char=${battle.charId} ${def.name} (wire $wire) offered evolution into ${evolvedDef.name} (wire $target)"
      }
    }
  }

  /**
   * Pickup after a won wild battle: every party monster with the ability and empty hands has a
   * one-in-ten chance to come out of the fight holding something from the level-banded table.
   */
  private fun pickup(battle: BattleInstance) {
    if (battle.trainer != null) return
    val rng = battle.rng
    for (state in battle.party) {
      if (state.fainted || state.ability != de.fiereu.openmmo.common.enums.Ability.PICKUP || state.heldItem != 0) continue
      if (rng.pick(10) != 0) continue
      val band = ((state.level - 1) / 10).coerceIn(0, 9)
      val roll = rng.pick(100)
      // 30/10/10/10/10/10/10/4/4/1/1 over a window that slides one slot per ten levels.
      val slot =
          when {
            roll < 30 -> 0
            roll < 40 -> 1
            roll < 50 -> 2
            roll < 60 -> 3
            roll < 70 -> 4
            roll < 80 -> 5
            roll < 90 -> 6
            roll < 94 -> 7
            roll < 98 -> 8
            roll < 99 -> 9
            else -> 10
          }
      val item = PICKUP_TABLE.getOrNull(band + slot) ?: continue
      val itemId = items.idsOf(item).firstOrNull() ?: continue
      state.heldItem = itemId
      emitter.sendNotice(battle, "${state.species.name} picked up one ${item.name}!")
      log.info { "char=${battle.charId} Pickup: ${state.species.name} found ${item.name}" }
    }
  }

  /** Write the battle's live hp and pp back into the party and flush the character. */
  private fun persistParty(battle: BattleInstance, skip: Set<Long> = emptySet()) {
    for (state in battle.party) {
      if (state.entityId in skip) continue
      // Toxic poison leaves the battle as ordinary poison, as the cartridges do.
      val status =
          if (state.status and de.fiereu.openmmo.common.StatusCondition.TOXIC != 0)
              (state.status and de.fiereu.openmmo.common.StatusCondition.TOXIC.inv()) or
                  de.fiereu.openmmo.common.StatusCondition.POISON
          else state.status
      val updated =
          state.source.copy(
              hp = state.currentHp.toShort(),
              status = if (state.currentHp <= 0) 0 else status,
              heldItem = state.heldItem,
              moves = state.moves.map { PokemonMove(it.id, it.pp) },
          )
      characterStore.updatePokemon(battle.charId, updated)
    }
    characterStore.flushCharacterAsync(battle.charId)
  }

  /** Soothe Bell in both held-item catalogs - doubles happiness gains while held. */
  private val SOOTHE_BELLS = setOf(5218, 6218)

  /** The Pickup ladder, cheapest first; a monster's level band picks the window into it. */
  private val PICKUP_TABLE =
      de.fiereu.openmmo.items.generated.Items.let { I ->
        listOf(
            I.POTION, I.ANTIDOTE, I.SUPER_POTION, I.GREAT_BALL, I.REPEL, I.ESCAPE_ROPE, I.FULL_HEAL,
            I.HYPER_POTION, I.ULTRA_BALL, I.RARE_CANDY, I.SUN_STONE, I.MOON_STONE, I.HEART_SCALE,
            I.FULL_RESTORE, I.MAX_REVIVE, I.PP_UP, I.MAX_ELIXIR, I.NUGGET, I.KING_S_ROCK, I.ETHER, I.WHITE_HERB,
            I.ELIXIR, I.LEFTOVERS)
      }

  /**
   * Back in the overworld after a wild battle: the encounter freeze lifts, and a trainer whose
   * line of sight covers the tile catches the player now - the cartridge re-checks sight after
   * every battle, so an encounter never lets anyone slip past a trainer.
   */
  private fun afterWildBattle(session: SessionContext, battle: BattleInstance, result: BattleResult) {
    if (battle.trainer != null) return
    val state = session.attributes[PLAYER_STATE] ?: return
    if (state.encounterHold) {
      state.encounterHold = false
      if (!state.scriptRunning) session.send(de.fiereu.openmmo.net.game.packets.DialogStatePacket(active = false))
    }
    if (result != BattleResult.VICTORY && result != BattleResult.FLED && result != BattleResult.CAUGHT) return
    if (state.scriptRunning) return
    val sight = trainerSight?.get() ?: return
    val map = mapManager?.getMap(state.regionId, state.bankId, state.mapId)
    val spotted =
        if (map != null) sight.onStep(session, state, map, state.x.toInt(), state.y.toInt())
        else sight.onNdsStep(session, state, state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt())
    if (spotted) log.info { "Trainer sight re-check after the wild battle caught char=${battle.charId}" }
  }

  private fun finishBattle(battle: BattleInstance, result: BattleResult) {
    interestManager.leave(battle.session, battle.key)
    battles.remove(battle.charId)
    battle.completion.complete(result)
  }
}
