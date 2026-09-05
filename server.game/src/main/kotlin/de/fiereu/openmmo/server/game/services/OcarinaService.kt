package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.EntityCoordSyncPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Move Ocarinas (bag items 1179-1198): each "summons" a stand-in monster that uses one field move
 * outside of battle. The client (f/Lh1.KM0, f/TT) keys the item's Spawn button on a summon record:
 * a monster it knows in the summon container (wire container 8) plus the 0x61 status packet
 * (entity, cooldown, summoned flag) that registers it (f/ln1.S8 -> f/os0). Pressing the button is
 * the ordinary use-item packet (0x26) with the ocarina's id.
 *
 * Uses are the stand-in's move pp: a maxed Sweet Scent holds 32, a use costs 5 (six uses), and a
 * Pokemon Center or an opened PC refills it. The pp spent lives in a story var per ocarina.
 */
@Singleton
class OcarinaService
@Inject
constructor(
    private val characters: CharacterStore,
    private val wildMons: WildMonFactory,
    private val encounters: javax.inject.Provider<EncounterService>,
    private val maps: MapManager,
    private val ids: EntityIdService,
    private val emitter: BattlePacketEmitter,
) {

  fun isOcarina(itemId: Int): Boolean = itemId in OCARINA_MOVES

  /** Sends the stand-ins for every owned ocarina and registers each as a summon. */
  fun sendSummons(ctx: SessionContext, stored: StoredCharacter) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val owned = stored.items.keys.filter { isOcarina(it) }.sorted()
    if (owned.isEmpty()) return
    state.ocarinaSummons.clear()
    val summons =
        owned.mapIndexed { index, itemId ->
          val moveId = OCARINA_MOVES.getValue(itemId)
          val species = summonSpecies(state.regionId, moveId)
          val base = wildMons.create(species, SUMMON_LEVEL, BattleRng()) ?: return
          val id = ids.newMonsterId()
          state.ocarinaSummons[itemId] = id
          base.copy(
              id = id,
              ownerId = stored.info.id,
              container = SUMMON_CONTAINER,
              containerSlot = index.toShort(),
              moves = moveSlots(moveId, remainingPp(stored, itemId)),
          )
        }
    ctx.send(PokemonContainerPacket(container = SUMMON_CONTAINER, hasChange = true, delete = false, pokemon = summons))
    for (summon in summons) ctx.send(EntityCoordSyncPacket(summon.id, 0, false, 0))
    log.info { "[Ocarina] char=${stored.info.id} summons ${owned.joinToString()} -> ${summons.map { it.dexId }}" }
  }

  /** The bag's use of an ocarina: the stand-in uses its move here, spending pp. */
  suspend fun use(ctx: SessionContext, state: PlayerState, charId: Long, itemId: Int) {
    val stored = characters.getCharacter(charId) ?: return
    val moveId = OCARINA_MOVES[itemId] ?: return
    val remaining = remainingPp(stored, itemId)
    if (remaining < USE_COST) {
      ctx.send(notice("${name(itemId)} has no uses left. Heal at a Pokemon Center or open a PC to refill it."))
      return
    }
    val problem =
        when (moveId) {
          SWEET_SCENT -> encounters.get().startHorde(ctx, charId, state, maps.getMap(state.regionId, state.bankId, state.mapId), HORDE_SIZE)
          else -> "${name(itemId)} is not wired up yet."
        }
    if (problem != null) {
      ctx.send(notice(problem))
      return
    }
    characters.setStoryVar(charId, key(itemId), MAX_PP - remaining + USE_COST)
    val left = remaining - USE_COST
    sendPp(ctx, state, itemId, moveId, left)
    ctx.send(notice("${name(itemId)}: ${left / USE_COST} uses left."))
  }

  /** A Pokemon Center heal or an opened PC restores every ocarina. */
  fun refill(ctx: SessionContext, charId: Long) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val stored = characters.getCharacter(charId) ?: return
    for (itemId in stored.items.keys.filter { isOcarina(it) }) {
      if (remainingPp(stored, itemId) >= MAX_PP) continue
      characters.setStoryVar(charId, key(itemId), 0)
      sendPp(ctx, state, itemId, OCARINA_MOVES.getValue(itemId), MAX_PP)
      ctx.send(notice("${name(itemId)} refilled."))
    }
  }

  private fun remainingPp(stored: StoredCharacter, itemId: Int): Int =
      (MAX_PP - (stored.storyVars[key(itemId)] ?: 0)).coerceIn(0, MAX_PP)

  private fun sendPp(ctx: SessionContext, state: PlayerState, itemId: Int, moveId: Int, pp: Int) {
    val id = state.ocarinaSummons[itemId] ?: return
    ctx.send(emitter.moveSlotsDelta(id, moveSlots(moveId, pp).map { it.id to it.pp }, PP_UPS))
  }

  private fun moveSlots(moveId: Int, pp: Int): List<PokemonMove> =
      listOf(PokemonMove(moveId.toShort(), pp.toByte())) + List(3) { PokemonMove(0, 0) }

  private fun key(itemId: Int) = "ocarina/$itemId/spent"

  private fun name(itemId: Int) = OCARINA_NAMES[itemId] ?: "Ocarina $itemId"

  /** The stand-in each region summons for a move; a dummy, but the one the client shows. */
  private fun summonSpecies(regionId: Int, moveId: Int): Int =
      when (moveId) {
        SWEET_SCENT ->
            when (regionId) {
              1 -> 357 // Tropius
              4 -> 216 // Teddiursa
              3 -> 420 // Cherubi
              2 -> 546 // Cottonee
              else -> 44 // Gloom
            }
        else -> 63 // Abra
      }

  private companion object {
    const val SWEET_SCENT = 230
    const val MAX_PP = 32
    const val USE_COST = 5
    /** Three pp ups on the first slot, the way a maxed Sweet Scent reaches 32. */
    const val PP_UPS: Byte = 3
    const val SUMMON_LEVEL = 50
    const val HORDE_SIZE = 5
    /**
     * Wire container 11: the client's summon shelf (f/Cy.gw1, 60 cells). The container packet's
     * handler (f/yT0.X91) only takes the ordinals in its switch, and 8 is not one of them.
     */
    val SUMMON_CONTAINER = PokemonContainer.entries[11]
    /** Sweet Scent is the one confirmed in game; the rest of the block awaits its own move ids. */
    val OCARINA_MOVES = mapOf(1179 to SWEET_SCENT)
    val OCARINA_NAMES = mapOf(1179 to "Sweet Scent Ocarina")
  }
}
