package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.ScriptContext

/**
 * Server-side touches on top of the ROM's map entry scripts: polish the cartridge never needed
 * because its scripts ran on the same frame as the player's step. Each touch runs before the
 * map's ON_TRANSITION, after the tile overrides of the previous map were dropped, so a tile it
 * sets lasts exactly one visit like a ROM setmetatile.
 *
 * The tool is the elevation lock: a tile re-sent with its own graphic at another elevation. The
 * client's step validator refuses a step between different non-zero elevations before it ever
 * looks for a door or sends the step (f/NV0.Pj1), so the player bumps on the spot with nothing to
 * undo afterwards - play-verified on Cinnabar's locked Gym door.
 */
object MapEntryPolish {
  fun interface Touch {
    fun apply(ctx: ScriptContext)
  }

  /** Every touch that applies to [map], or null when none does. */
  fun touchFor(map: MapDef): Touch? {
    val touches = mutableListOf<Touch>()
    if (map.regionId.toInt() == KANTO && map.bankId.toInt() == CINNABAR_BANK && map.mapId.toInt() == CINNABAR_MAP) {
      touches += Touch { ctx -> lockCinnabarGymDoor(ctx) }
    }
    val gates = map.coordScripts.filter { it.script.endsWith(BADGE_GATE_SUFFIX) }
    if (gates.isNotEmpty()) touches += Touch { ctx -> lockBadgeGates(ctx, map, gates) }
    if (touches.isEmpty()) return null
    return Touch { ctx -> touches.forEach { it.apply(ctx) } }
  }

  /**
   * Cinnabar Island while the Gym is locked (no Secret Key yet): the ROM keeps the door a real
   * door and relies on the trigger tile below it to push the player back; the client starts its
   * door animation the moment the player presses into a door. Until the key is found the door
   * keeps its graphic at elevation 4 instead of the ground's 3: the player bumps, the trigger
   * still says the door is locked, and the door is a door again once the key is in the bag.
   */
  private fun lockCinnabarGymDoor(ctx: ScriptContext) {
    if (ctx.isFlagSet(CINNABAR_SECRET_KEY_FLAG)) return
    ctx.setMetatile(CINNABAR_GYM_DOOR_X, CINNABAR_GYM_DOOR_Y, CINNABAR_GYM_DOOR_METATILE, impassable = true, elevation = LOCKED_ELEVATION)
  }

  /**
   * The badge guards of Route 23 and the Route 22 gate (data/scripts/route23.inc): a trigger row
   * in front of each guard checks one badge and walks the player a tile back without it. The
   * tiles just north of each trigger - the way onward - are raised while that badge is missing,
   * so nobody slips past the row before the guard's script lands. Guards on the row itself stay
   * the client's own npc collision. Nothing is touched once the badge is held.
   */
  private fun lockBadgeGates(ctx: ScriptContext, map: MapDef, gates: List<de.fiereu.openmmo.maps.MapCoordScript>) {
    val namespace = de.fiereu.openmmo.common.enums.Region.byId(map.regionId.toInt())?.name?.lowercase() ?: return
    val locked = HashSet<Int>()
    for (gate in gates) {
      val badge = BADGE_BY_GATE.entries.firstOrNull { gate.script.contains(it.key) }?.value ?: continue
      if (ctx.isFlagSet("$namespace/FLAG_BADGE0${badge}_GET")) continue
      val x = gate.x
      val y = gate.y - 1
      val tile = map.tileAt(x, y) ?: continue
      if (tile.blocksMovement()) continue
      if (!locked.add((x shl 16) or y)) continue
      ctx.setMetatile(x, y, tile.material.toInt() and 0xFFFF, impassable = true, elevation = LOCKED_ELEVATION)
    }
  }

  private const val KANTO = 0
  private const val CINNABAR_BANK = 3
  private const val CINNABAR_MAP = 8
  /** FireRed: set when the Secret Key's item ball in the Mansion basement is taken. */
  const val CINNABAR_SECRET_KEY_FLAG = "kanto/FLAG_HIDE_POKEMON_MANSION_B1F_SECRET_KEY"
  const val CINNABAR_GYM_DOOR_X = 20
  const val CINNABAR_GYM_DOOR_Y = 4
  /** gTileset_CinnabarIsland 0x15B: the gym door itself. */
  const val CINNABAR_GYM_DOOR_METATILE = 0x15B
  /** Any GBA elevation but the ground's 3 (and not the 0 wildcard): the client refuses the step. */
  const val LOCKED_ELEVATION = 4
  private const val BADGE_GATE_SUFFIX = "BadgeGuardTrigger"
  private val BADGE_BY_GATE =
      linkedMapOf(
          "Boulder" to 1, "Cascade" to 2, "Thunder" to 3, "Rainbow" to 4,
          "Soul" to 5, "Marsh" to 6, "Volcano" to 7, "Earth" to 8,
      )
}
