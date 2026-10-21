package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.U8

data class GuildMemberRankAssignPacket(
    val memberEntityId: Long,
    val rankOrdinal: Int,
)

object GuildMemberRankAssignPacketCodec : PacketCodec<GuildMemberRankAssignPacket>() {
  override fun CodecScope<GuildMemberRankAssignPacket>.body(): GuildMemberRankAssignPacket {
    val memberEntityId = field(S64LE) { it.memberEntityId }
    val rankOrdinal = field(U8) { it.rankOrdinal }
    return GuildMemberRankAssignPacket(memberEntityId, rankOrdinal)
  }
}
