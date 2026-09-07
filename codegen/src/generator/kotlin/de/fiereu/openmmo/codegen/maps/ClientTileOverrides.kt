package de.fiereu.openmmo.codegen.maps

/**
 * The client's own edits to the GBA maps, applied on top of the decomp layouts so the server's
 * collision and behaviors match what the player actually walks on.
 *
 * PokeMMO's client (f/Xk0) carries a hardcoded table of block replacements keyed by
 * `region * 499 + layoutId`: the Vermilion City second pier, the raid-den sand on Route 10,
 * extra rocks, walled-off doors. It patches the ROM layout's block words before collision is
 * evaluated, so a server built from the decomp alone disagrees with the client on those tiles:
 * it refused the pier (ocean water underneath) and let players into the raid den. Resource
 * `monmmo/client-tile-overrides.csv` is that table, extracted with
 * tools/client-overrides/extract-tile-overrides.pl; `monmmo/client-layout-ids.csv` maps the
 * client's layout numbering to decomp LAYOUT_ ids (Hoenn's numbering is the Emerald table index;
 * FireRed's runs 17-18 ahead of the decomp's, fitted per layout from each patch's geometry).
 */
class ClientTileOverrides private constructor(
    /** (regionId, decomp LAYOUT_ id) -> tile index -> block word. */
    private val patches: Map<Pair<Int, String>, Map<Int, Int>>,
) {
  /** [blocks] (little-endian map.bin words) with this layout's client edits applied. */
  fun patched(regionId: Int, layoutId: String, blocks: ByteArray): ByteArray {
    val edits = patches[regionId to layoutId] ?: return blocks
    val out = blocks.copyOf()
    for ((index, word) in edits) {
      val offset = index * 2
      if (offset + 1 >= out.size) continue
      out[offset] = (word and 0xFF).toByte()
      out[offset + 1] = ((word shr 8) and 0xFF).toByte()
    }
    return out
  }

  fun count(): Int = patches.values.sumOf { it.size }

  companion object {
    fun load(): ClientTileOverrides {
      val layoutIds = HashMap<Pair<Int, Int>, String>()
      resourceLines("/monmmo/client-layout-ids.csv").forEach { line ->
        val (region, clientId, layout) = line.split(',')
        layoutIds[region.toInt() to clientId.toInt()] = layout
      }
      val patches = HashMap<Pair<Int, String>, MutableMap<Int, Int>>()
      resourceLines("/monmmo/client-tile-overrides.csv").forEach { line ->
        val (region, clientId, index, word) = line.split(',')
        val layout = layoutIds[region.toInt() to clientId.toInt()] ?: return@forEach
        patches.getOrPut(region.toInt() to layout) { HashMap() }[index.toInt()] = word.toInt()
      }
      return ClientTileOverrides(patches)
    }

    private fun resourceLines(path: String): List<String> =
        ClientTileOverrides::class.java.getResourceAsStream(path)?.bufferedReader()?.readLines()
            ?.drop(1)?.filter { it.isNotBlank() && !it.startsWith("#") } ?: emptyList()
  }
}
