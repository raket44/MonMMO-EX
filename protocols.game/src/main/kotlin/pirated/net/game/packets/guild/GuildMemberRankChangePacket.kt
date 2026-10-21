package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class GuildMemberRankChangePacket(
    val memberId: Long,
    val rank: Byte,
)

object GuildMemberRankChangePacketCodec : PacketCodec<GuildMemberRankChangePacket>() {
  override fun CodecScope<GuildMemberRankChangePacket>.body(): GuildMemberRankChangePacket {
    field(S64LE) { 0L }
    val memberId = field(S64LE, GuildMemberRankChangePacket::memberId)
    val rank = field(S8, GuildMemberRankChangePacket::rank)
    return GuildMemberRankChangePacket(memberId, rank)
  }
}
