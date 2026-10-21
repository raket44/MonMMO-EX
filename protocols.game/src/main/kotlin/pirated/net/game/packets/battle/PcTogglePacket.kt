package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class PcTogglePacket(
    val shown: Boolean,
)

object PcTogglePacketCodec : PacketCodec<PcTogglePacket>() {
  override fun CodecScope<PcTogglePacket>.body(): PcTogglePacket {
    val shown = field(Bool) { it.shown }
    return PcTogglePacket(shown)
  }
}
