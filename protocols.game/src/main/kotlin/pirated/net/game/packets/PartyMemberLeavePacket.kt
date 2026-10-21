package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE

data class PartyMemberLeavePacket(
    val memberId: Long,
)

object PartyMemberLeavePacketCodec : PacketCodec<PartyMemberLeavePacket>() {
  override fun CodecScope<PartyMemberLeavePacket>.body(): PartyMemberLeavePacket {
    val memberId = field(S64LE) { it.memberId }
    return PartyMemberLeavePacket(memberId)
  }
}
