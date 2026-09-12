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
    if (map.regionId.toInt() == KANTO && map.sourceName == WAREHOUSE_MAP) {
      touches += Touch { ctx -> lockRocketWarehouseDoor(ctx) }
    }
    if (map.regionId.toInt() == KANTO && map.sourceName == RUIN_VALLEY_MAP) {
      touches += Touch { ctx -> lockDottedHoleDoor(ctx) }
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
  /**
   * Five Island's Rocket Warehouse: the ROM keeps the door a warp and lets its script say "another
   * password is needed" as the player is already stepping in. Until both passwords have opened
   * it (FLAG_UNLOCKED_ROCKET_WAREHOUSE) the locked door graphic sits at the locked elevation, so
   * the client bumps; the unlock script's own setmetatile puts the open door back at the
   * ground's elevation (ScriptContext.setMetatile keeps the ROM tile's bits, not the lock's).
   */
  /**
   * Ruin Valley's Dotted Hole: the braille door opens only to Cut used facing it
   * (fldeff_cut.c CutMoveRuinValleyCheck, InteractionService.openDottedHole). Until then the
   * closed door sits on the locked layer; the client's own ROM data calls that tile a door, so
   * without the lock it kept walking into it while the server had no warp to give.
   */
  private fun lockDottedHoleDoor(ctx: ScriptContext) {
    if (ctx.isFlagSet(DOTTED_HOLE_CUT_FLAG)) return
    ctx.setMetatile(DOTTED_HOLE_DOOR_X, DOTTED_HOLE_DOOR_Y, DOTTED_HOLE_DOOR_CLOSED_METATILE, impassable = true, elevation = LOCKED_ELEVATION)
  }

  private fun lockRocketWarehouseDoor(ctx: ScriptContext) {
    if (ctx.isFlagSet(WAREHOUSE_UNLOCKED_FLAG)) return
    ctx.setMetatile(WAREHOUSE_DOOR_X, WAREHOUSE_DOOR_Y, WAREHOUSE_DOOR_LOCKED_METATILE, impassable = true, elevation = LOCKED_ELEVATION)
  }

  private fun lockCinnabarGymDoor(ctx: ScriptContext) {
    if (ctx.isFlagSet(CINNABAR_SECRET_KEY_FLAG)) return
    ctx.setMetatile(CINNABAR_GYM_DOOR_X, CINNABAR_GYM_DOOR_Y, CINNABAR_GYM_DOOR_METATILE, impassable = true, elevation = LOCKED_ELEVATION)
  }

  /**
   * The badge guards of Route 23 and the Route 22 gate (data/scripts/route23.inc): a trigger row
   * in front of each guard checks one badge, and the row is ARMED while the map's scene variable
   * still names that gate. The tiles just north of each armed trigger - the way onward - are
   * raised, badge or no badge: on the cartridge the guard stops everyone first and only then lets
   * the badge holder through. The guard's "go right ahead" sets the scene variable past the
   * gate, which [onVarChanged] sees and lifts the row before the script releases the player.
   * Guards on the row itself stay the client's own npc collision.
   */
  private fun lockBadgeGates(ctx: ScriptContext, map: MapDef, gates: List<de.fiereu.openmmo.maps.MapCoordScript>) {
    val seen = HashSet<Int>()
    for (gate in gates) {
      val x = gate.x
      val y = gate.y - 1
      if (!seen.add((x shl 16) or y)) continue
      val tile = map.tileAt(x, y) ?: continue
      if (tile.blocksMovement()) continue
      val armed = ctx.getVar(gate.varKey) == gate.value
      if (armed) {
        if (!ctx.hasTileOverride(x, y)) ctx.setMetatile(x, y, tile.material.toInt() and 0xFFFF, impassable = true, elevation = LOCKED_ELEVATION)
      } else if (ctx.hasTileOverride(x, y)) {
        ctx.restoreMetatile(x, y)
      }
    }
  }

  /** A script wrote [key]: if it is a badge gate's scene variable on the current map, re-evaluate the locks. */
  fun onVarChanged(ctx: ScriptContext, key: String) {
    val map = ctx.currentMap() ?: return
    val gates = map.coordScripts.filter { it.script.endsWith(BADGE_GATE_SUFFIX) }
    if (gates.none { it.varKey == key }) return
    lockBadgeGates(ctx, map, gates)
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
  const val RUIN_VALLEY_MAP = "SixIsland_RuinValley"
  const val DOTTED_HOLE_CUT_FLAG = "kanto/FLAG_USED_CUT_ON_RUIN_VALLEY_BRAILLE"
  const val DOTTED_HOLE_DOOR_X = 24
  const val DOTTED_HOLE_DOOR_Y = 24
  /** METATILE_SeviiIslands67_DottedHoleDoor_Closed / _Open. */
  const val DOTTED_HOLE_DOOR_CLOSED_METATILE = 0x357
  const val DOTTED_HOLE_DOOR_OPEN_METATILE = 0x358
  private const val WAREHOUSE_MAP = "FiveIsland_Meadow"
  const val WAREHOUSE_UNLOCKED_FLAG = "kanto/FLAG_UNLOCKED_ROCKET_WAREHOUSE"
  const val WAREHOUSE_DOOR_X = 12
  const val WAREHOUSE_DOOR_Y = 21
  /** METATILE_SeviiIslands45_RocketWarehouseDoor_Locked. */
  const val WAREHOUSE_DOOR_LOCKED_METATILE = 0x30B
  private const val BADGE_GATE_SUFFIX = "BadgeGuardTrigger"
}
