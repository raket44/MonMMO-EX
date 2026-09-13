package de.fiereu.openmmo.server.game.battle

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.battle.BattleFormat
import de.fiereu.openmmo.server.game.world.interest.BattleInterestKey
import de.fiereu.openmmo.trainer.TrainerDef
import kotlinx.coroutines.CompletableDeferred

enum class BattleResult {
  VICTORY,
  DEFEAT,
  FLED,
  CAUGHT,
  DISCONNECTED,
  FAILED,
}

/** What kind of battle this is, beyond the two teams. A [trainer] owns the opposing side. */
data class BattleRules(
    val catchable: Boolean = true,
    val escapable: Boolean = true,
    val trainer: TrainerDef? = null,
    /** ROM dialog id of the trainer's in-battle defeat speech; shown before the prize money. */
    val defeatTextId: Int? = null,
    /** False for the early rival: a loss hands the outcome to the script instead of a whiteout. */
    val whiteoutOnDefeat: Boolean = true,
    /** Region whose ROM trainer table names the opponent (client W9.io(region, id)). */
    val trainerRegion: Int = 0,
    /** A second trainer fighting alongside [trainer] (double sighting); its party follows the first's. */
    val partner: TrainerDef? = null,
    /** The partner's in-battle defeat speech, played after [defeatTextId]. */
    val partnerDefeatTextId: Int? = null,
)

/**
 * What the player asked one of their field positions to do this turn. The client packs the acting
 * position and, for a move, the target position into its action packet (`f/fd1`: side in the high
 * nibble, position in the low one).
 */
data class ChosenAction(
    val position: Int,
    val kind: Kind,
    val moveId: Short = 0,
    /** The side the chosen target stands on: 0 the player's, 1 the opposing side. */
    val targetSide: Int = 1,
    val targetPosition: Int = 0,
    /** SWITCH: the party slot coming in. */
    val partyIndex: Int = -1,
    /** ITEM: the ball thrown (client item id). */
    val itemId: Int = 0,
) {
  enum class Kind {
    MOVE,
    SWITCH,
    ITEM,
    SAFARI_BALL,
    SAFARI_BAIT,
    SAFARI_ROCK,
    RUN,
  }
}

/**
 * One running battle. Each side has [BattleFormat.playerSlots] / [BattleFormat.opponentSlots] field
 * positions holding an index into [party] / [opponent], or -1 while empty. A wild encounter is the
 * singles case where [opponent] holds one monster; a horde fills up to five opposing positions.
 */
class BattleInstance(
    val battleId: Long,
    val charId: Long,
    val session: SessionContext,
    val party: List<BattleMonState>,
    val opponent: List<BattleMonState>,
    val rng: BattleRng,
    val catchable: Boolean = true,
    val escapable: Boolean = true,
    /** The trainer who owns [opponent], or null for a wild encounter. */
    val trainer: TrainerDef? = null,
    /** ROM dialog id of the trainer's in-battle defeat speech; shown before the prize money. */
    val defeatTextId: Int? = null,
    /** False for the early rival: a loss hands the outcome to the script instead of a whiteout. */
    val whiteoutOnDefeat: Boolean = true,
    /** Region whose ROM trainer table names the opponent; the client shows class + name from it. */
    val trainerRegion: Int = 0,
    val format: BattleFormat = BattleFormat.SINGLES,
    /** The second trainer of a double sighting, paid and flagged beaten with the first. */
    val partner: TrainerDef? = null,
    val partnerDefeatTextId: Int? = null,
) {
  val key: BattleInterestKey = BattleInterestKey(battleId)
  var turn: Int = 1

  /** Bag items spent during the battle (balls thrown): their counts are synced to the client once it is back in the overworld. */
  val consumedItems = LinkedHashSet<Int>()

  /**
   * Deltas for monsters caught in this battle, sent once the client is back in the overworld: a
   * 0x16 delta with an experience field received DURING a battle makes the client print
   * "{mon} gained 0 Exp. Points!" at once (f/Y9 -> f/L71), before the throw animation has even
   * played - which told the player the catch was in.
   */
  val acquiredDeltas = mutableListOf<de.fiereu.openmmo.net.game.packets.battle.BattleEntityDeltaPacket>()

  /** The party index standing on each of the player's field positions, -1 for an empty one. Position 0 opens on slot 0, as a bare instance always did. */
  val playerPositions: IntArray = IntArray(format.playerSlots) { if (it == 0) 0 else -1 }
  /** The opponent index standing on each opposing field position, -1 for an empty one. */
  val opponentPositions: IntArray = IntArray(format.opponentSlots) { if (it == 0) 0 else -1 }

  /** Position 0 on the player's side: the whole field in singles. */
  var activeSlot: Int
    get() = playerPositions[0]
    set(value) {
      playerPositions[0] = value
    }

  var opponentSlot: Int
    get() = opponentPositions[0]
    set(value) {
      opponentPositions[0] = value
    }

  // Which slots have been sent out as active. A monster's first appearance carries its full block,
  // a return only its active detail.
  val seenActive: MutableSet<Int> = mutableSetOf(0)
  val opponentSeen: MutableSet<Int> = mutableSetOf(0)

  /** The player's positions still owed an action this turn, and the actions received so far. */
  val awaitingPositions: MutableSet<Int> = linkedSetOf()
  val pendingActions: MutableMap<Int, ChosenAction> = linkedMapOf()
  /** Player positions whose monster fainted and that owe a replacement before the next turn. */
  val forcedSwitchPositions: MutableSet<Int> = linkedSetOf()
  /** Opponents already paid out for, so a faint rewards exactly once. */
  val rewardedFaints: MutableSet<Long> = mutableSetOf()
  /** Party monsters the rewards already wrote to the store; the final persist must not undo that. */
  val rewardedWinners: MutableSet<Long> = mutableSetOf()

  /** Completes when the battle leaves the registry. */
  val completion = CompletableDeferred<BattleResult>()

  /** Result held until the client confirms that its battle-to-map transition has finished. */
  var pendingResult: BattleResult? = null
  /** The Safari Game's counters when this is a safari battle (SafariService), else null. */
  var safari: SafariBattleState? = null

  /** Where the wild monster was met, for the Dive, Dusk and Lure Balls. */
  var encounter: EncounterContext = EncounterContext()

  var weather: Weather? = null
  var weatherTurns: Int = 0
  val playerSide = SideState()
  val opponentSide = SideState()

  /** The monster on the player's first filled position (the only one in singles). */
  fun activeMon(): BattleMonState = party[playerPositions.firstOrNull { it >= 0 } ?: 0]

  /** The monster on the first filled opposing position; the first alive one when several are out. */
  fun opponentMon(): BattleMonState =
      opponentActives().firstOrNull { !it.fainted } ?: opponent[opponentPositions.firstOrNull { it >= 0 } ?: 0]

  fun playerActives(): List<BattleMonState> = playerPositions.filter { it >= 0 }.map { party[it] }

  fun opponentActives(): List<BattleMonState> = opponentPositions.filter { it >= 0 }.map { opponent[it] }

  /** Every monster on the field, the player's side first. */
  fun actives(): List<BattleMonState> = playerActives() + opponentActives()

  fun monAt(side: Int, position: Int): BattleMonState? {
    val positions = if (side == 0) playerPositions else opponentPositions
    val list = if (side == 0) party else opponent
    val index = positions.getOrNull(position) ?: return null
    return if (index < 0) null else list[index]
  }

  /** The field position [mon] stands on, or -1 when it is benched. */
  fun positionOf(mon: BattleMonState): Int {
    val onPlayerSide = isPlayerSide(mon.entityId)
    val positions = if (onPlayerSide) playerPositions else opponentPositions
    val list = if (onPlayerSide) party else opponent
    return positions.indexOfFirst { it >= 0 && list[it] === mon }
  }

  fun isPlayerSide(entityId: Long): Boolean = party.any { it.entityId == entityId }

  fun sideOf(mon: BattleMonState): SideState =
      if (isPlayerSide(mon.entityId)) playerSide else opponentSide

  /** The monsters facing [mon] that are still standing. */
  fun foesOf(mon: BattleMonState): List<BattleMonState> =
      (if (isPlayerSide(mon.entityId)) opponentActives() else playerActives()).filter { !it.fainted }

  /** [mon]'s partners on the field, itself excluded. */
  fun alliesOf(mon: BattleMonState): List<BattleMonState> =
      (if (isPlayerSide(mon.entityId)) playerActives() else opponentActives()).filter { it !== mon && !it.fainted }

  /**
   * The foe in front of [mon]: the one on the facing position while it stands, else the first foe
   * still up, else whatever is on the facing side (a fainted one, so callers can still name it).
   */
  fun opponentOf(mon: BattleMonState): BattleMonState {
    val onPlayerSide = isPlayerSide(mon.entityId)
    val facing = if (onPlayerSide) 1 else 0
    monAt(facing, positionOf(mon))?.takeIf { !it.fainted }?.let { return it }
    foesOf(mon).firstOrNull()?.let { return it }
    return if (onPlayerSide) opponent[opponentPositions.firstOrNull { it >= 0 } ?: 0] else party[playerPositions.firstOrNull { it >= 0 } ?: 0]
  }
}

/**
 * FireRed's safari battle counters (src/battle_main.c): the catch factor starts at the species'
 * catch rate * 100 / 1275, the escape factor at its safari flee rate * 100 / 1275 (at least 2);
 * bait halves the catch factor (floor 3) and rock doubles it (cap 20), each for 2..6 of the wild
 * monster's turns, while it watches, eats or is angry, and may flee.
 */
class SafariBattleState(
    var balls: Int,
    val baseCatchFactor: Int,
    val escapeFactor: Int,
) {
  var catchFactor: Int = baseCatchFactor
  var baitTurns: Int = 0
  var rockTurns: Int = 0
}
