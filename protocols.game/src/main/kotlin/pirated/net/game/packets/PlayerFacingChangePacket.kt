package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class PlayerFacingChangePacket(val facingDirection: Byte)

object PlayerFacingChangePacketCodec : PacketCodec<PlayerFacingChangePacket>() {
  override fun CodecScope<PlayerFacingChangePacket>.body(): PlayerFacingChangePacket {
    val facingDirection = field(S8) { it.facingDirection }
    return PlayerFacingChangePacket(facingDirection)
  }
}
