package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class AppearanceRequestResultPacket(
    val entityId: Long,
    val accepted: Boolean,
)

object AppearanceRequestResultPacketCodec : PacketCodec<AppearanceRequestResultPacket>() {
  override fun CodecScope<AppearanceRequestResultPacket>.body(): AppearanceRequestResultPacket {
    val entityId = field(S64LE) { it.entityId }
    val accepted = field(Bool) { it.accepted }
    return AppearanceRequestResultPacket(entityId, accepted)
  }
}
