package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class ByteChoicePacket(val value: Byte)

object ByteChoicePacketCodec : PacketCodec<ByteChoicePacket>() {
  override fun CodecScope<ByteChoicePacket>.body(): ByteChoicePacket {
    val value = field(S8) { it.value }
    return ByteChoicePacket(value)
  }
}
