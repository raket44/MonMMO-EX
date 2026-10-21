package pirated.net.game.packets.guild

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class GuildMemberPresencePacket(
    val memberId: Long,
    val online: Boolean,
)

object GuildMemberPresencePacketCodec : PacketCodec<GuildMemberPresencePacket>() {
  override fun CodecScope<GuildMemberPresencePacket>.body(): GuildMemberPresencePacket {
    val memberId = field(S64LE, GuildMemberPresencePacket::memberId)
    val online = field(Bool, GuildMemberPresencePacket::online)
    return GuildMemberPresencePacket(memberId, online)
  }
}
