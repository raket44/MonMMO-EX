package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class SceneEntityActionDispatchPacket(
    val entityId: Long,
    val action: Byte,
)

object SceneEntityActionDispatchPacketCodec : PacketCodec<SceneEntityActionDispatchPacket>() {
  override fun CodecScope<SceneEntityActionDispatchPacket>.body(): SceneEntityActionDispatchPacket {
    val entityId = field(S64LE) { it.entityId }
    val action = field(S8) { it.action }
    return SceneEntityActionDispatchPacket(entityId, action)
  }
}
