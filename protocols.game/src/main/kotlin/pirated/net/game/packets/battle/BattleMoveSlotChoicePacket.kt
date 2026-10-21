package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleMoveSlotChoicePacket(val moveSlotIndex: Byte)

object BattleMoveSlotChoicePacketCodec : PacketCodec<BattleMoveSlotChoicePacket>() {
  override fun CodecScope<BattleMoveSlotChoicePacket>.body(): BattleMoveSlotChoicePacket {
    val moveSlotIndex = field(S8) { it.moveSlotIndex }
    return BattleMoveSlotChoicePacket(moveSlotIndex)
  }
}
