package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.Utf16LeNullTerminated

data class SendChatCommandPacket(
    val message: String,
) {
  override fun toString(): String =
      "SendChatCommandPacket(message='$message' units=" + message.map { "%04x".format(it.code) }.joinToString(" ") + ")"
}

object SendChatCommandPacketCodec : PacketCodec<SendChatCommandPacket>() {
  override fun CodecScope<SendChatCommandPacket>.body(): SendChatCommandPacket {
    val message = field(Utf16LeNullTerminated) { it.message }
    return SendChatCommandPacket(message)
  }
}
