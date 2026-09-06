package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.Utf16LeNullTerminated

/**
 * c2s 0xD0: the player invited [targetName] to a link. Seen live: the whole payload is the name.
 * The server answers by prompting the target (dialog kind 4).
 */
data class LinkRequestPacket(val targetName: String)

object LinkRequestPacketCodec : PacketCodec<LinkRequestPacket>() {
  override fun CodecScope<LinkRequestPacket>.body(): LinkRequestPacket {
    val targetName = field(Utf16LeNullTerminated) { it.targetName }
    return LinkRequestPacket(targetName)
  }
}
