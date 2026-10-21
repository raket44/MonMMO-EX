package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class BattleCancelRequestPacket

object BattleCancelRequestPacketCodec : PacketCodec<BattleCancelRequestPacket>() {
  override fun CodecScope<BattleCancelRequestPacket>.body(): BattleCancelRequestPacket {
    return BattleCancelRequestPacket()
  }
}
