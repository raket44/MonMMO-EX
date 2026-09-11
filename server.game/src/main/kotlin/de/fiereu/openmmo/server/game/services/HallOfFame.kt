package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket
import de.fiereu.openmmo.net.game.packets.WorldActionDispatchPacket

/**
 * The client's own Hall of Fame (bytecode, client 31914, 2026-09-11):
 *
 * - s2c 0xB6 (f/Ty0, [WorldActionDispatchPacket]) with action 32 opens the built-in screen f/AO0
 *   ("League Champion! Congratulations!", the player's name, the party around the trainer, "Time
 *   played") through f/ln1.jK1(subject); the subject byte is the region the screen is shown for.
 *   The screen reads the party and the play time from the client's own state - nothing else is
 *   sent. It is the screen for EVERY region's ending; the ROM's hall-of-fame sequence never runs.
 * - When the player closes it, f/ln1.MM1 -> PX1(region, 0) sends c2s 0x21 (f/vG1, the dialog
 *   action response) with id = region and code 0. The server treats that as the acknowledgement
 *   of a pending dialog, so a script can wait for it and then warp the player home.
 * - The encounter counter (f/hS1 "encounter-counter-frame" on the HUD, f/NA0.sW1) exists while
 *   f/qK.u81(10, 770) holds: flag 770 in client store 10, whitelisted in f/LG0.Yw1 and written by
 *   the ordinary 0x2A flag packet. The first Hall of Fame entry unlocks it for good.
 */
object HallOfFame {
  /** The character flag that records a Hall of Fame entry (any region). */
  const val FLAG = "global/FLAG_HALL_OF_FAME"

  /** The client flag store the encounter counter reads (not a region). */
  const val ENCOUNTER_COUNTER_STORE: Byte = 10
  const val ENCOUNTER_COUNTER_FLAG = 770

  /** 0xB6 action that opens the built-in Hall of Fame screen. */
  const val SHOW_ACTION: Byte = 32

  /** Where the champion wakes up afterwards: the bedroom the game started in. */
  data class Home(val mapName: String, val x: Int, val y: Int, val facing: Direction = Direction.DOWN)

  /** new_game.c: FireRed starts at PlayersHouse_2F (6, 6); Emerald's bedroom heal locations are (4, 2). */
  fun home(regionId: Int, female: Boolean): Home? =
      when (regionId) {
        0 -> Home("PalletTown_PlayersHouse_2F", 6, 6)
        1 -> if (female) Home("LittlerootTown_MaysHouse_2F", 4, 2) else Home("LittlerootTown_BrendansHouse_2F", 4, 2)
        else -> null
      }

  fun showPacket(regionId: Int) = WorldActionDispatchPacket(SHOW_ACTION, regionId.toByte(), emptyList())

  fun encounterCounterPacket() = StoryFlagUpdatePacket(ENCOUNTER_COUNTER_STORE, ENCOUNTER_COUNTER_FLAG, 1)
}
