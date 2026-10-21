package pirated.net.game.packets.gtl

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class GtlListingActionPacket(
    val entryKindId: Byte,
    val itemId: Short,
)

object GtlListingActionPacketCodec : PacketCodec<GtlListingActionPacket>() {
  override fun CodecScope<GtlListingActionPacket>.body(): GtlListingActionPacket {
    val entryKindId = field(S8) { it.entryKindId }
    val itemId = field(S16LE) { it.itemId }
    return GtlListingActionPacket(entryKindId, itemId)
  }
}
