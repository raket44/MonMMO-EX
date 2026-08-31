package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/**
 * Opcode 0x09 from the client: dragging a monster to another party slot.
 *
 * Decoded from live drags - `01 01 <from u16le> <to u16le> 00` - after the opcode. The slot numbers
 * are one-based, matching how the party reads on screen. The old registration decoded this opcode
 * as a chat message, which is what turned every drag into a dropped session; the chat the client
 * actually sends rides opcode 0x08.
 */
data class PartyReorderPacket(
    val container: Int,
    val op: Int,
    val fromSlot: Int,
    val toSlot: Int,
    val tail: Int,
)

object PartyReorderPacketCodec : PacketCodec<PartyReorderPacket>() {
  override fun CodecScope<PartyReorderPacket>.body(): PartyReorderPacket {
    val container = field(U8, PartyReorderPacket::container)
    val op = field(U8, PartyReorderPacket::op)
    val fromSlot = field(U16LE, PartyReorderPacket::fromSlot)
    val toSlot = field(U16LE, PartyReorderPacket::toSlot)
    val tail = field(U8, PartyReorderPacket::tail)
    return PartyReorderPacket(container, op, fromSlot, toSlot, tail)
  }
}
