package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.server.game.services.NdsWarps
import de.fiereu.openmmo.server.game.services.WarpService
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Teleports the client's own GM Menu asks for. Its Teleport window (decompiled `f.EU1`) posts them
 * as ordinary chat lines, so implementing them here is what makes the built-in buttons work:
 * - `//moveto <region> <bank> <map> <x> <y>` from `f.EU1.jk0(byte,byte,byte,short,short)`
 * - `//moveto2 <region> <bank> <x> <y> <map> <flag>` from `f.EU1.tm(byte,short,short,short,byte,
 *   boolean)` - note the coordinates sit BEFORE the map id here. Confirmed against live clicks: `2
 *   6 781 589 0 false` is Striaton City, which the ROM's own map header also places at bank 6, map
 *   0, (781, 589).
 *
 * Outdoor NDS maps carry global matrix coordinates, interiors carry local ones; both are passed
 * through untouched because they are exactly what the client expects back.
 */
@Singleton
class ClientMoveToCommand
@Inject
constructor(
    private val warps: WarpService,
    private val maps: MapManager,
    private val ndsWarps: NdsWarps,
) : ChatCommand {
  override val name = "moveto"
  override val usage = "//moveto <region> <bank> <map> <x> <y>"
  override val description = "teleport used by the client's GM Menu"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val numbers = ctx.args.mapNotNull { it.toIntOrNull() }
    if (numbers.size < 5) {
      ctx.reply(usage)
      return
    }
    moveTo(ctx, warps, maps, ndsWarps, numbers[0], numbers[1], numbers[2], numbers[3], numbers[4])
  }
}

/** The GM Menu's other teleport form, with the coordinates ahead of the map id. */
@Singleton
class ClientMoveTo2Command
@Inject
constructor(
    private val warps: WarpService,
    private val maps: MapManager,
    private val ndsWarps: NdsWarps,
) : ChatCommand {
  override val name = "moveto2"
  override val usage = "//moveto2 <region> <bank> <x> <y> <map> [flag]"
  override val description = "teleport used by the client's GM Menu hotspot buttons"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val numbers = ctx.args.mapNotNull { it.toIntOrNull() }
    if (numbers.size < 4) {
      ctx.reply(usage)
      return
    }
    // The SHORT already carries the FULL header index (it fits 412 fine); the trailing byte is
    // the ENTRANCE ANCHOR within that map, NOT an index-high byte. Folding it into the index
    // sent every anchor=1 button to header index+256 (a random gym interior) and refused every
    // anchor>=2 button as out of range - all anchor=0 buttons worked, which hid the bug.
    val anchor = numbers.getOrElse(4) { 0 }
    moveTo(ctx, warps, maps, ndsWarps, numbers[0], numbers[1], 0, numbers[2], numbers[3], anchor)
  }
}

private suspend fun moveTo(
    ctx: CommandContext,
    warps: WarpService,
    maps: MapManager,
    ndsWarps: NdsWarps,
    region: Int,
    rawBank: Int,
    rawMap: Int,
    x: Int,
    y: Int,
    anchor: Int = 0,
) {
  // The GM Menu addresses NDS maps by their ROM map-header index, which runs past 255 - Undella
  // Town is 412. //moveto carries it split across two BYTES (recombine map shl 8 or bank);
  // //moveto2 passes the full index in its short and rawMap=0, so the same expression is a
  // no-op there. The anchor (moveto2's trailing byte) names an entrance within the map.
  val index = (rawMap shl 8) or (rawBank and 0xFFFF)
  val bank = index and 0xFF
  val map = index shr 8
  // ROM header counts: White 427, Platinum 593, HeartGold 540. The GM Menu carries a few
  // bookmarks beyond them; forwarding those makes the client throw on the header lookup.
  val headerCount =
      when (region) {
        2 -> 427
        3 -> 593
        4 -> 540
        else -> Int.MAX_VALUE
      }
  if (index >= headerCount) {
    ctx.reply("Map index $index does not exist in this region's ROM ($headerCount maps).")
    return
  }
  // The client's hotspot coordinates are the map header's world position: a fine city centre for
  // maps on the world matrix (matrix 0), but junk for everything else - an interior's header
  // carries its EXTERIOR's position (Cold Storage's says "somewhere in Driftveil"). Off the world
  // matrix the generated spawn table lands on the map's own door instead.
  var spawnX = x
  var spawnY = y
  val onWorldMatrix = NdsMapSpawns.matrixAt(region, bank, map) == 0
  if ((spawnX == 0 && spawnY == 0) || !onWorldMatrix) {
    NdsMapSpawns.at(region, bank, map)?.let {
      spawnX = it.first
      spawnY = it.second
    }
  }
  log.info {
    "[GM Menu] teleport $region:$bank:$map ($spawnX, $spawnY) from index $index anchor=$anchor"
  }

  // Every destination the client's own map lists send lands here, so browsing them in game
  // records the raw bank/map directory as a side effect.
  runCatching {
    java.io.File("map-directory-observed.txt").appendText("$region;$bank;$map;$spawnX;$spawnY\n")
  }

  val destination = maps.getMap(region.toByte(), bank.toByte(), map.toByte())
  if (destination == null) {
    // NDS regions render from the client's own ROM; the server holds no tiles for them. The
    // button's own coordinates are used AS-IS (they are the ROM's, and they are right) - the
    // one thing a raw spawn misses is the RAIL: landing unattached on a rail street leaves
    // movement locked to one axis, which is why some buttons only half-worked. Attach to the
    // nearest rail line when the landing area has one.
    val railLine = ndsWarps.nearestRailLine(region, bank, map, spawnX, spawnY)
    warps.executeRawWarp(ctx.session, ctx.characterId, region, bank, map, spawnX, spawnY, railLine)
    ctx.reply("Teleported to $region:$bank:$map ($spawnX, $spawnY).")
    return
  }
  warps.executeWarp(
      ctx.session,
      ctx.characterId,
      WarpTile(
          x = ctx.state.x.toInt(),
          y = ctx.state.y.toInt(),
          targetRegionId = region.toByte(),
          targetBankId = bank.toByte(),
          targetMapId = map.toByte(),
          targetX = spawnX,
          targetY = spawnY,
          exitFacing = Direction.DOWN,
      ),
  )
  ctx.reply("Teleported to $region:$bank:$map ($spawnX, $spawnY).")
}

/**
 * Entry tiles for NDS maps, read out of each ROM map header (`nds-map-spawns.txt`, generated by
 * tools/nds/UnovaDir.java). The client's GM Menu sends (0, 0) for maps it has no hotspot
 * coordinates for; this fills those in. Reloaded whenever the file changes.
 */
internal object NdsMapSpawns {
  private val file = java.io.File("nds-map-spawns.txt")
  @Volatile private var loadedAt = 0L
  @Volatile private var spawns: Map<Long, Pair<Int, Int>> = emptyMap()
  @Volatile private var matrices: Map<Long, Int> = emptyMap()

  fun at(region: Int, bank: Int, map: Int): Pair<Int, Int>? {
    refresh()
    return spawns[key(region, bank, map)]
  }

  /** The ROM matrix the map is stitched on (0 = the seamless world), or null when unknown. */
  fun matrixAt(region: Int, bank: Int, map: Int): Int? {
    refresh()
    return matrices[key(region, bank, map)]
  }

  private fun refresh() {
    val stamp = if (file.isFile) file.lastModified() else 0L
    if (stamp == loadedAt) return
    loadedAt = stamp
    val rows =
        if (stamp == 0L) emptyList()
        else
            file.readLines().mapNotNull { line ->
              val numbers = line.split(';').mapNotNull { it.trim().toIntOrNull() }
              if (numbers.size < 5) null else numbers
            }
    spawns = rows.associate { key(it[0], it[1], it[2]) to (it[3] to it[4]) }
    matrices = rows.filter { it.size >= 6 }.associate { key(it[0], it[1], it[2]) to it[5] }
  }

  private fun key(region: Int, bank: Int, map: Int): Long =
      (region.toLong() shl 32) or (bank.toLong() shl 16) or map.toLong()
}
