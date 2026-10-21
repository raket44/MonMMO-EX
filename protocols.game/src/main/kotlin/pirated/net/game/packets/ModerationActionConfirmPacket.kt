package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class ModerationActionConfirmPacket(
    val targetEntityId: Long,
)

object ModerationActionConfirmPacketCodec : PacketCodec<ModerationActionConfirmPacket>() {
  override fun CodecScope<ModerationActionConfirmPacket>.body(): ModerationActionConfirmPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return ModerationActionConfirmPacket(targetEntityId)
  }
}
