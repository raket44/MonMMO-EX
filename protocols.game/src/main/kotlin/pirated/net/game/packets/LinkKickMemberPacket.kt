package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class LinkKickMemberPacket(
    val targetEntityId: Long,
)

object LinkKickMemberPacketCodec : PacketCodec<LinkKickMemberPacket>() {
  override fun CodecScope<LinkKickMemberPacket>.body(): LinkKickMemberPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return LinkKickMemberPacket(targetEntityId)
  }
}
