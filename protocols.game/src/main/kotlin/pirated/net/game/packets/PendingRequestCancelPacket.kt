package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class PendingRequestCancelPacket(
    val requestEntityId: Long,
)

object PendingRequestCancelPacketCodec : PacketCodec<PendingRequestCancelPacket>() {
  override fun CodecScope<PendingRequestCancelPacket>.body(): PendingRequestCancelPacket {
    val requestEntityId = field(S64LE) { it.requestEntityId }
    return PendingRequestCancelPacket(requestEntityId)
  }
}
