package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class BattleActionPromptTogglePacket(
    val shown: Boolean,
)

object BattleActionPromptTogglePacketCodec : PacketCodec<BattleActionPromptTogglePacket>() {
  override fun CodecScope<BattleActionPromptTogglePacket>.body(): BattleActionPromptTogglePacket {
    val shown = field(Bool) { it.shown }
    return BattleActionPromptTogglePacket(shown)
  }
}
