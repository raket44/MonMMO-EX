package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class GtlListingCancelPacket(
    val listingId: String,
)

object GtlListingCancelPacketCodec : PacketCodec<GtlListingCancelPacket>() {
  override fun CodecScope<GtlListingCancelPacket>.body(): GtlListingCancelPacket {
    val listingId = field(Utf16LeNullTerminated) { it.listingId }
    return GtlListingCancelPacket(listingId)
  }
}
