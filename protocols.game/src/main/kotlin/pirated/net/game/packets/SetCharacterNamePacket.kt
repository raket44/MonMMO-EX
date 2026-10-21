package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.Utf16LeNullTerminated

data class SetCharacterNamePacket(
    val characterEntityId: Long,
    val newName: String,
)

object SetCharacterNamePacketCodec : PacketCodec<SetCharacterNamePacket>() {
  override fun CodecScope<SetCharacterNamePacket>.body(): SetCharacterNamePacket {
    val characterEntityId = field(S64LE) { it.characterEntityId }
    val newName = field(Utf16LeNullTerminated) { it.newName }
    return SetCharacterNamePacket(characterEntityId, newName)
  }
}
