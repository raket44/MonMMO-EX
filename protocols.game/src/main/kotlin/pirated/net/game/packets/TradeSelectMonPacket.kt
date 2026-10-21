package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE

data class TradeSelectMonPacket(val slotIndex: Int)

object TradeSelectMonPacketCodec : PacketCodec<TradeSelectMonPacket>() {
  override fun CodecScope<TradeSelectMonPacket>.body(): TradeSelectMonPacket {
    val slotIndex = field(S32LE) { it.slotIndex }
    return TradeSelectMonPacket(slotIndex)
  }
}
