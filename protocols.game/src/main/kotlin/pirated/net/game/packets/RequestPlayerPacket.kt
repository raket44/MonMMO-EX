package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class RequestPlayerPacket

object RequestPlayerPacketCodec : PacketCodec<RequestPlayerPacket>() {
  override fun CodecScope<RequestPlayerPacket>.body() = RequestPlayerPacket()
}
