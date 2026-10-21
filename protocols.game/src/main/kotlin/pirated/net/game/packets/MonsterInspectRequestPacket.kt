package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8

data class MonsterInspectRequestPacket(
    val entityId: Long,
    val contextType: Byte,
    val viewMode: Byte,
)

object MonsterInspectRequestPacketCodec : PacketCodec<MonsterInspectRequestPacket>() {
  override fun CodecScope<MonsterInspectRequestPacket>.body(): MonsterInspectRequestPacket {
    val entityId = field(S64LE) { it.entityId }
    val contextType = field(S8) { it.contextType }
    val viewMode = field(S8) { it.viewMode }
    return MonsterInspectRequestPacket(entityId, contextType, viewMode)
  }
}
