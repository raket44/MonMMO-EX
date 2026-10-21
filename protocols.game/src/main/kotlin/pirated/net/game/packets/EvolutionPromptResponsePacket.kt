package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class EvolutionPromptResponsePacket(val pokemonEntityId: Long, val accepted: Boolean)

object EvolutionPromptResponsePacketCodec : PacketCodec<EvolutionPromptResponsePacket>() {
  override fun CodecScope<EvolutionPromptResponsePacket>.body(): EvolutionPromptResponsePacket {
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val accepted = field(Bool) { it.accepted }
    return EvolutionPromptResponsePacket(pokemonEntityId, accepted)
  }
}
