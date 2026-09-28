package de.fiereu.openmmo.server.game.services

/**
 * Mistralton Gym's cannons (Unova header 108 = bank 108 map 0). The r32645 client flies the player
 * by itself: a handler (f/vo1) on the tile beside each cannon starts the flight (f/gj5, f/xv) and
 * sets the client's input lock (eb5.cP). When the flight ends the client puts its player on the
 * landing tile and reports one flag-less move from the launch tile (f/bd, state 0x0N) - and nothing
 * on the client lifts the lock again but a server position set, s2c 0x11 (f/f53 -> mx3.Lu). Without
 * it the player froze on landing and the server kept him on the launch tile (owner, 2026-09-28).
 *
 * All three tables are the client's own (f/pr1 clinit and f/wk7's header-108 case): the cannon
 * bases (pr1.ww0), which directions each fires and where it lands (pr1.LN0/wb1). Directions are the
 * client's bytes: 0 down, 1 up, 2 left, 3 right. The handler tile is the one stepped onto toward the
 * cannon: base y-1 for down, y+1 for up, x+1 for left, x-1 for right. Once the Jet Badge is won the
 * entrance cannon (0) fired up lands straight at Skyla, (15, 7): the client checks its store-2 flag
 * 1526 (f/xv, f/gj5), which is the badge itself (ClientStoryWhitelist.badgeIds[2][5]).
 */
object NdsCannons {
  private const val REGION = 2
  private const val BANK = 108
  private const val MAP = 0

  private val BASES =
      listOf(15 to 28, 10 to 19, 20 to 29, 10 to 25, 15 to 13, 5 to 8, 7 to 36, 26 to 34, 24 to 6, 15 to 3)

  private val SKYLA_SHORTCUT = 15 to 7

  /** (cannon, direction) -> landing tile. */
  private val LANDINGS =
      mapOf(
          (0 to 0) to (15 to 35),
          (0 to 1) to (15 to 19),
          (1 to 2) to (3 to 19),
          (1 to 3) to (21 to 19),
          (2 to 2) to (10 to 29),
          (3 to 1) to (10 to 13),
          (3 to 3) to (20 to 25),
          (4 to 2) to (5 to 13),
          (4 to 3) to (25 to 13),
          (5 to 0) to (5 to 34),
          (6 to 3) to (26 to 36),
          (7 to 1) to (26 to 4),
          (8 to 2) to (15 to 6),
          (9 to 0) to (15 to 32),
      )

  /**
   * The landing tile when stepping onto ([toX], [toY]) with the client direction byte [dir] fires a
   * cannon, else null.
   */
  fun landing(regionId: Int, bankId: Int, mapId: Int, toX: Int, toY: Int, dir: Int, jetBadge: Boolean): Pair<Int, Int>? {
    if (regionId != REGION || bankId != BANK || mapId != MAP) return null
    for ((cannon, base) in BASES.withIndex()) {
      val landing = LANDINGS[cannon to dir] ?: continue
      val (x, y) = base
      val handler =
          when (dir) {
            0 -> x to y - 1
            1 -> x to y + 1
            2 -> x + 1 to y
            else -> x - 1 to y
          }
      if (handler == (toX to toY)) return if (cannon == 0 && dir == 1 && jetBadge) SKYLA_SHORTCUT else landing
    }
    return null
  }
}
