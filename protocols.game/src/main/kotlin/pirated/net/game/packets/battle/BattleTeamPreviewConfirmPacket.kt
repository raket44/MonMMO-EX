package pirated.net.game.packets.battle

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class BattleTeamPreviewConfirmPacket(
    val side: Short,
    val isOwnTeam: Boolean,
)

object BattleTeamPreviewConfirmPacketCodec : PacketCodec<BattleTeamPreviewConfirmPacket>() {
  override fun CodecScope<BattleTeamPreviewConfirmPacket>.body(): BattleTeamPreviewConfirmPacket {
    val side = field(S16LE) { it.side }
    val isOwnTeam = field(Bool) { it.isOwnTeam }
    return BattleTeamPreviewConfirmPacket(side, isOwnTeam)
  }
}
