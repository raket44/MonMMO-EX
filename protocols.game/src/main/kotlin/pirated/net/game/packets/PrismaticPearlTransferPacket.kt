package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class PrismaticPearlTransferPacket(
    val sourceEntityId: Long,
    val targetEntityId: Long,
)

object PrismaticPearlTransferPacketCodec : PacketCodec<PrismaticPearlTransferPacket>() {
  override fun CodecScope<PrismaticPearlTransferPacket>.body(): PrismaticPearlTransferPacket {
    val sourceEntityId = field(S64LE) { it.sourceEntityId }
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return PrismaticPearlTransferPacket(sourceEntityId, targetEntityId)
  }
}
