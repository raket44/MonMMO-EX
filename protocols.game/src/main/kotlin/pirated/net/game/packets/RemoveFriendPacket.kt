package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class RemoveFriendPacket(val username: String)

object RemoveFriendPacketCodec : PacketCodec<RemoveFriendPacket>() {
  override fun CodecScope<RemoveFriendPacket>.body(): RemoveFriendPacket {
    val username = field(Utf16LeNullTerminated) { it.username }
    return RemoveFriendPacket(username)
  }
}
