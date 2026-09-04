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
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventDetail
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnReplyPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.BattleInstance
import de.fiereu.openmmo.server.game.battle.BattleMonState
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.server.game.battle.BattleRegistry
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.battle.BattleRewards
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.BattleRules
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
    log.info { "Battle action char=$charId: $action" }
    // While the active mon is fainted the player owes a replacement and may only switch.
    if (battle.activeMon().fainted && action.action != BattleAction.SWITCH) return
    when (action.action) {
      BattleAction.MOVE -> resolveTurn(battle, action.moveOrItemId)
      BattleAction.ITEM -> catchWild(battle)
      BattleAction.SWITCH -> switchMon(battle, action.moveOrItemId)
      BattleAction.RUN -> flee(battle)
    }
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
  ): BattleResult {
    val battle =
        createBattle(
            session,
            trainer.party.map { OpponentSpec(it.dexId, it.level, it.moveIds, it.iv) },
            catchable = false,
            escapable = false,
            trainer = trainer,
            defeatTextId = defeatTextId,
            whiteoutOnDefeat = whiteoutOnDefeat,
        ) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /** A scripted wild battle (setwildbattle/dowildbattle: legendaries, Snorlax) awaited by the script. */
  suspend fun startScriptedWildBattle(session: SessionContext, dexId: Int, level: Int): BattleResult {
    val battle = createWildBattle(session, dexId, level, catchable = true, escapable = true) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /** Resolves the original pret TRAINER_* constant inside one region's generated trainer table. */
  fun resolveTrainer(region: Region, constant: String): TrainerDef? = trainers.get(region, constant)

  fun resolveTrainer(region: Region, id: Int): TrainerDef? = trainers.get(region, id)

  /** Empty [moveIds] keeps the level up moveset, a null [iv] rolls one like a wild encounter. */
  private data class OpponentSpec(
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
    for (spec in opponents) {
      var rolled = wildMons.create(spec.dexId, spec.level, rng)
      if (rolled == null) {
        session.send(notice("Unknown species ${spec.dexId}."))
        return null
      }
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
    val battle =
        battles.create(
            charId,
            session,
            party,
            enemies,
            rng,
            BattleRules(catchable, escapable, trainer, defeatTextId))
    val firstAlive = party.indexOfFirst { !it.fainted }
    battle.activeSlot = firstAlive
    battle.seenActive.clear()
    battle.seenActive.add(firstAlive)
    engine.prepareIllusion(battle, battle.activeMon())
    engine.prepareIllusion(battle, battle.opponentMon())
    interestManager.join(session, battle.key)
    emitter.sendStart(battle, stored.info.name)
    // Both leads' switch-in abilities fire as the battle opens, the faster one first.
    val opening = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    val leads = listOf(battle.activeMon(), battle.opponentMon())
    for (mon in leads.sortedByDescending { it.effective(de.fiereu.openmmo.server.game.battle.BattleStat.SPEED) }) {
      engine.switchIn(battle, mon, opening)
    }
    emitter.sendEvents(battle, opening)
    // Facing a species is seeing it: the Pokedex tiers update the moment the battle opens.
    dexProgress.markSeen(session, charId, enemies.map { clientSpeciesId(it.source.dexId) })
    return battle
  }

  private suspend fun resolveTurn(battle: BattleInstance, moveId: Short) {
    val events = engine.resolveTurn(battle, moveId)
    emitter.sendEvents(battle, events)
    afterTurn(battle)
  }

  private suspend fun afterTurn(battle: BattleInstance) {
    when {
      battle.opponent.all { it.fainted } -> endVictory(battle)
      battle.party.all { it.fainted } -> endDefeat(battle)
      else -> {
        if (battle.opponentMon().fainted) {
          awardXp(battle, battle.opponentMon())
          sendOutNextOpponent(battle)
        }
        // The active mon fainted with a live backup. Open the switch screen instead of the action
        // prompt. The replacement arrives as a normal SWITCH action.
        if (battle.activeMon().fainted) {
          emitter.sendSwitchPrompt(battle)
        } else {
          battle.turn += 1
          // A two-turn move or a recharge turn owns the next action: no prompt, the turn runs
          // straight on with the locked move, the way the cartridges keep the player out of the menu.
          val active = battle.activeMon()
          val locked =
              when {
                active.chargingMoveId != 0 -> active.chargingMoveId
                active.mustRecharge -> active.moves.firstOrNull { it.id.toInt() != 0 }?.id?.toInt() ?: 0
                else -> 0
              }
          if (locked != 0) resolveTurn(battle, locked.toShort()) else emitter.sendPrompt(battle)
        }
      }
    }
  }

  private suspend fun switchMon(battle: BattleInstance, partyIndex: Short) {
    val target = partyIndex.toInt()
    val mon = battle.party.getOrNull(target)
    val forced = battle.activeMon().fainted
    if (mon == null || mon.fainted || target == battle.activeSlot) {
      // Reopen the switch screen on an invalid forced choice, otherwise re-prompt for an action.
      if (forced) {
        emitter.sendSwitchPrompt(battle)
      } else {
        battle.turn += 1
        emitter.sendPrompt(battle)
      }
      return
    }
    // A forced switch confirms the choice before the switch-in. The captures pair the confirm with
    // a full block for a new mon and with a return block for a mon that was already active.
    if (forced) emitter.sendSwitchConfirm(battle)
    performSwitch(battle, target)
    if (forced) {
      // Replacing a fainted mon does not spend a turn, the new mon acts next.
      battle.turn += 1
      emitter.sendPrompt(battle)
    } else {
      // A voluntary switch spends the turn, so the wild attacks the incoming mon.
      emitter.sendEvents(battle, engine.resolveSwitchTurn(battle))
      afterTurn(battle)
    }
  }

  private fun sendOutNextOpponent(battle: BattleInstance) {
    val next = battle.opponent.indexOfFirst { !it.fainted }
    if (next < 0) return
    val fullBlock = next !in battle.opponentSeen
    val oldSlot = battle.opponentSlot
    engine.switchOut(battle, battle.opponent[battle.opponentSlot])
    battle.opponent[battle.opponentSlot].resetVolatile()
    battle.opponentSlot = next
    battle.opponentSeen.add(next)
    engine.prepareIllusion(battle, battle.opponentMon())
    log.info { "Opponent sends out slot $next for char=${battle.charId}" }
    emitter.sendOpponentSwitchIn(battle, oldSlot, fullBlock)
    val entering = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    engine.switchIn(battle, battle.opponentMon(), entering)
    emitter.sendEvents(battle, entering)
  }

  private fun performSwitch(battle: BattleInstance, target: Int) {
    val oldSlot = battle.activeSlot
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
    battle.activeSlot = target
    battle.seenActive.add(target)
    engine.prepareIllusion(battle, battle.activeMon())
    log.info { "Switch char=${battle.charId} slot $oldSlot -> $target (fullBlock=$fullBlock)" }
    emitter.sendSwitchIn(battle, oldSlot, fullBlock)
    val entering = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    engine.switchIn(battle, battle.activeMon(), entering)
    emitter.sendEvents(battle, entering)
  }

  private suspend fun flee(battle: BattleInstance) {
    if (!battle.escapable) {
      emitter.sendNotice(battle, "You can't run from this battle.")
      emitter.sendPrompt(battle)
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
      emitter.sendPrompt(battle)
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
    awardXp(battle, battle.opponentMon())
    evolveEligible(battle)
    pickup(battle)
    var prize = battle.trainer?.let { rewards.trainerPrize(it, battle.opponent.last().level) } ?: 0
    // An Amulet Coin anywhere in the party doubles the prize money.
    if (prize > 0 && battle.party.any { items.get(it.heldItem) == de.fiereu.openmmo.items.generated.Items.AMULET_COIN })
        prize *= 2
    val paid = prize > 0 && characterStore.addMoney(battle.charId, prize)
    if (prize > 0 && !paid) {
      log.error { "Could not pay char=${battle.charId} the $prize prize" }
    }
    endBattle(battle, BattleResult.VICTORY, battle.activeMon().entityId, if (paid) prize else 0)
  }

  /**
   * Pays the active monster for knocking [defeated] out. A trainer's team is paid for one at a
   * time, as each faints, which is when the captures show the delta going out.
   */
  private fun awardXp(battle: BattleInstance, defeated: BattleMonState) {
    val winner = battle.activeMon()
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
      skip: Long? = null,
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
      val female =
          when (def.genderRatio) {
            0 -> false
            254,
            255 -> def.genderRatio == 254
            else -> (mon.seed and 0xFF) < def.genderRatio
          }
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
  private fun persistParty(battle: BattleInstance, skip: Long? = null) {
    for (state in battle.party) {
      if (state.entityId == skip) continue
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

  private fun finishBattle(battle: BattleInstance, result: BattleResult) {
    interestManager.leave(battle.session, battle.key)
    battles.remove(battle.charId)
    battle.completion.complete(result)
  }
}
