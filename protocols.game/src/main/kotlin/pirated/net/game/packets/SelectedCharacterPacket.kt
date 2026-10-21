package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.optional
import pirated.common.CharacterInfo
import pirated.net.game.codecs.CharacterInfoCodecShort

data class SelectedCharacterPacket(val character: CharacterInfo?)

private val OptionalCharacterInfo = CharacterInfoCodecShort.optional()

object SelectedCharacterPacketCodec : PacketCodec<SelectedCharacterPacket>() {
  override fun CodecScope<SelectedCharacterPacket>.body() =
      SelectedCharacterPacket(
          character = field(OptionalCharacterInfo, SelectedCharacterPacket::character),
      )
}
