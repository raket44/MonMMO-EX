package pirated.server.game.script

/**
 * Pokemon White's mart shelves, read from the ROM by tools/nds/Marts5 (overlay 21, the shop app).
 *
 * The clerk's script (CallStd 10110, file 871) passes a shelf id in VAR_0x8000. The shop code
 * (0x21B3900) takes 255 as the common shelf, picked by badge count through the u8 table at
 * 0x21D9484, and any id below 247 as shelf `id + 6` of the pointer table at 0x21DE1C8, with the
 * item count from the u8 table at 0x21D95F6. 247-254 are other shops (0x21B3A84 / 0x21B39E4).
 *
 * Every Pokemon Center has two clerks on the mart counter: the upper one (the ROM's 255) sells the
 * common shelf, the lower one the town's own (Accumula 0, Striaton 1, Nacrene 2, ...). Item numbers
 * are the game's own indexes.
 */
object UnovaMarts {
  const val COMMON = 255

  /** Common shelf index by badge count 0..8 (0x21D9484). */
  private val TIER_BY_BADGES = intArrayOf(0, 1, 1, 2, 2, 3, 3, 4, 5)

  /** The six common shelves (pointer table entries 0-5). */
  private val COMMON_SHELVES =
      listOf(
          listOf(4, 17),
          listOf(4, 3, 17, 26, 18, 22, 21, 19, 20, 78, 79),
          listOf(4, 3, 17, 26, 25, 28, 18, 22, 21, 19, 20, 78, 79, 76),
          listOf(4, 3, 2, 17, 26, 25, 28, 18, 22, 21, 19, 20, 27, 78, 79, 76, 77),
          listOf(4, 3, 2, 17, 26, 25, 24, 28, 18, 22, 21, 19, 20, 27, 78, 79, 76, 77),
          listOf(4, 3, 2, 17, 26, 25, 24, 23, 28, 18, 22, 21, 19, 20, 27, 78, 79, 76, 77),
      )

  private val MAILS = listOf(137, 138, 139, 140, 141, 142, 143)

  /** The town shelves, by the id the clerk's script passes (pointer table entries 6-25). */
  private val SPECIALTY_SHELVES =
      listOf(
          MAILS,
          listOf(14) + MAILS,
          listOf(14, 6) + MAILS,
          listOf(14, 6, 8, 144) + MAILS,
          listOf(348, 354, 400, 401, 343, 347, 360),
          listOf(6, 8, 13, 145) + MAILS,
          listOf(334, 338, 345, 364),
          listOf(341, 352, 365),
          listOf(13, 10, 15, 146) + MAILS,
          listOf(14, 6, 8, 15, 13, 10, 9, 11),
          listOf(147) + MAILS,
          listOf(11, 7) + MAILS,
          listOf(148) + MAILS,
          listOf(59, 57, 58, 55, 56, 60, 61, 62),
          listOf(36, 34, 35, 37),
          listOf(314, 254, 318, 317, 315, 320, 319, 255, 316),
          listOf(17, 26, 25, 24, 28, 18, 22, 19, 20, 21, 27, 78, 577, 79, 76, 77),
          listOf(342, 395),
          listOf(4, 3, 2, 14, 6, 8, 13, 15, 10) + MAILS,
          listOf(46, 47, 49, 52, 48, 45),
      )

  /** The item indexes shelf [shelfId] sells with [badges] badges, or null for a shop that is not a mart shelf. */
  fun shelf(shelfId: Int, badges: Int): List<Int>? =
      when {
        shelfId == COMMON -> COMMON_SHELVES[TIER_BY_BADGES[badges.coerceIn(0, 8)]]
        else -> SPECIALTY_SHELVES.getOrNull(shelfId)
      }
}
