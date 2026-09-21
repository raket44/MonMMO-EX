package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.server.game.session.PlayerState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Striaton gym's curtains, made solid.
 *
 * The curtains themselves are an animation on the building model: the client's own handler
 * (`f/iu5`) only sets an animation progress and plays it, and the ROM's tile plane leaves the whole
 * opening (x 9..15) walkable. What actually stops the player is an OBJECT - the ROM puts one
 * invisible actor (sprite 185, the "no talk" script 2000) at the left end of each opening, and the
 * client makes any object solid on its tile. So exactly one tile of each curtain blocked and the
 * player walked around it (owner, 2026-09-21).
 *
 * The owner's call: whatever makes that one tile solid should make the whole opening solid until
 * the curtain opens. This puts the same kind of object on the rest of each row and takes them away
 * as each curtain opens, which is the one lever the client already honours - DS maps are
 * client-authoritative for movement, so a server-side refusal would only rubber-band the player.
 *
 * Which curtains are open is the ROM's own answer: the gym's map-enter script (file 14 entry index
 * 15) turns VAR 16514 into the mask it hands the client, and that table is [MASK_BY_STATE].
 */
@Singleton
class NdsCurtainWalls @Inject constructor(private val ndsNpcs: NdsNpcs, private val npcService: NpcService) {

  fun sync(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    if (state.regionId != UNOVA || state.bankId != GYM_BANK || state.mapId != GYM_MAP) return
    val open = MASK_BY_STATE.getOrElse(storyFlags[PUZZLE_VAR] ?: 0) { ALL_OPEN }
    CURTAIN_ROWS.forEachIndexed { curtain, y ->
      val closed = (open shr curtain) and 1 == 0
      for ((i, x) in WALL_COLUMNS.withIndex()) {
        val id = FIRST_WALL_ID + curtain * WALL_COLUMNS.size + i
        if (closed) {
          if (!ndsNpcs.isMade(UNOVA, GYM_BANK, GYM_MAP, id)) {
            ndsNpcs.define(
                UNOVA, GYM_BANK, GYM_MAP,
                NdsNpcs.Npc(
                    index = id, id = id, sprite = WALL_SPRITE, movement = 0, flag = 0, script = 0,
                    facing = 1, xRange = 0, yRange = 0, x = x, y = y))
          }
          npcService.spawnNpc(session, UNOVA, GYM_BANK, GYM_MAP, id)
        } else {
          npcService.despawnNpc(session, UNOVA, GYM_BANK, GYM_MAP, id)
        }
      }
    }
  }

  private companion object {
    const val UNOVA = 2
    const val GYM_BANK = 7
    const val GYM_MAP = 0

    /** VAR 16514 (0x4082), the gym's puzzle state. */
    const val PUZZLE_VAR = "unova/VAR_0x4082"

    /** The row each curtain hangs on, bottom first - the ROM's own objects sit at x=9 on these. */
    val CURTAIN_ROWS = listOf(34, 24, 14)

    /** The rest of the opening; x=9 is the ROM's own object and is left alone. */
    val WALL_COLUMNS = (10..15).toList()

    /** Sprite 185, the one the ROM uses for these - it draws nothing. */
    const val WALL_SPRITE = 185

    /** Well clear of the map's own 0..9. */
    const val FIRST_WALL_ID = 200

    const val ALL_OPEN = 7

    /**
     * VAR 16514 -> the open-curtain bitmask, read straight off the gym's map-enter script:
     * 0 and 1 none, 2 the first, 3 the first two, 4 and 5 (and anything past) all three.
     */
    val MASK_BY_STATE = listOf(0, 0, 1, 3, 7, 7)
  }
}
