package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.Utf16LeNullTerminated

/**
 * c2s 0x51 (client writer f/Lpt8): the player asked to trade with [targetName]. Seen live: the
 * whole payload is the name. The server answers by prompting the target (dialog kind 3).
 */
data class TradeRequestPacket(val targetName: String)

object TradeRequestPacketCodec : PacketCodec<TradeRequestPacket>() {
  override fun CodecScope<TradeRequestPacket>.body(): TradeRequestPacket {
    val targetName = field(Utf16LeNullTerminated) { it.targetName }
    return TradeRequestPacket(targetName)
  }
}
