package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class OverworldStepMovePacket(
    val targetX: Byte,
    val targetY: Byte,
)

object OverworldStepMovePacketCodec : PacketCodec<OverworldStepMovePacket>() {
  override fun CodecScope<OverworldStepMovePacket>.body(): OverworldStepMovePacket {
    val targetX = field(S8) { it.targetX }
    val targetY = field(S8) { it.targetY }
    return OverworldStepMovePacket(targetX, targetY)
  }
}
