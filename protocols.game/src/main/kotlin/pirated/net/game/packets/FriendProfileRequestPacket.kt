package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class FriendProfileRequestPacket(val targetEntityId: Long)

object FriendProfileRequestPacketCodec : PacketCodec<FriendProfileRequestPacket>() {
  override fun CodecScope<FriendProfileRequestPacket>.body(): FriendProfileRequestPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return FriendProfileRequestPacket(targetEntityId)
  }
}
