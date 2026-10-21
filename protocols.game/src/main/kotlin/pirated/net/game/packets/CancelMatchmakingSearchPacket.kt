package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class CancelMatchmakingSearchPacket

object CancelMatchmakingSearchPacketCodec : PacketCodec<CancelMatchmakingSearchPacket>() {
  override fun CodecScope<CancelMatchmakingSearchPacket>.body(): CancelMatchmakingSearchPacket {
    return CancelMatchmakingSearchPacket()
  }
}
