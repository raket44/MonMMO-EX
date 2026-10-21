package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class TeamPreviewRevealPickPacket(
    val entityId: Long,
)

object TeamPreviewRevealPickPacketCodec : PacketCodec<TeamPreviewRevealPickPacket>() {
  override fun CodecScope<TeamPreviewRevealPickPacket>.body(): TeamPreviewRevealPickPacket {
    val entityId = field(S64LE) { it.entityId }
    return TeamPreviewRevealPickPacket(entityId)
  }
}
