package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Unova's Gen 5 camera rails (Castelia's streets, Skyarrow Bridge, the League lobby, Dragonspiral,
 * Village Bridge...), from `nds-rails-2.txt` (tools/nds/Rail5.java, the ROM's /a/0/7/9 read the
 * way the client's f.DG1 / f.AL1 read it).
 *
 * A rail map is a set of LINES, each a straight run from one point to another with a length in
 * cells and a lateral width. Everything on such a map - the player, npcs, warp boxes - is placed
 * in a line's own frame: (line id, x along the line from its from-point, y across it). The retail
 * client reports the player's (x, y) on every step but never the line, and switches lines by
 * itself where two share a point, so the server tracks the line: known from the arrival warp,
 * carried across a line change by [transition], which is deterministic because the client can
 * only step onto a line at the shared point - entering at x 0 if that is the line's from-point,
 * at length-1 if its to-point.
 */
@Singleton
class NdsRails @Inject constructor() {
  /**
   * [mode] is the line's AM1 word: which rail axis each screen direction moves along, read off
   * the client's cell linking (f.k90, the four Lh1 blocks) and verified live - Skyarrow (mode 1)
   * walks UP as x+1 and LEFT as y-1, Castelia's streets (mode 4) walk UP as y+1 and LEFT as x+1.
   */
  data class Line(val id: Int, val from: Int, val to: Int, val length: Int, val width: Int, val mode: Int = 2)

  class Area(val index: Int) {
    val lines = ArrayList<Line>()
    val byPoint = HashMap<Int, MutableList<Line>>()
  }

  private val areas: Map<Int, Area> by lazy { load() }
  private val areaByHeader: Map<Int, Int> by lazy { loadHeaders() }

  /** The rail area of a DS map header, or null for an ordinary tile map. */
  fun areaOf(region: Int, bank: Int, map: Int): Area? {
    if (region != UNOVA) return null
    val idx = areaByHeader[(map shl 8) or bank] ?: return null
    return areas[idx]
  }

  fun isRailMap(region: Int, bank: Int, map: Int): Boolean = areaOf(region, bank, map) != null

  fun line(area: Area, id: Int): Line? = area.lines.getOrNull(id)

  /**
   * The line the client moved onto when its reported coordinates jumped from ([lastX], on
   * [fromLine]) to ([newX], [newY]): the one sharing the point at the end of [fromLine] the
   * player was near, entered at that point. Null when nothing fits, which means the guess would
   * be wrong and the caller should stop trusting the line.
   */
  fun transition(area: Area, fromLine: Int, lastX: Int, newX: Int): Line? {
    val from = line(area, fromLine) ?: return null
    // The end we left by: the client walks off a line only past its first or last cell.
    val nearFrom = lastX <= EDGE
    val nearTo = lastX >= from.length - 1 - EDGE
    val candidates = ArrayList<Line>()
    if (nearFrom) area.byPoint[from.from]?.let(candidates::addAll)
    if (nearTo) area.byPoint[from.to]?.let(candidates::addAll)
    val point = if (nearFrom && !nearTo) from.from else if (nearTo && !nearFrom) from.to else -1
    var best: Line? = null
    for (c in candidates) {
      if (c.id == fromLine) continue
      val entry = entryX(c, if (point >= 0) point else if (c.from == from.from || c.to == from.from) from.from else from.to)
      if (kotlin.math.abs(entry - newX) <= EDGE) {
        if (best != null && best.id != c.id) return null // ambiguous: two lines fit
        best = c
      }
    }
    return best
  }

  /** Where a line is entered at [point]: 0 at its from-point, length-1 at its to-point. */
  fun entryX(line: Line, point: Int): Int = if (line.from == point) 0 else line.length - 1

  /**
   * One step in screen direction [dir] on [line], as a (dx, dy) in the line's frame. The client
   * links each rail cell to its four screen-direction neighbours by the line's mode (f.k90):
   *  mode 1: DOWN x-1, UP x+1, LEFT y-1, RIGHT y+1
   *  mode 2: DOWN y+1, UP y-1, LEFT x-1, RIGHT x+1
   *  mode 3: DOWN x+1, UP x-1, LEFT y+1, RIGHT y-1
   *  mode 4: DOWN y-1, UP y+1, LEFT x+1, RIGHT x-1
   */
  fun delta(line: Line, dir: de.fiereu.openmmo.common.enums.Direction): Pair<Int, Int> {
    val d = de.fiereu.openmmo.common.enums.Direction.entries.indexOf(dir) // DOWN 0, UP 1, LEFT 2, RIGHT 3
    return when (line.mode) {
      1 -> when (d) { 0 -> -1 to 0; 1 -> 1 to 0; 2 -> 0 to -1; else -> 0 to 1 }
      3 -> when (d) { 0 -> 1 to 0; 1 -> -1 to 0; 2 -> 0 to 1; else -> 0 to -1 }
      4 -> when (d) { 0 -> 0 to -1; 1 -> 0 to 1; 2 -> 1 to 0; else -> -1 to 0 }
      else -> when (d) { 0 -> 0 to 1; 1 -> 0 to -1; 2 -> -1 to 0; else -> 1 to 0 }
    }
  }

  private fun load(): Map<Int, Area> {
    val file = dataFile() ?: return emptyMap()
    val out = HashMap<Int, Area>()
    file.bufferedReader().useLines { lines ->
      for (l in lines) {
        if (!l.startsWith("line;")) continue
        val p = l.split(';')
        val area = out.getOrPut(p[1].toInt()) { Area(p[1].toInt()) }
        // line;area;id;fromPoint;toPoint;mode;length;width
        val line = Line(p[2].toInt(), p[3].toInt(), p[4].toInt(), p[6].toInt(), p[7].toInt(), mode = p[5].toInt())
        while (area.lines.size <= line.id) area.lines.add(Line(area.lines.size, -1, -1, 0, 0))
        area.lines[line.id] = line
        area.byPoint.getOrPut(line.from) { mutableListOf() } += line
        area.byPoint.getOrPut(line.to) { mutableListOf() } += line
      }
    }
    log.info { "NDS rails: ${out.size} areas, ${out.values.sumOf { it.lines.size }} lines from ${file.path}" }
    return out
  }

  private fun loadHeaders(): Map<Int, Int> {
    val file = dataFile() ?: return emptyMap()
    val out = HashMap<Int, Int>()
    file.bufferedReader().useLines { lines ->
      for (l in lines) {
        if (!l.startsWith("area;")) continue
        val p = l.split(';')
        out[p[2].toInt()] = p[1].toInt()
      }
    }
    return out
  }

  private fun dataFile(): File? =
      listOf(File("nds-rails-2.txt"), File("server.game/nds-rails-2.txt")).firstOrNull { it.isFile }

  companion object {
    const val UNOVA = 2
    /** How far from a line's end the client may still report before it walks off it. */
    private const val EDGE = 1
  }
}
