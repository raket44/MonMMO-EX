package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec

/**
 * c2s 0x70: the incubator page's "Remove All" button (client string 1479), which asks the server to
 * empty every incubator into the PC. The client class is `f/dp5` and its writer `n3` is EMPTY, so
 * the whole meaning is the opcode.
 *
 * Its sibling "Fill All" (string 1480) needs no packet at all: that button is computed client-side
 * (f/vg5 mode 6 reads the PC and the incubator container and walks the free slots), so it just
 * issues ordinary container drags, which is why pressing it sent nothing while the player had no
 * eggs to move (owner test, 2026-09-16).
 *
 * This opcode used to be modelled as a GTL market-listings request, which is why both button
 * presses landed on the wrong handler and did nothing.
 */
data class IncubatorRemoveAllPacket(val placeholder: Unit = Unit)

object IncubatorRemoveAllPacketCodec : PacketCodec<IncubatorRemoveAllPacket>() {
  override fun CodecScope<IncubatorRemoveAllPacket>.body(): IncubatorRemoveAllPacket {
    return IncubatorRemoveAllPacket()
  }
}
