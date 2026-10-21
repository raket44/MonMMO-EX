package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class ShopSellRequestPacket(
    val itemEntityId: Long,
    val quantity: Short,
)

object ShopSellRequestPacketCodec : PacketCodec<ShopSellRequestPacket>() {
  override fun CodecScope<ShopSellRequestPacket>.body(): ShopSellRequestPacket {
    val itemEntityId = field(S64LE) { it.itemEntityId }
    val quantity = field(S16LE) { it.quantity }
    return ShopSellRequestPacket(itemEntityId, quantity)
  }
}
