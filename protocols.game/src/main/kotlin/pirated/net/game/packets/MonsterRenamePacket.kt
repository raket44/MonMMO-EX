package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.Utf16LeNullTerminated

data class MonsterRenamePacket(val pokemonEntityId: Long, val nickname: String)

object MonsterRenamePacketCodec : PacketCodec<MonsterRenamePacket>() {
  override fun CodecScope<MonsterRenamePacket>.body(): MonsterRenamePacket {
    val pokemonEntityId = field(S64LE) { it.pokemonEntityId }
    val nickname = field(Utf16LeNullTerminated) { it.nickname }
    return MonsterRenamePacket(pokemonEntityId, nickname)
  }
}
