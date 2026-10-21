package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class GtlSearchFilterPacket(
    val filterState: Byte,
)

object GtlSearchFilterPacketCodec : PacketCodec<GtlSearchFilterPacket>() {
  override fun CodecScope<GtlSearchFilterPacket>.body(): GtlSearchFilterPacket {
    val filterState = field(S8) { it.filterState }
    return GtlSearchFilterPacket(filterState)
  }
}
