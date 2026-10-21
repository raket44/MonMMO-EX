package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleStateBytePacket(
    val state: Byte,
)

object BattleStateBytePacketCodec : PacketCodec<BattleStateBytePacket>() {
  override fun CodecScope<BattleStateBytePacket>.body(): BattleStateBytePacket {
    val state = field(S8) { it.state }
    return BattleStateBytePacket(state)
  }
}
