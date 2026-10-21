package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE

data class GtlListingSearchPacket(
    val searchId: Int,
)

object GtlListingSearchPacketCodec : PacketCodec<GtlListingSearchPacket>() {
  override fun CodecScope<GtlListingSearchPacket>.body(): GtlListingSearchPacket {
    val searchId = field(S32LE) { it.searchId }
    return GtlListingSearchPacket(searchId)
  }
}
