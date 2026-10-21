package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class MonsterFavoriteTogglePacket(val pokemonEntityId: Long, val favorite: Boolean)

object MonsterFavoriteTogglePacketCodec : PacketCodec<MonsterFavoriteTogglePacket>() {
  override fun CodecScope<MonsterFavoriteTogglePacket>.body(): MonsterFavoriteTogglePacket {
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val favorite = field(Bool) { it.favorite }
    return MonsterFavoriteTogglePacket(pokemonEntityId, favorite)
  }
}
