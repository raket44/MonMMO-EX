package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class WorldFlagSetPacket(
    val group: Byte,
    val index: Short,
    val value: Byte,
)

object WorldFlagSetPacketCodec : PacketCodec<WorldFlagSetPacket>() {
  override fun CodecScope<WorldFlagSetPacket>.body(): WorldFlagSetPacket {
    val group = field(S8) { it.group }
    val index = field(S16LE) { it.index }
    val value = field(S8) { it.value }
    return WorldFlagSetPacket(group, index, value)
  }
}
