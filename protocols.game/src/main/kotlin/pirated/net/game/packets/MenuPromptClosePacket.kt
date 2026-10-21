package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class MenuPromptClosePacket(
    val promptType: Byte,
)

object MenuPromptClosePacketCodec : PacketCodec<MenuPromptClosePacket>() {
  override fun CodecScope<MenuPromptClosePacket>.body(): MenuPromptClosePacket {
    val promptType = field(S8) { it.promptType }
    return MenuPromptClosePacket(promptType)
  }
}
