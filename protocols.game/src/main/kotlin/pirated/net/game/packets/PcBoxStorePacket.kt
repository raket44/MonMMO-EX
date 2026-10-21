package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class PcBoxStorePacket(
    val boxId: Byte,
    val slotIndex: Byte,
    val pokemonId: Long,
)

object PcBoxStorePacketCodec : PacketCodec<PcBoxStorePacket>() {
  override fun CodecScope<PcBoxStorePacket>.body(): PcBoxStorePacket {
    val boxId = field(S8) { it.boxId }
    val slotIndex = field(S8) { it.slotIndex }
    val pokemonId = field(S64LE) { it.pokemonId }
    return PcBoxStorePacket(boxId, slotIndex, pokemonId)
  }
}
