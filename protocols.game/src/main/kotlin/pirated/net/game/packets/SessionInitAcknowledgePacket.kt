package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class SessionInitAcknowledgePacket(val placeholder: Unit = Unit)

object SessionInitAcknowledgePacketCodec : PacketCodec<SessionInitAcknowledgePacket>() {
  override fun CodecScope<SessionInitAcknowledgePacket>.body(): SessionInitAcknowledgePacket {
    return SessionInitAcknowledgePacket()
  }
}
