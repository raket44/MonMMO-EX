package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class MapTileEntityStateSetPacket(
    val mode: Byte,
    val entityId: Short,
    val value: Short,
    val key: Short?,
)

object MapTileEntityStateSetPacketCodec : PacketCodec<MapTileEntityStateSetPacket>() {
  override fun CodecScope<MapTileEntityStateSetPacket>.body(): MapTileEntityStateSetPacket {
    val mode = field(S8) { it.mode }
    val entityId = field(S16LE) { it.entityId }
    val value = field(S16LE) { it.value }
    val key = if (mode.toInt() == 0) field(S16LE) { it.key!! } else null
    return MapTileEntityStateSetPacket(mode, entityId, value, key)
  }
}
