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
    /**
     * The real northern-hemisphere season in the world clock zone: Dec-Feb winter, Mar-May spring,
     * Jun-Aug summer, Sep-Nov autumn - not the Gen 5 monthly rotation, which had September spring.
     */
    /** A developer-forced season (/devseason); null follows the calendar. */
    @Volatile var override: Season? = null

    fun current(): Season =
        override ?: when (WorldClock.now().monthValue) {
          12, 1, 2 -> WINTER
          3, 4, 5 -> SPRING
          6, 7, 8 -> SUMMER
          else -> AUTUMN
        }

    fun fromId(id: Int): Season = entries.firstOrNull { it.id == id } ?: NONE
  }
}

data class SeasonPacket(val season: Season)

val SeasonPacketCodec: Codec<SeasonPacket> =
    U8.imap(
        decode = { SeasonPacket(Season.fromId(it.toByte().toInt())) },
        encode = { it.season.id and 0xFF },
    )
