package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.ndsWallKey
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
      state.bankId == GYM_BANK && state.mapId == GYM_MAP -> syncStriaton(session, state, storyFlags)
      state.bankId == NACRENE_BANK && state.mapId == NACRENE_MAP -> syncNacrene(session, state, storyFlags)
      state.bankId == CASTELIA_STREET_BANK && state.mapId == CASTELIA_STREET_MAP -> syncCasteliaGym(session, state, storyFlags)
    }
  }

  private fun syncStriaton(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    val open = MASK_BY_STATE.getOrElse(storyFlags[PUZZLE_VAR] ?: 0) { ALL_OPEN }
    CURTAIN_ROWS.forEachIndexed { curtain, y ->
      val closed = (open shr curtain) and 1 == 0
      for ((i, x) in WALL_COLUMNS.withIndex()) {
        wall(session, state, GYM_BANK, GYM_MAP, FIRST_WALL_ID + curtain * WALL_COLUMNS.size + i, x, y, closed)
      }
    }
  }

  /**
   * The Castelia Gym's door, held shut until Burgh's scene on the gym street has played. The
   * ROM blocks the door with the three Plasma grunts and the scene's trigger tiles, but the
   * trigger box has a gap on the west side: a player walking down past the grunts reached the
   * door and warped into the gym before Burgh and Cheren had even walked out (owner,
   * 2026-09-23). The door's three tiles carry a wall while the street's var is below 3, the
   * value the grunt scene (file 62 entry 7) leaves it at.
   */
  private fun syncCasteliaGym(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    val closed = (storyFlags[CASTELIA_GYM_VAR] ?: 0) < CASTELIA_GYM_OPEN
    CASTELIA_GYM_DOOR.forEachIndexed { i, (x, y) ->
      wall(session, state, CASTELIA_STREET_BANK, CASTELIA_STREET_MAP, FIRST_WALL_ID + 40 + i, x, y, closed)
    }
  }

  /**
   * Nacrene Gym: the sliding bookshelf at the back, the same shape as a curtain. The ROM's own
   * invisible object (npc 3, sprite 185) holds the shelf's left end at (11,10) and, when the last
   * book is read, walks three tiles east with it (file 36 entry 30: movement 23 x3 on npc 3, the
   * building animation CMD_185 beside it), so the shelf's body ends at x 14..17 and the gap opens
   * at x 11..13. The shelf is two rows tall (y 9 and 10) and, but for that one object, an
   * animation on the building model that the client leaves walkable - the player walked through
   * it before the last book (owner, 2026-09-23). The rest of its footprint is made solid here,
   * closed or slid, keyed on the quiz var (7 = slid, what the map-load restore CMD_182 keys on).
   */
  private fun syncNacrene(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    val slid = (storyFlags[NACRENE_VAR] ?: 0) >= NACRENE_OPEN
    NACRENE_CLOSED.forEachIndexed { i, (x, y) -> wall(session, state, NACRENE_BANK, NACRENE_MAP, FIRST_WALL_ID + i, x, y, !slid) }
    NACRENE_SLID.forEachIndexed { i, (x, y) -> wall(session, state, NACRENE_BANK, NACRENE_MAP, FIRST_WALL_ID + 20 + i, x, y, slid) }
  }

  private fun wall(session: SessionContext, state: PlayerState, bank: Int, map: Int, id: Int, x: Int, y: Int, closed: Boolean) {
    val key = ndsWallKey(bank, map, x, y)
    if (closed) {
      if (!ndsNpcs.isMade(UNOVA, bank, map, id)) {
        ndsNpcs.define(
            UNOVA, bank, map,
            NdsNpcs.Npc(
                index = id, id = id, sprite = WALL_SPRITE, movement = 0, flag = 0, script = 0,
                facing = 1, xRange = 0, yRange = 0, x = x, y = y))
      }
      state.ndsWalls += key
      npcService.spawnNpc(session, UNOVA, bank, map, id)
    } else {
      state.ndsWalls -= key
      npcService.despawnNpc(session, UNOVA, bank, map, id)
    }
  }

  companion object {
    const val UNOVA = 2
    const val GYM_BANK = 7
    const val GYM_MAP = 0

    /** Castelia's gym street (header 31) and its gym door's three warp tiles, x 6, y 32..34. */
    const val CASTELIA_STREET_BANK = 31
    const val CASTELIA_STREET_MAP = 0
    val CASTELIA_GYM_DOOR: List<Pair<Int, Int>> = listOf(6 to 32, 6 to 33, 6 to 34)
    /** VAR 16559 (0x40AF), the gym street's story var; 3 = the Plasma grunt scene has played. */
    const val CASTELIA_GYM_VAR = "unova/VAR_0x40AF"
    const val CASTELIA_GYM_OPEN = 3

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

    /** Nacrene Gym (header 18): the sliding shelf's footprint, the ROM's own object's tile left out. */
    const val NACRENE_BANK = 18
    const val NACRENE_MAP = 0
    /**
     * Shelf at x 11..14, rows 7..10 - four rows tall, like its neighbour at x 4..7 whose footprint
     * the ROM land marks blocked on y 7..10 (the record sits on the bottom row). The ROM object
     * holds (11,10).
     */
    val NACRENE_CLOSED: List<Pair<Int, Int>> = (7..10).flatMap { y -> (11..14).map { it to y } } - (11 to 10)
    /** Slid three tiles east: x 14..17, the ROM object now on (14,10). */
    val NACRENE_SLID: List<Pair<Int, Int>> = (7..10).flatMap { y -> (14..17).map { it to y } } - (14 to 10)
    /** VAR 16522 (0x408A), the book quiz's progress; 7 = the last book read, the shelf slid. */
    const val NACRENE_VAR = "unova/VAR_0x408A"
    const val NACRENE_OPEN = 7
  }
}
