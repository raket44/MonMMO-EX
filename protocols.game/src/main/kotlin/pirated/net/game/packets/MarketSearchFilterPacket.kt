package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class MarketSearchFilterPacket(
    val tab: Byte,
    val categoryId: Byte,
    val speciesFilter: Short,
)

object MarketSearchFilterPacketCodec : PacketCodec<MarketSearchFilterPacket>() {
  override fun CodecScope<MarketSearchFilterPacket>.body(): MarketSearchFilterPacket {
    val tab = field(S8) { it.tab }
    val categoryId = field(S8) { it.categoryId }
    val speciesFilter = field(S16LE) { it.speciesFilter }
    return MarketSearchFilterPacket(tab, categoryId, speciesFilter)
  }
}
