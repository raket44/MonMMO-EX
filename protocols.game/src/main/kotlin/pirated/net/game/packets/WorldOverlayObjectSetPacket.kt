package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class WorldOverlayObjectSetPacket(
    val objectId: Byte,
    val action: Byte,
    val byteA: Byte?,
    val byteB: Byte?,
    val typeId: Short?,
    val flag: Byte?,
    val x: Short?,
    val y: Short?,
)

object WorldOverlayObjectSetPacketCodec : PacketCodec<WorldOverlayObjectSetPacket>() {
  override fun CodecScope<WorldOverlayObjectSetPacket>.body(): WorldOverlayObjectSetPacket {
    val objectId = field(S8) { it.objectId }
    val action = field(S8) { it.action }
    val present = action.toInt() != 0
    val byteA = if (present) field(S8) { it.byteA!! } else null
    val byteB = if (present) field(S8) { it.byteB!! } else null
    val typeId = if (present) field(S16LE) { it.typeId!! } else null
    val flag = if (present) field(S8) { it.flag!! } else null
    val x = if (present) field(S16LE) { it.x!! } else null
    val y = if (present) field(S16LE) { it.y!! } else null
    return WorldOverlayObjectSetPacket(objectId, action, byteA, byteB, typeId, flag, x, y)
  }
}
