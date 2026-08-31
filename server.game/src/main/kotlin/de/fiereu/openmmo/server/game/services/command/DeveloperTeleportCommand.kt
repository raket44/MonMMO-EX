package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.WarpService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Teleport, with the same forgiving name matching as /givemon: any fragment lists what it matches,
 * a lone match goes, coordinates are optional, and a region prefix disambiguates - `/tp
 * hoenn/route110`. `/tp raw <region> <bank> <map> [x y]` pushes the client to a map the server does
 * not host at all, which is how NDS-region rendering gets probed.
 */
@Singleton
class DeveloperTeleportCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val maps: MapManager,
    private val warps: WarpService,
    private val battles: BattleService,
) : ChatCommand {
  override val name = "tp"
  override val aliases = listOf("teleport", "goto")
  override val usage =
      "/tp <map or mark> [x y]; /tp next|prev|nextbank|prevbank to browse; " +
          "/tp where; /tp mark <name>; /tp raw <region> <bank> <map>"
  override val description = "warps by map name or saved mark, and browses client-only regions"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    if (ctx.state.blocksNewScript) {
      ctx.reply("Finish the current script first.")
      return
    }
    if (battles.inBattle(ctx.characterId)) {
      ctx.reply("Finish the battle first.")
      return
    }
    when (ctx.args.firstOrNull()) {
      "raw" -> {
        rawTeleport(ctx)
        return
      }
      "probe" -> {
        // Finding how the client turns bank/map into an NDS zone id. The ROM's own numbering is
        // known (New Bark Town is zone 60 in HeartGold), so pushing candidate encodings of a
        // known zone and seeing which one draws that town settles it in one pass.
        val zone = ctx.args.getOrNull(1)?.toIntOrNull()
        val step = ctx.args.getOrNull(2)?.toIntOrNull() ?: 0
        if (zone == null) {
          ctx.reply(
              "/tp probe <zoneId> [step] - step 0 sends bank=zone/256 map=zone%256, 1 sends bank=0 map=zone, 2 sends bank=zone map=0.")
          return
        }
        val (bank, map) =
            when (step) {
              1 -> 0 to zone
              2 -> zone to 0
              else -> (zone / 256) to (zone % 256)
            }
        warps.executeRawWarp(ctx.session, ctx.characterId, ctx.state.regionId, bank, map, 10, 10)
        ctx.reply("probe step $step: region ${ctx.state.regionId} bank $bank map $map (zone $zone)")
        return
      }
      "where" -> {
        ctx.reply(
            "You are at ${ctx.state.regionId}:${ctx.state.bankId}:${ctx.state.mapId} " +
                "(${ctx.state.x}, ${ctx.state.y}). /tp mark <name> saves this spot.")
        return
      }
      "next" -> {
        step(ctx, bankDelta = 0, mapDelta = 1)
        return
      }
      "prev" -> {
        step(ctx, bankDelta = 0, mapDelta = -1)
        return
      }
      "nextbank" -> {
        step(ctx, bankDelta = 1, mapDelta = 0, resetMap = true)
        return
      }
      "prevbank" -> {
        step(ctx, bankDelta = -1, mapDelta = 0, resetMap = true)
        return
      }
      "mark" -> {
        val markName = ctx.args.drop(1).joinToString(" ").trim()
        if (markName.isEmpty()) {
          ctx.reply("/tp mark <name> - saves where you stand under that name.")
          return
        }
        Bookmarks.save(
            markName,
            ctx.state.regionId,
            ctx.state.bankId,
            ctx.state.mapId,
            ctx.state.x.toInt(),
            ctx.state.y.toInt())
        ctx.reply(
            "Marked \"$markName\" at ${ctx.state.regionId}:${ctx.state.bankId}:${ctx.state.mapId}.")
        return
      }
      "marks" -> {
        val all = Bookmarks.all()
        ctx.reply(
            if (all.isEmpty()) "No marks saved yet. Explore with /tp next, then /tp mark <name>."
            else "Marks: " + all.joinToString(", ") { it.name })
        return
      }
      null -> {
        ctx.reply(usage)
        return
      }
    }
    // A bookmark by name beats a map by name, so marked spots in client-only regions resolve.
    Bookmarks.find(ctx.args.joinToString(" "))?.let { mark ->
      goTo(ctx, mark.region, mark.bank, mark.map, mark.x, mark.y)
      ctx.reply("Teleported to ${mark.name}.")
      return
    }
    // Coordinates are whatever trailing numbers were given; everything before them is the name,
    // so multi-word names need no quoting.
    val numbers = ctx.args.takeLastWhile { it.toIntOrNull() != null }.map { it.toInt() }
    val nameArgs = ctx.args.dropLast(numbers.size)
    if (nameArgs.isEmpty()) {
      ctx.reply(usage)
      return
    }
    val map = resolveMap(ctx, nameArgs.joinToString("")) ?: return
    val requested = if (numbers.size >= 2) (numbers[0] to numbers[1]) else null
    val landing = requested ?: landingSpot(map)
    if (landing == null) {
      ctx.reply("${map.sourceName} has no walkable tile to land on; give x and y.")
      return
    }
    val (x, y) = landing
    val tile = map.tileAt(x, y)
    if (tile == null) {
      ctx.reply("($x, $y) is outside ${map.sourceName} (${map.width}x${map.height}).")
      return
    }

    warps.executeWarp(
        ctx.session,
        ctx.characterId,
        WarpTile(
            x = ctx.state.x.toInt(),
            y = ctx.state.y.toInt(),
            targetRegionId = map.regionId,
            targetBankId = map.bankId,
            targetMapId = map.mapId,
            targetX = x,
            targetY = y,
            targetElevation = tile.elevation,
            exitFacing = ctx.state.facingDirection,
        ),
    )
    ctx.reply("Teleported to ${pretty(map.sourceName)} ($x, $y).")
  }

  /**
   * Steps to a neighbouring map id or bank - the browse gesture for regions whose map names nobody
   * knows yet. A hosted destination warps normally; anything else goes raw for the client to render
   * from its own data.
   */
  private fun step(ctx: CommandContext, bankDelta: Int, mapDelta: Int, resetMap: Boolean = false) {
    val region = ctx.state.regionId
    val bank = ctx.state.bankId + bankDelta
    val map = if (resetMap) 1 else ctx.state.mapId + mapDelta
    goTo(ctx, region, bank, map, 8, 8)
    ctx.reply("Now at $region:$bank:$map. /tp next, prev, nextbank, prevbank; /tp mark <name>.")
  }

  /** Hosted maps get the real warp with a safe landing; everything else goes raw. */
  private fun goTo(ctx: CommandContext, region: Int, bank: Int, map: Int, x: Int, y: Int) {
    val hosted = maps.getMap(region, bank, map)
    if (hosted != null) {
      val (safeX, safeY) =
          if (hosted.tileAt(x, y)?.blocksMovement() == false) x to y
          else landingSpot(hosted) ?: (x to y)
      warps.executeWarp(
          ctx.session,
          ctx.characterId,
          WarpTile(
              x = ctx.state.x.toInt(),
              y = ctx.state.y.toInt(),
              targetRegionId = hosted.regionId,
              targetBankId = hosted.bankId,
              targetMapId = hosted.mapId,
              targetX = safeX,
              targetY = safeY,
              targetElevation = hosted.tileAt(safeX, safeY)?.elevation ?: 0,
              exitFacing = ctx.state.facingDirection,
          ),
      )
    } else {
      warps.executeRawWarp(ctx.session, ctx.characterId, region, bank, map, x, y)
    }
  }

  /** Places the client on a map the server has no data for - purely to see what it renders. */
  private fun rawTeleport(ctx: CommandContext) {
    val numbers = ctx.args.drop(1).mapNotNull { it.toIntOrNull() }
    if (numbers.size < 3) {
      ctx.reply("/tp raw <region> <bank> <map> [x y] - ids may be negative (Galar banks are).")
      return
    }
    val x = numbers.getOrElse(3) { 10 }
    val y = numbers.getOrElse(4) { 10 }
    warps.executeRawWarp(ctx.session, ctx.characterId, numbers[0], numbers[1], numbers[2], x, y)
    ctx.reply(
        "Pushed the client to ${numbers[0]}:${numbers[1]}:${numbers[2]} ($x, $y). The server " +
            "holds no data there, so rendering is the client's own and movement will not work; " +
            "/tp back out by name.")
  }

  /** The first walkable tile nearest the map's centre, so a bare /tp never lands in a wall. */
  private fun landingSpot(map: MapDef): Pair<Int, Int>? {
    val centreX = map.width / 2
    val centreY = map.height / 2
    var best: Pair<Int, Int>? = null
    var bestDistance = Int.MAX_VALUE
    for (y in 0 until map.height) {
      for (x in 0 until map.width) {
        val tile = map.tileAt(x, y) ?: continue
        if (tile.blocksMovement()) continue
        val distance = (x - centreX) * (x - centreX) + (y - centreY) * (y - centreY)
        if (distance < bestDistance) {
          bestDistance = distance
          best = x to y
        }
      }
    }
    return best
  }

  private fun resolveMap(ctx: CommandContext, argument: String): MapDef? {
    val raw = normalize(argument)
    // A region prefix pins the region: hoenn/route110, kanto/viridianforest.
    val (regionFilter, query) =
        REGIONS.entries
            .firstOrNull { raw.startsWith(normalize(it.value)) }
            ?.let { it.key to raw.removePrefix(normalize(it.value)) } ?: (null to raw)
    if (query.isEmpty()) {
      ctx.reply(usage)
      return null
    }
    val candidates =
        maps
            .allMaps()
            .filter { it.sourceName.isNotBlank() }
            .filter { regionFilter == null || it.regionId.toInt() == regionFilter }
    val keyed = candidates.map { normalize(it.sourceName) to it }
    val exact = keyed.filter { (key, _) -> key == query }
    val prefix = keyed.filter { (key, _) -> key.startsWith(query) }
    val substring = keyed.filter { (key, _) -> query in key }
    // Misspellings land here: "vermillion" is neither a prefix nor a substring of
    // "vermilioncity", so the query is also compared against a same-length slice of each name.
    val tolerance = if (query.length > 4) 2 else 1
    val fuzzy =
        keyed.filter { (key, _) ->
          editDistance(key.take(query.length + 1), query) <= tolerance ||
              editDistance(key, query) <= tolerance
        }
    val matches =
        (exact.ifEmpty { prefix }.ifEmpty { substring }.ifEmpty { fuzzy }).map { it.second }
    if (matches.isEmpty()) {
      ctx.reply("No map matches \"$argument\". Try any part of the name, like route or cave.")
      return null
    }
    // The player's own region wins a cross-region tie, so /tp route1 in Kanto stays in Kanto.
    matches.singleOrNull()?.let {
      return it
    }
    matches
        .filter { it.regionId.toInt() == ctx.state.regionId }
        .singleOrNull()
        ?.let {
          return it
        }
    val shown = matches.take(MAX_SUGGESTIONS).joinToString(", ") { label(it) }
    val more = matches.size - MAX_SUGGESTIONS
    ctx.reply(
        "Matching maps: $shown" +
            (if (more > 0) " and $more more (narrow it down)" else "") +
            ". Send /tp with one of these; prefix region/ to pin a region.")
    return null
  }

  private fun label(map: MapDef): String =
      "${REGIONS[map.regionId.toInt()] ?: "${map.regionId}/"}${pretty(map.sourceName)}"

  /**
   * Decomp names read like ViridianCity_Gym_B1F; people say "Viridian City Gym B1F". Spaces go in
   * at case and letter-digit boundaries, and matching is normalization-based, so either form works
   * as input.
   */
  private fun pretty(sourceName: String): String =
      sourceName
          .replace('_', ' ')
          .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
          .replace(Regex("(?<=[A-Za-z])(?=[0-9])"), " ")

  private fun normalize(text: String): String =
      text.lowercase().filter { it.isLetterOrDigit() || it == '/' }

  /** Plain Levenshtein, small strings only, for catching misspellings like "vermillion". */
  private fun editDistance(left: String, right: String): Int {
    if (left == right) return 0
    var previous = IntArray(right.length + 1) { it }
    for (i in 1..left.length) {
      val current = IntArray(right.length + 1)
      current[0] = i
      for (j in 1..right.length) {
        val substitution = previous[j - 1] + if (left[i - 1] == right[j - 1]) 0 else 1
        current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
      }
      previous = current
    }
    return previous[right.length]
  }

  private companion object {
    const val MAX_SUGGESTIONS = 15

    // Client rendering order for the five supported ROMs, verified in-game for 2.
    val REGIONS = mapOf(0 to "kanto/", 1 to "hoenn/", 2 to "unova/", 3 to "sinnoh/", 4 to "johto/")
  }
}

/**
 * Named teleport spots, saved by standing somewhere and typing `/tp mark <name>`. They exist for
 * the regions the server does not host: nobody knows Unova's bank and map numbers, so the flow is
 * browse with /tp next, recognise a place, mark it, and teleport by name ever after. A flat file
 * beside the server, one `name;region;bank;map;x;y` line each.
 */
private object Bookmarks {
  private val file = java.io.File("tp-bookmarks.txt")

  data class Mark(
      val name: String,
      val region: Int,
      val bank: Int,
      val map: Int,
      val x: Int,
      val y: Int,
  )

  fun all(): List<Mark> =
      if (!file.isFile) emptyList()
      else
          file.readLines().mapNotNull { line ->
            val p = line.split(';')
            if (p.size != 6) return@mapNotNull null
            val numbers = p.drop(1).map { it.toIntOrNull() ?: return@mapNotNull null }
            Mark(p[0], numbers[0], numbers[1], numbers[2], numbers[3], numbers[4])
          }

  fun find(name: String): Mark? {
    val wanted = normalize(name)
    return all().firstOrNull { normalize(it.name) == wanted }
  }

  fun save(name: String, region: Int, bank: Int, map: Int, x: Int, y: Int) {
    val kept = all().filterNot { normalize(it.name) == normalize(name) }
    val lines =
        kept.map { "${it.name};${it.region};${it.bank};${it.map};${it.x};${it.y}" } +
            "${name.replace(';', ',')};$region;$bank;$map;$x;$y"
    file.writeText(lines.joinToString("\n") + "\n")
  }

  private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }
}
