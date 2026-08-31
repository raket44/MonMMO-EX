package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.U8
import de.fiereu.openmmo.common.enums.Direction

/**
 * Opcode 0xEC (s2c). One animated step for an entity ON a Gen 5 rail. The client routes every
 * opcode 224-239 into the same EntityMove handler (f.pC) with the opcode's low 4 bits as field
 * flags: bit2 = short coords, bit3 = the extra byte that lands in the position's RW1 slot - the
 * RAIL LINE (verified against the f.Wi1(B,B,B,Z,S,S,B,B) constructor: arg 7 -> RW1). The direction
 * byte carries the rail flag in bit 3 (KS1); without KS1 + the right RW1 the target resolves on the
 * wrong rail and the client discards the move (the earlier 0xE5 variant put the line into Ai and
 * was ignored). Plain-tile steps use [EntityMovePacket] (0xE4).
 */
data class RailEntityMovePacket(
    val entityId: Long,
    val railLine: Int,
    val x: Int,
    val y: Int,
    val direction: Direction,
)

object RailEntityMovePacketCodec : PacketCodec<RailEntityMovePacket>() {

  override fun CodecScope<RailEntityMovePacket>.body(): RailEntityMovePacket {
    val entityId = field(S64LE) { it.entityId }
    val x = field(S16LE) { it.x.toShort() }.toInt()
    val y = field(S16LE) { it.y.toShort() }.toInt()
    val railLine = field(U8) { it.railLine and 0xFF }
    val dirByte = field(U8) { it.direction.ordinal or 0x08 }
    return RailEntityMovePacket(entityId, railLine, x, y, Direction.entries[dirByte and 0x03])
  }
}
