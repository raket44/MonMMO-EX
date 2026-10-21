package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class MailTogglePacket(
    val shown: Boolean,
)

object MailTogglePacketCodec : PacketCodec<MailTogglePacket>() {
  override fun CodecScope<MailTogglePacket>.body(): MailTogglePacket {
    val shown = field(Bool) { it.shown }
    return MailTogglePacket(shown)
  }
}
