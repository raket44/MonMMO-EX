package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.Utf16LeNullTerminated

data class DuelInvitePacket(
    val flags: Byte,
    val requestType: Byte,
    val name: String,
)

object DuelInvitePacketCodec : PacketCodec<DuelInvitePacket>() {
  override fun CodecScope<DuelInvitePacket>.body(): DuelInvitePacket {
    val flags = field(S8) { it.flags }
    val requestType = field(S8) { it.requestType }
    val name = field(Utf16LeNullTerminated) { it.name }
    return DuelInvitePacket(flags, requestType, name)
  }
}
