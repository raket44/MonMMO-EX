package de.fiereu.openmmo.net.game.packets.battle.moves

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.S8

/**
 * c2s 0x0A - the forget dialog's answer, written by client 31914's `f/com4`: the monster uid, the
 * slot byte, the offered move id. The dialog's slot pick (`f/mB.Z90`) sends the slot the new move
 * takes over; giving up sends a negative slot, and the monster keeps its moves.
 */
data class MoveLearnReplyPacket(
    val entityId: Long,
    /** 0-3 = replace that slot with [moveId]; negative = declined. */
    val slot: Byte,
    val moveId: Short,
)

object MoveLearnReplyPacketCodec : PacketCodec<MoveLearnReplyPacket>() {
  override fun CodecScope<MoveLearnReplyPacket>.body(): MoveLearnReplyPacket {
    val entityId = field(S64LE) { it.entityId }
    val slot = field(S8) { it.slot }
    val moveId = field(S16LE) { it.moveId }
    return MoveLearnReplyPacket(entityId, slot, moveId)
  }
}
