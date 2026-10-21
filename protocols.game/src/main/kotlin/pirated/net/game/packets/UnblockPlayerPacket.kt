package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class UnblockPlayerPacket(val username: String)

object UnblockPlayerPacketCodec : PacketCodec<UnblockPlayerPacket>() {
  override fun CodecScope<UnblockPlayerPacket>.body(): UnblockPlayerPacket {
    val username = field(Utf16LeNullTerminated) { it.username }
    return UnblockPlayerPacket(username)
  }
}
