package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class RenderScreenPacket(val renderScreen: Boolean)

object RenderScreenPacketCodec : PacketCodec<RenderScreenPacket>() {
  override fun CodecScope<RenderScreenPacket>.body(): RenderScreenPacket {
    val renderScreen = field(Bool) { it.renderScreen }
    return RenderScreenPacket(renderScreen)
  }
}
