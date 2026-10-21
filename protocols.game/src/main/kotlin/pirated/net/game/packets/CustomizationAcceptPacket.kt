package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class CustomizationAcceptPacket(val entityId: Long, val verificationToken: Long)

object CustomizationAcceptPacketCodec : PacketCodec<CustomizationAcceptPacket>() {
  override fun CodecScope<CustomizationAcceptPacket>.body(): CustomizationAcceptPacket {
    val entityId = field(S64LE) { it.entityId }
    val verificationToken = field(S64LE) { it.verificationToken }
    return CustomizationAcceptPacket(entityId, verificationToken)
  }
}
