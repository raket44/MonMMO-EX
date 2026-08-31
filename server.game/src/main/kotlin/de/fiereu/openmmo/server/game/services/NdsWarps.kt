package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The warp links and entry tiles of the NDS regions, extracted from the ROMs by
 * tools/nds/Warps.java.
 *
 * `nds-warps.txt` is
 * `region;bank;map;x;y;dir;destBank;destMap;destX;destY;srcLine;destLine;srcMatrix`
 * - one line per door TILE (wide doors and Gen 5 rail boxes are one row per tile). Warps are
 *   PAIRED: the destination tile is the partner warp in the destination map, which is what makes a
 *   building exit come out of its own door rather than the town's first one. The line columns are
 *   the Gen 5 rail line ids (-1 for ordinary tile warps). srcMatrix is the ROM matrix (map header
 *   bytes 4-5) the source map sits on - maps stitched on the same matrix share one coordinate
 *   frame.
 *
 * `nds-map-spawns.txt` is `region;bank;map;x;y;matrix`, the fallback tile a player should land on
 * when they arrive somewhere plus that map's matrix id.
 *
 * Both files reload when they change on disk, so regenerating after a ROM swap needs no restart.
 */
@Singleton
class NdsWarps @Inject constructor() {

  private val warpFile = File("nds-warps.txt")
  private val spawnFile = File("nds-map-spawns.txt")

  @Volatile private var warpStamp = 0L
  @Volatile private var spawnStamp = 0L
  @Volatile private var warps: Map<Long, List<Destination>> = emptyMap()
  @Volatile private var matrixWarps: Map<Long, List<Destination>> = emptyMap()
  @Volatile private var spawns: Map<Long, Pair<Int, Int>> = emptyMap()
  @Volatile private var mapMatrix: Map<Long, Int> = emptyMap()

  data class Destination(
      val bank: Int,
      val map: Int,
      val direction: Int,
      val x: Int,
      val y: Int,
      /** Rail line the source warp sits on; -1 for an ordinary tile warp. */
      val srcLine: Int = -1,
      /** Rail line of the arrival tile; -1 when the destination is an ordinary tile. */
      val destLine: Int = -1,
  )

  /**
   * The door on this tile, or null when the player is just walking. [railLine] is the line the
   * client reports riding (movement state bits 2-5; 0 = unknown/not on a rail): a tile shared by
   * several rail lines picks the matching line's warp, an ordinary tile warp matches regardless.
   */
  fun warpAt(region: Int, bank: Int, map: Int, x: Int, y: Int, railLine: Int = 0): Destination? {
    refresh()
    return pick(warps[tileKey(region, bank, map, x, y)], railLine)
  }

  private fun pick(rows: List<Destination>?, railLine: Int): Destination? {
    if (rows.isNullOrEmpty()) return null
    return rows.firstOrNull { it.srcLine >= 0 && it.srcLine == railLine }
        ?: rows.firstOrNull { it.srcLine < 0 }
        ?: if (railLine == 0) rows.first() else null
  }

  /**
   * The door on this tile of the given map's MATRIX, regardless of which map it belongs to. The
   * client crosses seams between maps stitched on one ROM matrix silently, so the tracked bank/map
   * goes stale - but seams never leave a matrix, so the tracked map's matrix plus the reported
   * coordinates still identify the tile. Tiles that are ambiguous within their matrix (several
   * single-cell interiors share a matrix at the same local coordinates) are left out of this index
   * on purpose; they are always reachable through the exact [warpAt] lookup instead.
   */
  fun warpAtMatrix(
      region: Int,
      bank: Int,
      map: Int,
      x: Int,
      y: Int,
      railLine: Int = 0,
  ): Destination? {
    refresh()
    val matrix = mapMatrix[mapKey(region, bank, map)] ?: return null
    if (matrix != 0) return null
    return pick(matrixWarps[matrixKey(region, matrix, x, y)], railLine)
  }

  /** Every warp row on this tile, regardless of rail line - empty when the tile is warp-free. */
  fun rowsAt(region: Int, bank: Int, map: Int, x: Int, y: Int): List<Destination> {
    refresh()
    return warps[tileKey(region, bank, map, x, y)] ?: emptyList()
  }

  // Gen 4 movement-permission TYPE byte under each warp tile (tools/nds/Perm4.java, from the
  // ROMs' land data - `region;bank;map;x;y;type;collision`). This is where real Gen 4 keeps the
  // semantics its warp records lack: 0x69+blocked = a building door (entered pressing INTO it),
  // 0x62/0x63 = east/west wall stairs (entered walking that way), 0x65/0x5E/0x5F = interior
  // mats and stairs (arrivals REST on them, vanilla-style).
  private val behaviorFile = File("nds-warp-behaviors.txt")
  @Volatile private var behaviorStamp = 0L
  @Volatile private var behaviors: Map<Long, Int> = emptyMap()
  @Volatile private var fixtureMasks: Map<Long, Int> = emptyMap()

  /** The Gen 4 permission TYPE byte on this warp tile, or -1 when unknown (e.g. all of Gen 5). */
  fun behaviorAt(region: Int, bank: Int, map: Int, x: Int, y: Int): Int {
    refreshBehaviors()
    return behaviors[tileKey(region, bank, map, x, y)] ?: -1
  }

  /**
   * Which sides of this warp tile hold its FIXTURE (blocked or stair-typed neighbors; bit 1=up
   * 2=down 4=left 8=right), or 0 when unknown. The complement is where a walk-off can go - the
   * data-driven arrival direction when the tile's own type carries none (escalators).
   */
  fun fixtureMaskAt(region: Int, bank: Int, map: Int, x: Int, y: Int): Int {
    refreshBehaviors()
    return fixtureMasks[tileKey(region, bank, map, x, y)] ?: 0
  }

  private fun refreshBehaviors() {
    val now = if (behaviorFile.isFile) behaviorFile.lastModified() else 0L
    if (now == behaviorStamp) return
    behaviorStamp = now
    val rows = readNumbers(behaviorFile, 7)
    behaviors = rows.associate { tileKey(it[0], it[1], it[2], it[3], it[4]) to it[5] }
    fixtureMasks =
        rows
            .filter { it.size >= 8 }
            .associate { tileKey(it[0], it[1], it[2], it[3], it[4]) to it[7] }
    log.info { "NdsWarps: ${behaviors.size} warp-tile behaviors" }
  }

  // Approach tiles: which tile players stand on when a warp pairing fires - ROM geometry
  // learned by observation, so it is GLOBAL (shared by all players) and PERSISTED. This is the
  // only correct emergence target on rail maps, whose arc-space coordinates make facing dx/dy
  // guesses invalid; a session-scoped copy died on every restart and rail steps silently failed.
  private val approachFile = File("nds-door-approach.txt")
  private val approaches = java.util.concurrent.ConcurrentHashMap<Long, Long>()
  @Volatile private var approachesLoaded = false

  fun recordApproach(key: Long, packedTile: Long) {
    loadApproaches()
    if (approaches.put(key, packedTile) != packedTile) {
      runCatching {
        approachFile.writeText(approaches.entries.joinToString("\n") { "${it.key};${it.value}" })
      }
    }
  }

  fun approachFor(key: Long): Long? {
    loadApproaches()
    return approaches[key]
  }

  private fun loadApproaches() {
    if (approachesLoaded) return
    approachesLoaded = true
    if (!approachFile.isFile) return
    approachFile.readLines().forEach { line ->
      val parts = line.split(';')
      val k = parts.getOrNull(0)?.trim()?.toLongOrNull()
      val v = parts.getOrNull(1)?.trim()?.toLongOrNull()
      if (k != null && v != null) approaches[k] = v
    }
    log.info { "NdsWarps: ${approaches.size} recorded door approaches" }
  }

  /**
   * The tile in front of this map's Pokecenter door plus the rail line it sits on (-1 = none), or
   * null when the map has no Pokecenter. Every Pokecenter interior sits on ROM matrix 13, so any
   * warp row from this map into a matrix-13 map is a Pokecenter entrance box tile - the ideal
   * landing spot for teleports ("all teleports should lead in front of a Pokecenter").
   */
  fun pokecenterEntry(region: Int, bank: Int, map: Int): Triple<Int, Int, Int>? {
    refresh()
    var best: Triple<Int, Int, Int>? = null
    for ((key, rows) in warps) {
      if ((key shr 56).toInt() != region) continue
      if (((key shr 48) and 0xFF).toInt() != bank) continue
      if (((key shr 40) and 0xFF).toInt() != map) continue
      val row = rows.firstOrNull { mapMatrix[mapKey(region, it.bank, it.map)] == 13 } ?: continue
      val x = ((key shr 20) and 0xFFFFF).toInt()
      val y = (key and 0xFFFFF).toInt()
      val candidate = Triple(x, y, row.srcLine)
      // Prefer a direction-gated tile (it fires only when pressing into the door - safe to
      // stand on); keep the first otherwise.
      if (best == null || row.direction >= 0) best = candidate
      if (row.direction >= 0) return best
    }
    return best
  }

  /**
   * The rail line of a rail warp row on EXACTLY this tile, or -1. Any proximity-based guess is
   * wrong somewhere: attaching a plain-grid landing beside a rail box (Skyarrow's gate at (15,0), 2
   * tiles from a bridge-mouth box) froze the player at an off-rail position. Vanilla behavior for
   * everything except a landing directly on a box tile.
   */
  fun nearestRailLine(region: Int, bank: Int, map: Int, x: Int, y: Int): Int {
    refresh()
    return warps[tileKey(region, bank, map, x, y)]?.firstOrNull { it.srcLine >= 0 }?.srcLine ?: -1
  }

  /** Where a player arriving on this map should stand. */
  fun spawnOf(region: Int, bank: Int, map: Int): Pair<Int, Int>? {
    refresh()
    return spawns[mapKey(region, bank, map)]
  }

  /** The ROM matrix this map is stitched on, or null when unknown. */
  fun matrixOf(region: Int, bank: Int, map: Int): Int? {
    refresh()
    return mapMatrix[mapKey(region, bank, map)]
  }

  fun size(): Int {
    refresh()
    return warps.size
  }

  private fun refresh() {
    val warpNow = if (warpFile.isFile) warpFile.lastModified() else 0L
    if (warpNow != warpStamp) {
      warpStamp = warpNow
      val rows = readNumbers(warpFile, 10)
      fun dest(it: List<Int>) =
          Destination(
              it[6],
              it[7],
              it[5],
              it[8],
              it[9],
              srcLine = it.getOrElse(10) { -1 },
              destLine = it.getOrElse(11) { -1 },
          )
      // A tile can carry several rows - one per rail line for Castelia's overlapping street
      // mouths - and pick() chooses by the line the client reports. Row order within a tile
      // stays generator order, matching the client's own first-match lookup for ties.
      warps =
          rows
              .groupBy { tileKey(it[0], it[1], it[2], it[3], it[4]) }
              .mapValues { (_, r) -> r.map(::dest) }
      // Only WORLD-PLANE rows (matrix 0): silent seams exist solely there. Off-plane maps are
      // always entered by warp, so their tracking stays exact - and shared off-plane matrices
      // hold story-twin duplicates (drawbridge 253/301) whose rows would hijack lookups.
      val byMatrix = HashMap<Long, MutableList<Destination>>()
      for (row in rows) {
        val matrix = row.getOrNull(12) ?: continue
        if (matrix != 0) continue
        byMatrix
            .getOrPut(matrixKey(row[0], matrix, row[3], row[4])) { mutableListOf() }
            .add(dest(row))
      }
      matrixWarps = byMatrix
      log.info { "NdsWarps: ${rows.size} warp rows on ${warps.size} tiles" }
    }
    val spawnNow = if (spawnFile.isFile) spawnFile.lastModified() else 0L
    if (spawnNow != spawnStamp) {
      spawnStamp = spawnNow
      val rows = readNumbers(spawnFile, 5)
      spawns = rows.associate { mapKey(it[0], it[1], it[2]) to (it[3] to it[4]) }
      mapMatrix = rows.filter { it.size >= 6 }.associate { mapKey(it[0], it[1], it[2]) to it[5] }
    }
  }

  private fun readNumbers(file: File, fields: Int): List<List<Int>> =
      if (!file.isFile) emptyList()
      else
          file.readLines().mapNotNull { line ->
            val parts = line.split(';').mapNotNull { it.trim().toIntOrNull() }
            if (parts.size >= fields) parts else null
          }

  private fun matrixKey(region: Int, matrix: Int, x: Int, y: Int): Long =
      (region.toLong() shl 56) or
          ((matrix.toLong() and 0xFFFF) shl 40) or
          ((x.toLong() and 0xFFFFF) shl 20) or
          (y.toLong() and 0xFFFFF)

  private fun mapKey(region: Int, bank: Int, map: Int): Long =
      (region.toLong() shl 32) or (bank.toLong() shl 16) or map.toLong()

  private fun tileKey(region: Int, bank: Int, map: Int, x: Int, y: Int): Long =
      (region.toLong() shl 56) or
          (bank.toLong() shl 48) or
          (map.toLong() shl 40) or
          ((x.toLong() and 0xFFFFF) shl 20) or
          (y.toLong() and 0xFFFFF)
}
