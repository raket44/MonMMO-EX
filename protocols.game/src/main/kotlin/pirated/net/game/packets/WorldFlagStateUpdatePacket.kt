package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S8

data class WorldFlagStateUpdatePacket(
    val key: Byte,
    val state: Byte,
    val value: Int,
)

object WorldFlagStateUpdatePacketCodec : PacketCodec<WorldFlagStateUpdatePacket>() {
  override fun CodecScope<WorldFlagStateUpdatePacket>.body(): WorldFlagStateUpdatePacket {
    val key = field(S8) { it.key }
    val state = field(S8) { it.state }
    val value = field(S32LE) { it.value }
    return WorldFlagStateUpdatePacket(key, state, value)
  }
}
