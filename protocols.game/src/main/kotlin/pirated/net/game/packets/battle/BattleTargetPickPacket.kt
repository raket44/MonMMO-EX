package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class BattleTargetPickPacket(
    val targetEntityId: Long,
)

object BattleTargetPickPacketCodec : PacketCodec<BattleTargetPickPacket>() {
  override fun CodecScope<BattleTargetPickPacket>.body(): BattleTargetPickPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    return BattleTargetPickPacket(targetEntityId)
  }
}
