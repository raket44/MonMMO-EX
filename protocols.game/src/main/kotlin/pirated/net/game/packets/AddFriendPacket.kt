package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class AddFriendPacket(val username: String)

object AddFriendPacketCodec : PacketCodec<AddFriendPacket>() {
  override fun CodecScope<AddFriendPacket>.body(): AddFriendPacket {
    val username = field(Utf16LeNullTerminated) { it.username }
    return AddFriendPacket(username)
  }
}
