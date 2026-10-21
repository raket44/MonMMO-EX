package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class EntityActionRequestPacket(
    val targetEntityId: Long,
    val actionType: Short,
    val sourceEntityId: Long,
    val entityId: Long,
)

object EntityActionRequestPacketCodec : PacketCodec<EntityActionRequestPacket>() {
  override fun CodecScope<EntityActionRequestPacket>.body(): EntityActionRequestPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    val actionType = field(S16LE) { it.actionType }
    val sourceEntityId = field(S64LE) { it.sourceEntityId }
    val entityId = field(S64LE) { it.entityId }
    return EntityActionRequestPacket(targetEntityId, actionType, sourceEntityId, entityId)
  }
}
