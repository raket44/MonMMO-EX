package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.Utf16LeNullTerminated

data class PcBoxRenamePacket(
    val boxIndex: Byte,
    val newName: String,
)

object PcBoxRenamePacketCodec : PacketCodec<PcBoxRenamePacket>() {
  override fun CodecScope<PcBoxRenamePacket>.body(): PcBoxRenamePacket {
    val boxIndex = field(S8) { it.boxIndex }
    val newName = field(Utf16LeNullTerminated) { it.newName }
    return PcBoxRenamePacket(boxIndex, newName)
  }
}
