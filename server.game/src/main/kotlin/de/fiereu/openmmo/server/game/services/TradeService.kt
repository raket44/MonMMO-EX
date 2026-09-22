package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.DuelInviteOutcomePacket
import de.fiereu.openmmo.net.game.packets.DuelInvitePacket
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.TradeActionPacket
import de.fiereu.openmmo.net.game.packets.TradeListEntryPacket
import de.fiereu.openmmo.net.game.packets.TradeSelectMonPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleBoardCellPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleEntityDeltaPacket
import de.fiereu.openmmo.net.game.packets.battle.BattlePartySlotSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleStateSlotIntPacket
import de.fiereu.openmmo.net.game.packets.battle.Listing
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Player-to-player trades. Wire decoded from client 31914 (window f/Dt0, session f/nr0), 2026-09-08:
 * - s2c 0x50 (f/jm0) opens the window: flags byte (bit0 = offering allowed, nr0.BY0), my side
 *   (0/1), peer name.
 * - c2s 0x53 (f/uN1: short list, long uid, short quantity) is EVERY offer click, monsters and
 *   items alike (f/JK.lj1): the uid is a party monster's or a bag stack's (`itemId shl 16 or
 *   0x5000`, the id our bag snapshot hands out). A stack above one asks "How many {00} do you
 *   want to trade?" (string 1962) and sends the count; a single one sends 1. Clicking an offered
 *   monster again withdraws it.
 * - The offerer's own monsters show through a container listing into f/Cy 10 (f/Y9 case 10 files
 *   it into nr0.T90[my side]); the peer gets the whole monster as s2c 0x52 (f/PA1 -> T90[other]).
 * - Item offers go out as s2c 0x54 (f/Dr: side, slot, uid, itemId, quantity, flag) to BOTH
 *   players: each client files nr0.fB1[side][slot] and re-renders its bag for its own side.
 * - c2s 0x52 (f/be1: int) is the money box (f/Dt0.Ad -> nr0.Kr0[me]); relayed as s2c 0x53
 *   (f/QE0: side, amount) to both.
 * - c2s 0x50 (f/H02) buttons: 0 cancel, 1 lock, 2 confirm. s2c 0x51 (f/Yd1) state = (code, side)
 *   with code through f/kG1: 1 completed, 2 canceled, 4 LOCKED (nr0.D21), 3 CONFIRMED
 *   (nr0.LpT6). 3 and 4 were the other way round here, so a peer's lock rendered as a confirm and
 *   the Confirm button (enabled once both D21 flags are set) never lit.
 * Trades live only in memory; a disconnect cancels. Nothing moves until both confirm.
 */
@Singleton
class TradeService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
    private val appearance: AppearanceService,
) {

  class ItemOffer(val itemId: Int, var quantity: Int)

  class Trade(val chars: LongArray) {
    val monsters = arrayOf(mutableListOf<Long>(), mutableListOf<Long>())
    val items = arrayOf(mutableListOf<ItemOffer>(), mutableListOf<ItemOffer>())
    val money = IntArray(2)
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

  /**
   * An offer click in the trade window (c2s 0x53, shared with the battle party pick, so the
   * handler asks here first). True when a trade consumed the packet.
   */
  fun onOffer(event: PacketEvent<BattlePartySlotSelectPacket>): Boolean {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return false
    val trade = byChar[charId] ?: return false
    val side = trade.side(charId)
    val packet = event.packet
    val uid = packet.pokemonEntityId
    log.info { "Trade: char=$charId offer click list=${packet.slotIndex} uid=$uid quantity=${packet.contextId}" }
    if (trade.locked[side]) {
      log.info { "Trade: char=$charId is locked, offer ignored" }
      return true
    }
    val stored = characterStore.getCharacter(charId) ?: return true
    val mon = stored.pokemon.firstOrNull { it.id == uid }
    if (mon != null) {
      offerMonster(event.session, trade, side, charId, stored.pokemon, mon)
      return true
    }
    if ((uid and 0xFFFF) == ITEM_ENTITY_TAG) {
      offerItem(trade, side, charId, stored.items, (uid shr 16).toInt(), packet.contextId.toInt())
      return true
    }
    log.info { "Trade: char=$charId offered uid $uid, neither a party monster nor a bag stack" }
    return true
  }

  /**
   * A party drag in or out of the client's trade container (c2s 0x09 PartyReorder, f/Cy 2): the
   * party picker behind the window's party slots sends `party slot -> trade`, taking one back
   * sends `trade index -> party`. Party slots are 0-based party indexes.
   */
  fun onPartyMove(session: SessionContext, charId: Long, fromContainer: Int, fromSlot: Int, toContainer: Int) {
    val trade = byChar[charId]
    if (trade == null) {
      log.info { "Trade: char=$charId dragged $fromContainer:$fromSlot -> $toContainer with no trade open" }
      return
    }
    val side = trade.side(charId)
    if (trade.locked[side]) {
      log.info { "Trade: char=$charId is locked, drag ignored" }
      return
    }
    val party = characterStore.getCharacter(charId)?.pokemon ?: return
    val mon =
        when (fromContainer) {
          PARTY_CONTAINER -> party.getOrNull(fromSlot)
          TRADE_CONTAINER -> trade.monsters[side].getOrNull(fromSlot)?.let { id -> party.firstOrNull { it.id == id } }
          else -> null
        }
    if (mon == null) {
      log.info { "Trade: char=$charId dragged $fromContainer:$fromSlot -> $toContainer, nothing there" }
      return
    }
    val offering = toContainer == TRADE_CONTAINER
    if (offering == (mon.id in trade.monsters[side])) return
    offerMonster(session, trade, side, charId, party, mon)
  }

  private fun offerMonster(session: SessionContext, trade: Trade, side: Int, charId: Long, party: List<Pokemon>, mon: Pokemon) {
    val offered = trade.monsters[side]
    if (mon.id in offered) {
      offered -= mon.id
      log.info { "Trade: char=$charId withdraws monster ${mon.dexId}#${mon.id}" }
      // Back to its party slot: the listing move (f/Y9) takes it off the own trade list.
      session.send(BattleEntityDeltaPacket(entityId = mon.id, listing = Listing(PokemonContainer.PARTY.ordinal.toByte(), mon.containerSlot)))
      return
    }
    if (party.size - offered.size <= 1) {
      log.info { "Trade: char=$charId cannot offer its last party monster" }
      return
    }
    offered += mon.id
    log.info { "Trade: char=$charId offers monster ${mon.dexId}#${mon.id}" }
    // The peer's window places the entry by the record's slot field, so it carries the offer
    // index (0-based), not the party slot the monster came from.
    val index = (offered.size - 1).toShort()
    peerSession(trade, side)?.send(TradeListEntryPacket(mon.copy(containerSlot = index)))
    // The own side of the window: a listing move into the client's trade container.
    session.send(BattleEntityDeltaPacket(entityId = mon.id, listing = Listing(TRADE_LIST_CONTAINER, index)))
  }

  private fun offerItem(trade: Trade, side: Int, charId: Long, bag: Map<Int, Int>, itemId: Int, quantity: Int) {
    val held = bag[itemId] ?: 0
    val offers = trade.items[side]
    val existing = offers.indexOfFirst { it.itemId == itemId }
    val wanted = quantity.coerceIn(0, held)
    if (wanted <= 0) {
      if (existing < 0) {
        log.info { "Trade: char=$charId offered item $itemId x$quantity but holds $held" }
        return
      }
      offers[existing].quantity = 0
      log.info { "Trade: char=$charId withdraws item $itemId" }
      broadcastItem(trade, side, existing, offers[existing])
      return
    }
    val slot =
        if (existing >= 0) {
          offers[existing].quantity = wanted
          existing
        } else {
          offers += ItemOffer(itemId, wanted)
          offers.size - 1
        }
    log.info { "Trade: char=$charId offers item $itemId x$wanted (slot $slot)" }
    broadcastItem(trade, side, slot, offers[slot])
  }

  /** s2c 0x54 to both players: each window files it under [side], the owner re-renders its bag. */
  private fun broadcastItem(trade: Trade, side: Int, slot: Int, offer: ItemOffer) {
    val packet =
        BattleBoardCellPacket(
            row = side.toByte(),
            column = slot.toShort(),
            entityId = (offer.itemId.toLong() shl 16) or ITEM_ENTITY_TAG,
            valueA = offer.itemId.toShort(),
            valueB = offer.quantity.toShort(),
            flags = 0)
    for (charId in trade.chars) sessionRegistry.getByCharacterId(charId)?.send(packet)
  }

  /** c2s 0x52: the money box. Relayed to both windows as s2c 0x53 (side, amount). */
  fun onMoney(event: PacketEvent<TradeSelectMonPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val trade = byChar[charId] ?: return
    val side = trade.side(charId)
    val held = characterStore.getCharacter(charId)?.info?.money ?: 0
    val amount = event.packet.slotIndex.coerceIn(0, held)
    log.info { "Trade: char=$charId offers money ${event.packet.slotIndex} (holds $held) -> $amount locked=${trade.locked[side]}" }
    if (trade.locked[side]) return
    trade.money[side] = amount
    val packet = BattleStateSlotIntPacket(side.toByte(), amount)
    for (id in trade.chars) sessionRegistry.getByCharacterId(id)?.send(packet)
  }

  suspend fun onAction(event: PacketEvent<TradeActionPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val trade = byChar[charId] ?: return
    val side = trade.side(charId)
    log.info { "Trade: char=$charId action ${event.packet.action} (locked=${trade.locked[side]} confirmed=${trade.confirmed[side]})" }
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
      give[side] += trade.monsters[side].mapNotNull { id -> party.firstOrNull { it.id == id } }
    }
    // Monsters: every departure first (removePokemon renumbers the giver's party), then each
    // arrival into a FREE slot - the party's next index, or the PC's next slot. Landing on slot 0
    // regardless collided with the receiver's own slot 0 and the write failed (2026-09-08).
    val moving = arrayOf(mutableListOf<Pokemon>(), mutableListOf<Pokemon>())
    for (side in 0..1) {
      for (mon in give[side]) if (characterStore.removePokemon(trade.chars[side], mon.id)) moving[side] += mon
    }
    for (side in 0..1) {
      val from = trade.chars[side]
      val to = trade.chars[1 - side]
      for (mon in moving[side]) {
        val receiver = characterStore.getCharacter(to)
        val party = receiver?.pokemon.orEmpty()
        val room = party.size < de.fiereu.openmmo.common.MAX_PARTY_SIZE
        val slot =
            if (room) party.size
            else (receiver?.pcStorage.orEmpty().maxOfOrNull { it.containerSlot.toInt() } ?: -1) + 1
        val moved =
            mon.copy(
                ownerId = to,
                container = if (room) PokemonContainer.PARTY else PokemonContainer.PC,
                containerSlot = slot.toShort())
        if (!characterStore.addPokemon(to, moved)) {
          log.error { "Trade: monster ${mon.dexId}#${mon.id} could not be added to $to; returning it to ${trade.chars[side]}" }
          val back = (characterStore.getCharacter(from)?.pcStorage.orEmpty().maxOfOrNull { it.containerSlot.toInt() } ?: -1) + 1
          characterStore.addPokemon(from, mon.copy(container = PokemonContainer.PC, containerSlot = back.toShort()))
        }
      }
      for (offer in trade.items[side]) {
        if (offer.quantity <= 0) continue
        // The giver's stack is checked again at the moment of transfer; a short stack moves nothing.
        if (!characterStore.addItem(from, offer.itemId, -offer.quantity)) {
          log.info { "Trade: char=$from no longer holds ${offer.quantity} of item ${offer.itemId}" }
          continue
        }
        characterStore.addItem(to, offer.itemId, offer.quantity)
        // A cosmetic the giver was wearing comes off with the item (the bag cannot render a worn
        // cosmetic it does not hold).
        appearance.dropUnownedWorn(from, sessionRegistry.getByCharacterId(from))
      }
      val money = trade.money[side]
      if (money > 0 && characterStore.addMoney(from, -money)) characterStore.addMoney(to, money)
    }
    log.info {
      "Trade completed: ${trade.chars[0]} gave ${give[0].size} monsters, ${trade.items[0].sumOf { it.quantity }} items, $${trade.money[0]}; " +
          "${trade.chars[1]} gave ${give[1].size} monsters, ${trade.items[1].sumOf { it.quantity }} items, $${trade.money[1]}"
    }
    broadcast(trade, STATE_COMPLETED, 0)
    for (side in 0..1) {
      val charId = trade.chars[side]
      byChar.remove(charId)
      characterStore.flushCharacterAsync(charId)
      val ctx = sessionRegistry.getByCharacterId(charId) ?: continue
      val stored = characterStore.getCharacter(charId) ?: continue
      val party = stored.pokemon
      ctx.send(PokemonContainerPacket(container = PokemonContainer.PARTY, hasChange = true, delete = false, pokemon = party))
      storyItemStacksPackets(stored).forEach { p -> ctx.send(p) }
      ctx.send(LocalCharacterDeltaPacket(money = stored.info.money))
      // Trade evolutions: what this side just received evolves now, the way the cartridges do it
      // right after the trade, through the same cancellable prompt as any other evolution.
      val state = ctx.attributes[PLAYER_STATE] ?: continue
      val received = give[1 - side]
      val sentAway = give[side].map { clientSpeciesId(it.dexId) }
      for (arrived in received) {
        val inParty = party.firstOrNull { it.id == arrived.id } ?: continue
        val target = EvolutionTable.tradeEvolution(clientSpeciesId(inParty.dexId), inParty.heldItem, sentAway) ?: continue
        log.info { "Trade evolution: char=$charId monster=${inParty.id} dex=${inParty.dexId} -> wire $target" }
        val usesHeldItem = EvolutionTable.tradeEvolutionUsesHeldItem(clientSpeciesId(inParty.dexId), inParty.heldItem, sentAway)
        promptEvolution(ctx, state, inParty, target, consumeHeldItem = usesHeldItem)
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
    /**
     * s2c 0x50 flags: bit0 -> nr0.BY0 enables the bag lists and the money box (f/Dt0 KT0 gates),
     * bit1 -> nr0.cOM2 enables the party slots up top (f/Dt0 E0[side][i].dA(lD())). With only
     * bit0 the circles never opened the party picker (2026-09-08).
     */
    const val OPEN_FLAGS: Byte = 3
    /**
     * The client's trade list container on the wire: f/Cy ZV1 = 2. (f/Y9's `case 10` is a compiler
     * switch-map index - f/YZ1.MZ[ZV1] = 10 - not a container id; sending 10 filed the monster into
     * an unrelated list and the own side stayed empty, 2026-09-08.)
     */
    const val TRADE_LIST_CONTAINER: Byte = 2
    /** Client drag-packet container ids (f/Cy): party and the trade window's own list. */
    const val PARTY_CONTAINER = 1
    const val TRADE_CONTAINER = 2
    const val ACTION_CANCEL = 0
    const val ACTION_LOCK = 1
    const val ACTION_CONFIRM = 2
    // f/kG1 codes, matched to f/Yd1's switch: 4 sets nr0.D21 (locked), 3 sets nr0.LpT6 (confirmed).
    const val STATE_COMPLETED = 1
    const val STATE_CANCELED = 2
    const val STATE_CONFIRMED = 3
    const val STATE_LOCKED = 4
  }
}
