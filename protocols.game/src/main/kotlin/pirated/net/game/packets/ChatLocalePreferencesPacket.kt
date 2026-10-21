package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class ChatLocalePreferencesPacket(
    val languageId: Byte,
    val chatChannelMask: Short,
    val languageFilterMask: Short,
)

object ChatLocalePreferencesPacketCodec : PacketCodec<ChatLocalePreferencesPacket>() {
  override fun CodecScope<ChatLocalePreferencesPacket>.body(): ChatLocalePreferencesPacket {
    val languageId = field(S8) { it.languageId }
    val chatChannelMask = field(S16LE) { it.chatChannelMask }
    val languageFilterMask = field(S16LE) { it.languageFilterMask }
    return ChatLocalePreferencesPacket(languageId, chatChannelMask, languageFilterMask)
  }
}
