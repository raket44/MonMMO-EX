package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class OpenAppearanceEditorPacket(
    val entityId: Long,
)

object OpenAppearanceEditorPacketCodec : PacketCodec<OpenAppearanceEditorPacket>() {
  override fun CodecScope<OpenAppearanceEditorPacket>.body(): OpenAppearanceEditorPacket {
    val entityId = field(S64LE) { it.entityId }
    return OpenAppearanceEditorPacket(entityId)
  }
}
