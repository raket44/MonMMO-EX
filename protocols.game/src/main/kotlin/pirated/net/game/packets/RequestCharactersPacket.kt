package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class RequestCharactersPacket

object RequestCharactersPacketCodec : PacketCodec<RequestCharactersPacket>() {
  override fun CodecScope<RequestCharactersPacket>.body() = RequestCharactersPacket()
}
