package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class ViewScalePacket(
    val viewScale: Byte,
)

object ViewScalePacketCodec : PacketCodec<ViewScalePacket>() {
  override fun CodecScope<ViewScalePacket>.body(): ViewScalePacket {
    val viewScale = field(S8) { it.viewScale }
    return ViewScalePacket(viewScale)
  }
}
