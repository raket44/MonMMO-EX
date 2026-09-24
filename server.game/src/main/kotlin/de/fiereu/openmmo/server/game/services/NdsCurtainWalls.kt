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
class NdsCurtainWalls
@Inject
constructor(
    private val ndsNpcs: NdsNpcs,
    private val npcService: NpcService,
    private val characterStore: de.fiereu.openmmo.server.game.storage.CharacterStore,
) {

  fun sync(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    if (state.regionId != UNOVA) return
    when {
      state.bankId == GYM_BANK && state.mapId == GYM_MAP -> syncStriaton(session, state, storyFlags)
      state.bankId == NACRENE_BANK && state.mapId == NACRENE_MAP -> syncNacrene(session, state, storyFlags)
      state.bankId == CASTELIA_STREET_BANK && state.mapId == CASTELIA_STREET_MAP -> syncCasteliaGym(session, state, storyFlags)
      state.bankId == HONEY_BANK && state.mapId == HONEY_MAP -> syncHoneyGates(session, state, storyFlags)
    }
  }

  /**
   * The Castelia Gym's six honey gates (header 29).
   *
   * Every way between two of the gym's hive rooms is ONE tile of honey, walkable in the ROM's
   * land but with land-blocked tiles on both sides, and the ROM stands an object on each (sprite
   * 0, the "no talk" script 2000 - npcs 0..6). On the cartridge the gym's field overlay (ov38
   * 0x021F3AA8) catches the player pushing up or down into one of those seven tiles and slides
   * them through when that honey's gate is down (ov38 0x021F3B5C), or plays a bump when it is up.
   * The entrance honey (32,33) has no gate and is always open (ov37 sets its flag at load). The
   * client has none of that: it only animates the models, and makes every object solid - so the
   * entrance honey, npc 0, walled the player into the entrance hall (owner, 2026-09-24).
   *
   * So those seven ROM objects are never spawned ([replacesRomNpc]), and each gate's honey tile
   * carries a wall object while the gate is up. Which gates are down follows the ROM: switch n
   * (VAR 0x4000 + n = 1) drops the posts [SWITCH_POSTS], and a gate is down once all its posts are
   * ([GATE_POSTS]) - ov37's own tables at 0x021F3860 / 0x021F3854, the same ones kr4 draws by.
   */
  private fun syncHoneyGates(session: SessionContext, state: PlayerState, storyFlags: Map<String, Int>) {
    val charId = state.characterId
    val flags = charId?.let { characterStore.getCharacter(it)?.storyFlags }.orEmpty()
    val switches = HONEY_SWITCH_VARS.map { (storyFlags[it] ?: 0) != 0 }
    var down = gatesDown(postsDown(switches))
    // The owner's rule (2026-09-24): the switches are the cartridge's map-local temps and reset
    // on re-entry, but the gym is solved for good the moment its last gate opens - from then on
    // every gate stays down, here and in the map-load restore (honeyRestoreMasks).
    if (down.all { it } && charId != null && HONEY_SOLVED_FLAG !in flags) characterStore.setStoryFlag(charId, HONEY_SOLVED_FLAG)
    if (HONEY_SOLVED_FLAG in flags || down.all { it }) down = List(down.size) { true }
    HONEY_GATE_TILES.forEachIndexed { gate, (x, y) ->
      wall(session, state, HONEY_BANK, HONEY_MAP, FIRST_WALL_ID + 60 + gate, x, y, !down[gate])
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
   * The Castelia Gym's door (31:0 (25,48) -> 29:0), held shut until the walk-out scene has
   * played: the trigger tile at (24,48) starts it (file 62 entry 3: Cheren, then Burgh, step
   * out of this very door), and a step buffered into the door beside it carried the player
   * into the gym while the scene was starting, so it played to an empty street (owner,
   * 2026-09-23, from the log). The door carries a wall while the street's var is 0; the
   * scene leaves it at 1.
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

    /** Castelia's gym street (header 31) and the gym's door tile (its warp into 29:0). */
    const val CASTELIA_STREET_BANK = 31
    const val CASTELIA_STREET_MAP = 0
    val CASTELIA_GYM_DOOR: List<Pair<Int, Int>> = listOf(25 to 48)
    /** VAR 16559 (0x40AF), the gym street's story var; 1 = the walk-out scene has played. */
    const val CASTELIA_GYM_VAR = "unova/VAR_0x40AF"
    const val CASTELIA_GYM_OPEN = 1

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

    /** Castelia Gym (header 29, client handler f/kr4). */
    const val HONEY_BANK = 29
    const val HONEY_MAP = 0

    /** Switch n's var, VAR 0x4000 + n: its floor panel's coord trigger (file 58 entry 6 + n) sets it to 1. */
    val HONEY_SWITCH_VARS: List<String> = (0..7).map { "unova/VAR_0x" + (0x4000 + it).toString(16).uppercase() }

    /**
     * Switch n -> the gate posts it drops: ov37 0x021F3860 (two signed bytes per switch, -1 = none),
     * and the trigger scripts' own CMD_18D calls agree (entries 9 and 12 drop two posts each).
     */
    val SWITCH_POSTS: List<List<Int>> = listOf(listOf(0), listOf(1), listOf(2), listOf(3, 4), listOf(5), listOf(6), listOf(7, 8), listOf(9))

    /** Gate g -> its posts: ov37 0x021F3854 (a repeated byte = a one-post gate) = kr4.oH0. */
    val GATE_POSTS: List<List<Int>> = listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6), listOf(7), listOf(8, 9))

    /**
     * Gate g's honey tile: ov38's table at 0x021F46B4 (fx32 x, y, z; entry 6 is the entrance's
     * (32,33), which has no gate) = kr4's gate models (Qq0 x 4). Each is the only walkable tile of
     * its passage - the land blocks x-1 and x+1 - so one object closes the whole passage.
     */
    val HONEY_GATE_TILES: List<Pair<Int, Int>> = listOf(32 to 17, 26 to 21, 38 to 21, 20 to 25, 26 to 29, 38 to 29)

    /** The ROM's own honey objects (sprite 0, script 2000): npcs 0..6 on 29:0 - 0 is the entrance's. */
    val HONEY_ROM_NPCS: Set<Int> = (0..6).toSet()

    /** True for a ROM npc this class stands in for, which the map spawn must leave out. */
    fun replacesRomNpc(region: Int, bank: Int, map: Int, index: Int): Boolean =
        region == UNOVA && bank == HONEY_BANK && map == HONEY_MAP && index in HONEY_ROM_NPCS

    /** Which of the ten posts are down, from the eight switches' state. */
    fun postsDown(switches: List<Boolean>): BooleanArray {
      val down = BooleanArray(10)
      switches.forEachIndexed { n, on -> if (on) SWITCH_POSTS.getOrNull(n)?.forEach { down[it] = true } }
      return down
    }

    /** Which of the six gates are down: all of their posts are. */
    fun gatesDown(posts: BooleanArray): List<Boolean> = GATE_POSTS.map { g -> g.all { posts[it] } }

    /**
     * The client's 0x18F restore (kr4.aL): [switch bitmask, post bitmask] - it lights the pressed
     * panels, drops the posts and then the gates whose posts are all down (kr4.FZ).
     */
    /** Our own persistent flag: the Castelia Gym's last gate has opened once, so all stay open. */
    const val HONEY_SOLVED_FLAG = "unova/FLAG_CASTELIA_GYM_SOLVED"

    /** With [solved], every switch lit and every post down, whatever the temps say. */
    fun honeyRestoreMasks(switches: List<Boolean>, solved: Boolean = false): Pair<Int, Int> {
      if (solved) return 0xFF to 0x3FF
      val switchMask = switches.withIndex().filter { it.value }.fold(0) { m, (n, _) -> m or (1 shl n) }
      val posts = postsDown(switches)
      val postMask = posts.withIndex().filter { it.value }.fold(0) { m, (n, _) -> m or (1 shl n) }
      return switchMask to postMask
    }
  }
}
