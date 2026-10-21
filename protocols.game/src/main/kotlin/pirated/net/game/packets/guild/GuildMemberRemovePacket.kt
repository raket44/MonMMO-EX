package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class GuildMemberRemovePacket(
    val memberId: Long,
)

object GuildMemberRemovePacketCodec : PacketCodec<GuildMemberRemovePacket>() {
  override fun CodecScope<GuildMemberRemovePacket>.body(): GuildMemberRemovePacket {
    field(S64LE) { 0L }
    val memberId = field(S64LE, GuildMemberRemovePacket::memberId)
    return GuildMemberRemovePacket(memberId)
  }
}
