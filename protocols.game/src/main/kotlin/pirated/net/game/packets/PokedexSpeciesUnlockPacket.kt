package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE

data class PokedexSpeciesUnlockPacket(
    val speciesId: Short,
)

object PokedexSpeciesUnlockPacketCodec : PacketCodec<PokedexSpeciesUnlockPacket>() {
  override fun CodecScope<PokedexSpeciesUnlockPacket>.body(): PokedexSpeciesUnlockPacket {
    val speciesId = field(S16LE) { it.speciesId }
    return PokedexSpeciesUnlockPacket(speciesId)
  }
}
