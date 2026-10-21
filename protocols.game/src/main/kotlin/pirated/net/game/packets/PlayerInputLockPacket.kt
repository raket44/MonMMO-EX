package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.U8

data class PlayerInputLockPacket(
    val inputEnabled: Boolean,
)

object PlayerInputLockPacketCodec : PacketCodec<PlayerInputLockPacket>() {
  override fun CodecScope<PlayerInputLockPacket>.body(): PlayerInputLockPacket {
    val inputEnabled = field(U8) { if (it.inputEnabled) 1 else 0 } == 1
    return PlayerInputLockPacket(inputEnabled)
  }
}
