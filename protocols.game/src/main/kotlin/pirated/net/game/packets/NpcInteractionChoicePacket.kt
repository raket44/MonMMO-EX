package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class NpcInteractionChoicePacket(
    val targetId: Long,
)

object NpcInteractionChoicePacketCodec : PacketCodec<NpcInteractionChoicePacket>() {
  override fun CodecScope<NpcInteractionChoicePacket>.body(): NpcInteractionChoicePacket {
    val targetId = field(S64LE) { it.targetId }
    return NpcInteractionChoicePacket(targetId)
  }
}
