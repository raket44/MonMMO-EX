package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.common.Pokemon
import pirated.net.game.codecs.PokemonCodec

data class SocialListEntryAddPacket(
    val pokemon: Pokemon,
)

object SocialListEntryAddPacketCodec : PacketCodec<SocialListEntryAddPacket>() {
  override fun CodecScope<SocialListEntryAddPacket>.body(): SocialListEntryAddPacket {
    val pokemon = field(PokemonCodec) { it.pokemon }
    return SocialListEntryAddPacket(pokemon)
  }
}
