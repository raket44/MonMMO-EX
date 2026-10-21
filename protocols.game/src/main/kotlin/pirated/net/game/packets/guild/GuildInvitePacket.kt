package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class GuildInvitePacket(
    val targetName: String,
)

object GuildInvitePacketCodec : PacketCodec<GuildInvitePacket>() {
  override fun CodecScope<GuildInvitePacket>.body(): GuildInvitePacket {
    val targetName = field(Utf16LeNullTerminated) { it.targetName }
    return GuildInvitePacket(targetName)
  }
}
