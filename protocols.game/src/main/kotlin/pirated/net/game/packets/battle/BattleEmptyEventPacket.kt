package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class BattleEmptyEventPacket(
    val unit: Unit = Unit,
)

object BattleEmptyEventPacketCodec : PacketCodec<BattleEmptyEventPacket>() {
  override fun CodecScope<BattleEmptyEventPacket>.body(): BattleEmptyEventPacket {
    return BattleEmptyEventPacket()
  }
}
