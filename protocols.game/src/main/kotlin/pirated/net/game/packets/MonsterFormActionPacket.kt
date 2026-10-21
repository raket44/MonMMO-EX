package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class MonsterFormActionPacket(val monsterEntityId: Long, val stateValue: Short)

object MonsterFormActionPacketCodec : PacketCodec<MonsterFormActionPacket>() {
  override fun CodecScope<MonsterFormActionPacket>.body(): MonsterFormActionPacket {
    val monsterEntityId = field(S64LE) { it.monsterEntityId }
    val stateValue = field(S16LE) { it.stateValue }
    return MonsterFormActionPacket(monsterEntityId, stateValue)
  }
}
