package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class CancelSocialInteractionPacket

object CancelSocialInteractionPacketCodec : PacketCodec<CancelSocialInteractionPacket>() {
  override fun CodecScope<CancelSocialInteractionPacket>.body(): CancelSocialInteractionPacket {
    return CancelSocialInteractionPacket()
  }
}
