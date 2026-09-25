package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Objects a White MAP FILE places, not its event file: Chargestone Cave's electric stones live
 * in the map files' last section (tools/nds/MapObjs5), in three kinds - 0x6E the big ones the
 * client draws from its own table, 0x6F the small ones already attached to a big one, 0x70 the
 * loose ones the player pushes until they meet a stone or a wall. `nds-objects-2.txt` rows:
 * mobj;region;bank;map;kind;x;y;height. Found 2026-09-25 after the owner insisted the
 * cartridge carries them ("Bianca's dialog is about pushing those stones").
 */
@Singleton
class NdsMapObjects @Inject constructor() {
  data class Stone(val index: Int, val kind: Int, val x: Int, val y: Int, val height: Int) {
    val loose: Boolean get() = kind == LOOSE
    /** Drawn by the server: the loose and the attached ones. The big ones are the client's. */
    val spawned: Boolean get() = kind == LOOSE || kind == ATTACHED
  }

  private val byMap: Map<Triple<Int, Int, Int>, List<Stone>> by lazy { load() }

  fun of(regionId: Int, bankId: Int, mapId: Int): List<Stone> = byMap[Triple(regionId, bankId, mapId)].orEmpty()

  private fun load(): Map<Triple<Int, Int, Int>, List<Stone>> {
    val out = HashMap<Triple<Int, Int, Int>, MutableList<Stone>>()
    for (region in listOf(2)) {
      val file = listOf(File("nds-objects-$region.txt"), File("server.game/nds-objects-$region.txt")).firstOrNull { it.isFile } ?: continue
      var count = 0
      file.bufferedReader().useLines { lines ->
        for (line in lines) {
          if (!line.startsWith("mobj;")) continue
          val p = line.split(';')
          if (p.size < 8) continue
          val key = Triple(p[1].toInt(), p[2].toInt(), p[3].toInt())
          val list = out.getOrPut(key) { mutableListOf() }
          list += Stone(index = list.size, kind = p[4].toInt(), x = p[5].toInt(), y = p[6].toInt(), height = p[7].toInt())
          count++
        }
      }
      log.info { "NDS map objects: region $region $count objects on ${out.size} maps from ${file.path}" }
    }
    return out
  }

  companion object {
    const val BIG = 0x6E
    const val ATTACHED = 0x6F
    const val LOOSE = 0x70
    /** The floating-stone overworld sprite (the three N raises in the cave use it). */
    const val STONE_SPRITE = 182
    /** Entity index band for map objects: above the server's walls (300+). */
    const val FIRST_ID = 500
    /** A loose stone slides at most this far in one push. */
    const val MAX_SLIDE = 32
  }
}
