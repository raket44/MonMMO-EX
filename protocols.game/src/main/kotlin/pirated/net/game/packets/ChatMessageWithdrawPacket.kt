package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class ChatMessageWithdrawPacket(
    val authorId: Long,
)

object ChatMessageWithdrawPacketCodec : PacketCodec<ChatMessageWithdrawPacket>() {
  override fun CodecScope<ChatMessageWithdrawPacket>.body(): ChatMessageWithdrawPacket {
    val authorId = field(S64LE) { it.authorId }
    return ChatMessageWithdrawPacket(authorId)
  }
}
