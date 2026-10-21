package pirated.net.game.packets.gtl

import pirated.bytecodec.Codec
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S32LE
import pirated.bytecodec.U8
import pirated.bytecodec.listPrefixed

data class TradeListing(
    val typeId: Short,
    val price: Int,
    val fieldB: Int,
    val fieldC: Int,
    val fieldD: Int,
    val entries: List<TradeListingEntry>,
)

internal val TradeListingCodec: Codec<TradeListing> =
    object : PacketCodec<TradeListing>() {
      override fun CodecScope<TradeListing>.body(): TradeListing {
        val typeId = field(S16LE) { it.typeId }
        val price = field(S32LE) { it.price }
        val fieldB = field(S32LE) { it.fieldB }
        val fieldC = field(S32LE) { it.fieldC }
        val fieldD = field(S32LE) { it.fieldD }
        val entries = field(TradeListingEntryCodec.listPrefixed(U8)) { it.entries }
        return TradeListing(typeId, price, fieldB, fieldC, fieldD, entries)
      }
    }
