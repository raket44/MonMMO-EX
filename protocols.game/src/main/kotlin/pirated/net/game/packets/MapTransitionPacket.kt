package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

class MapTransitionPacket {
  override fun equals(other: Any?): Boolean = other is MapTransitionPacket

  override fun hashCode(): Int = MapTransitionPacket::class.hashCode()
}

object MapTransitionPacketCodec : PacketCodec<MapTransitionPacket>() {
  override fun CodecScope<MapTransitionPacket>.body() = MapTransitionPacket()
}
