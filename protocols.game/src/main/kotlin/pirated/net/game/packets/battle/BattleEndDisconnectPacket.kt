package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class BattleEndDisconnectPacket(
    val reason: Byte,
)

object BattleEndDisconnectPacketCodec : PacketCodec<BattleEndDisconnectPacket>() {
  override fun CodecScope<BattleEndDisconnectPacket>.body(): BattleEndDisconnectPacket {
    val reason = field(S8) { it.reason }
    return BattleEndDisconnectPacket(reason)
  }
}
