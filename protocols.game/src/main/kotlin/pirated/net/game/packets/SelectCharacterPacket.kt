package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class SelectCharacterPacket(val characterId: Long, val characterIdHash: Long)

object SelectCharacterPacketCodec : PacketCodec<SelectCharacterPacket>() {
  override fun CodecScope<SelectCharacterPacket>.body(): SelectCharacterPacket {
    val characterId = field(S64LE) { it.characterId }
    val characterIdHash = field(S64LE) { it.characterIdHash }
    return SelectCharacterPacket(characterId, characterIdHash)
  }
}
