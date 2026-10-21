package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.common.Pokemon
import pirated.net.game.codecs.PokemonCodec

data class SocialProfileDialogOpenPacket(
    val status: Byte,
    val content: Pokemon?,
)

object SocialProfileDialogOpenPacketCodec : PacketCodec<SocialProfileDialogOpenPacket>() {
  override fun CodecScope<SocialProfileDialogOpenPacket>.body(): SocialProfileDialogOpenPacket {
    val status = field(S8) { it.status }
    val content = if (status.toInt() == 0) field(PokemonCodec) { it.content!! } else null
    return SocialProfileDialogOpenPacket(status, content)
  }
}
