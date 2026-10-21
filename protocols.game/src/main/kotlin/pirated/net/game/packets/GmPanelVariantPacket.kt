package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8

data class GmPanelVariantPacket(
    val variant: Byte,
)

object GmPanelVariantPacketCodec : PacketCodec<GmPanelVariantPacket>() {
  override fun CodecScope<GmPanelVariantPacket>.body(): GmPanelVariantPacket {
    val variant = field(S8) { it.variant }
    return GmPanelVariantPacket(variant)
  }
}
