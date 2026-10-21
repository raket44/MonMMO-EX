package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class TournamentRegistrationPacket(
    val register: Boolean,
)

object TournamentRegistrationPacketCodec : PacketCodec<TournamentRegistrationPacket>() {
  override fun CodecScope<TournamentRegistrationPacket>.body(): TournamentRegistrationPacket {
    val register = field(Bool) { it.register }
    return TournamentRegistrationPacket(register)
  }
}
