package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.U8
import de.fiereu.bytecodec.enumByOrdinalByte
import de.fiereu.openmmo.common.enums.Direction

/** Opcode 0xEA (s2c). A GBA-map entity's new tile after one step. */
data class GbaEntityMovePacket(
    val entityId: Long,
    val bankId: Int,
    val mapId: Int,
    val x: Int,
    val y: Int,
    /**
     * The ELEVATION the client gives the entity (f/pC: opcode 0xEA's flag bit 8 makes this byte
     * Wi1.RW1, the tile elevation). FireRed water is 1, ordinary ground 3; a surfer snapped to 2
     * cannot step onto elevation-1 water again (the post-battle "stuck on the water" bug).
     */
    val elevation: Int = 3,
    val direction: Direction,
)

object GbaEntityMovePacketCodec : PacketCodec<GbaEntityMovePacket>() {
  private val DirectionCodec = enumByOrdinalByte<Direction>()

  override fun CodecScope<GbaEntityMovePacket>.body(): GbaEntityMovePacket {
    val entityId = field(S64LE) { it.entityId }
    val bankId = field(U8) { it.bankId }
    val mapId = field(U8) { it.mapId }
    val x = field(U8) { it.x }
    val y = field(U8) { it.y }
    val elevation = field(U8) { it.elevation }
    val direction = field(DirectionCodec) { it.direction }
    return GbaEntityMovePacket(entityId, bankId, mapId, x, y, elevation, direction)
  }
}
