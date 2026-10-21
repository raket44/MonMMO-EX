package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class TeamPreviewSidePickPacket(
    val pokemonEntityId: Long,
    val side: Short,
)

object TeamPreviewSidePickPacketCodec : PacketCodec<TeamPreviewSidePickPacket>() {
  override fun CodecScope<TeamPreviewSidePickPacket>.body(): TeamPreviewSidePickPacket {
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val side = field(S16LE) { it.side }
    return TeamPreviewSidePickPacket(pokemonEntityId, side)
  }
}
