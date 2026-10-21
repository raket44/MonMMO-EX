package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class TradeActionPacket(val action: Byte)

object TradeActionPacketCodec : PacketCodec<TradeActionPacket>() {
  override fun CodecScope<TradeActionPacket>.body(): TradeActionPacket {
    val action = field(S8) { it.action }
    return TradeActionPacket(action)
  }
}
