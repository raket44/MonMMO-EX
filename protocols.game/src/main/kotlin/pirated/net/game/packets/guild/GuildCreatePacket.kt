package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class GuildCreatePacket(
    val guildName: String,
    val guildTag: String,
)

object GuildCreatePacketCodec : PacketCodec<GuildCreatePacket>() {
  override fun CodecScope<GuildCreatePacket>.body(): GuildCreatePacket {
    val guildName = field(Utf16LeNullTerminated) { it.guildName }
    val guildTag = field(Utf16LeNullTerminated) { it.guildTag }
    return GuildCreatePacket(guildName, guildTag)
  }
}
