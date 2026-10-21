package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class WorldObjectInstanceDespawnPacket(
    val entityId: Long,
)

object WorldObjectInstanceDespawnPacketCodec : PacketCodec<WorldObjectInstanceDespawnPacket>() {
  override fun CodecScope<WorldObjectInstanceDespawnPacket>.body():
      WorldObjectInstanceDespawnPacket {
    val entityId = field(S64LE) { it.entityId }
    return WorldObjectInstanceDespawnPacket(entityId)
  }
}
