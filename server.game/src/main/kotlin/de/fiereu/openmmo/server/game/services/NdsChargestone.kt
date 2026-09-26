package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Chargestone Cave's electric stones, read from the cave's own code overlay (tools/nds/Chargestone5
 * -> nds-chargestone-2.txt: `cstone;region;bank;map;small|big;x;y;level`). The rules are the
 * overlay's (White overlay 51, read 2026-09-25):
 * - every small stone is a field object with code 253 (0x21F3D58), created at its table tile;
 * - a stone touching a charged one is charged; the big stones are charged (0x21F3CB8);
 * - a charged stone never moves; a loose one, pushed, looks up to 32 tiles along the push for a
 *   charged stone with nothing (wall or object) in between and slides until it touches it, and is
 *   charged from then on; with no such stone it holds (0x21F3BE4, range 32 via 0x21F3CA4).
 * That is the owner's "each one can only be pushed in one direction".
 */
@Singleton
class NdsChargestone @Inject constructor() {
  data class Stone(val index: Int, val big: Boolean, val x: Int, val y: Int, val level: Int)

  private val byMap: Map<Triple<Int, Int, Int>, List<Stone>> by lazy { load() }

  fun of(regionId: Int, bankId: Int, mapId: Int): List<Stone> = byMap[Triple(regionId, bankId, mapId)].orEmpty()

  private fun load(): Map<Triple<Int, Int, Int>, List<Stone>> {
    val out = HashMap<Triple<Int, Int, Int>, MutableList<Stone>>()
    val file = listOf(File("nds-chargestone-2.txt"), File("server.game/nds-chargestone-2.txt")).firstOrNull { it.isFile } ?: return out
    file.bufferedReader().useLines { lines ->
      for (line in lines) {
        if (!line.startsWith("cstone;")) continue
        val p = line.split(';')
        if (p.size < 8) continue
        val list = out.getOrPut(Triple(p[1].toInt(), p[2].toInt(), p[3].toInt())) { mutableListOf() }
        list += Stone(list.size, p[4] == "big", p[5].toInt(), p[6].toInt(), p[7].toInt())
      }
    }
    log.info { "Chargestone: ${out.values.sumOf { it.size }} stones on ${out.size} maps from ${file.path}" }
    return out
  }

  companion object {
    /**
     * The graphic the client draws a small stone with: 8202, found by the owner's /devsprite probe
     * in the cave (2026-09-25). The overlay's own object code 253 is a blank carrier this client
     * draws nothing for; the 8192+ band is BW's prop objects (8195/8197 = Strength boulders).
     */
    const val SMALL_STONE_SPRITE = 8202
    /** Npc index band for the stones on our side (above the server's walls, 300+). */
    const val FIRST_ID = 500
    const val MAX_SLIDE = 32
  }
}
