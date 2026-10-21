package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class ChannelSwitchPacket(val channelId: Byte, val silent: Boolean)

object ChannelSwitchPacketCodec : PacketCodec<ChannelSwitchPacket>() {
  override fun CodecScope<ChannelSwitchPacket>.body(): ChannelSwitchPacket {
    val channelId = field(S8) { it.channelId }
    val silent = field(Bool) { it.silent }
    return ChannelSwitchPacket(channelId, silent)
  }
}
