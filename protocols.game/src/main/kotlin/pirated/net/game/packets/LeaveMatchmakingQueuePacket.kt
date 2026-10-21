package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class LeaveMatchmakingQueuePacket

object LeaveMatchmakingQueuePacketCodec : PacketCodec<LeaveMatchmakingQueuePacket>() {
  override fun CodecScope<LeaveMatchmakingQueuePacket>.body(): LeaveMatchmakingQueuePacket {
    return LeaveMatchmakingQueuePacket()
  }
}
