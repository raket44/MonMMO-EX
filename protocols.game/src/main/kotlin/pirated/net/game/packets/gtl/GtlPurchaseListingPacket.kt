package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S8

data class GtlPurchaseListingPacket(
    val listingId: Int,
    val price: Int,
    val action: Byte,
)

object GtlPurchaseListingPacketCodec : PacketCodec<GtlPurchaseListingPacket>() {
  override fun CodecScope<GtlPurchaseListingPacket>.body(): GtlPurchaseListingPacket {
    val listingId = field(S32LE) { it.listingId }
    val price = field(S32LE) { it.price }
    val action = field(S8) { it.action }
    return GtlPurchaseListingPacket(listingId, price, action)
  }
}
