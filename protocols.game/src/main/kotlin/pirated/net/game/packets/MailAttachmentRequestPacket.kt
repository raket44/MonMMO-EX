package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class MailAttachmentRequestPacket(
    val targetId: Long,
)

object MailAttachmentRequestPacketCodec : PacketCodec<MailAttachmentRequestPacket>() {
  override fun CodecScope<MailAttachmentRequestPacket>.body(): MailAttachmentRequestPacket {
    val targetId = field(S64LE) { it.targetId }
    return MailAttachmentRequestPacket(targetId)
  }
}
