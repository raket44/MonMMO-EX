package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S64LE

/**
 * Opcode 0x2E (c2s, f/KY1): the player clicked a monster link in chat (f/lP1.nW1). The two ids
 * are the link's owner (the line's sender, or the clicker's own id for their own line) and the
 * monster. The client opens nothing itself; it waits for the server's reply (2026-09-11).
 */
data class ChatLinkInspectRequestPacket(
    val ownerId: Long,
    val monsterId: Long,
)

object ChatLinkInspectRequestPacketCodec : PacketCodec<ChatLinkInspectRequestPacket>() {
  override fun CodecScope<ChatLinkInspectRequestPacket>.body(): ChatLinkInspectRequestPacket {
    val ownerId = field(S64LE) { it.ownerId }
    val monsterId = field(S64LE) { it.monsterId }
    return ChatLinkInspectRequestPacket(ownerId, monsterId)
  }
}
