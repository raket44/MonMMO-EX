package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S32LE
import pirated.bytecodec.S8
import pirated.bytecodec.U8
import pirated.bytecodec.listPrefixed

data class GtlListingsPagePacket(
    val requestId: Byte,
    val listKind: GtlListKind,
    val page: Short,
    val totalCount: Int,
    val listings: List<TradeListing>,
)

object GtlListingsPagePacketCodec : PacketCodec<GtlListingsPagePacket>() {
  override fun CodecScope<GtlListingsPagePacket>.body(): GtlListingsPagePacket {
    val requestId = field(S8) { it.requestId }
    val listKind = field(GtlListKindCodec) { it.listKind }
    val page = field(S16LE) { it.page }
    val totalCount = field(S32LE) { it.totalCount }
    val listings = field(TradeListingCodec.listPrefixed(U8)) { it.listings }
    return GtlListingsPagePacket(requestId, listKind, page, totalCount, listings)
  }
}
