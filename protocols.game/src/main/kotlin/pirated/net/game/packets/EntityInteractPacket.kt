package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class EntityInteractPacket(val entityId: Long, val token: Long)

object EntityInteractPacketCodec : PacketCodec<EntityInteractPacket>() {
  override fun CodecScope<EntityInteractPacket>.body(): EntityInteractPacket {
    val entityId = field(S64LE) { it.entityId }
    val token = field(S64LE) { it.token }
    return EntityInteractPacket(entityId, token)
  }
}
