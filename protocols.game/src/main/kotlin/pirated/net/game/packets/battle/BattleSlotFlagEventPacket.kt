package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleSlotFlagEventPacket(
    val slot: Byte,
    val flag: Boolean,
    val immediate: Boolean,
)

object BattleSlotFlagEventPacketCodec : PacketCodec<BattleSlotFlagEventPacket>() {
  override fun CodecScope<BattleSlotFlagEventPacket>.body(): BattleSlotFlagEventPacket {
    val slot = field(S8) { it.slot }
    val flag = field(Bool) { it.flag }
    val immediate = field(Bool) { it.immediate }
    return BattleSlotFlagEventPacket(slot, flag, immediate)
  }
}
