package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class JoinMatchmakingQueuePacket

object JoinMatchmakingQueuePacketCodec : PacketCodec<JoinMatchmakingQueuePacket>() {
  override fun CodecScope<JoinMatchmakingQueuePacket>.body(): JoinMatchmakingQueuePacket {
    return JoinMatchmakingQueuePacket()
  }
}
