package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/**
 * The Gen 5 season. OpenMMO originally guessed this packet was a "map transition ack" with kinds
 * WARP(1)/WORLD_ENTRY(2), but the client handler (f.IK0 -> f.u2.GM0) clamps the byte to -1..3,
 * stores it as the global season, and fires graphics-refresh listeners when it changes; seasonal
 * resource lookups then add it to their file index (f.CK0, f.Hg). Sending WARP everywhere meant
 * permanent SUMMER. -1 (NONE) turns seasonal rendering off.
 */
enum class Season(val id: Int) {
  NONE(-1),
  SPRING(0),
  SUMMER(1),
  AUTUMN(2),
  WINTER(3),
  ;

  companion object {
    /** Gen 5 rotates monthly in the world clock zone: Jan=SPRING, Feb=SUMMER, Mar=AUTUMN, Apr=WINTER, then repeats. */
    fun current(): Season = entries[1 + (WorldClock.now().monthValue - 1) % 4]

    fun fromId(id: Int): Season = entries.firstOrNull { it.id == id } ?: NONE
  }
}

data class SeasonPacket(val season: Season)

val SeasonPacketCodec: Codec<SeasonPacket> =
    U8.imap(
        decode = { SeasonPacket(Season.fromId(it.toByte().toInt())) },
        encode = { it.season.id and 0xFF },
    )
