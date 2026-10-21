package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class BattleReadyPacket(val ready: Boolean)

object BattleReadyPacketCodec : PacketCodec<BattleReadyPacket>() {
  override fun CodecScope<BattleReadyPacket>.body(): BattleReadyPacket {
    val ready = field(Bool) { it.ready }
    return BattleReadyPacket(ready)
  }
}
