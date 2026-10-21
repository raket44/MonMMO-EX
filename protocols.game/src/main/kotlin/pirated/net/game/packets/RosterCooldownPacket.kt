package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE

data class RosterCooldownPacket(
    val cooldownOffsetSeconds: Int,
)

object RosterCooldownPacketCodec : PacketCodec<RosterCooldownPacket>() {
  override fun CodecScope<RosterCooldownPacket>.body(): RosterCooldownPacket {
    val cooldownOffsetSeconds = field(S32LE) { it.cooldownOffsetSeconds }
    return RosterCooldownPacket(cooldownOffsetSeconds)
  }
}
