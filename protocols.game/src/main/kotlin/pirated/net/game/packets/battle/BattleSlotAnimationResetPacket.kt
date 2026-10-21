package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class BattleSlotAnimationResetPacket(
    val entityId: Long,
    val slot: Byte,
)

object BattleSlotAnimationResetPacketCodec : PacketCodec<BattleSlotAnimationResetPacket>() {
  override fun CodecScope<BattleSlotAnimationResetPacket>.body(): BattleSlotAnimationResetPacket {
    val entityId = field(S64LE) { it.entityId }
    val slot = field(S8) { it.slot }
    return BattleSlotAnimationResetPacket(entityId, slot)
  }
}
