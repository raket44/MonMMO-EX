package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

data class StorageBoxClosePacket(val placeholder: Unit = Unit)

object StorageBoxClosePacketCodec : PacketCodec<StorageBoxClosePacket>() {
  override fun CodecScope<StorageBoxClosePacket>.body(): StorageBoxClosePacket {
    return StorageBoxClosePacket()
  }
}
