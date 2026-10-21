package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class PartyMemberSelectPacket(val selectionType: Byte, val entityId: Long)

object PartyMemberSelectPacketCodec : PacketCodec<PartyMemberSelectPacket>() {
  override fun CodecScope<PartyMemberSelectPacket>.body(): PartyMemberSelectPacket {
    val selectionType = field(S8) { it.selectionType }
    val entityId = field(S64LE) { it.entityId }
    return PartyMemberSelectPacket(selectionType, entityId)
  }
}
