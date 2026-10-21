package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.U16LE
import pirated.bytecodec.bytesPrefixed

data class DataDigestSyncBatchedPacket(
    val bitCount: Int,
    val digest: ByteArray,
) {
  override fun equals(other: Any?): Boolean =
      other is DataDigestSyncBatchedPacket &&
          bitCount == other.bitCount &&
          digest.contentEquals(other.digest)

  override fun hashCode(): Int = bitCount * 31 + digest.contentHashCode()
}

private val DigestBytes = bytesPrefixed(U16LE)

object DataDigestSyncBatchedPacketCodec : PacketCodec<DataDigestSyncBatchedPacket>() {
  override fun CodecScope<DataDigestSyncBatchedPacket>.body(): DataDigestSyncBatchedPacket {
    val bitCount = field(U16LE) { it.bitCount }
    val digest = field(DigestBytes) { it.digest }
    return DataDigestSyncBatchedPacket(bitCount, digest)
  }
}
