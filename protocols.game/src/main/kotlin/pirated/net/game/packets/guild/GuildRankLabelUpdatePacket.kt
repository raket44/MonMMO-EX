package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.U8
import pirated.bytecodec.Utf16LeNullTerminated

data class GuildRankLabelUpdatePacket(
    val rankOrdinal: Int,
    val rankLabel: String,
)

object GuildRankLabelUpdatePacketCodec : PacketCodec<GuildRankLabelUpdatePacket>() {
  override fun CodecScope<GuildRankLabelUpdatePacket>.body(): GuildRankLabelUpdatePacket {
    val rankOrdinal = field(U8) { it.rankOrdinal }
    val rankLabel = field(Utf16LeNullTerminated) { it.rankLabel }
    return GuildRankLabelUpdatePacket(rankOrdinal, rankLabel)
  }
}
