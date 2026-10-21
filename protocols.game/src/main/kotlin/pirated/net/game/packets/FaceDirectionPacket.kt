package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.enumByOrdinalByte
import pirated.common.enums.Direction

data class FaceDirectionPacket(val direction: Direction)

object FaceDirectionPacketCodec : PacketCodec<FaceDirectionPacket>() {
  private val DirectionCodec = enumByOrdinalByte<Direction>()

  override fun CodecScope<FaceDirectionPacket>.body(): FaceDirectionPacket {
    val direction = field(DirectionCodec) { it.direction }
    return FaceDirectionPacket(direction)
  }
}
