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
  private val SHARED_TEXT =
      Regex("""const u8 (g\w+PokedexText)\[\]\s*=\s*_\(((?:[^()]|\(\w*\))*)\)""")
  private val NAMED_DESCRIPTION = Regex("""\.description\s*=\s*(g\w+PokedexText)""")
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
    // Species with forms share one paragraph through a named variable in shared_dex_text.h;
    // resolve those first so a .description = gGreninjaPokedexText reference lands too.
    val sharedTexts = mutableMapOf<String, String>()
    val sharedFile = infoDir.resolve("shared_dex_text.h")
    if (Files.isRegularFile(sharedFile)) {
      SHARED_TEXT.findAll(Files.readString(sharedFile)).forEach { match ->
        val paragraph = joinQuoted(match.groupValues[2])
        if (paragraph.isNotEmpty()) sharedTexts[match.groupValues[1]] = paragraph
      }
    }

    sources.forEach { source ->
      val text = Files.readString(source)
      val markers = SPECIES_BLOCK.findAll(text).toList()
      markers.forEachIndexed { index, marker ->
        val end = markers.getOrNull(index + 1)?.range?.first ?: text.length
        val block = text.substring(marker.range.first, end)
        val body = DESCRIPTION.find(block)?.groupValues?.get(1)
        val paragraph =
            if (body != null) joinQuoted(body)
            else
                NAMED_DESCRIPTION.find(block)?.groupValues?.get(1)?.let { sharedTexts[it] }
                    ?: return@forEachIndexed
        if (paragraph.isNotEmpty()) {
          result.putIfAbsent(marker.groupValues[1], paragraph)
        }
      }
    }
    // A species whose whole info block is a macro (Scatterbug, Furfrou, Minior...) never shows
    // a .description inside its [SPECIES_X] entry, but its shared variable name spells the
    // species: gScatterbugPokedexText -> SCATTERBUG. Derive that as a last resort.
    sharedTexts.forEach { (variable, text) ->
      val symbol =
          variable
              .removePrefix("g")
              .removeSuffix("PokedexText")
              .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
              .uppercase()
      result.putIfAbsent(symbol, text)
      // Token-pasted names carry the form too (gAlcremieVanillaCreamPokedexText); offer every
      // shortened prefix so the base species picks up its first form's paragraph.
      var prefix = symbol
      while (true) {
        val cut = prefix.lastIndexOf('_')
        if (cut <= 0) break
        prefix = prefix.substring(0, cut)
        result.putIfAbsent(prefix, text)
      }
    }
    return result
  }

  /**
   * The quoted segments joined, keeping the Expansion's own line breaks as the string table's `\n`
   * (retail writes them the same way, e.g. string 5005). The r32645 dex panel does not wrap: joined
   * into one line, a paragraph ran off the page (project owner, 2026-09-13).
   */
  internal fun joinQuoted(body: String): String =
      QUOTED.findAll(body)
          .joinToString("") { it.groupValues[1] }
          .replace("\\\"", "\"")
          .replace(Regex("[ \\t\\r\\n]+"), " ")
          .replace(Regex(" *\\\\n *"), "\\\\n")
          .trim()
          .removeSuffix("\\n")
          .trim()

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
