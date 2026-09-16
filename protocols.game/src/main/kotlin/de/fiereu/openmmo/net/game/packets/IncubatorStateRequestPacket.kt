package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec

/**
 * c2s 0x70: the incubator page asking for its state. The client sends it from the screen's own
 * build method (`f/fb6.z61` constructs `f/dp5`, whose writer `n3` is EMPTY), so it arrives whenever
 * the player opens the page - the owner's "I" keypress, 2026-09-16.
 *
 * It is NOT a button. Both bulk buttons are computed client-side and move monsters with ordinary
 * container drags, needing nothing here: "Remove All" (string 1479) is `fb6.SB`, which checks the
 * PC has room and then walks the occupied incubator slots, and "Fill All" (1480) is `f/vg5` mode 6,
 * which walks the PC for eggs and the incubator for free slots.
 *
 * This opcode was first modelled as a GTL market-listings request, then briefly mistaken for the
 * Remove All button - which emptied the incubators every time the page was merely opened.
 */
data class IncubatorStateRequestPacket(val placeholder: Unit = Unit)

object IncubatorStateRequestPacketCodec : PacketCodec<IncubatorStateRequestPacket>() {
  override fun CodecScope<IncubatorStateRequestPacket>.body(): IncubatorStateRequestPacket {
    return IncubatorStateRequestPacket()
  }
}
