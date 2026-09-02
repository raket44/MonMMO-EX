package de.fiereu.openmmo.launcher.content

import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Item icon PNGs for every item the overlay creates, staged into the mod archive as
 * `sprites/itemicons/<itemId>.png` - the exact path the client's mod loader reads item icons from
 * (f/EP: "Only .png files supported for /sprites/itemicons/"). Without these, every created item
 * renders with its donor's icon: Water Stone for all the evolution items, one yellow disc for every
 * TM.
 *
 * Sources are the Expansion's own art: `graphics/items/icons/<name>.png` (indexed) plus
 * `graphics/items/icon_palettes/<name>.pal` (JASC, index 0 transparent), paired per item by
 * `src/data/items.h` (.iconPic/.iconPalette) and resolved to files through the INCGFX lines in
 * `src/data/graphics/items.h`. TMs share `icons/tm.png` rendered against the `<type>_tm_hm.pal`
 * palette of the move they teach - the same per-type disc scheme the games use.
 */
object ItemIconStaging {
  data class Summary(val staged: Int, val missing: List<String>)

  fun build(
      expansionRoot: Path,
      tmMoves: List<Int>,
      moveTypesById: Map<Int, String>,
  ): Pair<Map<Int, ByteArray>, Summary> {
    val iconPaths = incgfxPaths(expansionRoot, "gItemIcon_")
    val palettePaths = incgfxPaths(expansionRoot, "gItemIconPalette_")
    val itemArt = itemArtTable(expansionRoot)
    val icons = linkedMapOf<Int, ByteArray>()
    val missing = mutableListOf<String>()

    fun stageSymbol(itemId: Int, symbol: String) {
      val art = itemArt[symbol]
      val iconPath = art?.first?.let(iconPaths::get)
      val palPath = art?.second?.let(palettePaths::get)
      if (iconPath == null || palPath == null) {
        missing += symbol
        return
      }
      runCatching { icons[itemId] = render(expansionRoot, iconPath, palPath) }
          .onFailure { missing += "$symbol (${it.message})" }
    }

    // TMs: the shared disc against the taught move's type palette.
    tmMoves.forEachIndexed { index, moveId ->
      val type = moveTypesById[moveId]?.removePrefix("TYPE_")?.lowercase() ?: "normal"
      val pal = "graphics/items/icon_palettes/${type}_tm_hm.pal"
      runCatching {
            icons[TmPlan.FIRST_ITEM_ID + index] =
                render(expansionRoot, "graphics/items/icons/tm.png", pal)
          }
          .onFailure { missing += "tm:$moveId ($type)" }
    }
    EvoItemPlan.ITEMS.forEachIndexed { index, (symbol, _) ->
      stageSymbol(EvoItemPlan.FIRST_ITEM_ID + index, symbol.removePrefix("ITEM_"))
    }
    ItemImportPlan.compute(expansionRoot).forEach { item ->
      stageSymbol(item.itemId, item.symbol.removePrefix("ITEM_"))
    }
    return icons to Summary(icons.size, missing)
  }

  /** `const u32 gItemIcon_X[] = INCGFX_U32("graphics/items/icons/x.png", ...)` per symbol. */
  private fun incgfxPaths(expansionRoot: Path, prefix: String): Map<String, String> {
    val text = Files.readString(expansionRoot.resolve("src/data/graphics/items.h"))
    return Regex("""$prefix(\w+)\[]\s*=\s*\w+\("([^"]+)"""").findAll(text).associate {
      it.groupValues[1] to it.groupValues[2]
    }
  }

  /** ITEM_SYMBOL to (iconPic symbol, iconPalette symbol) from items.h. */
  private fun itemArtTable(expansionRoot: Path): Map<String, Pair<String, String>> {
    val text = Files.readString(expansionRoot.resolve("src/data/items.h"))
    return Regex(
            """\[ITEM_(\w+)]\s*=\s*\{(.*?)(?=\n\s*\[ITEM_|\n};)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        .findAll(text)
        .mapNotNull { match ->
          val body = match.groupValues[2]
          val pic =
              Regex("""\.iconPic\s*=\s*gItemIcon_(\w+)""").find(body)?.groupValues?.get(1)
                  ?: return@mapNotNull null
          val pal =
              Regex("""\.iconPalette\s*=\s*gItemIconPalette_(\w+)""")
                  .find(body)
                  ?.groupValues
                  ?.get(1) ?: return@mapNotNull null
          match.groupValues[1] to (pic to pal)
        }
        .toMap()
  }

  /** Indexed source rendered against a JASC palette; index 0 is transparent. */
  private fun render(expansionRoot: Path, iconPath: String, palettePath: String): ByteArray {
    val image = ImageIO.read(expansionRoot.resolve(iconPath).toFile())
    check(image.colorModel is IndexColorModel) { "Expected an indexed icon: $iconPath" }
    val raster = image.raster
    val palette = jascPalette(expansionRoot.resolve(palettePath))
    val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until image.height) {
      for (x in 0 until image.width) {
        val index = raster.getSample(x, y, 0)
        out.setRGB(x, y, if (index == 0) 0 else palette.getOrElse(index) { 0 })
      }
    }
    val bytes = ByteArrayOutputStream()
    ImageIO.write(out, "png", bytes)
    return bytes.toByteArray()
  }

  private fun jascPalette(path: Path): IntArray {
    val lines = Files.readAllLines(path)
    check(lines.firstOrNull() == "JASC-PAL") { "Not a JASC palette: $path" }
    return lines
        .drop(3)
        .filter { it.isNotBlank() }
        .map { line ->
          val (r, g, b) = line.trim().split(Regex("\\s+")).map(String::toInt)
          (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        .toIntArray()
  }
}
