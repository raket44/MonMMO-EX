package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class MapReadySignalPacket(val placeholder: Unit = Unit)

object MapReadySignalPacketCodec : PacketCodec<MapReadySignalPacket>() {
  override fun CodecScope<MapReadySignalPacket>.body(): MapReadySignalPacket {
    return MapReadySignalPacket()
  }
}
