package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.ScriptContext

/**
 * Server-side touches on top of the ROM's map entry scripts: polish the cartridge never needed
 * because its scripts ran on the same frame as the player's step. Each touch runs with the map's
 * ON_TRANSITION scripts, after the tile overrides of the previous map were dropped, so a tile it
 * sets lasts exactly one visit like a ROM setmetatile.
 */
object MapEntryPolish {
  fun interface Touch {
    fun apply(ctx: ScriptContext)
  }

  private val touches: Map<Triple<Int, Int, Int>, Touch> =
      mapOf(
          // Cinnabar Island while the Gym is locked (no Secret Key yet): the ROM keeps the door a
          // real door and relies on the trigger tile below it to push the player back. The client
          // starts its door animation and fade the moment the player presses into a door, before
          // the trigger's lock can reach it, and a door step the lock then mutes is a black
          // screen. So until the key is found the door tile shows the gym's wall piece and is
          // solid: the player bumps, the trigger still says the door is locked, and the door
          // appears once the key is in the bag.
          Triple(0, 3, 8) to
              Touch { ctx ->
                if (!ctx.isFlagSet(CINNABAR_SECRET_KEY_FLAG)) ctx.setMetatile(CINNABAR_GYM_DOOR_X, CINNABAR_GYM_DOOR_Y, CINNABAR_GYM_WALL_METATILE, impassable = true)
              },
      )

  fun touchFor(map: MapDef): Touch? =
      touches[Triple(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())]

  /** FireRed: set when the Secret Key's item ball in the Mansion basement is taken. */
  const val CINNABAR_SECRET_KEY_FLAG = "kanto/FLAG_HIDE_POKEMON_MANSION_B1F_SECRET_KEY"
  const val CINNABAR_GYM_DOOR_X = 20
  const val CINNABAR_GYM_DOOR_Y = 4
  /** gTileset_CinnabarIsland 0x159: the gym's front wall with its red band (the door is 0x15B). */
  const val CINNABAR_GYM_WALL_METATILE = 0x159
}
