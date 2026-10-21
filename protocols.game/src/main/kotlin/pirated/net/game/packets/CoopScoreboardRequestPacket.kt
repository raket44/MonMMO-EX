package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class CoopScoreboardRequestPacket

object CoopScoreboardRequestPacketCodec : PacketCodec<CoopScoreboardRequestPacket>() {
  override fun CodecScope<CoopScoreboardRequestPacket>.body(): CoopScoreboardRequestPacket {
    return CoopScoreboardRequestPacket()
  }
}
