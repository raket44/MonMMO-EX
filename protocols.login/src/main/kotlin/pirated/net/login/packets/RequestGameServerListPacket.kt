package pirated.net.login.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class RequestGameServerListPacket

object RequestGameServerListPacketCodec : PacketCodec<RequestGameServerListPacket>() {
  override fun CodecScope<RequestGameServerListPacket>.body() = RequestGameServerListPacket()
}
