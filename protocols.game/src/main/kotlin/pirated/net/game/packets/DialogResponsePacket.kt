package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class DialogResponsePacket(
    val accepted: Boolean,
    val dialogActionId: Short,
)

object DialogResponsePacketCodec : PacketCodec<DialogResponsePacket>() {
  override fun CodecScope<DialogResponsePacket>.body(): DialogResponsePacket {
    val accepted = field(Bool) { it.accepted }
    val dialogActionId = field(S16LE) { it.dialogActionId }
    return DialogResponsePacket(accepted, dialogActionId)
  }
}
