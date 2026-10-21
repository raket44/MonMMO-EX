package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated

data class BlockPlayerPacket(val username: String, val reason: String)

object BlockPlayerPacketCodec : PacketCodec<BlockPlayerPacket>() {
  override fun CodecScope<BlockPlayerPacket>.body(): BlockPlayerPacket {
    val username = field(Utf16LeNullTerminated) { it.username }
    val reason = field(Utf16LeNullTerminated) { it.reason }
    return BlockPlayerPacket(username, reason)
  }
}
