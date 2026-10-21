package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S64LE

data class BattleRewardSelectPacket(
    val targetEntityId: Long,
    val rewardIndex: Int,
)

object BattleRewardSelectPacketCodec : PacketCodec<BattleRewardSelectPacket>() {
  override fun CodecScope<BattleRewardSelectPacket>.body(): BattleRewardSelectPacket {
    val targetEntityId = field(S64LE) { it.targetEntityId }
    val rewardIndex = field(S32LE) { it.rewardIndex }
    return BattleRewardSelectPacket(targetEntityId, rewardIndex)
  }
}
