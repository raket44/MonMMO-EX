package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class OverworldPathToTilePacket(
    val zoneId: Short,
    val targetX: Byte,
    val targetY: Byte,
)

object OverworldPathToTilePacketCodec : PacketCodec<OverworldPathToTilePacket>() {
  override fun CodecScope<OverworldPathToTilePacket>.body(): OverworldPathToTilePacket {
    val zoneId = field(S16LE) { it.zoneId }
    val targetX = field(S8) { it.targetX }
    val targetY = field(S8) { it.targetY }
    return OverworldPathToTilePacket(zoneId, targetX, targetY)
  }
}
