package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.U16LE
import pirated.bytecodec.bytesPrefixed

data class ChunkedTransferAppendPacket(
    val data: ByteArray,
) {
  override fun equals(other: Any?): Boolean =
      other is ChunkedTransferAppendPacket && data.contentEquals(other.data)

  override fun hashCode(): Int = data.contentHashCode()
}

object ChunkedTransferAppendPacketCodec : PacketCodec<ChunkedTransferAppendPacket>() {
  override fun CodecScope<ChunkedTransferAppendPacket>.body(): ChunkedTransferAppendPacket {
    val data = field(bytesPrefixed(U16LE)) { it.data }
    return ChunkedTransferAppendPacket(data)
  }
}
