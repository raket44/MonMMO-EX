package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class BattleSequencePacket

object BattleSequencePacketCodec : PacketCodec<BattleSequencePacket>() {
  override fun CodecScope<BattleSequencePacket>.body(): BattleSequencePacket {
    return BattleSequencePacket()
  }
}
