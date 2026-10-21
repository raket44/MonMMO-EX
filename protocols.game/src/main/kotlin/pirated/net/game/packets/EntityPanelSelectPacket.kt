package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class EntityPanelSelectPacket(
    val entityId: Long,
)

object EntityPanelSelectPacketCodec : PacketCodec<EntityPanelSelectPacket>() {
  override fun CodecScope<EntityPanelSelectPacket>.body(): EntityPanelSelectPacket {
    val entityId = field(S64LE) { it.entityId }
    return EntityPanelSelectPacket(entityId)
  }
}
