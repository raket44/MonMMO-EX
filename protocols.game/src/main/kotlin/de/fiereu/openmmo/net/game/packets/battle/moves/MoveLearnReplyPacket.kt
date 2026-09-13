package de.fiereu.openmmo.net.game.packets.battle.moves

import de.fiereu.bytecodec.*

/**
 * c2s 0x0A - the move-learn screen's answer, written by client r32645's `f/th8.n3` from
 * `f/du5.fE`: the monster uid, a u8 count and the monster's final move per slot (a new move sits in
 * the slot it takes over), then a u8 count and the offered moves echoed back. Confirm and Skip send
 * the same packet; after Skip the moveset simply comes back unchanged.
 */
data class MoveLearnReplyPacket(
    val entityId: Long,
    val moveIds: List<Short>,
    val offered: List<Short>,
)

object MoveLearnReplyPacketCodec : PacketCodec<MoveLearnReplyPacket>() {
  override fun CodecScope<MoveLearnReplyPacket>.body(): MoveLearnReplyPacket {
    val entityId = field(S64LE) { it.entityId }
    val moveIds = field(S16LE.listPrefixed(U8)) { it.moveIds }
    val offered = field(S16LE.listPrefixed(U8)) { it.offered }
    return MoveLearnReplyPacket(entityId, moveIds, offered)
  }
}
