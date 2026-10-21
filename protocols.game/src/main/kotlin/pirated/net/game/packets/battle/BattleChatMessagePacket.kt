package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.Utf16LeNullTerminated

data class BattleChatMessagePacket(val channelOrSlot: Byte, val message: String)

object BattleChatMessagePacketCodec : PacketCodec<BattleChatMessagePacket>() {
  override fun CodecScope<BattleChatMessagePacket>.body(): BattleChatMessagePacket {
    val channelOrSlot = field(S8) { it.channelOrSlot }
    val message = field(Utf16LeNullTerminated) { it.message }
    return BattleChatMessagePacket(channelOrSlot, message)
  }
}
