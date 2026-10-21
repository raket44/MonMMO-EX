package pirated.net.game.packets.guild

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class GuildLeavePacket

object GuildLeavePacketCodec : PacketCodec<GuildLeavePacket>() {
  override fun CodecScope<GuildLeavePacket>.body(): GuildLeavePacket {
    return GuildLeavePacket()
  }
}
