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
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.MapLoadedAckPacket
import de.fiereu.openmmo.net.game.packets.SocialListEntryAddPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleActionSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleFormat
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventDetail
import de.fiereu.openmmo.net.game.packets.battle.BattleListEventPacket
import de.fiereu.openmmo.net.game.packets.battle.SafariEventPacket
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

private val SAFARI_KINDS = setOf(ChosenAction.Kind.SAFARI_BALL, ChosenAction.Kind.SAFARI_BAIT, ChosenAction.Kind.SAFARI_ROCK)
/** The bait event's toss kind whose template (200532) the launcher stages as "{00}": prints the thrower string as-is. */
private const val BALL_LINE_TOSS_KIND = 3

/** EntityTransportationPacket modes (client f.ti.J10): 0x01 surfing, 0x02 riding the bike. */
private const val SURF_TRANSPORTATION: Byte = 0x01
private const val RIDING_TRANSPORTATION: Byte = 0x02

/** Kanto and Hoenn: the regions whose safari packet strings are the launcher's own (the DS regions read their ROM). */
private val GBA_REGIONS = setOf(0, 1)

/** FireRed SafariZone_Text_OutOfBalls, "PA: Ding-dong! You are out of SAFARI BALLS!" (kanto.json). */
private const val SAFARI_OUT_OF_BALLS_TEXT = 1834067
// The ROM's own whiteout line (Unova text bank 10 entry 46, reference battle-strings.tsv):
// "You're out of Pokemon that can fight!"
private const val WHITEOUT_BANK = 10
private const val WHITEOUT_INDEX = 46

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
    /** A move tutor waiting on the forget dialog: true once the move took a slot, false otherwise. */
    val onAnswer: ((Boolean) -> Unit)? = null,
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
    private val safari: javax.inject.Provider<SafariService>? = null,
    private val encounterTracker: EncounterTrackerService? = null,
    private val presence: PresenceService? = null,
) {

  private val pokeBallItemId: Short by lazy { items.idOf(Items.POKE_BALL).toShort() }
  private val safariBallItemId: Short by lazy { items.idOf(Items.SAFARI_BALL).toShort() }

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
          BattleAction.ITEM -> ChosenAction(position, ChosenAction.Kind.ITEM, itemId = action.moveOrItemId.toInt() and 0xFFFF)
          BattleAction.SWITCH -> ChosenAction(position, ChosenAction.Kind.SWITCH, partyIndex = action.moveOrItemId.toInt())
          BattleAction.RUN -> ChosenAction(position, ChosenAction.Kind.RUN)
          BattleAction.BALL -> ChosenAction(position, ChosenAction.Kind.SAFARI_BALL)
          BattleAction.BAIT -> ChosenAction(position, ChosenAction.Kind.SAFARI_BAIT)
          BattleAction.ROCK -> ChosenAction(position, ChosenAction.Kind.SAFARI_ROCK)
        }
    battle.pendingActions[position] = chosen
    battle.awaitingPositions -= position
    // Running, a ball and the safari choices are the whole side's turn: the client offers no
    // second pick after them, so waiting for the other position hung every dark-grass double the
    // player tried to leave (owner, 2026-09-23).
    if (chosen.kind != ChosenAction.Kind.MOVE && chosen.kind != ChosenAction.Kind.SWITCH) battle.awaitingPositions.clear()
    if (battle.awaitingPositions.isEmpty()) resolvePending(battle)
  }

  /**
   * Every prompted position has answered. Running and a ball throw settle the turn on their own,
   * as they do in singles; voluntary switches go first, then the moves of the whole field.
   */
  private suspend fun resolvePending(battle: BattleInstance) {
    val actions = battle.pendingActions.values.toList()
    battle.pendingActions.clear()
    actions.firstOrNull { it.kind in SAFARI_KINDS }?.let { action ->
      if (battle.safari != null) {
        safariTurn(battle, action.kind)
        return
      }
      emitter.sendNotice(battle, "That only works in the Safari Zone.")
      prompt(battle)
      return
    }
    if (actions.any { it.kind == ChosenAction.Kind.RUN }) {
      flee(battle)
      return
    }
    actions.firstOrNull { it.kind == ChosenAction.Kind.ITEM }?.let { action ->
      throwBall(battle, action.itemId)
      return
    }
    for (switch in actions.filter { it.kind == ChosenAction.Kind.SWITCH }) {
      val target = switch.partyIndex
      val mon = battle.party.getOrNull(target)
      // The ally's monsters are not the player's to send in.
      if (mon == null || mon.fainted || target in battle.playerPositions || battle.isAllyIndex(target)) continue
      // A foe with Pursuit catches the monster on its way out; a knocked-out one never leaves.
      val leaving = battle.party[battle.playerPositions[switch.position]]
      emitter.sendEvents(battle, engine.pursuit(battle, leaving))
      if (leaving.fainted) continue
      performSwitch(battle, switch.position, target)
    }
    val events = engine.resolveTurn(battle, actions.filter { it.kind == ChosenAction.Kind.MOVE })
    // The raid logs every turn it plays, so a broken fight can be read back from the server log.
    if (battle.raid != null) {
      log.info { "Raid turn ${battle.turn} char=${battle.charId}: ${events.joinToString(" | ")}" }
    }
    emitter.sendEvents(battle, events)
    if (!interruptedByMove(battle)) afterTurn(battle)
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
      // The ally's position is the engine's to play; the client is never asked for it.
      if (battle.isAllyIndex(slot)) continue
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

  /**
   * Offers [moveId] to a full-moveset monster outside battle (a TM or a move tutor): the client
   * opens its move-learn screen and answers through the same reply packet, which calls [onAnswer].
   */
  fun offerMove(session: SessionContext, charId: Long, monId: Long, moveId: Short, onAnswer: (Boolean) -> Unit) {
    pendingLearns[monId] = PendingMoveLearn(charId, monId, listOf(moveId), onAnswer)
    session.send(MoveLearnPromptPacket(monId, listOf(moveId)))
  }

  /**
   * Applies the moveset the player picked on the move-learn screen (level up, TM, tutor). One
   * reply answers every move the screen offered; Skip sends the moveset back unchanged.
   */
  fun onMoveLearnReply(event: PacketEvent<MoveLearnReplyPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val reply = event.packet
    val pending = pendingLearns[reply.entityId] ?: return
    if (pending.charId != charId) return
    pendingLearns.remove(reply.entityId)
    val stored =
        characterStore.getCharacter(charId)?.pokemon?.firstOrNull { it.id == reply.entityId }
    if (stored == null) {
      pending.onAnswer?.invoke(false)
      return
    }
    val moves = stored.moves.toMutableList()
    if (!moveLearner.apply(moves, reply.moveIds, pending.offered)) {
      log.warn {
        "char=$charId sent an invalid moveset ${reply.moveIds} for ${reply.entityId} (offered ${pending.offered})"
      }
      pending.onAnswer?.invoke(false)
      return
    }
    if (moves.map { it.id } == stored.moves.map { it.id }) {
      log.info { "char=$charId kept the moves of ${reply.entityId} (skipped ${pending.offered})" }
      pending.onAnswer?.invoke(false)
      return
    }
    pending.onAnswer?.invoke(true)
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
    // Back in the overworld the bag can be told what the battle spent (see throwBall) and the
    // caught monster's delta can go out without printing an exp line into the fight.
    for (delta in battle.acquiredDeltas) event.session.send(delta)
    for (itemId in battle.consumedItems) {
      event.session.send(itemStackUpdatePacket(itemId, characterStore.getCharacter(charId)?.items?.get(itemId) ?: 0))
    }
    afterWildBattle(event.session, battle, result)
    // The client comes back from a battle on foot: a mount held before it (surfing, the bike)
    // is told again, or a surfer stands on the water unable to move (Route 19, 2026-09-11, the
    // seam ping-pong that followed). A whiteout respawn owns the player from here instead.
    if (!(result == BattleResult.DEFEAT && battle.whiteoutOnDefeat)) {
      event.session.attributes[PLAYER_STATE]?.let { state ->
        val mount: Byte? = if (state.surfing) SURF_TRANSPORTATION else if (state.riding) RIDING_TRANSPORTATION else null
        if (mount != null) {
          val packet = de.fiereu.openmmo.net.game.packets.EntityTransportationPacket(charId, mount)
          presence?.announce(event.session, packet) ?: event.session.send(packet)
        }
        // The client's overworld player may not be rebuilt yet when this ack arrives, and what is
        // sent now can be lost (the mount was, play-verified); the first step proves it is back,
        // and the release, the mount and a facing go out again then.
        if (battle.trainer == null) state.mountResendPending = true
        log.info { "Battle over for char=$charId: mount $mount (surfing=${state.surfing} riding=${state.riding}), wild=${battle.trainer == null}" }
      }
    }
    // A scripted loss without whiteout (early rival) is the script's to handle.
    return if (result == BattleResult.DEFEAT && !battle.whiteoutOnDefeat) null else result
  }

  /** True while the character has a battle running, so callers can skip starting another. */
  fun inBattle(charId: Long): Boolean = battles.byChar(charId) != null

  fun startWildBattle(
      session: SessionContext,
      dexId: Int,
      level: Int,
      hints: de.fiereu.openmmo.server.game.battle.WildRollHints? = null,
      encounter: de.fiereu.openmmo.server.game.battle.EncounterContext = de.fiereu.openmmo.server.game.battle.EncounterContext(),
      secretBonusPercent: Int = 0,
  ) {
    // Inside the Safari Game every wild encounter is a safari battle: no moves, Ball / Bait / Rock.
    val charId = session.attributes[PLAYER_STATE]?.characterId
    val game = safari?.get()?.takeIf { charId != null && it.isActive(charId) }
    val def = speciesRegistry.get(dexId)
    val safariState =
        if (game != null && charId != null && def != null)
            de.fiereu.openmmo.server.game.battle.SafariBattleState(
                balls = game.ballsLeft(charId),
                baseCatchFactor = def.catchRate * 100 / 1275,
                escapeFactor = (def.safariZoneFleeRate * 100 / 1275).coerceAtLeast(2),
            )
        else null
    val battle =
        createWildBattle(
            session,
            dexId,
            level,
            catchable = true,
            escapable = true,
            safari = safariState,
            hints = hints,
            encounter = encounter,
            secretBonusPercent = secretBonusPercent,
        )
    if (battle != null && charId != null) encounterTracker?.onWildEncounter(session, charId, dexId)
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
      /** An NPC trainer fighting beside the player (a tag battle); its team joins the player's side. */
      ally: TrainerDef? = null,
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
            ally = ally,
        ) ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  /**
   * A multi-monster wild battle. [sweetScent] separates the two ways one starts: Sweet Scent, which
   * the tracker counts under its own kind and which is the ONE encounter that can never produce a
   * Secret shiny, and a horde landed off an ordinary step roll, which is a plain wild encounter and
   * rolls for Secret like any other (owner, 2026-09-21). Before that distinction existed every
   * horde was logged as Sweet Scent.
   */
  fun startHordeBattle(
      session: SessionContext,
      specs: List<OpponentSpec>,
      encounter: de.fiereu.openmmo.server.game.battle.EncounterContext = de.fiereu.openmmo.server.game.battle.EncounterContext(),
      sweetScent: Boolean = false,
      secretBonusPercent: Int = 0,
      /** DOUBLES for Gen 5's dark-grass pair (the player fields two); null = a horde against one. */
      format: BattleFormat? = null,
  ): BattleInstance? {
    val battle =
        createBattle(
            session,
            specs,
            catchable = true,
            escapable = true,
            encounter = encounter,
            secretAllowed = !sweetScent,
            secretBonusPercent = secretBonusPercent,
            formatOverride = format,
        )
    val charId = session.attributes[PLAYER_STATE]?.characterId
    if (battle != null && charId != null) {
      encounterTracker?.onEncounter(
          session,
          charId,
          specs.map { it.dexId },
          if (sweetScent) EncounterTrackerService.TYPE_SWEET_SCENT else EncounterTrackerService.TYPE_WILD,
      )
    }
    return battle
  }

  /** A scripted wild battle (setwildbattle/dowildbattle: legendaries, Snorlax) awaited by the script. */
  suspend fun startScriptedWildBattle(session: SessionContext, dexId: Int, level: Int, catchable: Boolean = true): BattleResult {
    val battle = createWildBattle(session, dexId, level, catchable = catchable, escapable = true) ?: return BattleResult.FAILED
    session.attributes[PLAYER_STATE]?.characterId?.let { encounterTracker?.onWildEncounter(session, it, dexId) }
    return battle.completion.await()
  }

  /**
   * The Crystal Onix raid ([de.fiereu.openmmo.server.game.battle.CrystalOnixRaid]): the boss
   * between two summoned Onix, three a side, no catching and no running. Past the prize money
   * the rewards are the raid service's, once this returns a victory.
   */
  suspend fun startRaidBattle(session: SessionContext): BattleResult {
    val raid = de.fiereu.openmmo.server.game.battle.CrystalOnixRaid
    val boss = expansionSpecies.get(raid.SPECIES_SYMBOL) ?: return BattleResult.FAILED
    val specs =
        listOf(OpponentSpec(boss.serverId, raid.BOSS_LEVEL, raid.OPENING_MOVES, iv = 31, shiny = false, alpha = true)) +
            List(raid.HELPERS) { OpponentSpec(raid.HELPER_DEX, raid.HELPER_LEVEL, raid.HELPER_MOVES) }
    val battle =
        createBattle(session, specs, catchable = false, escapable = false, formatOverride = BattleFormat.TRIPLES, raid = true)
            ?: return BattleResult.FAILED
    return battle.completion.await()
  }

  private val expansionSpecies by lazy { de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry() }

  /** Resolves the original pret TRAINER_* constant inside one region's generated trainer table. */
  fun resolveTrainer(region: Region, constant: String): TrainerDef? = trainers.get(region, constant)

  fun resolveTrainer(region: Region, id: Int): TrainerDef? = trainers.get(region, id)

  /** Empty [moveIds] keeps the level up moveset, a null [iv] rolls one like a wild encounter. */
  data class OpponentSpec(
      val dexId: Int,
      val level: Int,
      val moveIds: List<Int>,
      val iv: Int? = null,
      /** The lead's Synchronize / Cute Charm / Compound Eyes (OverworldAbilities). */
      val hints: de.fiereu.openmmo.server.game.battle.WildRollHints? = null,
      /** Forced rarity flags (the raid boss is a shiny alpha: gold outline, alpha marks). */
      val shiny: Boolean = false,
      val alpha: Boolean = false,
  )

  private fun createWildBattle(
      session: SessionContext,
      dexId: Int,
      level: Int,
      catchable: Boolean,
      escapable: Boolean,
      moveIds: List<Int> = emptyList(),
      safari: de.fiereu.openmmo.server.game.battle.SafariBattleState? = null,
      hints: de.fiereu.openmmo.server.game.battle.WildRollHints? = null,
      encounter: de.fiereu.openmmo.server.game.battle.EncounterContext = de.fiereu.openmmo.server.game.battle.EncounterContext(),
      secretBonusPercent: Int = 0,
  ): BattleInstance? =
      createBattle(
          session,
          listOf(OpponentSpec(dexId, level, moveIds, hints = hints)),
          catchable,
          escapable,
          safari = safari,
          encounter = encounter,
          secretBonusPercent = secretBonusPercent,
      )

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
      /** An NPC trainer fighting beside the player: its team is built like a foe's and appended to the party. */
      ally: TrainerDef? = null,
      safari: de.fiereu.openmmo.server.game.battle.SafariBattleState? = null,
      encounter: de.fiereu.openmmo.server.game.battle.EncounterContext = de.fiereu.openmmo.server.game.battle.EncounterContext(),
      formatOverride: BattleFormat? = null,
      // Sweet Scent is the ONE encounter that cannot produce a Secret shiny (owner, 2026-09-21);
      // step hordes, dark grass doubles and hatched eggs all roll for it.
      secretAllowed: Boolean = true,
      // A premium lure raises the Secret odds by 25%.
      secretBonusPercent: Int = 0,
      /** The Crystal Onix raid: [opponents] opens with the boss, then its summoned Onix. */
      raid: Boolean = false,
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
      val def = speciesRegistry.forMonster(mon)
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
    // One monster the engine plays, built from its spec: a foe's, or an NPC ally's.
    fun build(spec: OpponentSpec): BattleMonState? {
      var rolled =
          wildMons.create(spec.dexId, spec.level, rng, shinyDenominator, spec.hints, secretAllowed, secretBonusPercent)
      if (rolled == null) {
        session.send(notice("Unknown species ${spec.dexId}."))
        return null
      }
      if (rolled.isShiny) {
        val secretTag = if (rolled.isSecret) " - SECRET" else ""
        log.info { "Shiny wild ${spec.dexId} L${spec.level} rolled for char=$charId (1 in $shinyDenominator)$secretTag" }
      }
      if (spec.shiny || spec.alpha) rolled = rolled.copy(isShiny = rolled.isShiny || spec.shiny, isAlpha = spec.alpha)
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
        // An IV is 0-31; a larger trainer value is bad data and must not multiply every stat.
        val iv = spec.iv.coerceIn(0, 31)
        val ivs =
            IVs().apply {
              hp = iv
              atk = iv
              this.def = iv
              spAtk = iv
              spDef = iv
              spd = iv
            }
        val fixed = rolled.copy(iVs = ivs)
        rolled = fixed.copy(hp = computeWildStats(def, fixed).hp.toShort())
      }
      val enemy = BattleMonState(rolled.id, def, null, rolled, computeWildStats(def, rolled))
      log.info {
        "Wild ${def.name} seed=${rolled.seed} slots=${def.ability1}/${def.ability2} -> ${enemy.ability} (char=$charId)"
      }
      return enemy
    }
    for (spec in opponents) enemies += build(spec) ?: return null
    // A tag battle: the ally's team is built the way a trainer's is (fixed IVs, ROM moves, no
    // shiny roll) and lines up after the player's own, so the whole side shares one party list.
    val allyStart = party.size
    val allyMons = mutableListOf<BattleMonState>()
    for (mon in ally?.party.orEmpty()) allyMons += build(OpponentSpec(mon.dexId, mon.level, mon.moveIds, mon.iv)) ?: return null
    party += allyMons
    log.info {
      "Starting battle for char=$charId (${stored.info.name}) against " +
          enemies.joinToString { "${it.species.name} level ${it.level}" } +
          (ally?.let { " with ${it.name} (${allyMons.joinToString { m -> "${m.species.name} level ${m.level}" }})" } ?: "")
    }
    // A trainer flagged for doubles fields two when the player can match it; several wild monsters
    // are a horde (up to five cells, the rest empty). An ally makes it a 2v2 whatever the foes.
    val alive = party.indices.filter { it < allyStart && !party[it].fainted }
    val format =
        when {
          formatOverride != null -> formatOverride
          allyMons.isNotEmpty() -> BattleFormat.DOUBLES
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
            BattleRules(catchable, escapable, trainer, defeatTextId, whiteoutOnDefeat, session.attributes[PLAYER_STATE]?.regionId ?: 0, partner, partnerDefeatTextId, ally = ally),
            format,
            allyStart = allyStart)
    if (allyMons.isNotEmpty()) {
      // The player's lead on the left, the ally's on the right.
      battle.playerPositions[0] = alive.first()
      battle.playerPositions[1] = allyStart + allyMons.indexOfFirst { !it.fainted }
    } else {
      for (position in 0 until format.playerSlots) battle.playerPositions[position] = alive.getOrElse(position) { -1 }
    }
    for (position in 0 until format.opponentSlots) battle.opponentPositions[position] = if (position < enemies.size) position else -1
    if (raid && format.opponentSlots == 3 && enemies.size >= 3) {
      // The boss stands in the middle, a summoned Onix on either side.
      battle.opponentPositions[0] = 1
      battle.opponentPositions[1] = 0
      battle.opponentPositions[2] = 2
      battle.raid = de.fiereu.openmmo.server.game.battle.RaidBossState(enemies[0].entityId)
    }
    battle.seenActive.clear()
    battle.seenActive.addAll(battle.playerPositions.filter { it >= 0 })
    battle.opponentSeen.clear()
    battle.opponentSeen.addAll(battle.opponentPositions.filter { it >= 0 })
    for (mon in battle.actives()) engine.prepareIllusion(battle, mon)
    battle.safari = safari
    battle.encounter = encounter
    interestManager.join(session, battle.key)
    emitter.sendStart(
        battle,
        stored.info.name,
        stored.info.rivalSex,
        SkinSet(stored.info.skinRegionSelectionIndex, stored.skins))
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
      // The raid's summoned Onix pay nothing: they are revived and called again, and would farm.
      val summoned = battle.raid?.let { !it.isBoss(foe) } == true
      if (foe.fainted && battle.rewardedFaints.add(foe.entityId) && !summoned) awardXp(battle, foe)
    }
    val raid = battle.raid
    when {
      battle.opponent.all { it.fainted } -> endVictory(battle)
      // The raid is won on the boss alone, whatever it summoned is still standing.
      raid != null && battle.opponent.first { raid.isBoss(it) }.fainted -> endVictory(battle)
      // A tag battle is lost on the player's OWN team (Emerald NoAliveMonsForPlayer checks the
      // player's slots alone); the ally standing on does not keep the fight going.
      battle.ownParty().all { it.fainted } -> endDefeat(battle)
      else -> {
        // A trainer refills an emptied position from the bench, and the raid boss calls another
        // Onix; a wild horde just thins out.
        for ((position, index) in battle.opponentPositions.withIndex()) {
          if (index < 0 || !battle.opponent[index].fainted) continue
          if (raid != null) {
            // The client announces the call itself ("called for help!" / "Onix joined the fight!")
            // when the switch-in lands. The client's party array is six long, so the raid cannot
            // bring more monsters than it opened with: with the bench spent, a fallen helper
            // answers the call again, fresh.
            val benched = battle.opponent.indices.any { !battle.opponent[it].fainted && it !in battle.opponentPositions }
            if (!benched) {
              battle.opponent.indices
                  .firstOrNull { i -> i != index && battle.opponent[i].fainted && !raid.isBoss(battle.opponent[i]) && i !in battle.opponentPositions }
                  ?.let { i ->
                    val helper = battle.opponent[i]
                    helper.resetVolatile()
                    helper.status = 0
                    helper.currentHp = helper.maxHp
                    // Unseen again, so it returns with a full block that carries its restored hp.
                    battle.opponentSeen.remove(i)
                  }
            }
          }
          if (battle.trainer != null || raid != null) sendOutNextOpponent(battle, position) else battle.opponentPositions[position] = -1
        }
        // The ally's fallen monster is replaced from the ally's own bench on the spot, the way a
        // trainer's is; its position empties when the bench is spent.
        for ((position, index) in battle.playerPositions.withIndex()) {
          if (index < 0 || !battle.isAllyIndex(index) || !battle.party[index].fainted) continue
          sendOutNextAlly(battle, position)
        }
        // Fainted positions owe a replacement while the bench has one; the switch screen opens
        // for each instead of the action prompt, and the replacements arrive as SWITCH actions.
        val owed =
            battle.playerPositions.indices.filter {
              val index = battle.playerPositions[it]
              index >= 0 && !battle.isAllyIndex(index) && battle.party[index].fainted
            }
        val benched = battle.ownParty().indices.count { it !in battle.playerPositions && !battle.party[it].fainted }
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

  /**
   * A move cut the turn short: Roar or Teleport ended the battle (closed here like a flight), or a
   * U-turn / Baton Pass by the player's monster owes a pick (the switch screen opens and the turn
   * waits for [forcedSwitch]). True when the turn must not go on to [afterTurn].
   */
  private fun interruptedByMove(battle: BattleInstance): Boolean {
    battle.moveEnded?.let { ending ->
      persistParty(battle)
      when (ending) {
        de.fiereu.openmmo.server.game.battle.MoveEnding.BLOWN_AWAY -> emitter.sendSilentEnd(battle)
        de.fiereu.openmmo.server.game.battle.MoveEnding.PLAYER_FLED -> emitter.sendFled(battle)
        de.fiereu.openmmo.server.game.battle.MoveEnding.WILD_FLED -> emitter.sendWildFled(battle)
      }
      battle.pendingResult = BattleResult.FLED
      return true
    }
    val pending = battle.pendingSelfSwitch ?: return false
    battle.forcedSwitchPositions += pending.position
    emitter.sendSwitchPrompt(battle, pending.position)
    return true
  }

  /** The replacement for a fainted position. Replacing does not spend a turn. */
  private suspend fun forcedSwitch(battle: BattleInstance, position: Int, target: Int) {
    val mon = battle.party.getOrNull(target)
    if (mon == null || mon.fainted || target in battle.playerPositions || battle.isAllyIndex(target)) {
      // Reopen the switch screen on an invalid choice.
      emitter.sendSwitchPrompt(battle, position)
      return
    }
    // A forced switch confirms the choice before the switch-in. The captures pair the confirm with
    // a full block for a new mon and with a return block for a mon that was already active.
    emitter.sendSwitchConfirm(battle, position)
    // The pick a U-turn or Baton Pass paused the turn for: swap in, then run the rest of the turn.
    val selfSwitch = battle.pendingSelfSwitch?.takeIf { it.position == position }
    if (selfSwitch != null) {
      battle.pendingSelfSwitch = null
      battle.forcedSwitchPositions -= position
      val events = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
      engine.swapIn(battle, true, position, target, 0, selfSwitch.batonPass, events)
      events += engine.resumeTurn(battle)
      emitter.sendEvents(battle, events)
      if (!interruptedByMove(battle)) afterTurn(battle)
      return
    }
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

  /** The NPC ally's next standing monster takes over [position]; an empty bench leaves the cell empty. */
  private fun sendOutNextAlly(battle: BattleInstance, position: Int) {
    val fallen = battle.party[battle.playerPositions[position]]
    val next = battle.benchOf(fallen).firstOrNull()
    if (next == null) {
      engine.switchOut(battle, fallen)
      fallen.resetVolatile()
      battle.playerPositions[position] = -1
      return
    }
    log.info { "Ally ${battle.ally?.name} sends out party index $next on position $position for char=${battle.charId}" }
    performSwitch(battle, position, next)
  }

  private fun performSwitch(battle: BattleInstance, position: Int, target: Int) {
    log.info {
      "Switch char=${battle.charId} position $position slot ${battle.playerPositions[position]} -> $target (fullBlock=${target !in battle.seenActive})"
    }
    // Stages, confusion, Leech Seed and the rest stay on the field, not on the monster.
    val events = mutableListOf<de.fiereu.openmmo.server.game.battle.BattleEvent>()
    engine.swapIn(battle, true, position, target, 0, false, events)
    emitter.sendEvents(battle, events)
  }

  /**
   * A ball thrown from the bag or the safari panel (src/battle_script_commands.c
   * Cmd_handleballthrow): the ball leaves the bag, the odds come from the target's catch rate, hp
   * and status and the ball's bonus, and the client plays the throw with the shakes (0x37 kind 0,
   * sub-kind = shakes 0..3, 4 = caught: f/Am prints bank 15 lines 61..64 or "Gotcha!"). A miss in
   * a normal battle hands the turn to the wild monster; in the Safari Game it is the wild's
   * watching turn instead.
   */
  private suspend fun throwBall(battle: BattleInstance, itemId: Int) {
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
    val item = items.get(itemId)
    if (item == null || (stored.items[itemId] ?: 0) <= 0) {
      emitter.sendNotice(battle, "You have no ${item?.name ?: "ball"} left.")
      prompt(battle)
      return
    }
    // The ball is spent on the throw, caught or not. The client is told the new count only once it
    // is back in the overworld (onClientReady): any container packet during a battle - the whole-bag
    // snapshot (0x40 replace) and the single-stack merge (0x42) alike - makes the client rebuild its
    // bag views and the open battle bag comes up empty (Argeno with the Snorlax, Turk with a Weedle,
    // 2026-09-10). The server's count is the one that is checked, so the stale display is harmless.
    if (!characterStore.addItem(battle.charId, itemId, -1)) return
    battle.consumedItems += itemId
    val game = battle.safari
    if (game != null && item == Items.SAFARI_BALL) {
      game.balls = safari?.get()?.consumeBall(battle.session, battle.charId) ?: (game.balls - 1)
    }
    val wild = battle.opponentMon()
    // FireRed's "{player} used {ball}!" ahead of the throw. In the Safari Game of a GBA region it
    // rides the safari packet's rock slot, whose Kanto string the launcher stages as "{23} used
    // Safari Ball!" - text only, the player's name filled by the client (the rock itself moved to
    // the bait event's toss, operator's call 2026-09-10). No other printer names the ball without
    // the toss's zoom and whistle (the free-text kind -22 never rendered in play), so every other
    // throw stays silent ahead of the ball: the throw, the shakes and the result, as before.
    val thrower = stored.info.name
    if (game != null && item == Items.SAFARI_BALL && (battle.session.attributes[PLAYER_STATE]?.regionId ?: 0) in GBA_REGIONS) {
      battle.session.send(SafariEventPacket.rock())
    }
    // Outside the Safari Game a throw announces nothing (operator's call, 2026-09-10): the
    // free-text kind -22 printed nothing on either anchor - dead, do not retry - and the engine's
    // own item line (BattleEvent.ItemUsed) names the monster, not the player.
    val shakes =
        when {
          item == Items.MASTER_BALL -> 4
          game != null && item == Items.SAFARI_BALL -> shakesFor(battle, wild, (game.catchFactor * 1275 / 100).coerceIn(0, 255), 150)
          else -> shakesFor(battle, wild, wild.species.catchRate, ballMultiplier(battle, item, wild, stored.storyVars))
        }
    if (shakes >= 4) {
      catchWild(battle, itemId)
      return
    }
    log.info { "[catch] char=${battle.charId} threw ${item.name} at ${wild.species.name}: $shakes shake(s), missed" }
    battle.session.send(BattleListEventPacket(kind = 0, value = itemId.toShort(), subKind = shakes.toByte(), detail = null))
    if (game != null) {
      if (game.balls <= 0) {
        // B_OUTCOME_NO_SAFARI_BALLS: the fight is over; the PA walks the player out after it.
        emitter.sendSafariOutOfBalls(battle, SAFARI_OUT_OF_BALLS_TEXT)
        persistParty(battle)
        battle.pendingResult = BattleResult.FLED
        return
      }
      safariWildTurn(battle)
      return
    }
    // The wild monster gets its move after a failed throw.
    emitter.sendEvents(battle, engine.resolveSwitchTurn(battle))
    if (!interruptedByMove(battle)) afterTurn(battle)
  }

  /** PokeMMO's bonus for [item] in hundredths (CatchModifiers); a Master Ball never rolls. */
  private fun ballMultiplier(battle: BattleInstance, item: de.fiereu.openmmo.items.ItemDef, wild: BattleMonState, storyVars: Map<String, Int>): Int {
    val user = battle.activeMon()
    val target = wild.originalSpecies
    val targetWire = de.fiereu.openmmo.common.clientSpeciesId(wild.source.dexId)
    val context =
        de.fiereu.openmmo.server.game.battle.CatchModifiers.BallContext(
            turn = battle.turn,
            targetLevel = wild.level,
            userLevel = user.level,
            targetTypes = target.types,
            targetBaseSpeed = target.baseSpeed,
            targetWeight = target.weight,
            targetGender = wild.gender.toInt(),
            userGender = user.gender.toInt(),
            sameEvolutionFamily = EvolutionTable.sameFamily(targetWire, de.fiereu.openmmo.common.clientSpeciesId(user.source.dexId)),
            familyEvolvesByMoonStone = EvolutionTable.familyEvolvesByItem(targetWire, items.idOf(Items.MOON_STONE)),
            familyEvolvesByFriendship = EvolutionTable.familyEvolvesByFriendship(targetWire),
            night = WorldClock.timeOfDay() == TimeOfDay.NIGHT,
            encounter = battle.encounter,
            targetSleepTurns = wild.sleepTurns,
            repeatChain = de.fiereu.openmmo.server.game.battle.RepeatBallStreak.chainFor(storyVars, wild.source.dexId, WorldClock.today()),
        )
    return de.fiereu.openmmo.server.game.battle.CatchModifiers.ballRate(item, context)
  }

  /**
   * The ball's shakes, 4 = caught: the odds are CatchModifiers.odds (catch rate, the ball's bonus in
   * hundredths, hp and status); 255 catches outright, else each of four shakes rolls under
   * 1048560 / sqrt(sqrt(16711680 / odds)).
   */
  private fun shakesFor(battle: BattleInstance, wild: BattleMonState, catchRate: Int, multiplier: Int): Int {
    val odds = de.fiereu.openmmo.server.game.battle.CatchModifiers.odds(catchRate, multiplier, wild.maxHp, wild.currentHp, wild.status)
    if (odds >= 255) return 4
    val shakeOdds = 1048560 / isqrt(isqrt(16711680 / odds))
    var shakes = 0
    while (shakes < 4 && battle.rng.pick(65536) < shakeOdds) shakes++
    return shakes
  }

  private fun isqrt(value: Int): Int {
    var r = kotlin.math.sqrt(value.toDouble()).toInt()
    while (r * r > value) r--
    while ((r + 1) * (r + 1) <= value) r++
    return r
  }

  /**
   * One Safari Game action other than the ball (src/battle_main.c HandleAction_ThrowBait /
   * ThrowRock), told through the client's own safari packet (0x3A): the client prints "{player}
   * threw some BAIT / a ROCK at the {mon}!" from the ROM in the DS regions and from its string
   * table in Kanto (staged by the launcher in FireRed's words).
   */
  private suspend fun safariTurn(battle: BattleInstance, kind: ChosenAction.Kind) {
    val s = battle.safari ?: return
    val wild = battle.opponentMon()
    when (kind) {
      ChosenAction.Kind.SAFARI_BALL -> {
        throwBall(battle, safariBallItemId.toInt())
        return
      }
      ChosenAction.Kind.SAFARI_BAIT -> {
        battle.session.send(SafariEventPacket.bait())
        s.baitTurns = (s.baitTurns + battle.rng.pick(5) + 2).coerceAtMost(6)
        s.rockTurns = 0
        s.catchFactor = (s.catchFactor shr 1).let { if (it <= 2) 3 else it }
      }
      ChosenAction.Kind.SAFARI_ROCK -> {
        // FireRed's "{player} threw a ROCK at the {mon}!" through the bait event's toss (zoom and
        // whistle included): the safari packet's rock slot now carries the ball line in Kanto.
        val thrower = characterStore.getCharacter(battle.charId)?.info?.name ?: "You"
        emitter.sendEvents(battle, listOf(de.fiereu.openmmo.server.game.battle.BattleEvent.SafariBait(wild.entityId, BALL_LINE_TOSS_KIND, "$thrower threw a ROCK at the ${wild.species.name}!")))
        s.rockTurns = (s.rockTurns + battle.rng.pick(5) + 2).coerceAtMost(6)
        s.baitTurns = 0
        s.catchFactor = (s.catchFactor shl 1).coerceAtMost(20)
        log.info { "[safari] char=${battle.charId} threw a rock at ${wild.species.name}: catch ${s.catchFactor}, angry for ${s.rockTurns}" }
      }
      else -> {}
    }
    safariWildTurn(battle)
  }

  /**
   * The wild monster's safari turn (HandleAction_WatchesCarefully, then the AI's
   * if_random_safari_flee): the last throw wears off one step and the monster says what it is
   * doing through the client's safari packet - "is watching carefully!", "is angry!", "is
   * eating!" - then it may bolt at escape factor * 5 % (doubled while angry, quartered while
   * eating), which the same packet tells as "Wild {mon} fled!" before a silent battle end.
   */
  private suspend fun safariWildTurn(battle: BattleInstance) {
    val s = battle.safari ?: return
    val wild = battle.opponentMon()
    val mood: Byte =
        when {
          s.rockTurns > 0 -> {
            s.rockTurns--
            if (s.rockTurns == 0) {
              s.catchFactor = s.baseCatchFactor
              SafariEventPacket.MOOD_WATCHING
            } else SafariEventPacket.MOOD_ANGRY
          }
          s.baitTurns > 0 -> {
            s.baitTurns--
            if (s.baitTurns == 0) SafariEventPacket.MOOD_WATCHING else SafariEventPacket.MOOD_EATING
          }
          else -> SafariEventPacket.MOOD_WATCHING
        }
    battle.session.send(SafariEventPacket.mood(mood))
    val fleeRate =
        when {
          s.rockTurns > 0 -> (s.escapeFactor * 2).coerceAtMost(20)
          s.baitTurns > 0 -> (s.escapeFactor / 4).coerceAtLeast(1)
          else -> s.escapeFactor
        } * 5
    if (battle.rng.pick(100) < fleeRate) {
      log.info { "[safari] ${wild.species.name} fled from char=${battle.charId} (rate $fleeRate%)" }
      battle.session.send(SafariEventPacket.mood(SafariEventPacket.MOOD_FLED))
      emitter.sendSilentEnd(battle)
      persistParty(battle)
      battle.pendingResult = BattleResult.FLED
      return
    }
    prompt(battle)
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
      if (!interruptedByMove(battle)) afterTurn(battle)
      return
    }
    emitter.sendFled(battle)
    persistParty(battle)
    battle.pendingResult = BattleResult.FLED
  }

  private suspend fun catchWild(battle: BattleInstance, ballItemId: Int = pokeBallItemId.toInt()) {
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
    // The caught monster goes out as a full record before the ball-throw event, so the client can
    // resolve it by (container, slot) when the throw lands (f/Am). The 0x14 add (f/LF0) files a
    // party monster at once but defers a PC one until the battle queue drains, which is after the
    // throw resolves, so a boxed catch is delivered through the container packet without the
    // change flag instead: f/yT0 adds it to the existing PC container immediately.
    if (partyFull) {
      battle.session.send(PokemonContainerPacket(PokemonContainer.PC, hasChange = false, delete = false, pokemon = listOf(caught)))
    } else {
      battle.session.send(SocialListEntryAddPacket(caught))
    }
    // The delta waits for the overworld (BattleInstance.acquiredDeltas): sent now it spoils the catch.
    battle.acquiredDeltas += acquiredMonsterDelta(caught, battle.opponentMon().species)
    // "Player threw a Poke Ball" event.
    battle.session.send(
        BattleListEventPacket(
            kind = 0,
            value = ballItemId.toShort(),
            subKind = 4,
            // f/Am reads this as (container byte, slot): it shows the "transferred to Box N" line
            // for a PC catch and opens the caught monster's summary from that slot.
            detail = BattleListEventDetail(listType = container.ordinal.toByte(), value = nextSlot),
        ),
    )
    if (!characterStore.addPokemon(battle.charId, caught)) {
      log.error { "Could not persist the monster char=${battle.charId} just caught" }
    }
    // Any catch extends or restarts the Repeat Ball chain, whatever ball made it.
    de.fiereu.openmmo.server.game.battle.RepeatBallStreak
        .recordCatch(stored.storyVars, battle.opponentMon().source.dexId, WorldClock.today())
        .forEach { (key, value) -> characterStore.setStoryVar(battle.charId, key, value) }
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
    // The raid pays its purse like a prize, so the Amulet Coin below doubles it too.
    if (battle.raid != null) prize += de.fiereu.openmmo.server.game.battle.CrystalOnixRaid.PRIZE_MONEY
    // An Amulet Coin anywhere in the party doubles the prize money.
    if (prize > 0 && battle.ownParty().any { items.get(it.heldItem) == de.fiereu.openmmo.items.generated.Items.AMULET_COIN })
        prize *= 2
    val paid = prize > 0 && characterStore.addMoney(battle.charId, prize)
    if (prize > 0 && !paid) {
      log.error { "Could not pay char=${battle.charId} the $prize prize" }
    }
    endBattle(battle, BattleResult.VICTORY, battle.rewardedWinners, if (paid) prize else 0)
    // The battle-end line only announces the prize; the client's wallet is a separate number that
    // nothing refreshed, so the HUD kept the old total until a shop or a relog (2026-09-08). The
    // same delta the shop sends after a purchase carries the new balance.
    if (paid) {
      characterStore.getCharacter(battle.charId)?.let {
        battle.session.send(de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket(money = it.info.money))
      }
    }
  }

  /**
   * Pays the active monster for knocking [defeated] out. A trainer's team is paid for one at a
   * time, as each faints, which is when the captures show the delta going out.
   */
  private fun awardXp(battle: BattleInstance, defeated: BattleMonState) {
    // Everyone of the player's OWN standing on the field took part (an NPC ally earns nothing); a
    // field with nobody standing (a mutual knockout) still pays the lead.
    val winners = battle.ownActives().filter { !it.fainted }.ifEmpty { listOf(battle.activeMon()) }.filter { !battle.isAllyMon(it) }
    for (winner in winners) awardXpTo(battle, winner, defeated)
    // An Exp. Share on a monster that sat out pays it half the experience (Gen 3); a fainted
    // holder gets nothing.
    for (holder in battle.ownParty()) {
      if (holder in winners || holder.fainted) continue
      if (items.get(holder.heldItem) != Items.EXP_SHARE) continue
      awardXpTo(battle, holder, defeated, viaExpShare = true)
    }
  }

  private fun awardXpTo(battle: BattleInstance, winner: BattleMonState, defeated: BattleMonState, viaExpShare: Boolean = false) {
    battle.rewardedWinners += winner.entityId
    val reward = rewards.apply(winner, defeated.species, defeated.level, battle.trainer != null, viaExpShare)
    log.info {
      "char=${battle.charId} won: +${reward.xpGained} xp, level ${winner.level} -> ${reward.newLevel}"
    }
    winner.currentHp = reward.newCurrentHp
    if (reward.leveled) winner.leveledThisBattle = true
    val outcome =
        moveLearner.learn(winner.moves, winner.source.dexId, winner.level, reward.newLevel)
    emitter.sendVictoryDelta(battle, winner.entityId, reward)
    // A move that took a free slot is only a moveset update: r32645's prompt has no "learned into a
    // slot" form, and any prompt opens the four-moves screen. Moves that need a slot freed go out as
    // ONE prompt listing all of them, answered by one reply.
    if (outcome.learned.isNotEmpty()) {
      battle.session.send(
          emitter.moveSlotsDelta(winner.entityId, winner.moves.map { it.id to it.pp }, 0))
    }
    if (outcome.offered.isNotEmpty()) {
      val offered = outcome.offered.map { it.moveId.toShort() }
      pendingLearns[winner.entityId] = PendingMoveLearn(battle.charId, winner.entityId, offered)
      battle.session.send(MoveLearnPromptPacket(winner.entityId, offered))
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
            status = persistedStatus(winner),
            heldItem = winner.heldItem,
        )
    winner.source = grown
    winner.stats = reward.newStats
    characterStore.updatePokemon(battle.charId, grown)
  }

  private fun endDefeat(battle: BattleInstance) {
    // The cartridge's "out of usable Pokemon / whited out" text. It rides a STANDING foe: the
    // player's side is empty by now, and the client drops sub-events aimed at a removed monster.
    if (battle.whiteoutOnDefeat) {
      battle.opponentActives().firstOrNull { !it.fainted }?.let { foe ->
        emitter.sendEvents(battle, listOf(de.fiereu.openmmo.server.game.battle.BattleEvent.RomLine(foe.entityId, WHITEOUT_BANK, WHITEOUT_INDEX)))
      }
    }
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
    for (state in battle.ownParty()) {
      if (!state.leveledThisBattle) continue
      if (state.source.id in playerState.pendingEvolutions) continue
      val mon = state.source
      if (mon.heldItem in BreedingService.EVERSTONES) continue
      val wire = clientSpeciesId(mon.dexId)
      val def = speciesRegistry.forMonster(mon) ?: continue
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
                  daytime = WorldClock.isDaytime(),
                  moves = mon.moves.map { it.id.toInt() }.toSet(),
                  partyWires = battle.ownParty().map { clientSpeciesId(it.source.dexId) }.toSet(),
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
    for (state in battle.ownParty()) {
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
  /**
   * What a battle leaves on the record: Toxic goes home as ordinary poison, as the cartridges do,
   * and a faint clears everything. The XP reward writes the winner's record itself (persistParty
   * skips rewarded winners), so it has to carry this too - a status caught in the winning fight
   * used to vanish with the reward's pre-battle copy.
   */
  private fun persistedStatus(state: BattleMonState): Int {
    if (state.currentHp <= 0) return 0
    val toxic = de.fiereu.openmmo.common.StatusCondition.TOXIC
    return if (state.status and toxic != 0) (state.status and toxic.inv()) or de.fiereu.openmmo.common.StatusCondition.POISON else state.status
  }

  private fun persistParty(battle: BattleInstance, skip: Set<Long> = emptySet()) {
    // The ally's monsters are nobody's stored monsters: they must never be written to the player.
    for (state in battle.ownParty()) {
      if (state.entityId in skip) continue
      val updated =
          state.source.copy(
              hp = state.currentHp.toShort(),
              status = persistedStatus(state),
              heldItem = state.heldItem,
              // Transform's and Mimic's copies never leave the battle; Sketch's does, as it should.
              moves = state.ownMoves(),
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
    safari?.get()?.afterBattle(session, state, battle, result)
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
