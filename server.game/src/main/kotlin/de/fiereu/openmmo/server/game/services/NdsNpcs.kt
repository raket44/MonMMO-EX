package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The DS maps' npc placements, extracted from the ROMs with `tools/nds/Npcs4.java` (the same
 * zone-event layout the client's own reader walks) into `nds-npcs-<region>.txt` rows of
 * `obj;region;bank;map;idx;id;sprite;movement;type;flag;script;facing;xRange;yRange;x;y;height`.
 * Coordinates are matrix-global. The sprite id is the ROM's own overlay id, which the client
 * resolves through the region's sprite table when the spawn names that region.
 */
@Singleton
class NdsNpcs @Inject constructor() {
  data class Npc(
      val index: Int,
      val id: Int,
      val sprite: Int,
      val movement: Int,
      val flag: Int,
      val script: Int,
      val facing: Int,
      val xRange: Int,
      val yRange: Int,
      val x: Int,
      val y: Int,
      /** Gen 4 trainer type: 0 none, 1 normal (faces one way), 2 sees all directions. */
      val type: Int = 0,
      /** param0: a trainer's sight range in tiles. */
      val sight: Int = 0,
  )

  private val byMap: Map<Triple<Int, Int, Int>, List<Npc>> by lazy { load() }

  fun of(region: Int, bank: Int, map: Int): List<Npc> = byMap[Triple(region, bank, map)].orEmpty()

  private fun load(): Map<Triple<Int, Int, Int>, List<Npc>> {
    val out = HashMap<Triple<Int, Int, Int>, MutableList<Npc>>()
    for (region in listOf(2, 3, 4)) {
      val file =
          listOf(File("nds-npcs-$region.txt"), File("server.game/nds-npcs-$region.txt")).firstOrNull { it.isFile }
              ?: continue
      var count = 0
      file.bufferedReader().useLines { lines ->
        for (line in lines) {
          if (!line.startsWith("obj;")) continue
          val p = line.split(';')
          if (p.size < 17) continue
          val key = Triple(p[1].toInt(), p[2].toInt(), p[3].toInt())
          out.getOrPut(key) { mutableListOf() } +=
              Npc(
                  index = p[4].toInt(),
                  id = p[5].toInt(),
                  sprite = p[6].toInt(),
                  movement = p[7].toInt(),
                  flag = p[9].toInt(),
                  script = p[10].toInt(),
                  facing = p[11].toInt(),
                  xRange = p[12].toInt(),
                  yRange = p[13].toInt(),
                  x = p[14].toInt(),
                  y = p[15].toInt(),
                  type = p[8].toInt(),
                  sight = p.getOrNull(17)?.toIntOrNull() ?: 0,
              )
          count++
        }
      }
      log.info { "NDS npcs: region $region $count npcs on ${out.size} maps from ${file.path}" }
    }
    return out
  }
}
