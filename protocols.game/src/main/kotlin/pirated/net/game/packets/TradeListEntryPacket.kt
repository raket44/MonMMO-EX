package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.common.Pokemon
import pirated.net.game.codecs.PokemonCodec

data class TradeListEntryPacket(
    val pokemon: Pokemon,
)

object TradeListEntryPacketCodec : PacketCodec<TradeListEntryPacket>() {
  override fun CodecScope<TradeListEntryPacket>.body(): TradeListEntryPacket {
    val pokemon = field(PokemonCodec) { it.pokemon }
    return TradeListEntryPacket(pokemon)
  }
}
