package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class EntityLeavePacket(val entityId: Long)

object EntityLeavePacketCodec : PacketCodec<EntityLeavePacket>() {
  override fun CodecScope<EntityLeavePacket>.body(): EntityLeavePacket {
    val entityId = field(S64LE) { it.entityId }
    return EntityLeavePacket(entityId)
  }
}
