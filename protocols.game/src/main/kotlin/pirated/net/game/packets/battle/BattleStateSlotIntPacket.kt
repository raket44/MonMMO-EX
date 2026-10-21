package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE
import pirated.bytecodec.S8

data class BattleStateSlotIntPacket(
    val index: Byte,
    val value: Int,
)

object BattleStateSlotIntPacketCodec : PacketCodec<BattleStateSlotIntPacket>() {
  override fun CodecScope<BattleStateSlotIntPacket>.body(): BattleStateSlotIntPacket {
    val index = field(S8) { it.index }
    val value = field(S32LE) { it.value }
    return BattleStateSlotIntPacket(index, value)
  }
}
