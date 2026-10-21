package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.U8

data class BattleStartScenePacket(
    val battleType: Byte,
    val doubleBattle: Boolean,
    val perspective: Byte,
)

object BattleStartScenePacketCodec : PacketCodec<BattleStartScenePacket>() {
  override fun CodecScope<BattleStartScenePacket>.body(): BattleStartScenePacket {
    val battleType = field(S8) { it.battleType }
    val doubleBattle = field(U8) { if (it.doubleBattle) 1 else 0 } == 1
    val perspective = field(S8) { it.perspective }
    return BattleStartScenePacket(battleType, doubleBattle, perspective)
  }
}
