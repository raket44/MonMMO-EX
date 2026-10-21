package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class GuildActivityLogPageRequestPacket(
    val pageIndex: Short,
)

object GuildActivityLogPageRequestPacketCodec : PacketCodec<GuildActivityLogPageRequestPacket>() {
  override fun CodecScope<GuildActivityLogPageRequestPacket>.body():
      GuildActivityLogPageRequestPacket {
    val pageIndex = field(S16LE) { it.pageIndex }
    return GuildActivityLogPageRequestPacket(pageIndex)
  }
}
