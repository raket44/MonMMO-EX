package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S8

/**
 * Swaps a loaded GBA map's block grid for another footer (s2c 0x2D, client f/x50). Bytecode-verified:
 * three bytes key the map (f/jp0.Jx0 -> f/tS1.tt, same key as LoadMap), then a short footer id
 * looked up in f/MT0.fv0[region].oF0 - the ROM's layouts plus every `world_map_footers/<region>-<id>.bin`
 * a mod ships - and f/Ro.pn1 rebuilds every block from it, textures and collision alike. Retail
 * uses it for map variants; we ship alternate footers in our mod (launcher ClientMapFooters) for
 * the puzzles the ROM solves with setmetatile, whose per-tile packet (0x22) draws wrong here.
 * A map that is not loaded, or a footer the client does not know, is silently ignored.
 */
data class MapLayoutSwitchPacket(
    val regionId: Byte,
    val bankId: Byte,
    val mapId: Byte,
    val footerId: Short,
)

object MapLayoutSwitchPacketCodec : PacketCodec<MapLayoutSwitchPacket>() {
  override fun CodecScope<MapLayoutSwitchPacket>.body(): MapLayoutSwitchPacket {
    val regionId = field(S8) { it.regionId }
    val bankId = field(S8) { it.bankId }
    val mapId = field(S8) { it.mapId }
    val footerId = field(S16LE) { it.footerId }
    return MapLayoutSwitchPacket(regionId, bankId, mapId, footerId)
  }
}
