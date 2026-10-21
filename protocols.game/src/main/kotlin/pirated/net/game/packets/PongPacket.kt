package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class PongPacket(
    val isServerPong: Boolean,
    val timestamp: Long,
)

object PongPacketCodec : PacketCodec<PongPacket>() {
  override fun CodecScope<PongPacket>.body(): PongPacket {
    val isServerPong = field(Bool) { it.isServerPong }
    val timestamp =
        if (isServerPong) {
          field(S64LE) { it.timestamp }
        } else {
          field(S64LE) { if (it.timestamp == -1L) System.currentTimeMillis() else it.timestamp }
        }
    return PongPacket(isServerPong, timestamp)
  }
}
