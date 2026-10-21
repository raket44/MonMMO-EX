package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class LeaveChatChannelPacket

object LeaveChatChannelPacketCodec : PacketCodec<LeaveChatChannelPacket>() {
  override fun CodecScope<LeaveChatChannelPacket>.body(): LeaveChatChannelPacket {
    return LeaveChatChannelPacket()
  }
}
