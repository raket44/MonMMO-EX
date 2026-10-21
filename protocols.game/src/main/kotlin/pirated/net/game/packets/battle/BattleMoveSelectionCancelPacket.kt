package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.reserved

data class BattleMoveSelectionCancelPacket(
    val unit: Unit = Unit,
)

object BattleMoveSelectionCancelPacketCodec : PacketCodec<BattleMoveSelectionCancelPacket>() {
  override fun CodecScope<BattleMoveSelectionCancelPacket>.body(): BattleMoveSelectionCancelPacket {
    reserved(byte = 0)
    return BattleMoveSelectionCancelPacket()
  }
}
