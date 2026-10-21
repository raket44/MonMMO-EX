package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleSlotActionPacket(
    val slotRefPacked: Byte,
    val actionByte: Byte,
)

object BattleSlotActionPacketCodec : PacketCodec<BattleSlotActionPacket>() {
  override fun CodecScope<BattleSlotActionPacket>.body(): BattleSlotActionPacket {
    val slotRefPacked = field(S8) { it.slotRefPacked }
    val actionByte = field(S8) { it.actionByte }
    return BattleSlotActionPacket(slotRefPacked, actionByte)
  }
}
