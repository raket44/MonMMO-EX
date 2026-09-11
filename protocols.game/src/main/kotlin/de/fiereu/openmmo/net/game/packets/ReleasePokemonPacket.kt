package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S64LE

/**
 * Opcode 0x0C (c2s, f/pq): release one monster by id. Sent by the summary window's "Confirm
 * Release" button (f/a.D80, after the "Are you sure you want to release {00}?" prompt), by the
 * box context menu's "Release All" (f/ON0.fW1, one packet per monster) and f/ln1.sv0. The client
 * removes nothing itself: it closes the summary and waits for the container to be re-sent
 * (2026-09-11).
 */
data class ReleasePokemonPacket(val monsterId: Long)

object ReleasePokemonPacketCodec : PacketCodec<ReleasePokemonPacket>() {
  override fun CodecScope<ReleasePokemonPacket>.body(): ReleasePokemonPacket {
    val monsterId = field(S64LE) { it.monsterId }
    return ReleasePokemonPacket(monsterId)
  }
}
