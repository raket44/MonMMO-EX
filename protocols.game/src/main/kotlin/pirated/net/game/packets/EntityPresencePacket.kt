package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class EntityPresencePacket(
    val entityId: Long,
    val status: Byte,
)

object EntityPresencePacketCodec : PacketCodec<EntityPresencePacket>() {
  override fun CodecScope<EntityPresencePacket>.body(): EntityPresencePacket {
    val entityId = field(S64LE) { it.entityId }
    val status = field(S8) { it.status }
    return EntityPresencePacket(entityId, status)
  }
}
