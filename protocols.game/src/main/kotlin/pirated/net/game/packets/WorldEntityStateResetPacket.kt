package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class WorldEntityStateResetPacket(
    val entityType: Short,
)

object WorldEntityStateResetPacketCodec : PacketCodec<WorldEntityStateResetPacket>() {
  override fun CodecScope<WorldEntityStateResetPacket>.body(): WorldEntityStateResetPacket {
    val entityType = field(S16LE) { it.entityType }
    return WorldEntityStateResetPacket(entityType)
  }
}
