package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.U8

data class NpcAnimationPacket(val entityId: Long, val animation: Int)

object NpcAnimationPacketCodec : PacketCodec<NpcAnimationPacket>() {
  override fun CodecScope<NpcAnimationPacket>.body(): NpcAnimationPacket {
    val entityId = field(S64LE) { it.entityId }
    val animation = field(U8) { it.animation }
    return NpcAnimationPacket(entityId, animation)
  }
}
