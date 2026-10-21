package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class GtlConfirmPurchasePacket(
    val listingEntityId: Long,
    val quantity: Short,
)

object GtlConfirmPurchasePacketCodec : PacketCodec<GtlConfirmPurchasePacket>() {
  override fun CodecScope<GtlConfirmPurchasePacket>.body(): GtlConfirmPurchasePacket {
    val listingEntityId = field(S64LE) { it.listingEntityId }
    val quantity = field(S16LE) { it.quantity }
    return GtlConfirmPurchasePacket(listingEntityId, quantity)
  }
}
