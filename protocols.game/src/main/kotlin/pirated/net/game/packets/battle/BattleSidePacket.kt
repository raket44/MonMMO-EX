package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleSidePacket(
    val side: Byte,
)

object BattleSidePacketCodec : PacketCodec<BattleSidePacket>() {
  override fun CodecScope<BattleSidePacket>.body(): BattleSidePacket {
    val side = field(S8) { it.side }
    return BattleSidePacket(side)
  }
}
