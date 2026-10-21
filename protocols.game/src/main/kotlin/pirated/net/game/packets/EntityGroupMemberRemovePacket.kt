package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class EntityGroupMemberRemovePacket(
    val removedId: Long,
    val leaderId: Long,
)

object EntityGroupMemberRemovePacketCodec : PacketCodec<EntityGroupMemberRemovePacket>() {
  override fun CodecScope<EntityGroupMemberRemovePacket>.body(): EntityGroupMemberRemovePacket {
    val removedId = field(S64LE) { it.removedId }
    val leaderId = field(S64LE) { it.leaderId }
    return EntityGroupMemberRemovePacket(removedId, leaderId)
  }
}
