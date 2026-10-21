package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.Utf16LeNullTerminated

data class NpcDialogResponsePacket(
    val dialogType: Byte,
    val responseText: String,
)

object NpcDialogResponsePacketCodec : PacketCodec<NpcDialogResponsePacket>() {
  override fun CodecScope<NpcDialogResponsePacket>.body(): NpcDialogResponsePacket {
    val dialogType = field(S8) { it.dialogType }
    val responseText = field(Utf16LeNullTerminated) { it.responseText }
    return NpcDialogResponsePacket(dialogType, responseText)
  }
}
