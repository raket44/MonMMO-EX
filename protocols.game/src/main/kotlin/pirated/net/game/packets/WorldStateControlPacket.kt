package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S8

data class WorldStateControlPacket(
    val actionType: Byte,
    val value: Int,
)

object WorldStateControlPacketCodec : PacketCodec<WorldStateControlPacket>() {
  override fun CodecScope<WorldStateControlPacket>.body(): WorldStateControlPacket {
    val actionType = field(S8) { it.actionType }
    val value = field(S32LE) { it.value }
    return WorldStateControlPacket(actionType, value)
  }
}
