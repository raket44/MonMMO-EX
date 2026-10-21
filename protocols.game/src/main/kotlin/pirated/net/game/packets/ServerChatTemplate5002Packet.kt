package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.Utf16LeNullTerminated

data class ServerChatTemplate5002Packet(
    val argument: String,
)

object ServerChatTemplate5002PacketCodec : PacketCodec<ServerChatTemplate5002Packet>() {
  override fun CodecScope<ServerChatTemplate5002Packet>.body(): ServerChatTemplate5002Packet {
    field(S64LE) { 0L }
    val argument = field(Utf16LeNullTerminated) { it.argument }
    return ServerChatTemplate5002Packet(argument)
  }
}
