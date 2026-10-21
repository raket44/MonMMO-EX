package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class RequestEntityInteractionPacket(
    val entityId: Long,
    val checksum: Long,
)

object RequestEntityInteractionPacketCodec : PacketCodec<RequestEntityInteractionPacket>() {
  override fun CodecScope<RequestEntityInteractionPacket>.body(): RequestEntityInteractionPacket {
    val entityId = field(S64LE) { it.entityId }
    val checksum = field(S64LE) { it.checksum }
    return RequestEntityInteractionPacket(entityId, checksum)
  }
}
