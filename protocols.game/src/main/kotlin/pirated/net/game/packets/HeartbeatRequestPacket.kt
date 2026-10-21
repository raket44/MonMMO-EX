package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class HeartbeatRequestPacket(val placeholder: Unit = Unit)

object HeartbeatRequestPacketCodec : PacketCodec<HeartbeatRequestPacket>() {
  override fun CodecScope<HeartbeatRequestPacket>.body(): HeartbeatRequestPacket {
    return HeartbeatRequestPacket()
  }
}
