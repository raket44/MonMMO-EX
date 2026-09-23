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
    if (state.regionId != UNOVA) return
    when {
      state.bankId == GYM_BANK && state.mapId == GYM_MAP -> syncStriaton(session, storyFlags)
      state.bankId == NACRENE_BANK && state.mapId == NACRENE_MAP -> syncNacrene(session, storyFlags)
    }
  }

  private fun syncStriaton(session: SessionContext, storyFlags: Map<String, Int>) {
    val open = MASK_BY_STATE.getOrElse(storyFlags[PUZZLE_VAR] ?: 0) { ALL_OPEN }
    CURTAIN_ROWS.forEachIndexed { curtain, y ->
      val closed = (open shr curtain) and 1 == 0
      for ((i, x) in WALL_COLUMNS.withIndex()) {
        wall(session, GYM_BANK, GYM_MAP, FIRST_WALL_ID + curtain * WALL_COLUMNS.size + i, x, y, closed)
      }
    }
  }

  /**
   * Nacrene Gym: the sliding bookshelf at the back, the same shape as a curtain. The ROM's own
   * invisible object (npc 3, sprite 185) holds its left end at (11,10); the rest of the shelf,
   * x 12..14, is an animation on the building model and walkable, so the player walked through
   * it before the last book was read (owner, 2026-09-23). Solid until the quiz var says the
   * shelf slid (7, the value the map-load restore CMD_182 keys on).
   */
  private fun syncNacrene(session: SessionContext, storyFlags: Map<String, Int>) {
    val closed = (storyFlags[NACRENE_VAR] ?: 0) < NACRENE_OPEN
    for ((i, x) in NACRENE_COLUMNS.withIndex()) {
      wall(session, NACRENE_BANK, NACRENE_MAP, FIRST_WALL_ID + i, x, NACRENE_ROW, closed)
    }
  }

  private fun wall(session: SessionContext, bank: Int, map: Int, id: Int, x: Int, y: Int, closed: Boolean) {
    if (closed) {
      if (!ndsNpcs.isMade(UNOVA, bank, map, id)) {
        ndsNpcs.define(
            UNOVA, bank, map,
            NdsNpcs.Npc(
                index = id, id = id, sprite = WALL_SPRITE, movement = 0, flag = 0, script = 0,
                facing = 1, xRange = 0, yRange = 0, x = x, y = y))
      }
      npcService.spawnNpc(session, UNOVA, bank, map, id)
    } else {
      npcService.despawnNpc(session, UNOVA, bank, map, id)
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

    /** Nacrene Gym (header 18): the sliding shelf's row and the tiles the model leaves walkable. */
    const val NACRENE_BANK = 18
    const val NACRENE_MAP = 0
    const val NACRENE_ROW = 10
    val NACRENE_COLUMNS = (12..14).toList()
    /** VAR 16522 (0x408A), the book quiz's progress; 7 = the last book read, the shelf slid. */
    const val NACRENE_VAR = "unova/VAR_0x408A"
    const val NACRENE_OPEN = 7
  }
}
