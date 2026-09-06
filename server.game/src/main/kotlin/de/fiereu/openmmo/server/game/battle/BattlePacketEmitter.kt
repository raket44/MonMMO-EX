package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.utils.hexToBytes
import de.fiereu.openmmo.net.game.packets.EntityMovePpPacket
import de.fiereu.openmmo.net.game.packets.EntityPresencePacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleActionEvent
import de.fiereu.openmmo.net.game.packets.battle.BattleBulkStatePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTarget
import de.fiereu.openmmo.net.game.packets.battle.BattleEntityDeltaPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleEntityMoveEventPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleEventBody
import de.fiereu.openmmo.net.game.packets.battle.BattleFieldStatePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleOpponentBlock
import de.fiereu.openmmo.net.game.packets.battle.BattleQueuedEventPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSidePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSlotEventEnumPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSlotFlagEventPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleStatCountersPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSwitchInPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleTileMapPacket
import de.fiereu.openmmo.net.game.packets.battle.Experience
import de.fiereu.openmmo.net.game.packets.battle.MoveSlots
import de.fiereu.openmmo.net.game.packets.battle.OpposingSide
import de.fiereu.openmmo.server.game.services.notice
import de.fiereu.openmmo.server.game.world.interest.InterestManager
import de.fiereu.openmmo.typechart.TypeChart
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

private val TWO_TRAINER_HEADER: Boolean = System.getProperty("monmmo.twoTrainerHeader") == "true"

private const val ACTION_PROMPT: Byte = -128 // 0x80
private const val MOVE_EVENT_KIND: Byte = 1
// Slot-event type shown when the player gets away from a wild battle.
private const val FLED_EVENT: Byte = 0

// The player's overworld entity is hidden while the battle scene is up and shown again when it
// ends.
private const val PRESENCE_IN_BATTLE: Byte = 1
private const val PRESENCE_OVERWORLD: Byte = 0

// The active battle side reported to the client so the bag knows which monster an item targets.
private const val PLAYER_SIDE: Byte = 1

// The side byte a switch-in carries, which is not the same numbering as BattleSidePacket.
private const val OPPONENT_SIDE: Byte = 1

private val CAPTURED_APPEARANCE = "00024c031aac0f00038001a40004".hexToBytes()

// The target's outcome word: a bit set, each bit a line the client prints for that target
// (f/O20.t20 - decompiled): 1 "avoided the attack", 2 "A critical hit!", 4 "But it failed!",
// 8 "It doesn't affect {00}...", 0x10 "not very effective", 0x20 "super effective", 0x80
// "{00} protected itself!". 0x200 marks a damaging hit and prints nothing; a status move that
// only moves a stat carries none of them. The events under the target are read either way.
private const val HP_TARGET_MOVE: Short = 0x0200
private const val MISSED_TARGET_MOVE: Short = 1
private const val CRITICAL_HIT_BIT = 0x02
private const val FAILED_TARGET_MOVE: Short = 4
private const val IMMUNE_TARGET_MOVE: Short = 8
private const val PROTECTED_TARGET_MOVE: Short = 0x80
// 0x40 on the attacker's own entry marks the first half of a two-turn move: the client picks the
// line from the move id ("{00} flew up high!", "burrowed its way under the ground!", ...).
private const val CHARGING_TARGET_MOVE: Short = 0x40
private const val DEFAULT_TARGET_MOVE: Short = 0
private const val SUPER_EFFECTIVE_BIT = 0x20
private const val NOT_VERY_EFFECTIVE_BIT = 0x10

/**
 * Turns battle state and [BattleEvent]s into packets. Everything battle wide goes through the
 * battle's interest key, so spectators and later trainer opponents receive it too. Packets tied to
 * one viewer (side, bag, party sync) go to that session directly.
 */
@Singleton
class BattlePacketEmitter @Inject constructor(private val interestManager: InterestManager) {

  fun sendStart(battle: BattleInstance, playerName: String) {
    battle.session.send(EntityPresencePacket(entityId = battle.charId, status = PRESENCE_IN_BATTLE))
    // Tell the client which side is local so the battle bag knows which monster an item targets.
    // Without it, opening the bag crashes. Opcode 0x40 is left alone here, since re-sending it
    // would wipe the balls out of the battle bag.
    battle.session.send(BattleSidePacket(side = PLAYER_SIDE))
    broadcast(
        battle,
        BattleFieldStatePacket(
            playerName = playerName,
            playerId = battle.charId,
            // TODO Send the player's own appearance and the map's battle backdrop
            //  These are the captured values, so every player appears as the captured character.
            playerAppearance = CAPTURED_APPEARANCE,
            background = 0,
            opposing = if (battle.trainer == null) OpposingSide.WILD else OpposingSide.TRAINER,
            // The client resolves class and name through its per-region ROM trainer table
            // (f/W9.io(region, id)); the id alone lands in the Kanto table.
            trainerId = (battle.trainer?.id ?: 0).toShort(),
            // The composite two-trainer header parses on the client but its monster records then look
            // the owning entry up by a key byte we do not send yet (f/BM1.qg1 NPE). Off by default
            // until that key is decoded; -Dmonmmo.twoTrainerHeader=true to keep iterating on it.
            partnerTrainerId = if (TWO_TRAINER_HEADER) battle.partner?.id?.toShort() else null,
            // Only a trainer side names a region (the client keys its ROM trainer table with it). On
            // a wild side the same byte is read as side flags: a non-zero value (Hoenn = 1) made the
            // client expect an extra field and die on the monster's entity id - wild battles outside
            // Kanto never showed. It must be zero there.
            trainerRegion = if (battle.trainer == null) 0 else battle.trainerRegion.toByte(),
            playerParty = battle.party.mapIndexed { slot, mon -> mon.toBlock(slot, true) },
            playerActive = battle.playerPositions.map { it.takeIf { slot -> slot >= 0 } },
            opponentParty =
                battle.opponent.mapIndexed { slot, mon ->
                  if (slot in battle.opponentSeen) mon.toOpponentBlock(slot)
                  else BattleOpponentBlock(slot = slot, revealed = false)
                },
            opponentActive = battle.opponentPositions.map { it.takeIf { slot -> slot >= 0 } },
            format = battle.format,
        ),
    )
  }

  /**
   * A [BattleEvent.MoveUsed] or [BattleEvent.TurnEffect] opens one move-event packet; the target
   * events that follow ride under it, one [BattleEffectTarget] per affected monster, until the
   * next opener. The client animates what it finds there: an hp update moves the bar (and faints
   * the monster at 0, so no faint sub-event is sent), a stat change with a non-zero delta plays
   * the stage animation, a status change sets the status byte and prints its line, a weather
   * change switches the field. A miss or failure is the outcome word alone with nothing under it.
   * Events the client has no verified rendering for yet (a lost action, a multi-hit count, a
   * charge turn, Protect) are logged and otherwise silent.
   */
  fun sendEvents(battle: BattleInstance, events: List<BattleEvent>) {
    var group: EventGroup? = null
    fun flush() {
      val current = group ?: return
      group = null
      if (current.targets.isEmpty() && current.moveId == 0.toShort()) return
      broadcast(
          battle,
          BattleEntityMoveEventPacket(
              current.sourceId,
              current.moveId,
              MOVE_EVENT_KIND,
              current.targets.values.map { it.build() }))
    }
    fun target(entityId: Long): TargetAccumulator {
      val current = group ?: EventGroup(entityId, 0).also { group = it }
      return current.targets.getOrPut(entityId) { TargetAccumulator(entityId) }
    }
    for (event in events) {
      when (event) {
        is BattleEvent.MoveUsed -> {
          flush()
          if (battle.isPlayerSide(event.attackerId)) {
            battle.session.send(
                EntityMovePpPacket(
                    event.attackerId, event.moveSlot.toByte(), event.ppLeft.toByte()))
          }
          group = EventGroup(event.attackerId, event.moveId)
        }
        is BattleEvent.TurnEffect -> {
          flush()
          group = EventGroup(event.sourceId, 0)
        }
        is BattleEvent.DamageDealt -> {
          val acc = target(event.targetId)
          acc.outcome =
              (HP_TARGET_MOVE.toInt() or
                      effectivenessBit(event.effectiveness) or
                      (if (event.crit) CRITICAL_HIT_BIT else 0))
                  .toShort()
          acc.subEvents += BattleActionEvent(null, null, BattleEventBody.HpUpdate(event.newHp.toShort()))
        }
        is BattleEvent.AbilityShown ->
            target(event.targetId).subEvents +=
                BattleActionEvent(
                    null,
                    null,
                    BattleEventBody.AbilityPopup(
                        abilityId = event.ability.ordinal,
                        kind =
                            (if (event.otherId != 0L) 1 else 0) or
                                (if (event.moveId != 0) 2 else 0) or
                                (if (event.itemId != 0) 8 else 0),
                        self = event.targetId,
                        other = event.otherId,
                        moveId = event.moveId,
                        itemId = event.itemId))
        is BattleEvent.ItemChanged ->
            broadcast(battle, BattleEntityDeltaPacket(entityId = event.targetId, heldItem = event.itemId.toShort()))
        is BattleEvent.SpeciesShown ->
            broadcast(
                battle,
                BattleEntityDeltaPacket(
                    entityId = event.targetId,
                    species = de.fiereu.openmmo.net.game.packets.battle.Species(event.wireSpecies.toShort(), 0)))
        is BattleEvent.Protected -> target(event.targetId).outcome = PROTECTED_TARGET_MOVE
        is BattleEvent.Immune -> target(event.targetId).outcome = IMMUNE_TARGET_MOVE
        is BattleEvent.Line ->
            target(event.targetId).subEvents +=
                BattleActionEvent(null, null, BattleEventBody.Line(event.line, event.values))
        is BattleEvent.HpChanged ->
            target(event.targetId).subEvents +=
                BattleActionEvent(null, null, BattleEventBody.HpUpdate(event.newHp.toShort()))
        is BattleEvent.StageChanged ->
            if (!event.failed) {
              target(event.targetId).subEvents +=
                  BattleActionEvent(
                      null, null, BattleEventBody.StatChange(statIndex(event.stat), event.delta.toShort()))
            }
        is BattleEvent.StatusChanged ->
            target(event.targetId).subEvents +=
                BattleActionEvent(null, null, BattleEventBody.StatusChange(event.status.toByte()))
        is BattleEvent.WeatherChanged -> {
          val current = group ?: EventGroup(battle.activeMon().entityId, 0).also { group = it }
          target(current.sourceId).subEvents +=
              BattleActionEvent(
                  null, null, BattleEventBody.WeatherChange((event.weather?.wireValue ?: 0).toByte()))
        }
        is BattleEvent.MoveWithoutTarget -> {
          val defender =
              if (battle.isPlayerSide(event.attackerId)) battle.opponentMon().entityId
              else battle.activeMon().entityId
          target(defender).outcome =
              if (event is BattleEvent.MoveMissed) MISSED_TARGET_MOVE else FAILED_TARGET_MOVE
        }
        is BattleEvent.Hidden ->
            target(event.targetId).subEvents +=
                BattleActionEvent(null, null, BattleEventBody.Visibility(event.hidden))
        is BattleEvent.Fainted -> Unit
        is BattleEvent.Charging -> target(event.attackerId).outcome = CHARGING_TARGET_MOVE
        is BattleEvent.CantMove,
        is BattleEvent.MultiHit -> log.debug { "silent battle event $event" }
      }
    }
    flush()
  }

  private class EventGroup(val sourceId: Long, val moveId: Short) {
    val targets = LinkedHashMap<Long, TargetAccumulator>()
  }

  private class TargetAccumulator(val entityId: Long) {
    var outcome: Short = DEFAULT_TARGET_MOVE
    val subEvents = mutableListOf<BattleActionEvent>()

    fun build(): BattleEffectTarget = BattleEffectTarget(entityId, outcome, subEvents.toList())
  }

  fun sendSwitchIn(battle: BattleInstance, position: Int, oldSlot: Int, fullBlock: Boolean) {
    val slot = battle.playerPositions[position]
    broadcast(
        battle,
        BattleSwitchInPacket(
            // The packed header names the battle-field POSITION, not the party slot: the client
            // indexes its per-side active array with it, which in singles has exactly one cell.
            // Party slot 4 in that nibble was the ArrayIndexOutOfBounds crash on send-out. The
            // party slot rides inside the monster block instead.
            newSlot = position,
            oldSlot = oldSlot,
            mon = battle.party[slot].toBlock(slot = slot, movesPresent = true),
            fullBlock = fullBlock,
        ),
    )
  }

  /** The opposing side sends out its next monster. Its moves stay hidden from the player. */
  fun sendOpponentSwitchIn(battle: BattleInstance, position: Int, oldSlot: Int, fullBlock: Boolean) {
    val index = battle.opponentPositions[position]
    broadcast(
        battle,
        BattleSwitchInPacket(
            newSlot = position,
            oldSlot = oldSlot,
            mon = battle.opponent[index].toBlock(index, movesPresent = false),
            fullBlock = fullBlock,
            side = OPPONENT_SIDE,
        ),
    )
  }

  /**
   * Plays the client's own evolution sequence on a battle mon: event 107 rebuilds the entity as the
   * new species, morphs the sprite and announces the evolution. Ridden on the same move-event
   * stream every turn already uses, addressed at the evolving mon.
   */
  fun sendEvolution(
      battle: BattleInstance,
      entityId: Long,
      species: Short,
      currentHp: Short,
      maxHp: Short,
  ) {
    broadcast(
        battle,
        BattleEntityMoveEventPacket(
            sourceEntity = entityId,
            sourceMove = 0,
            kind = MOVE_EVENT_KIND,
            targets =
                listOf(
                    BattleEffectTarget(
                        entityId = entityId,
                        targetMove = 0,
                        subEvents =
                            listOf(
                                BattleActionEvent(
                                    entityA = null,
                                    entityB = null,
                                    body =
                                        BattleEventBody.Evolution(
                                            species = species,
                                            currentHp = currentHp,
                                            maxHp = maxHp,
                                        ),
                                )),
                    )),
        ),
    )
  }

  /** Asks for an action on each of [positions]: the client collects one per prompted position and sends them together. */
  fun sendPrompt(battle: BattleInstance, positions: Collection<Int> = listOf(0)) {
    broadcast(battle, BattleTileMapPacket(groupId = battle.turn.toShort(), slotTiles = null))
    for (position in positions) broadcast(battle, BattleQueuedEventPacket(packed = (ACTION_PROMPT.toInt() or position).toByte()))
  }

  /** Opens the party switch screen after the active mon faints, in place of the action prompt. */
  fun sendSwitchPrompt(battle: BattleInstance, position: Int = 0) {
    broadcast(battle, BattleSlotFlagEventPacket(slot = position.toByte(), flag = false, immediate = false))
  }

  /** Confirms the forced replacement choice just before its switch-in. */
  fun sendSwitchConfirm(battle: BattleInstance, position: Int = 0) {
    broadcast(battle, BattleSlotFlagEventPacket(slot = position.toByte(), flag = false, immediate = true))
  }

  fun sendFled(battle: BattleInstance) {
    broadcast(battle, BattleSlotEventEnumPacket(slot = 0, eventType = FLED_EVENT))
    broadcast(battle, BattleBulkStatePacket.fled())
    battle.session.send(EntityPresencePacket(entityId = battle.charId, status = PRESENCE_OVERWORLD))
  }

  fun sendBattleEnd(battle: BattleInstance, party: List<Pokemon>, prizeMoney: Int = 0) {
    // The defeat speech only plays on a VICTORY end - prize money is the marker for one, and
    // a loss/flee must not show the trainer's beaten line.
    val defeatText = if (prizeMoney > 0) battle.defeatTextId else null
    val partnerText = if (prizeMoney > 0) battle.partnerDefeatTextId else null
    broadcast(battle, BattleBulkStatePacket.battleEnd(prizeMoney, defeatText, partnerText))
    battle.session.send(EntityPresencePacket(entityId = battle.charId, status = PRESENCE_OVERWORLD))
    battle.session.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = party,
        ),
    )
  }

  fun sendVictoryDelta(battle: BattleInstance, entityId: Long, reward: RewardResult) {
    broadcast(
        battle,
        BattleEntityDeltaPacket(
            entityId = entityId,
            experience = Experience(reward.newLevel.toByte(), reward.newXp),
        ),
    )
    // The delta above moves the bar, the reward text reads its number from here.
    broadcast(battle, experienceReward(entityId, reward.xpGained))
    if (!reward.leveled) return
    broadcast(
        battle,
        BattleEntityDeltaPacket(
            entityId = entityId,
            statValues = reward.newStats.asWireList(),
            currentHp = reward.newCurrentHp.toShort(),
            evValues = reward.newEvs.asWireList(),
        ),
    )
  }

  // The other six counters are rewards we do not award yet.
  private fun experienceReward(entityId: Long, gained: Int): BattleStatCountersPacket =
      BattleStatCountersPacket(
          entityId = entityId,
          baseCounter = gained,
          counter1 = null,
          counter2 = null,
          counter3 = null,
          counter4 = null,
          counter5 = null,
          counter6 = null,
      )

  fun sendNotice(battle: BattleInstance, message: String) {
    battle.session.send(notice(message))
  }

  /** The effectiveness line rides in the outcome word rather than in an event of its own. */
  private fun effectivenessBit(effectiveness: Int): Int =
      when {
        effectiveness > TypeChart.NEUTRAL -> SUPER_EFFECTIVE_BIT
        effectiveness in 1..<TypeChart.NEUTRAL -> NOT_VERY_EFFECTIVE_BIT
        else -> 0
      }

  fun broadcast(battle: BattleInstance, packet: Any) {
    interestManager.broadcast(battle.key, packet)
  }

  /** The delta that tells the client a monster's moveset changed. */
  fun moveSlotsDelta(
      entityId: Long,
      moveSlots: List<Pair<Short, Byte>>,
      ppUps: Byte,
  ): BattleEntityDeltaPacket =
      BattleEntityDeltaPacket(entityId = entityId, moves = MoveSlots(moveSlots, ppUps))

  // The decomp battle stat order. Not verified against the live client yet.
  private fun statIndex(stat: BattleStat): Byte =
      when (stat) {
        BattleStat.ATTACK -> 1
        BattleStat.DEFENSE -> 2
        BattleStat.SPEED -> 3
        BattleStat.SP_ATTACK -> 4
        BattleStat.SP_DEFENSE -> 5
        BattleStat.ACCURACY -> 6
        BattleStat.EVASION -> 7
      }
}

// The decomp in-game stat order. Not verified against the live client yet.
private fun statOrder(hp: Int, atk: Int, def: Int, spd: Int, spAtk: Int, spDef: Int): List<Short> =
    listOf(
        hp.toShort(), atk.toShort(), def.toShort(), spd.toShort(), spAtk.toShort(), spDef.toShort())

internal fun ComputedStats.asWireList(): List<Short> = statOrder(hp, atk, def, spd, spAtk, spDef)

private fun EVs.asWireList(): List<Short> = statOrder(hp, atk, def, spd, spAtk, spDef)
