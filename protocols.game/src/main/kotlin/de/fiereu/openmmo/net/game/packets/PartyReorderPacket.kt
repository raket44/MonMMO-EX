package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.U8
import de.fiereu.bytecodec.listPrefixed

/**
 * One monster move inside a container drag: where it came from and where it was dropped. Slots are
 * zero-based; the container byte is the client's `f/Cy` ordinal (1 = party).
 */
data class PartyMove(
    val fromContainer: Int,
    val fromSlot: Int,
    val toContainer: Int,
    val toSlot: Int,
)

/**
 * c2s 0x09 - dragging monsters between slots, written by client 31914's `f/xk`: a u8 count, then
 * for every move the source `[container u8, slot s16]` followed by the destination
 * `[container u8, slot s16]`.
 *
 * The earlier codec read the count as a "container", the source container as an "op", and the
 * destination container byte plus the low byte of the destination slot as the target - so a drag
 * from slot 1 to slot 0 arrived as "1 -> 1" and was rejected as a no-op.
 */
data class PartyReorderPacket(val moves: List<PartyMove>)

private object PartyMoveCodec : PacketCodec<PartyMove>() {
  override fun CodecScope<PartyMove>.body(): PartyMove {
    val fromContainer = field(U8) { it.fromContainer }
    val fromSlot = field(S16LE) { it.fromSlot.toShort() }.toInt()
    val toContainer = field(U8) { it.toContainer }
    val toSlot = field(S16LE) { it.toSlot.toShort() }.toInt()
    return PartyMove(fromContainer, fromSlot, toContainer, toSlot)
  }
}

object PartyReorderPacketCodec : PacketCodec<PartyReorderPacket>() {
  override fun CodecScope<PartyReorderPacket>.body(): PartyReorderPacket {
    val moves = field(PartyMoveCodec.listPrefixed(U8)) { it.moves }
    return PartyReorderPacket(moves)
  }
}
