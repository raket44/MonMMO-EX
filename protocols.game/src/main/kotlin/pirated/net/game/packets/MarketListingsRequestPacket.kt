package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class MarketListingsRequestPacket(val tab: Byte, val categoryId: Byte)

object MarketListingsRequestPacketCodec : PacketCodec<MarketListingsRequestPacket>() {
  override fun CodecScope<MarketListingsRequestPacket>.body(): MarketListingsRequestPacket {
    val tab = field(S8) { it.tab }
    val categoryId = field(S8) { it.categoryId }
    return MarketListingsRequestPacket(tab, categoryId)
  }
}
