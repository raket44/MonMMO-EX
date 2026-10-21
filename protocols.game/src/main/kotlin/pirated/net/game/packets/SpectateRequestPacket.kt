package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class SpectateRequestPacket(
    val targetEntityId: Long,
)

object SpectateRequestPacketCodec : PacketCodec<SpectateRequestPacket>() {
  override fun CodecScope<SpectateRequestPacket>.body(): SpectateRequestPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return SpectateRequestPacket(targetEntityId)
  }
}
