package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class RequestSocialProfilePacket(
    val targetId: Long,
)

object RequestSocialProfilePacketCodec : PacketCodec<RequestSocialProfilePacket>() {
  override fun CodecScope<RequestSocialProfilePacket>.body(): RequestSocialProfilePacket {
    val targetId = field(S64LE) { it.targetId }
    return RequestSocialProfilePacket(targetId)
  }
}
