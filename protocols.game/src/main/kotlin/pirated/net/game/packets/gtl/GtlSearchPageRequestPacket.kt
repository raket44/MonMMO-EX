package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8
import pirated.bytecodec.U8
import pirated.bytecodec.listPrefixed

/** [requestId] counts up per board and comes back in [GtlSearchPagePacket]. */
data class GtlSearchPageRequestPacket(
    val requestId: Byte,
    val listKind: GtlListKind,
    val categoryId: Byte,
    val page: Short,
    val filters: List<GtlSearchFilter> = emptyList(),
)

object GtlSearchPageRequestPacketCodec : PacketCodec<GtlSearchPageRequestPacket>() {
  override fun CodecScope<GtlSearchPageRequestPacket>.body(): GtlSearchPageRequestPacket {
    val requestId = field(S8) { it.requestId }
    val listKind = field(GtlListKindCodec) { it.listKind }
    val categoryId = field(S8) { it.categoryId }
    val page = field(S16LE) { it.page }
    val filters = field(GtlSearchFilterCodec.listPrefixed(U8)) { it.filters }
    return GtlSearchPageRequestPacket(requestId, listKind, categoryId, page, filters)
  }
}
