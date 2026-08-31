@file:JvmName("NdsCollisionExtractor")

package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads walkability for the NDS regions out of the player's own ROM.
 *
 * A gen-4 map is a grid of 32x32-tile blocks. `fielddata/mapmatrix` says which block model sits at
 * each grid cell for a given map, and each block's terrain attributes - one u16 per tile, at a
 * fixed offset inside its land-data member - say what may be walked on. Both formats are stated
 * outright in the decomp (`src/map_matrix.c`, `include/terrain_attributes.h`), including that the
 * attribute offset moved from 0x10 in Platinum to 0x14 in HeartGold.
 *
 * The output is one line per map: `matrixIndex;widthTiles;heightTiles;base64(walkable bitmap)`,
 * where the bitmap has one bit per tile, set when the tile can be stood on. That is everything the
 * server needs to stop a player at a wall; the client keeps drawing the world from the ROM itself.
 */
fun main(args: Array<String>) {
  require(args.size >= 3) { "Usage: <rom> <game: hgss|platinum> <output-dir>" }
  val rom = Path.of(args[0])
  val game = args[1].lowercase()
  val output = Path.of(args[2])
  require(Files.isRegularFile(rom)) { "ROM not found: $rom" }
  Files.createDirectories(output)

  val attributeOffset = if (game == "platinum") 0x10 else 0x14
  NdsRomArchive(rom).use { archive ->
    val matrices = archive.narcMembers(MATRIX_PATHS.first { it in archive.filesByPath })
    val landData = archive.narcMembers(LAND_DATA_PATHS.first { it in archive.filesByPath })
    println(
        "[nds-collision] ${archive.title}: matrices=${matrices.size} landBlocks=${landData.size}")

    // A land block's walkability, computed once and shared by every matrix cell that uses it.
    val blockWalkable = HashMap<Int, BooleanArray>()
    fun walkableOf(blockId: Int): BooleanArray? =
        blockWalkable
            .getOrPut(blockId) {
              val member = landData.getOrNull(blockId) ?: return@getOrPut BooleanArray(0)
              if (member.size < attributeOffset + ATTRIBUTES_SIZE) return@getOrPut BooleanArray(0)
              BooleanArray(TILES_PER_BLOCK) { tile ->
                val at = attributeOffset + tile * 2
                val attribute =
                    (member[at].toInt() and 0xFF) or ((member[at + 1].toInt() and 0xFF) shl 8)
                isWalkable(attribute)
              }
            }
            .takeIf { it.isNotEmpty() }

    var written = 0
    var emptyMatrices = 0
    Files.newBufferedWriter(output.resolve("nds-collision.csv")).use { writer ->
      writer.appendLine("matrixIndex;widthTiles;heightTiles;walkableBase64")
      matrices.forEachIndexed { matrixIndex, matrix ->
        val parsed = parseMatrix(matrix)
        if (parsed == null || parsed.models.isEmpty()) {
          emptyMatrices++
          return@forEachIndexed
        }
        val widthTiles = parsed.width * BLOCK_TILES
        val heightTiles = parsed.height * BLOCK_TILES
        val bits = java.util.BitSet(widthTiles * heightTiles)
        for (cellY in 0 until parsed.height) {
          for (cellX in 0 until parsed.width) {
            val model = parsed.models.getOrNull(cellY * parsed.width + cellX) ?: continue
            val walkable = walkableOf(model) ?: continue
            for (tileY in 0 until BLOCK_TILES) {
              for (tileX in 0 until BLOCK_TILES) {
                if (!walkable[tileY * BLOCK_TILES + tileX]) continue
                val x = cellX * BLOCK_TILES + tileX
                val y = cellY * BLOCK_TILES + tileY
                bits.set(y * widthTiles + x)
              }
            }
          }
        }
        writer.appendLine(
            "$matrixIndex;$widthTiles;$heightTiles;" +
                java.util.Base64.getEncoder().encodeToString(bits.toByteArray()))
        written++
      }
    }
    println(
        "[nds-collision] wrote $written map grids (skipped $emptyMatrices without models) -> $output")
  }
}

private class Matrix(val width: Int, val height: Int, val models: List<Int>)

/**
 * `u8 width, u8 height, u8 hasHeaders, u8 hasAltitudes, u8 nameLength, name...` then the optional
 * header and altitude sections, then the block model ids - straight from `MapMatrixData_Load`.
 */
private fun parseMatrix(data: ByteArray): Matrix? {
  if (data.size < 5) return null
  var cursor = 0
  fun u8() = data[cursor++].toInt() and 0xFF
  val width = u8()
  val height = u8()
  val hasHeaders = u8() != 0
  val hasAltitudes = u8() != 0
  val nameLength = u8()
  cursor += nameLength
  val cells = width * height
  if (width == 0 || height == 0) return null
  if (hasHeaders) cursor += cells * 2
  if (hasAltitudes) cursor += cells
  if (cursor + cells * 2 > data.size) return null
  val models =
      List(cells) {
        val at = cursor + it * 2
        (data[at].toInt() and 0xFF) or ((data[at + 1].toInt() and 0xFF) shl 8)
      }
  return Matrix(width, height, models)
}

/**
 * Terrain attribute to walkability. Gen 4 keeps the collision flag in the high byte: zero means
 * open ground, and the well-known impassable values are the wall family. Everything unknown is
 * treated as walkable, because a false wall strands a player while a false floor is merely a place
 * the client will not draw them into anyway.
 */
private fun isWalkable(attribute: Int): Boolean {
  val collision = (attribute ushr 8) and 0xFF
  return collision == 0x00 || collision == 0x04
}

private const val BLOCK_TILES = 32
private const val TILES_PER_BLOCK = BLOCK_TILES * BLOCK_TILES
private const val ATTRIBUTES_SIZE = 0x800
// HeartGold's retail filesystem keeps these archives unnamed under a/x/y/z; the named paths only
// exist in the decomp. Identified by fingerprint against the ROM: a/0/4/1 holds 288 small members
// whose first bytes are a width/height pair (the matrices), and a/0/6/5 holds 676 members of about
// 20KB (the land blocks). The named paths are tried first so a decomp-built ROM also works.
private val MATRIX_PATHS = listOf("fielddata/mapmatrix/map_matrix.narc", "a/0/4/1")
private val LAND_DATA_PATHS = listOf("fielddata/land_data/land_data.narc", "a/0/6/5")
