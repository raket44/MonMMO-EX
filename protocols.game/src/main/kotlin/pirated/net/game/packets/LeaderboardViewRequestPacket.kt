package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class LeaderboardViewRequestPacket(
    val categoryId: Byte,
)

object LeaderboardViewRequestPacketCodec : PacketCodec<LeaderboardViewRequestPacket>() {
  override fun CodecScope<LeaderboardViewRequestPacket>.body(): LeaderboardViewRequestPacket {
    val categoryId = field(S8) { it.categoryId }
    return LeaderboardViewRequestPacket(categoryId)
  }
}
