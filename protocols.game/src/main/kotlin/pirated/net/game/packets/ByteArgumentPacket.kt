package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class ByteArgumentPacket(val value: Byte)

object ByteArgumentPacketCodec : PacketCodec<ByteArgumentPacket>() {
  override fun CodecScope<ByteArgumentPacket>.body(): ByteArgumentPacket {
    val value = field(S8) { it.value }
    return ByteArgumentPacket(value)
  }
}
