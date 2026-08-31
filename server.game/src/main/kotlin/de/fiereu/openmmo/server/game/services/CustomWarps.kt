package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Warps that admins place by hand from inside the game - the tool that makes regions the server has
 * no map data for traversable without anyone writing code.
 *
 * The flow: stand on a door or gate tile and type `/warp from`, travel to where it should lead
 * (walk, /tp, or /tp raw), and type `/warp to`. From then on, any player stepping onto that tile is
 * warped - and because the check runs in the trusted-movement path too, this works in Johto, Sinnoh
 * and Unova, where the client fades out at its own doors and waits for exactly this response.
 * `/warp both` adds the return trip.
 *
 * Persisted as `custom-warps.txt` beside the server, one warp per line:
 * `region;bank;map;x;y;destRegion;destBank;destMap;destX;destY`.
 */
@Singleton
class CustomWarps @Inject constructor() {

  data class Warp(
      val region: Int,
      val bank: Int,
      val map: Int,
      val x: Int,
      val y: Int,
      val destRegion: Int,
      val destBank: Int,
      val destMap: Int,
      val destX: Int,
      val destY: Int,
  )

  private val file = File("custom-warps.txt")
  private val byTile = ConcurrentHashMap<Long, Warp>()

  init {
    load()
  }

  fun at(region: Int, bank: Int, map: Int, x: Int, y: Int): Warp? =
      byTile[key(region, bank, map, x, y)]

  fun all(): List<Warp> = byTile.values.toList()

  fun add(warp: Warp) {
    byTile[key(warp.region, warp.bank, warp.map, warp.x, warp.y)] = warp
    save()
    log.info {
      "Custom warp saved: ${warp.region}:${warp.bank}:${warp.map} (${warp.x},${warp.y}) -> " +
          "${warp.destRegion}:${warp.destBank}:${warp.destMap} (${warp.destX},${warp.destY})"
    }
  }

  /** Removes the warp on the given tile; true when one was there. */
  fun remove(region: Int, bank: Int, map: Int, x: Int, y: Int): Boolean {
    val removed = byTile.remove(key(region, bank, map, x, y)) != null
    if (removed) save()
    return removed
  }

  private fun load() {
    if (!file.isFile) return
    file.readLines().forEach { line ->
      val parts = line.split(';').mapNotNull { it.trim().toIntOrNull() }
      if (parts.size != 10) return@forEach
      val warp =
          Warp(
              parts[0],
              parts[1],
              parts[2],
              parts[3],
              parts[4],
              parts[5],
              parts[6],
              parts[7],
              parts[8],
              parts[9])
      byTile[key(warp.region, warp.bank, warp.map, warp.x, warp.y)] = warp
    }
    log.info { "Loaded ${byTile.size} custom warps" }
  }

  private fun save() {
    val lines =
        byTile.values.map {
          listOf(
                  it.region,
                  it.bank,
                  it.map,
                  it.x,
                  it.y,
                  it.destRegion,
                  it.destBank,
                  it.destMap,
                  it.destX,
                  it.destY)
              .joinToString(";")
        }
    file.writeText(lines.sorted().joinToString("\n") + if (lines.isEmpty()) "" else "\n")
  }

  private fun key(region: Int, bank: Int, map: Int, x: Int, y: Int): Long =
      ((region.toLong() and 0xFF) shl 40) or
          ((bank.toLong() and 0xFF) shl 32) or
          ((map.toLong() and 0xFF) shl 24) or
          ((x.toLong() and 0xFFF) shl 12) or
          (y.toLong() and 0xFFF)
}
