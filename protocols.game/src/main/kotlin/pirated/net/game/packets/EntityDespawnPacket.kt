package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.reserved

data class EntityDespawnPacket(
    val entityId: Short,
)

object EntityDespawnPacketCodec : PacketCodec<EntityDespawnPacket>() {
  override fun CodecScope<EntityDespawnPacket>.body(): EntityDespawnPacket {
    reserved(byte = 0)
    val entityId = field(S16LE) { it.entityId }
    return EntityDespawnPacket(entityId)
  }
}
