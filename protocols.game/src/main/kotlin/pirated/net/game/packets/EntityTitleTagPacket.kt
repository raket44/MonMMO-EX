package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.Utf16LeNullTerminated

data class EntityTitleTagPacket(
    val entityId: Long,
    val nameTag: String,
)

object EntityTitleTagPacketCodec : PacketCodec<EntityTitleTagPacket>() {
  override fun CodecScope<EntityTitleTagPacket>.body(): EntityTitleTagPacket {
    val entityId = field(S64LE) { it.entityId }
    val nameTag = field(Utf16LeNullTerminated) { it.nameTag }
    return EntityTitleTagPacket(entityId, nameTag)
  }
}
