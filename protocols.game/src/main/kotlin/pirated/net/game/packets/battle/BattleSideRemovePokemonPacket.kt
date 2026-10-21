package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class BattleSideRemovePokemonPacket(
    val side: Byte,
    val entityId: Long,
)

object BattleSideRemovePokemonPacketCodec : PacketCodec<BattleSideRemovePokemonPacket>() {
  override fun CodecScope<BattleSideRemovePokemonPacket>.body(): BattleSideRemovePokemonPacket {
    val side = field(S8) { it.side }
    val entityId = field(S64LE) { it.entityId }
    return BattleSideRemovePokemonPacket(side, entityId)
  }
}
