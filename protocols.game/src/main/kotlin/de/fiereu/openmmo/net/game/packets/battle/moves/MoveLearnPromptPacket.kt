package de.fiereu.openmmo.net.game.packets.battle.moves

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.S8

/**
 * s2c 0x17 - one move a monster is learning, decoded from client 31914's reader `f/QX`: the
 * monster uid, a slot byte, the move id. The handler queues `f/vR0` on the battle: a slot of 0-3
 * prints "{mon} learned {move}!" (ROM text 157/41) for a move that went straight into that slot; a
 * NEGATIVE slot prints "{mon} wants to learn {move}" (157/32) and opens the forget dialog `f/mB`,
 * which answers with [MoveLearnReplyPacket]. Several moves mean several packets.
 *
 * The earlier codec (uid + u8-counted move list, from client 32710 captures) sent the COUNT where
 * this client reads the slot, so every single-move prompt rendered as "learned" into slot 1 and no
 * dialog ever opened.
 */
data class MoveLearnPromptPacket(
    val entityId: Long,
    /** 0-3 = learned into that slot; -1 = ask which move to forget. */
    val slot: Byte,
    val moveId: Short,
) {
  companion object {
    const val ASK: Byte = -1
  }
}

object MoveLearnPromptPacketCodec : PacketCodec<MoveLearnPromptPacket>() {
  override fun CodecScope<MoveLearnPromptPacket>.body(): MoveLearnPromptPacket {
    val entityId = field(S64LE) { it.entityId }
    val slot = field(S8) { it.slot }
    val moveId = field(S16LE) { it.moveId }
    return MoveLearnPromptPacket(entityId, slot, moveId)
  }
}
