package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class SocialListEntryRemovePacket(
    val listType: Byte,
    val entityId: Long,
)

object SocialListEntryRemovePacketCodec : PacketCodec<SocialListEntryRemovePacket>() {
  override fun CodecScope<SocialListEntryRemovePacket>.body(): SocialListEntryRemovePacket {
    val listType = field(S8) { it.listType }
    val entityId = field(S64LE) { it.entityId }
    return SocialListEntryRemovePacket(listType, entityId)
  }
}
