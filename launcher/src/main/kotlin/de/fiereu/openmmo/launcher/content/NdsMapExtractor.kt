@file:JvmName("NdsMapExtractor")

package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Extracts server-side map data for the NDS regions from the local decomp projects.
 *
 * The client renders Johto, Sinnoh and Unova entirely from the player's own ROMs, but the server
 * owns collision, warps and encounters - without them a door fades to black and never arrives.
 * Everything needed is in the decomps checked out under `decomp/`: `src/data/map_headers.h` names
 * every map and points at its matrix, event and encounter banks, and the per-zone JSON files under
 * `files/fielddata/eventdata/zone_event` carry the warps, objects and triggers as readable JSON.
 *
 * This pass produces the inventory the MapDef generator consumes:
 * - `nds-maps.csv`: `constant;index;matrixId;eventsBank;encounterBank;mapsec`
 * - `nds-warps.csv`: `mapConstant;x;z;y;anchor;targetConstant`
 *
 * Output lands in `build/nds-maps/<game>/` and is summarized to stdout, so a run says what it found
 * without printing the data itself.
 */
fun main(args: Array<String>) {
  val decompRoot = Path.of(args.getOrElse(0) { "decomp" })
  val outputRoot = Path.of(args.getOrElse(1) { "build/nds-maps" })
  listOf("pokeheartgold", "pokeplatinum", "pokeblack").forEach { game ->
    val root = decompRoot.resolve(game)
    if (!Files.isDirectory(root)) {
      println("[nds-maps] $game: decomp not present, skipped")
      return@forEach
    }
    runCatching { extract(game, root, outputRoot.resolve(game)) }
        .onFailure { println("[nds-maps] $game failed: ${it.message}") }
  }
}

private fun extract(game: String, root: Path, output: Path) {
  Files.createDirectories(output)
  val headers = parseHeaders(root)
  val warps = parseWarps(root)
  Files.newBufferedWriter(output.resolve("nds-maps.csv")).use { writer ->
    writer.appendLine("constant;index;matrixId;eventsBank;encounterBank;mapsec")
    headers.forEach { header ->
      writer.appendLine(
          listOf(
                  header.constant,
                  header.index,
                  header.matrixId,
                  header.eventsBank,
                  header.encounterBank,
                  header.mapsec,
              )
              .joinToString(";"))
    }
  }
  Files.newBufferedWriter(output.resolve("nds-warps.csv")).use { writer ->
    writer.appendLine("mapFile;x;z;y;anchor;targetConstant")
    warps.forEach { warp ->
      writer.appendLine(
          listOf(warp.mapFile, warp.x, warp.z, warp.y, warp.anchor, warp.target).joinToString(";"))
    }
  }
  println(
      "[nds-maps] $game: maps=${headers.size} warps=${warps.size} " +
          "eventFiles=${warps.map { it.mapFile }.distinct().size} -> $output")
}

private data class MapHeaderRow(
    val constant: String,
    val index: Int,
    val matrixId: String,
    val eventsBank: String,
    val encounterBank: String,
    val mapsec: String,
)

private data class WarpRow(
    val mapFile: String,
    val x: Int,
    val z: Int,
    val y: Int,
    val anchor: Int,
    val target: String,
)

/**
 * `src/data/map_headers.h` is a designated-initializer array: `[MAP_NEW_BARK_TOWN] = { .field =
 * value, ... }`. The array index is the map's own id, which is what the ROM and the client both
 * address maps by.
 */
private fun parseHeaders(root: Path): List<MapHeaderRow> {
  val file = root.resolve("src/data/map_headers.h")
  if (!Files.isRegularFile(file)) return emptyList()
  val text = Files.readString(file)
  val entries = ENTRY.findAll(text).toList()
  val order = mutableMapOf<String, Int>()
  entries.forEachIndexed { index, match -> order.putIfAbsent(match.groupValues[1], index) }
  return entries.map { match ->
    val constant = match.groupValues[1]
    val body = match.groupValues[2]
    MapHeaderRow(
        constant = constant,
        index = order.getValue(constant),
        matrixId = field(body, "matrixId"),
        eventsBank = field(body, "eventsBank"),
        encounterBank = field(body, "wildEncounterBank"),
        mapsec = field(body, "mapsec"),
    )
  }
}

/** Zone-event JSON, read as text: the warp blocks are a fixed five-key shape. */
private fun parseWarps(root: Path): List<WarpRow> {
  val directory = root.resolve("files/fielddata/eventdata/zone_event")
  if (!Files.isDirectory(directory)) return emptyList()
  val result = mutableListOf<WarpRow>()
  Files.list(directory).use { files ->
    files
        .filter { it.name.endsWith(".json") }
        .sorted()
        .forEach { file ->
          val text = Files.readString(file)
          val warpsBlock = WARPS.find(text)?.groupValues?.get(1) ?: return@forEach
          WARP.findAll(warpsBlock).forEach { match ->
            result +=
                WarpRow(
                    mapFile = file.name.removeSuffix(".json"),
                    x = match.groupValues[1].toInt(),
                    z = match.groupValues[2].toInt(),
                    y = match.groupValues[5].toInt(),
                    anchor = match.groupValues[4].toInt(),
                    target = match.groupValues[3],
                )
          }
        }
  }
  return result
}

private fun field(body: String, name: String): String =
    Regex("""\.$name\s*=\s*([A-Za-z0-9_]+)""").find(body)?.groupValues?.get(1).orEmpty()

private val ENTRY =
    Regex("""\[(MAP_[A-Z0-9_]+)]\s*=\s*\{(.*?)\n\s*\},""", RegexOption.DOT_MATCHES_ALL)

private val WARPS = Regex(""""warps"\s*:\s*\[(.*?)\n\s*]""", RegexOption.DOT_MATCHES_ALL)

// Built from a string rather than a raw literal: the pattern contains a slash-star sequence that
// would close the enclosing comment style Kotlin uses for raw strings in this file.
private val WARP =
    Regex(
        "\\{\\s*\"x\"\\s*:\\s*(-?\\d+),\\s*\"z\"\\s*:\\s*(-?\\d+)," +
            "\\s*\"header\"\\s*:\\s*\"([A-Za-z0-9_]+)\"," +
            "\\s*\"anchor\"\\s*:\\s*(-?\\d+),\\s*\"y\"\\s*:\\s*(-?\\d+)",
        RegexOption.DOT_MATCHES_ALL,
    )
