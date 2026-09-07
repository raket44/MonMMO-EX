package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S8

/**
 * Replaces one metatile on a loaded map (s2c 0x22, client f/eZ) - the GBA's setmetatile.
 * Bytecode-verified: three bytes key the map (f/jp0.Jx0 -> f/tS1.tt), two shorts pick the block
 * (f/tM.Pp(x, y, 0)), then f/eP1.Bp0(collision, metatile) stores the tile: the short indexes the
 * tileset's tile table and the byte becomes the block's collision. The client then re-checks the
 * local player and every entity standing on the map (f/eZ.aK). OpenMMO had this opcode as the
 * client-to-server interaction request's mirror; the two directions are unrelated packets.
 */
data class MapTileSetPacket(
    val regionId: Byte,
    val bankId: Byte,
    val mapId: Byte,
    val x: Short,
    val y: Short,
    val collision: Short,
    val metatileId: Short,
)

object MapTileSetPacketCodec : PacketCodec<MapTileSetPacket>() {
  override fun CodecScope<MapTileSetPacket>.body(): MapTileSetPacket {
    val regionId = field(S8) { it.regionId }
    val bankId = field(S8) { it.bankId }
    val mapId = field(S8) { it.mapId }
    val x = field(S16LE) { it.x }
    val y = field(S16LE) { it.y }
    val collision = field(S16LE) { it.collision }
    val metatileId = field(S16LE) { it.metatileId }
    return MapTileSetPacket(regionId, bankId, mapId, x, y, collision, metatileId)
  }
}
