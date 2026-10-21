package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class QueuePositionPacket(
    val position: Short,
    val total: Short,
)

object QueuePositionPacketCodec : PacketCodec<QueuePositionPacket>() {
  override fun CodecScope<QueuePositionPacket>.body(): QueuePositionPacket {
    val position = field(S16LE) { it.position }
    val total = field(S16LE) { it.total }
    return QueuePositionPacket(position, total)
  }
}
