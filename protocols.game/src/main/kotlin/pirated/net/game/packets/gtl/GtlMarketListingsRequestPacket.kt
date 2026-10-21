package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class GtlMarketListingsRequestPacket

object GtlMarketListingsRequestPacketCodec : PacketCodec<GtlMarketListingsRequestPacket>() {
  override fun CodecScope<GtlMarketListingsRequestPacket>.body(): GtlMarketListingsRequestPacket {
    return GtlMarketListingsRequestPacket()
  }
}
