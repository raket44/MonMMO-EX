package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class BattleTransitionReadyPacket

object BattleTransitionReadyPacketCodec : PacketCodec<BattleTransitionReadyPacket>() {
  override fun CodecScope<BattleTransitionReadyPacket>.body(): BattleTransitionReadyPacket {
    return BattleTransitionReadyPacket()
  }
}
