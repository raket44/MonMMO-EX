package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class PlayerChatInputPacket(
    val message: String,
)

object PlayerChatInputPacketCodec : PacketCodec<PlayerChatInputPacket>() {
  override fun CodecScope<PlayerChatInputPacket>.body(): PlayerChatInputPacket {
    val message = field(Utf16LeNullTerminated) { it.message }
    return PlayerChatInputPacket(message)
  }
}
