package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class ContactDeletePacket(
    val entityId: Long,
)

object ContactDeletePacketCodec : PacketCodec<ContactDeletePacket>() {
  override fun CodecScope<ContactDeletePacket>.body(): ContactDeletePacket {
    val entityId = field(S64LE) { it.entityId }
    return ContactDeletePacket(entityId)
  }
}
