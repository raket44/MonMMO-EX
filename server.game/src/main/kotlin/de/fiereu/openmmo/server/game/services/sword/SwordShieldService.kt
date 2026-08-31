package de.fiereu.openmmo.server.game.services.sword

import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Sword/Shield encounter and Dynamax integration.
 *
 * Drop decomp/pokesword/recomp/libsword_recomp.a (from RecompSwordC.zip) and build
 * decomp/pokesword/openmmo/CMakeLists.txt → libsword_bridge.so into the JVM lib path to enable the
 * native bridge. Without it, the stub encounter table below is used instead.
 */
@Singleton
class SwordShieldService @Inject constructor() {

  private var bridgeLoaded = false

  init {
    tryLoadBridge()
  }

  private fun tryLoadBridge() {
    try {
      System.loadLibrary("sword_bridge")
      bridgeInit()
      bridgeLoaded = true
      log.info { "sword_bridge loaded — native Galar encounters active" }
    } catch (_: UnsatisfiedLinkError) {
      log.info { "sword_bridge not found — using stub Galar encounter table" }
    }
  }

  /** Pick a wild Pokémon dex ID for the given Galar area (area IDs match GalarEncounters.h). */
  fun pickEncounter(areaId: Int, level: Int): Int {
    if (bridgeLoaded) return bridgePickEncounter(areaId, level)
    return stubEncounter(areaId)
  }

  // ── Area ID constants (mirror GalarEncounters.h) ─────────────────────────
  object Areas {
    const val ROUTE_1 = 1
    const val ROUTE_2 = 2
    const val ROUTE_3 = 3
    const val MINE_1 = 4
    const val ROUTE_4 = 5
    const val ROUTE_5 = 6
    const val MINE_2 = 7
    const val ROUTE_6 = 8
    const val ROUTE_7 = 9
    const val ROUTE_8 = 10
    const val ROUTE_9 = 11
    const val ROUTE_10 = 12
    const val WILD_SOUTH = 20
    const val WILD_EAST = 21
    const val WILD_NORTH = 22
    const val WILD_LAKE = 23
    const val WILD_GIANT_SEAT = 24
    const val WILD_HAMMERLOCK = 25
    const val WILD_DUSTY_BOWL = 26
    const val WILD_GIANT_CAP = 27
    const val WILD_SNOWFIELDS = 28
    const val WILD_WATCHTOWER = 29
    const val WILD_BRIDGE = 30
  }

  // Probability-weighted pick from a list of (dexId, weight) pairs.
  private fun pick(table: List<Pair<Int, Int>>): Int {
    val total = table.sumOf { it.second }
    var roll = (Math.random() * total).toInt()
    for ((dex, w) in table) {
      roll -= w
      if (roll < 0) return dex
    }
    return table.last().first
  }

  // Stub encounter tables mirroring GalarEncounters.cpp (Gen8 national dex IDs).
  private fun stubEncounter(areaId: Int): Int =
      when (areaId) {
        Areas.ROUTE_1 -> pick(listOf(819 to 30, 831 to 30, 821 to 20, 827 to 20))
        Areas.ROUTE_2 -> pick(listOf(819 to 25, 831 to 25, 821 to 20, 827 to 15, 835 to 15))
        Areas.ROUTE_3 -> pick(listOf(819 to 20, 831 to 20, 833 to 20, 829 to 20, 52 to 20))
        Areas.MINE_1 -> pick(listOf(837 to 40, 527 to 30, 52 to 30))
        Areas.ROUTE_4 -> pick(listOf(829 to 25, 833 to 25, 835 to 25, 840 to 25))
        Areas.ROUTE_5 -> pick(listOf(840 to 25, 843 to 25, 848 to 25, 850 to 25))
        Areas.MINE_2 -> pick(listOf(837 to 35, 852 to 35, 848 to 30))
        Areas.ROUTE_6 -> pick(listOf(843 to 25, 854 to 25, 856 to 25, 859 to 25))
        Areas.ROUTE_7 -> pick(listOf(856 to 25, 859 to 25, 870 to 25, 871 to 25))
        Areas.ROUTE_8 -> pick(listOf(871 to 30, 872 to 30, 875 to 20, 877 to 20))
        Areas.ROUTE_9 -> pick(listOf(875 to 30, 877 to 30, 884 to 20, 885 to 20))
        Areas.ROUTE_10 -> pick(listOf(884 to 30, 885 to 30, 872 to 20, 875 to 20))
        Areas.WILD_SOUTH -> pick(listOf(819 to 20, 831 to 20, 833 to 20, 835 to 20, 829 to 20))
        Areas.WILD_EAST -> pick(listOf(840 to 20, 843 to 20, 845 to 20, 848 to 20, 850 to 20))
        Areas.WILD_NORTH -> pick(listOf(872 to 20, 875 to 20, 877 to 20, 884 to 20, 885 to 20))
        else -> 819 // Skwovet fallback
      }

  // ── JNI entry points (only called when libsword_bridge.so is loaded) ─────
  private external fun bridgeInit()

  private external fun bridgePickEncounter(areaId: Int, level: Int): Int
}
