package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class AssignBreedingSlotPacket(
    val ownPokemonEntityId: Long,
    val partnerPokemonEntityId: Long,
    val slotIndex: Byte,
)

object AssignBreedingSlotPacketCodec : PacketCodec<AssignBreedingSlotPacket>() {
  override fun CodecScope<AssignBreedingSlotPacket>.body(): AssignBreedingSlotPacket {
    val ownPokemonEntityId = field(S64LE) { it.ownPokemonEntityId }
    val partnerPokemonEntityId = field(S64LE) { it.partnerPokemonEntityId }
    val slotIndex = field(S8) { it.slotIndex }
    return AssignBreedingSlotPacket(ownPokemonEntityId, partnerPokemonEntityId, slotIndex)
  }
}
