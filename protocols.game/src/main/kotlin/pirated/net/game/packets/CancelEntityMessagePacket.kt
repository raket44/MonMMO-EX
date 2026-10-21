package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class CancelEntityMessagePacket(
    val entityId: Long,
)

object CancelEntityMessagePacketCodec : PacketCodec<CancelEntityMessagePacket>() {
  override fun CodecScope<CancelEntityMessagePacket>.body(): CancelEntityMessagePacket {
    val entityId = field(S64LE) { it.entityId }
    return CancelEntityMessagePacket(entityId)
  }
}
