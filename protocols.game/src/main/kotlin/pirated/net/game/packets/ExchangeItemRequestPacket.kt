package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class ExchangeItemRequestPacket(
    val itemEntityRef: Short,
    val quantity: Short,
    val exchangeTypeIndex: Byte,
)

object ExchangeItemRequestPacketCodec : PacketCodec<ExchangeItemRequestPacket>() {
  override fun CodecScope<ExchangeItemRequestPacket>.body(): ExchangeItemRequestPacket {
    val itemEntityRef = field(S16LE) { it.itemEntityRef }
    val quantity = field(S16LE) { it.quantity }
    val exchangeTypeIndex = field(S8) { it.exchangeTypeIndex }
    return ExchangeItemRequestPacket(itemEntityRef, quantity, exchangeTypeIndex)
  }
}
