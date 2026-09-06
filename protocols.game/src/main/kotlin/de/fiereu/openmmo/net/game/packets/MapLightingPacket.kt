package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8
import de.fiereu.bytecodec.U8

/**
 * Lights a dark map (s2c 0xC1, client f/COM3). Bytecode-verified: byte [level] lands in
 * f/ln1.HF0 (the ROM's flash level, 0 = fully lit), and [lit] true sets f/tM.Xl1 on the map the
 * player stands in. The map darkens on load when its lighting byte is not REGULAR and Xl1 is
 * false, so a lit map goes dark again on the next map load - which is how Flash works on the GBA.
 */
data class MapLightingPacket(val level: Byte, val lit: Boolean)

object MapLightingPacketCodec : PacketCodec<MapLightingPacket>() {
  override fun CodecScope<MapLightingPacket>.body(): MapLightingPacket {
    val level = field(S8) { it.level }
    val lit = field(U8) { if (it.lit) 1 else 0 } == 1
    return MapLightingPacket(level, lit)
  }
}
