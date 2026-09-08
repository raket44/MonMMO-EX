package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8
import de.fiereu.bytecodec.U8

/**
 * The darkness overlay (s2c 0xC1, client f/COM3 -> f/ln1.HF0 and f/tM.Xl1), read out of the
 * renderer this time (f/Ma0 3202, f/w0.gM0): [level] is the CLIENT's light radius, 0 = pitch
 * black up to 7 = no overlay at all - the ROM's flash level runs the other way, so the sender
 * converts. On a map whose lighting byte is not REGULAR the overlay is drawn unless HF0 is 7; on
 * a REGULAR map it is drawn only when Xl1 is set, so [forceDark] is the way to darken a lit map,
 * never a "lit" flag. Sending (0, true) for Flash therefore made the cave darker and stuck
 * (2026-09-08). A map load resets HF0, which is why Flash lasts one map on the GBA.
 */
data class MapLightingPacket(val level: Byte, val forceDark: Boolean = false) {
  companion object {
    /** No overlay: the client's fully-lit radius. */
    const val LIT: Byte = 7

    /** From the ROM's setflashlevel argument (0 = lit, larger = darker) to the client's radius. */
    fun fromRomLevel(romLevel: Int): MapLightingPacket = MapLightingPacket((7 - romLevel).coerceIn(0, 7).toByte())
  }
}

object MapLightingPacketCodec : PacketCodec<MapLightingPacket>() {
  override fun CodecScope<MapLightingPacket>.body(): MapLightingPacket {
    val level = field(S8) { it.level }
    val forceDark = field(U8) { if (it.forceDark) 1 else 0 } == 1
    return MapLightingPacket(level, forceDark)
  }
}
