package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class EntityFaceTurnPacket(
    val entityId: Long,
    val facing: Byte,
)

object EntityFaceTurnPacketCodec : PacketCodec<EntityFaceTurnPacket>() {
  override fun CodecScope<EntityFaceTurnPacket>.body(): EntityFaceTurnPacket {
    val entityId = field(S64LE) { it.entityId }
    val facing = field(S8) { it.facing }
    return EntityFaceTurnPacket(entityId, facing)
  }
}
