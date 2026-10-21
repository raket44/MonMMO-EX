package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class PokemonFlagsUpdatePacket(val entityId: Long, val flags: Byte)

object PokemonFlagsUpdatePacketCodec : PacketCodec<PokemonFlagsUpdatePacket>() {
  override fun CodecScope<PokemonFlagsUpdatePacket>.body(): PokemonFlagsUpdatePacket {
    val entityId = field(S64LE) { it.entityId }
    val flags = field(S8) { it.flags }
    return PokemonFlagsUpdatePacket(entityId, flags)
  }
}
