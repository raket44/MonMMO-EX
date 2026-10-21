package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class SelectSinglePokemonPacket(
    val entityId: Long,
)

object SelectSinglePokemonPacketCodec : PacketCodec<SelectSinglePokemonPacket>() {
  override fun CodecScope<SelectSinglePokemonPacket>.body(): SelectSinglePokemonPacket {
    val entityId = field(S64LE) { it.entityId }
    return SelectSinglePokemonPacket(entityId)
  }
}
