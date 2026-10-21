package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class GuildMemberKickPacket(
    val targetEntityId: Long,
)

object GuildMemberKickPacketCodec : PacketCodec<GuildMemberKickPacket>() {
  override fun CodecScope<GuildMemberKickPacket>.body(): GuildMemberKickPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return GuildMemberKickPacket(targetEntityId)
  }
}
