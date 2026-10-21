package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.U8

data class MapTileAnimationTogglePacket(
    val x: Short,
    val y: Short,
    val enabled: Boolean,
)

object MapTileAnimationTogglePacketCodec : PacketCodec<MapTileAnimationTogglePacket>() {
  override fun CodecScope<MapTileAnimationTogglePacket>.body(): MapTileAnimationTogglePacket {
    val x = field(S16LE) { it.x }
    val y = field(S16LE) { it.y }
    val enabled = field(U8) { if (it.enabled) 1 else 0 } == 1
    return MapTileAnimationTogglePacket(x, y, enabled)
  }
}
