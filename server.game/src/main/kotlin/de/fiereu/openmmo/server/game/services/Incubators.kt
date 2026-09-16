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

  /**
   * Where eggs actually sit: client container `f/xe1.ma`, wire byte 13, capacity FIFTEEN - the
   * eight permanent slots plus the seven temporary ones. The page (f/fb6) lists it next to the PC
   * (byte 0), which is what its two buttons move eggs between:
   *  - c2s 0x70 (client f/dp5, EMPTY body) from the incubator page itself, and
   *  - c2s 0xd3 (client f/wf0, EMPTY body) from f/vg5.
   * Our protocol table still maps those two opcodes to GtlMarketListingsRequest and
   * CancelSocialInteraction, so both currently land on the wrong handler - nothing implements the
   * moves yet (owner pressed both on 2026-09-16 to surface them).
   */
  val CONTAINER = de.fiereu.openmmo.common.enums.PokemonContainer.INCUBATOR

  /**
   * How many permanent incubator slots the character has earned, from the flags it holds. The
   * client derives the same number from the flags we replay, so an egg must never be filed past
   * this or it lands in a slot still painted "not yet unlocked".
   */
  fun unlockedSlots(storyFlags: Set<String>): Int =
      (if (HallOfFame.FLAG in storyFlags) 5 else 0) + (if (MET_DAYCARE_MAN in storyFlags) 1 else 0)

  /**
   * PokeMMO eggs hatch on a TIMER, not on steps (owner, 2026-09-16). Seconds per egg cycle, so a
   * common 20-cycle species takes about five minutes and a 120-cycle legendary about half an hour.
   * Override with -Dmonmmo.eggSecondsPerCycle.
   */
  val secondsPerEggCycle: Int =
      System.getProperty("monmmo.eggSecondsPerCycle")?.toIntOrNull()?.coerceAtLeast(1) ?: 15

  /**
   * No egg ever takes longer than five minutes (owner, 2026-09-16), so the per-cycle scale only
   * separates the quick species from the slow ones up to this ceiling.
   */
  val MAX_HATCH_SECONDS: Int =
      System.getProperty("monmmo.eggMaxHatchSeconds")?.toIntOrNull()?.coerceAtLeast(1) ?: 300

  /** Flame Body or Magma Armor sitting in the incubator's own slot takes 20% off the wait. */
  const val FLAME_BODY_BONUS = 0.20

  /** Donator Status takes another 10% off, as its blurb says (client string 4001). */
  const val DONATOR_HATCH_BONUS = 0.10

  /** The abilities the page asks for: "Attach a {mon} with {Flame Body} or {Magma Armor}" (1475). */
  val HATCH_ABILITIES =
      setOf(
          de.fiereu.openmmo.common.enums.Ability.FLAME_BODY,
          de.fiereu.openmmo.common.enums.Ability.MAGMA_ARMOR,
      )

  /** Where an egg's hatch time is remembered, one per incubator slot. */
  fun hatchVarKey(slot: Int): String = "egg/hatch/$slot"

  /**
   * How long an egg of [eggCycles] takes to hatch, in seconds. The two bonuses stack, which is how
   * the client can report a combined "Hatching rate increased by {01}%" (string 1477).
   */
  fun hatchSeconds(eggCycles: Int, flameBody: Boolean = false, donator: Boolean = false): Int {
    val base = (eggCycles.coerceAtLeast(1) * secondsPerEggCycle).coerceAtMost(MAX_HATCH_SECONDS)
    var reduction = 0.0
    if (flameBody) reduction += FLAME_BODY_BONUS
    if (donator) reduction += DONATOR_HATCH_BONUS
    return (base * (1.0 - reduction)).toInt().coerceAtLeast(1)
  }

  fun packet(flag: Int) = StoryFlagUpdatePacket(STORE, flag, 1)

  fun firstChampionPacket() = packet(FIRST_CHAMPION_FLAG)

  fun daycareManPacket() = packet(DAYCARE_MAN_FLAG)
}
