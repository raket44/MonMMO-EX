package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class GtlOpenSessionPacket(
    val entryKindId: Byte,
    val sessionTimestamp: Long,
)

object GtlOpenSessionPacketCodec : PacketCodec<GtlOpenSessionPacket>() {
  override fun CodecScope<GtlOpenSessionPacket>.body(): GtlOpenSessionPacket {
    val entryKindId = field(S8) { it.entryKindId }
    val sessionTimestamp = field(S64LE) { it.sessionTimestamp }
    return GtlOpenSessionPacket(entryKindId, sessionTimestamp)
  }
}
