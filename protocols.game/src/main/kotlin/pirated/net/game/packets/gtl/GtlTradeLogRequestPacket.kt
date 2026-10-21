package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class GtlTradeLogRequestPacket

object GtlTradeLogRequestPacketCodec : PacketCodec<GtlTradeLogRequestPacket>() {
  override fun CodecScope<GtlTradeLogRequestPacket>.body(): GtlTradeLogRequestPacket {
    return GtlTradeLogRequestPacket()
  }
}
