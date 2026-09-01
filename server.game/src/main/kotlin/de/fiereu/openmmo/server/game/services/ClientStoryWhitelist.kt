package de.fiereu.openmmo.server.game.services

/**
 * Mirror of the client's story flag/var whitelist - `f/LG0.Yw1(region, id)` in build 31914,
 * verified against the disassembled bytecode (2026-08-31).
 *
 * The client keeps only a curated subset of each region's story state: badge flags, fly-point /
 * visited-city flags, a few HM and story gates, dynamic-NPC-sprite vars, and UI refresh triggers.
 * Its 0x2A handler (`f/eO0.X91`) THROWS `"Attempt to set non-client aware flag."` for any id
 * outside this table - the packet loop logs a WARN and drops the update, and the handler's
 * UI-refresh pass never runs. The server must therefore never send a non-whitelisted id: every
 * other flag and var is server-side save state only, which is the whole permission stance - the
 * server is the save file, the client mirrors only what its UI reads.
 *
 * Region indices are the wire region ids (0 Kanto, 1 Hoenn, 2 Unova, 3 Sinnoh, 4 Johto). Id 0 is
 * accepted for every region. Ids >= 0x4000 are vars in the same table (the client stores flags and
 * vars in one short->short map per region).
 */
internal object ClientStoryWhitelist {

  private val kanto: Set<Int> = buildSet {
    add(675)
    addAll(2080..2087) // badges
    add(2092)
    add(2095)
    add(2116)
    add(2121)
    addAll(2192..2210) // world-map fly spots
    add(2228)
  }

  private val hoenn: Set<Int> = buildSet {
    addAll(214..216)
    add(253)
    add(281)
    add(303)
    add(305)
    add(306)
    addAll(421..425)
    addAll(467..473)
    add(2146)
    addAll(2151..2175) // badges + visited cities (fly unlocks)
    add(2216)
    add(2228)
    add(2240)
  }

  private val unova: Set<Int> = buildSet {
    addAll(1521..1528) // badges
    add(1568)
    add(1570)
    add(2400)
    add(2403)
    addAll(2480..2495)
  }

  private val sinnoh: Set<Int> = buildSet {
    addAll(1360..1368) // badges
    add(2404)
    addAll(2480..2497)
    add(2546)
    add(2548)
    add(16469) // 0x4055 - a story VAR; the table covers vars too
  }

  private val johto: Set<Int> = buildSet {
    add(299)
    add(607)
    addAll(1360..1368) // badges (shared numbering with Sinnoh)
    add(1490) // client special-cases this id as always-true in its getter
    add(1495)
    add(1496)
    add(2404)
    add(2409)
    add(2451)
    add(2459)
    addAll(2480..2502)
    add(2507)
    add(2510)
    add(2511)
    add(2513)
    addAll(2515..2517)
  }

  private val byRegion: Map<Int, Set<Int>> =
      mapOf(0 to kanto, 1 to hoenn, 2 to unova, 3 to sinnoh, 4 to johto)

  /** True when the client's `LG0.Yw1` accepts this (region, id) - i.e. a 0x2A send is safe. */
  fun accepts(regionId: Int, id: Int): Boolean = id == 0 || byRegion[regionId]?.contains(id) == true

  /** Every id the client mirrors for a region - the set a full re-sync must account for. */
  fun ids(regionId: Int): Set<Int> = byRegion[regionId] ?: emptySet()

  /**
   * The client's own badge tables (`f/qK.O9` static initializer, bytecode-verified): per region,
   * the client ids of badges 1..8 plus a ninth champion/game-clear slot. Its badge HUD counts set
   * flags among exactly these ids, so a region's story engine syncs "badge N earned" by sending
   * 0x2A for badgeIds[region][N] - no probing needed, the semantics are the client's own code. GBA
   * ids are the ROM's real flag ids; NDS ids are the client's numbering (Sinnoh's order is
   * scrambled on purpose - it mirrors the client's badge-index remap in LG0.tA1).
   */
  val badgeIds: Map<Int, List<Int>> =
      mapOf(
          0 to listOf(2080, 2081, 2082, 2083, 2084, 2085, 2086, 2087, 2092),
          1 to listOf(2151, 2152, 2153, 2154, 2155, 2156, 2157, 2158, 2175),
          2 to listOf(1521, 1522, 1523, 1524, 1525, 1526, 1527, 1528, 2400),
          3 to listOf(1361, 1362, 1365, 1363, 1364, 1366, 1367, 1368, 2404),
          4 to listOf(1361, 1362, 1363, 1364, 1365, 1366, 1367, 1368, 2404),
      )
}
