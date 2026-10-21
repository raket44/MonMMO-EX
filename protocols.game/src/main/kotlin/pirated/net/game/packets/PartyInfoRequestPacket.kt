package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class PartyInfoRequestPacket

object PartyInfoRequestPacketCodec : PacketCodec<PartyInfoRequestPacket>() {
  override fun CodecScope<PartyInfoRequestPacket>.body(): PartyInfoRequestPacket {
    return PartyInfoRequestPacket()
  }
}
