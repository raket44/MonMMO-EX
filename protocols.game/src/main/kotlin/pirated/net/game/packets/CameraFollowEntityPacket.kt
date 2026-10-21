package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class CameraFollowEntityPacket(
    val entityId: Long,
)

object CameraFollowEntityPacketCodec : PacketCodec<CameraFollowEntityPacket>() {
  override fun CodecScope<CameraFollowEntityPacket>.body(): CameraFollowEntityPacket {
    val entityId = field(S64LE) { it.entityId }
    return CameraFollowEntityPacket(entityId)
  }
}
