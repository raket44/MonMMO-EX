package pirated.net.login.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.Utf16LeNullTerminated

data class MfaChallengePacket(val unk: Byte, val email: String)

object MfaChallengePacketCodec : PacketCodec<MfaChallengePacket>() {
  override fun CodecScope<MfaChallengePacket>.body(): MfaChallengePacket {
    val unk = field(S8) { it.unk }
    val email = field(Utf16LeNullTerminated) { it.email }
    return MfaChallengePacket(unk, email)
  }
}
