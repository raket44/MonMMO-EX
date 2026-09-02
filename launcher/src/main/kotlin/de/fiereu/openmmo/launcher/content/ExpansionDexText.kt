package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The Expansion's own dex-entry paragraphs, parsed from the species_info headers.
 *
 * Each species block carries `.description = COMPOUND_STRING("line\n" "line...")`; the quoted
 * segments are joined into one paragraph with the ROM's hard line breaks turned into spaces, since
 * the client's label does its own wrapping. A form without its own description falls back to its
 * base species by walking the symbol's underscore suffixes, the same family walk the learnsets use.
 */
object ExpansionDexText {
  private val SPECIES_BLOCK = Regex("""\[SPECIES_([A-Z0-9_]+)]\s*=""")
  private val DESCRIPTION =
      Regex("""\.description\s*=\s*COMPOUND_STRING\(((?:[^()]|\([^)]*\))*)\)""")
  private val QUOTED = Regex(""""((?:[^"\\]|\\.)*)"""")

  /** Species symbol (without the SPECIES_ prefix) to its dex paragraph. */
  fun parse(expansionRoot: Path): Map<String, String> {
    val result = linkedMapOf<String, String>()
    val infoDir = expansionRoot.resolve("src/data/pokemon/species_info")
    val sources =
        (Files.list(infoDir).use { entries -> entries.filter { it.name.endsWith(".h") }.toList() } +
                expansionRoot.resolve("src/data/pokemon/species_info.h").takeIf {
                  Files.isRegularFile(it)
                })
            .filterNotNull()
    sources.forEach { source ->
      val text = Files.readString(source)
      val markers = SPECIES_BLOCK.findAll(text).toList()
      markers.forEachIndexed { index, marker ->
        val end = markers.getOrNull(index + 1)?.range?.first ?: text.length
        val block = text.substring(marker.range.first, end)
        val body = DESCRIPTION.find(block)?.groupValues?.get(1) ?: return@forEachIndexed
        val paragraph =
            QUOTED.findAll(body)
                .joinToString("") { it.groupValues[1] }
                .replace("\\n", " ")
                .replace("\\\"", "\"")
                .replace(Regex("\\s+"), " ")
                .trim()
        if (paragraph.isNotEmpty()) {
          result.putIfAbsent(marker.groupValues[1], paragraph)
        }
      }
    }
    return result
  }

  /** The paragraph for a symbol, falling back through the family the way learnsets do. */
  fun forSymbol(table: Map<String, String>, symbol: String): String? {
    var current = symbol.removePrefix("SPECIES_")
    while (current.isNotEmpty()) {
      table[current]?.let {
        return it
      }
      val cut = current.lastIndexOf('_')
      if (cut <= 0) return null
      current = current.substring(0, cut)
    }
    return null
  }
}
