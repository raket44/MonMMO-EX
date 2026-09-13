package de.fiereu.openmmo.items

/**
 * The client's own tools (TMs and HMs): item id to the move it teaches, from its tools table
 * (monmmo/tool-moves.csv - every one of r32645's data.pak tool records included).
 *
 * One rule for the client staging and the server (project owner, 2026-09-13: never duplicate a retail
 * item): a move one of these teaches gets no TM of ours, and anything that hands out "the TM for a move"
 * hands out this one.
 */
object ClientTools {
  /** Ids from here up are the block MonMMO-EX creates items in, not the client's own. */
  const val FIRST_CREATED_ITEM_ID = 20000

  /** The client lists every 5000-band tool again at +1000 and its 1600 tools at 7600. */
  private val COPY_BANDS = 6000..7999

  val itemToMove: Map<Int, Int> by lazy {
    ClientTools::class.java.getResourceAsStream("/monmmo/tool-moves.csv")?.bufferedReader()?.useLines { lines ->
      lines
          .filter { it.isNotBlank() && !it.startsWith("#") }
          .mapNotNull { line ->
            val parts = line.split(',')
            val item = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            val move = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            item to move
          }
          // The dump was taken with the desktop patch loaded, so it lists the TMs we created
          // (20000+) too. Only the client's own tools count, or every move looks taught already.
          .filter { (item, _) -> item < FIRST_CREATED_ITEM_ID }
          .toMap()
    } ?: emptyMap()
  }

  val taughtMoves: Set<Int> by lazy { itemToMove.values.toSet() }

  /** The client's own tool for [moveId]: the lowest id outside the copy bands, or null. */
  fun toolFor(moveId: Int): Int? {
    val ids = itemToMove.filterValues { it == moveId }.keys
    return ids.filter { it !in COPY_BANDS }.minOrNull() ?: ids.minOrNull()
  }
}
