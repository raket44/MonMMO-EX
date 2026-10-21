package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class InstanceReadyCheckPacket(val isReady: Boolean)

object InstanceReadyCheckPacketCodec : PacketCodec<InstanceReadyCheckPacket>() {
  override fun CodecScope<InstanceReadyCheckPacket>.body(): InstanceReadyCheckPacket {
    val isReady = field(Bool) { it.isReady }
    return InstanceReadyCheckPacket(isReady)
  }
}
