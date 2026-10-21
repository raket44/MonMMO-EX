package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class MonsterStatusConditionPacket(val targetId: Long, val statusCondition: Byte)

object MonsterStatusConditionPacketCodec : PacketCodec<MonsterStatusConditionPacket>() {
  override fun CodecScope<MonsterStatusConditionPacket>.body(): MonsterStatusConditionPacket {
    val targetId = field(S64LE) { it.targetId }
    val statusCondition = field(S8) { it.statusCondition }
    return MonsterStatusConditionPacket(targetId, statusCondition)
  }
}
