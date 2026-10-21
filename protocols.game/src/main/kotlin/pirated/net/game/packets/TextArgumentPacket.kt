package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class TextArgumentPacket(val text: String)

object TextArgumentPacketCodec : PacketCodec<TextArgumentPacket>() {
  override fun CodecScope<TextArgumentPacket>.body(): TextArgumentPacket {
    val text = field(Utf16LeNullTerminated) { it.text }
    return TextArgumentPacket(text)
  }
}
