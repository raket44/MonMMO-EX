package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.Utf16LeNullTerminated

data class EntityRenamePacket(
    val entityId: Long,
    val name: String,
)

object EntityRenamePacketCodec : PacketCodec<EntityRenamePacket>() {
  override fun CodecScope<EntityRenamePacket>.body(): EntityRenamePacket {
    val entityId = field(S64LE) { it.entityId }
    val name = field(Utf16LeNullTerminated) { it.name }
    return EntityRenamePacket(entityId, name)
  }
}
