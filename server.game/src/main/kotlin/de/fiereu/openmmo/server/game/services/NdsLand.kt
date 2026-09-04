package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The DS maps' tile permissions, extracted from the ROMs with `tools/nds/Land4.java` (mirroring
 * the client's own land-data parser) into `nds-land-<region>.txt` rows of
 * `region;bank;map;x;y;type;coll`. Coordinates are matrix-global, the frame the client reports
 * DS movement in. The collision byte's high bit blocks; the type byte is the Gen 4 tile behaviour
 * (pokeheartgold include/constants/metatile_behavior.h): 2/3 tall grass, 8 cave floor, 16/17/19/21
 * water. Unova has no land file yet.
 */
@Singleton
class NdsLand @Inject constructor() {
  class MapTiles(val minX: Int, val minY: Int, val width: Int, val height: Int) {
    val type = ByteArray(width * height)
    val coll = ByteArray(width * height) { BLOCKED.toByte() }
    val present = BooleanArray(width * height)

    fun index(x: Int, y: Int): Int? {
      val lx = x - minX
      val ly = y - minY
      if (lx < 0 || ly < 0 || lx >= width || ly >= height) return null
      val i = ly * width + lx
      return if (present[i]) i else null
    }
  }

  private val maps: Map<Triple<Int, Int, Int>, MapTiles> by lazy { load() }

  fun has(region: Int, bank: Int, map: Int): Boolean = maps.containsKey(Triple(region, bank, map))

  fun typeAt(region: Int, bank: Int, map: Int, x: Int, y: Int): Int? {
    val tiles = maps[Triple(region, bank, map)] ?: return null
    val i = tiles.index(x, y) ?: return null
    return tiles.type[i].toInt() and 0xFF
  }

  fun blocked(region: Int, bank: Int, map: Int, x: Int, y: Int): Boolean? {
    val tiles = maps[Triple(region, bank, map)] ?: return null
    val i = tiles.index(x, y) ?: return true
    return (tiles.coll[i].toInt() and BLOCKED) != 0
  }

  fun isGrass(type: Int): Boolean = type == TALL_GRASS || type == VERY_TALL_GRASS
  fun isCaveFloor(type: Int): Boolean = type == CAVE_FLOOR
  fun isWater(type: Int): Boolean = type == WATER_RIVER || type == WHIRLPOOL || type == WATERFALL || type == WATER_SEA

  private fun load(): Map<Triple<Int, Int, Int>, MapTiles> {
    val out = HashMap<Triple<Int, Int, Int>, MapTiles>()
    for (region in listOf(2, 3, 4)) {
      val file =
          listOf(File("nds-land-$region.txt"), File("server.game/nds-land-$region.txt")).firstOrNull { it.isFile }
              ?: continue
      // Two passes: extents first, then the bytes, so each map is one dense block.
      val extents = HashMap<Triple<Int, Int, Int>, IntArray>()
      file.bufferedReader().useLines { lines ->
        for (line in lines) {
          val p = line.split(';')
          if (p.size < 7) continue
          val key = Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())
          val x = p[3].toInt()
          val y = p[4].toInt()
          val e = extents.getOrPut(key) { intArrayOf(x, y, x, y) }
          if (x < e[0]) e[0] = x
          if (y < e[1]) e[1] = y
          if (x > e[2]) e[2] = x
          if (y > e[3]) e[3] = y
        }
      }
      for ((key, e) in extents) out[key] = MapTiles(e[0], e[1], e[2] - e[0] + 1, e[3] - e[1] + 1)
      file.bufferedReader().useLines { lines ->
        for (line in lines) {
          val p = line.split(';')
          if (p.size < 7) continue
          val tiles = out[Triple(p[0].toInt(), p[1].toInt(), p[2].toInt())] ?: continue
          val i = (p[4].toInt() - tiles.minY) * tiles.width + (p[3].toInt() - tiles.minX)
          tiles.type[i] = p[5].toInt().toByte()
          tiles.coll[i] = p[6].toInt().toByte()
          tiles.present[i] = true
        }
      }
      log.info { "NDS land: region $region ${extents.size} maps from ${file.path}" }
    }
    return out
  }

  companion object {
    const val BLOCKED = 0x80
    const val TALL_GRASS = 2
    const val VERY_TALL_GRASS = 3
    const val CAVE_FLOOR = 8
    const val WATER_RIVER = 16
    const val WHIRLPOOL = 17
    const val WATERFALL = 19
    const val WATER_SEA = 21
  }
}
