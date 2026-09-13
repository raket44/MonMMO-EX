package de.fiereu.openmmo.net.game.packets.battle.moves

import de.fiereu.bytecodec.*

/**
 * s2c 0x17 - opens the move-learn screen for a monster that already knows four moves. Client r32645
 * reads it in `f/lg.Rl0`: the monster uid, a u8 count, that many offered move ids. The handler
 * queues `f/gq5` (string 3409 "wants to learn... already knows four moves"), which opens the
 * "Select your moves" screen `f/du5`; its answer is [MoveLearnReplyPacket]. An empty list does
 * nothing, and there is no form for a move that took a free slot - that is only a moveset update.
 *
 * The desktop client 31914 read uid + slot + move instead (commit 3801e162a); r32645 is the only
 * client now, and that layout made every prompt a short read the client dropped.
 */
data class MoveLearnPromptPacket(
    val entityId: Long,
    val offered: List<Short>,
)

object MoveLearnPromptPacketCodec : PacketCodec<MoveLearnPromptPacket>() {
  override fun CodecScope<MoveLearnPromptPacket>.body(): MoveLearnPromptPacket {
    val entityId = field(S64LE) { it.entityId }
    val offered = field(S16LE.listPrefixed(U8)) { it.offered }
    return MoveLearnPromptPacket(entityId, offered)
  }
}
