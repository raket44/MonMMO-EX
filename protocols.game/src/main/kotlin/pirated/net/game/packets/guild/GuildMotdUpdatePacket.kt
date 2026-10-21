package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class GuildMotdUpdatePacket(
    val motdText: String,
)

object GuildMotdUpdatePacketCodec : PacketCodec<GuildMotdUpdatePacket>() {
  override fun CodecScope<GuildMotdUpdatePacket>.body(): GuildMotdUpdatePacket {
    val motdText = field(Utf16LeNullTerminated) { it.motdText }
    return GuildMotdUpdatePacket(motdText)
  }
}
