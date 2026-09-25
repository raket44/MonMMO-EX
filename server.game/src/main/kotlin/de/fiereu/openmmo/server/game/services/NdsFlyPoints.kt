package de.fiereu.openmmo.server.game.services

/**
 * Unova's fly spots, as the client r32645 reads them off White's town-map table (/a/0/8/6, one
 * 54-byte record per place; f/kw builds an f/ls3 per record, f/z93.run sends c2s 0x10 with the
 * record INDEX as the destination, and a spot is clickable when its record's flag - short 15,
 * the ROM's own 2480-2495 - is set on the client). The cartridge sets that flag on arriving in
 * the town; four of them are also set by story scripts (2483, 2485, 2486, 2489). Dumped
 * 2026-09-25 (scratch Fly5): the sixteen records with a flag, in record order.
 */
object NdsFlyPoints {
  data class Spot(val id: Int, val header: Int, val flag: Int, val name: String) {
    val bank: Int get() = header and 0xFF
    val map: Int get() = header shr 8
  }

  val UNOVA: List<Spot> =
      listOf(
          Spot(0, 389, 2480, "NUVEMA_TOWN"),
          Spot(1, 397, 2481, "ACCUMULA_TOWN"),
          Spot(2, 6, 2482, "STRIATON_CITY"),
          Spot(3, 16, 2483, "NACRENE_CITY"),
          Spot(4, 28, 2484, "CASTELIA_CITY"),
          Spot(5, 62, 2485, "NIMBASA_CITY"),
          Spot(6, 96, 2486, "DRIFTVEIL_CITY"),
          Spot(7, 107, 2487, "MISTRALTON_CITY"),
          Spot(8, 113, 2488, "ICIRRUS_CITY"),
          Spot(9, 120, 2489, "OPELUCID_CITY"),
          Spot(10, 136, 2491, "POKEMON_LEAGUE"),
          Spot(11, 406, 2490, "LACUNOSA_TOWN"),
          Spot(12, 412, 2493, "UNDELLA_TOWN"),
          Spot(14, 424, 2492, "BLACK_CITY_WHITE_FOREST"),
          Spot(15, 147, 2494, "ANVILLE_TOWN"),
          Spot(26, 214, 2495, "HEADER_214"),
      )

  private val unovaByHeader: Map<Int, Spot> = UNOVA.associateBy { it.header }

  /** The story flag key a player earns by standing on this DS map, or null when it is no fly town. */
  fun flagKey(regionId: Int, bankId: Int, mapId: Int): String? {
    if (regionId != UNOVA_REGION) return null
    val spot = unovaByHeader[(mapId shl 8) or bankId] ?: return null
    return "unova/FLAG_${spot.flag}"
  }

  /** The client-side flag ids of Unova's fly spots (plus the two the client hard-wires, 1568 and 1570). */
  val UNOVA_CLIENT_FLAG_IDS: Set<Int> = (UNOVA.map { it.flag } + listOf(1568, 1570)).toSet()

  const val UNOVA_REGION = 2
}
