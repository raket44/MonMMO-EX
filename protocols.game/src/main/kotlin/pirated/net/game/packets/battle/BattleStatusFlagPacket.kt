package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class BattleStatusFlagPacket(
    val scope: Byte,
    val flagId: Short,
    val flagValue: Short,
)

object BattleStatusFlagPacketCodec : PacketCodec<BattleStatusFlagPacket>() {
  override fun CodecScope<BattleStatusFlagPacket>.body(): BattleStatusFlagPacket {
    val scope = field(S8) { it.scope }
    val flagId = field(S16LE) { it.flagId }
    val flagValue = field(S16LE) { it.flagValue }
    return BattleStatusFlagPacket(scope, flagId, flagValue)
  }
}
