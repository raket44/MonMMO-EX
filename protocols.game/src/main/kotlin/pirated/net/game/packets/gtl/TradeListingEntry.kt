package pirated.net.game.packets.gtl

import pirated.bytecodec.Codec
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S64LE

data class TradeListingEntry(
    val a: Int,
    val b: Int,
    val c: Long,
)

internal val TradeListingEntryCodec: Codec<TradeListingEntry> =
    object : PacketCodec<TradeListingEntry>() {
      override fun CodecScope<TradeListingEntry>.body(): TradeListingEntry {
        val a = field(S32LE) { it.a }
        val b = field(S32LE) { it.b }
        val c = field(S64LE) { it.c }
        return TradeListingEntry(a, b, c)
      }
    }
