package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket

/**
 * The client's permanent egg incubators (r32645 f/fb6.z61 -> f/be0.U00, bytecode-walked
 * 2026-09-16).
 *
 * The incubator page draws EIGHT permanent slots and paints `8 - U00()` of them with string 101796
 * ("This Permanent Incubator has not yet been unlocked."). U00 is NOT a count the server sends: the
 * client sums four flags of its own store 10 - the same store as the Hall of Fame encounter counter
 * at flag 770 - each worth a fixed number of slots (ids in f/qy4.gY1):
 *
 *     771 -> 5 slots    772 -> 1    773 -> 1    774 -> 1    (eight in total)
 *
 * Those weights mean only 0, 1, 2, 3, 5, 6, 7 and 8 are reachable; FOUR CANNOT BE EXPRESSED, so a
 * one-slot-per-milestone ramp is not possible. Owner's call (2026-09-16): the first Hall of Fame
 * entry, in any region, flips 771 for five slots, and the first daycare man spoken to adds 772 for
 * a sixth. 773 and 774 are deliberately held back for special unlocks later.
 *
 * Both are permanent: the character flags below record them, and login replays the client flags.
 */
object Incubators {
  /** The client flag store the incubator page reads (not a region). */
  const val STORE: Byte = 10

  /** Five slots, on the first Hall of Fame entry in any region. */
  const val FIRST_CHAMPION_FLAG = 771

  /** One slot, the first time any region's daycare man is spoken to. */
  const val DAYCARE_MAN_FLAG = 772

  /** Held back for special unlocks; one slot each. */
  val RESERVED_FLAGS = listOf(773, 774)

  /** The character flag recording that a daycare man has been met (any region). */
  const val MET_DAYCARE_MAN = "global/FLAG_MET_DAYCARE_MAN"

  fun packet(flag: Int) = StoryFlagUpdatePacket(STORE, flag, 1)

  fun firstChampionPacket() = packet(FIRST_CHAMPION_FLAG)

  fun daycareManPacket() = packet(DAYCARE_MAN_FLAG)
}
