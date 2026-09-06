package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.DuelInviteOutcomePacket
import de.fiereu.openmmo.net.game.packets.DuelInvitePacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.TradeActionPacket
import de.fiereu.openmmo.net.game.packets.TradeListEntryPacket
import de.fiereu.openmmo.net.game.packets.TradeSelectMonPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Player-to-player trades. Bytecode-verified wire (client 31914):
 * - s2c 0x50 (f/jm0) opens the trade window f/Dt0: flags byte (bit0 must be set or the window
 *   refuses to offer anything - f/Dt0.Ad checks nr0.BY0), my side (0/1), peer name.
 * - c2s 0x52 (f/be1) offers the party monster in a slot; the server relays it to the peer as
 *   s2c 0x52 (f/PA1), whose client files it on the OTHER side of its window.
 * - c2s 0x50 (f/H02) is a button: 0 cancel (f/Dt0.Dt1), 1 lock list (UB1, string 1955),
 *   2 confirm (lz1, string 1956).
 * - s2c 0x51 (f/Yd1) is a state change: code byte (f/kG1: 1 completed, 2 canceled, 3 locked,
 *   4 confirmed) and the side it applies to. 3/4 set that side's flag in the window; 1/2 end the
 *   session and print "Trade completed." / "Trade canceled.".
 * Trades live only in memory; a disconnect cancels.
 */
@Singleton
class TradeService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
) {

  class Trade(val chars: LongArray) {
    val offers = arrayOf(mutableListOf<Long>(), mutableListOf<Long>())
    val locked = BooleanArray(2)
    val confirmed = BooleanArray(2)

    fun side(charId: Long): Int = chars.indexOf(charId)
  }

  private val byChar = ConcurrentHashMap<Long, Trade>()

  fun open(requester: Long, requesterName: String, target: Long, targetName: String) {
    if (byChar[requester] != null || byChar[target] != null) return
    val trade = Trade(longArrayOf(requester, target))
    byChar[requester] = trade
    byChar[target] = trade
    sessionRegistry.getByCharacterId(requester)?.send(DuelInvitePacket(OPEN_FLAGS, 0, targetName))
    sessionRegistry.getByCharacterId(target)?.send(DuelInvitePacket(OPEN_FLAGS, 1, requesterName))
    log.info { "Trade opened: '$requesterName' (side 0) <-> '$targetName' (side 1)" }
  }

  fun onSelectMon(event: PacketEvent<TradeSelectMonPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val trade = byChar[charId] ?: return
    val side = trade.side(charId)
    if (trade.locked[side]) return
    val party = characterStore.getCharacter(charId)?.pokemon ?: return
    val slot = event.packet.slotIndex
    val mon = party.firstOrNull { it.containerSlot.toInt() == slot } ?: party.getOrNull(slot) ?: return
    if (mon.id in trade.offers[side]) return
    if (party.size - trade.offers[side].size <= 1) {
      log.info { "Trade: char=$charId cannot offer its last party monster" }
      return
    }
    trade.offers[side] += mon.id
    log.info { "Trade: char=$charId offers ${mon.dexId} (slot $slot)" }
    peerSession(trade, side)?.send(TradeListEntryPacket(mon))
  }

  suspend fun onAction(event: PacketEvent<TradeActionPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val trade = byChar[charId] ?: return
    val side = trade.side(charId)
    when (event.packet.action.toInt()) {
      ACTION_CANCEL -> cancel(trade, "char=$charId canceled")
      ACTION_LOCK -> {
        trade.locked[side] = true
        broadcast(trade, STATE_LOCKED, side)
      }
      ACTION_CONFIRM -> {
        if (!trade.locked[side]) return
        trade.confirmed[side] = true
        broadcast(trade, STATE_CONFIRMED, side)
        if (trade.confirmed[0] && trade.confirmed[1]) complete(trade)
      }
      else -> log.info { "Trade: char=$charId unknown action ${event.packet.action}" }
    }
  }

  fun onDisconnect(charId: Long) {
    val trade = byChar[charId] ?: return
    cancel(trade, "char=$charId disconnected")
  }

  private suspend fun complete(trade: Trade) {
    val give = arrayOf(mutableListOf<Pokemon>(), mutableListOf<Pokemon>())
    for (side in 0..1) {
      val party = characterStore.getCharacter(trade.chars[side])?.pokemon ?: emptyList()
      give[side] += trade.offers[side].mapNotNull { id -> party.firstOrNull { it.id == id } }
    }
    for (side in 0..1) {
      val from = trade.chars[side]
      val to = trade.chars[1 - side]
      for (mon in give[side]) {
        if (!characterStore.removePokemon(from, mon.id)) continue
        val room = (characterStore.getCharacter(to)?.pokemon?.size ?: 6) < 6
        val moved =
            mon.copy(
                ownerId = to,
                container = if (room) PokemonContainer.PARTY else PokemonContainer.PC,
                containerSlot = 0)
        characterStore.addPokemon(to, moved)
      }
    }
    log.info { "Trade completed: ${trade.chars[0]} gave ${give[0].size}, ${trade.chars[1]} gave ${give[1].size}" }
    broadcast(trade, STATE_COMPLETED, 0)
    for (side in 0..1) {
      val charId = trade.chars[side]
      byChar.remove(charId)
      val ctx = sessionRegistry.getByCharacterId(charId) ?: continue
      val party = characterStore.getCharacter(charId)?.pokemon ?: continue
      ctx.send(PokemonContainerPacket(container = PokemonContainer.PARTY, hasChange = true, delete = false, pokemon = party))
      // Trade evolutions: what this side just received evolves now, the way the cartridges do it
      // right after the trade, through the same cancellable prompt as any other evolution.
      val state = ctx.attributes[PLAYER_STATE] ?: continue
      val received = give[1 - side]
      val sentAway = give[side].map { clientSpeciesId(it.dexId) }
      for (arrived in received) {
        val inParty = party.firstOrNull { it.id == arrived.id } ?: continue
        val target = EvolutionTable.tradeEvolution(clientSpeciesId(inParty.dexId), inParty.heldItem, sentAway) ?: continue
        log.info { "Trade evolution: char=$charId monster=${inParty.id} dex=${inParty.dexId} -> wire $target" }
        promptEvolution(ctx, state, inParty, target)
      }
    }
  }

  private fun cancel(trade: Trade, why: String) {
    log.info { "Trade canceled: $why" }
    broadcast(trade, STATE_CANCELED, 0)
    for (charId in trade.chars) byChar.remove(charId)
  }

  private fun broadcast(trade: Trade, code: Int, side: Int) {
    val packet = DuelInviteOutcomePacket(code.toByte(), side.toByte())
    for (charId in trade.chars) sessionRegistry.getByCharacterId(charId)?.send(packet)
  }

  private fun peerSession(trade: Trade, side: Int): SessionContext? =
      sessionRegistry.getByCharacterId(trade.chars[1 - side])

  private companion object {
    const val OPEN_FLAGS: Byte = 1
    const val ACTION_CANCEL = 0
    const val ACTION_LOCK = 1
    const val ACTION_CONFIRM = 2
    const val STATE_COMPLETED = 1
    const val STATE_CANCELED = 2
    const val STATE_LOCKED = 3
    const val STATE_CONFIRMED = 4
  }
}
