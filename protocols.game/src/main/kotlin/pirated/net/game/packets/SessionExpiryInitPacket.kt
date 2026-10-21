package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class SessionExpiryInitPacket(
    val tileSessionByte: Byte,
    val expirySeconds: Short,
)

object SessionExpiryInitPacketCodec : PacketCodec<SessionExpiryInitPacket>() {
  override fun CodecScope<SessionExpiryInitPacket>.body(): SessionExpiryInitPacket {
    val tileSessionByte = field(S8) { it.tileSessionByte }
    val expirySeconds = field(S16LE) { it.expirySeconds }
    return SessionExpiryInitPacket(tileSessionByte, expirySeconds)
  }
}
