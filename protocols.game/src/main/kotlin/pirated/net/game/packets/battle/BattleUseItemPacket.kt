package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class BattleUseItemPacket(
    val pokemonEntityId: Long,
    val quantity: Short,
)

object BattleUseItemPacketCodec : PacketCodec<BattleUseItemPacket>() {
  override fun CodecScope<BattleUseItemPacket>.body(): BattleUseItemPacket {
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val quantity = field(S16LE) { it.quantity }
    return BattleUseItemPacket(pokemonEntityId, quantity)
  }
}
