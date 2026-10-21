package pirated.net.game.packets.battle

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class BattleMarkerPacket(
    val slot: Byte,
    val markerType: Byte,
    val value: Short,
)

object BattleMarkerPacketCodec : PacketCodec<BattleMarkerPacket>() {
  override fun CodecScope<BattleMarkerPacket>.body(): BattleMarkerPacket {
    val slot = field(S8) { it.slot }
    val markerType = field(S8) { it.markerType }
    val value = field(S16LE) { it.value }
    return BattleMarkerPacket(slot, markerType, value)
  }
}
