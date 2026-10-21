package pirated.net.game.packets.battle

import pirated.bytecodec.*

data class BattleSwitchSelectionsPacket(
    val selectedEntityIds: List<Long>,
)

private val SelectedEntityIdsCodec = S64LE.listPrefixed(U8)

object BattleSwitchSelectionsPacketCodec : PacketCodec<BattleSwitchSelectionsPacket>() {
  override fun CodecScope<BattleSwitchSelectionsPacket>.body(): BattleSwitchSelectionsPacket {
    val selectedEntityIds = field(SelectedEntityIdsCodec) { it.selectedEntityIds }
    return BattleSwitchSelectionsPacket(selectedEntityIds)
  }
}
