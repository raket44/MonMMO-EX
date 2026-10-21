package pirated.net.game.packets.guild

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class GuildDisbandPacket(
    val initiate: Boolean,
    val ownerEntityId: Long,
)

object GuildDisbandPacketCodec : PacketCodec<GuildDisbandPacket>() {
  override fun CodecScope<GuildDisbandPacket>.body(): GuildDisbandPacket {
    val initiate = field(Bool) { it.initiate }
    val ownerEntityId = field(S64LE) { it.ownerEntityId }
    return GuildDisbandPacket(initiate, ownerEntityId)
  }
}
