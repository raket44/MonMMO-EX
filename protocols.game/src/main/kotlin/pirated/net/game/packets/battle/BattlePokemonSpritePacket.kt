package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S64LE

data class BattlePokemonSpritePacket(
    val entityId: Long,
    val spriteId: Short,
)

object BattlePokemonSpritePacketCodec : PacketCodec<BattlePokemonSpritePacket>() {
  override fun CodecScope<BattlePokemonSpritePacket>.body(): BattlePokemonSpritePacket {
    val entityId = field(S64LE) { it.entityId }
    val spriteId = field(S16LE) { it.spriteId }
    return BattlePokemonSpritePacket(entityId, spriteId)
  }
}
