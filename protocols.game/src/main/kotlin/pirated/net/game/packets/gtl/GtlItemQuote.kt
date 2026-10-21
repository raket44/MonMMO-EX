package pirated.net.game.packets.gtl

import pirated.bytecodec.Codec
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S32LE

data class GtlItemQuote(
    val itemId: Short,
    val price: Int,
)

internal val GtlItemQuoteCodec: Codec<GtlItemQuote> =
    object : PacketCodec<GtlItemQuote>() {
      override fun CodecScope<GtlItemQuote>.body(): GtlItemQuote {
        val itemId = field(S16LE) { it.itemId }
        val price = field(S32LE) { it.price }
        return GtlItemQuote(itemId, price)
      }
    }
