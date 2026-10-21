package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class TournamentTeleportAcceptPacket

object TournamentTeleportAcceptPacketCodec : PacketCodec<TournamentTeleportAcceptPacket>() {
  override fun CodecScope<TournamentTeleportAcceptPacket>.body(): TournamentTeleportAcceptPacket {
    return TournamentTeleportAcceptPacket()
  }
}
