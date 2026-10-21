package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class BattleLeavePacket

object BattleLeavePacketCodec : PacketCodec<BattleLeavePacket>() {
  override fun CodecScope<BattleLeavePacket>.body(): BattleLeavePacket {
    return BattleLeavePacket()
  }
}
