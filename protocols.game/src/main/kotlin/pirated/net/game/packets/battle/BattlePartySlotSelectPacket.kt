package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class BattlePartySlotSelectPacket(
    val slotIndex: Short,
    val pokemonEntityId: Long,
    val contextId: Short,
)

object BattlePartySlotSelectPacketCodec : PacketCodec<BattlePartySlotSelectPacket>() {
  override fun CodecScope<BattlePartySlotSelectPacket>.body(): BattlePartySlotSelectPacket {
    val slotIndex = field(S16LE) { it.slotIndex }
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val contextId = field(S16LE) { it.contextId }
    return BattlePartySlotSelectPacket(slotIndex, pokemonEntityId, contextId)
  }
}
