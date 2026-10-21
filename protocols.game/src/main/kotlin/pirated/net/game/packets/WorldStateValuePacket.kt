package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class WorldStateValuePacket(
    val value: Short,
)

object WorldStateValuePacketCodec : PacketCodec<WorldStateValuePacket>() {
  override fun CodecScope<WorldStateValuePacket>.body(): WorldStateValuePacket {
    val value = field(S16LE, WorldStateValuePacket::value)
    return WorldStateValuePacket(value)
  }
}
