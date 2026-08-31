package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * The Expansion's item table - names, descriptions and pockets from `src/data/items.h`.
 *
 * This is the source both for the descriptions of items imported into the client and, paired with a
 * runtime dump of the client's own item names, for computing which Expansion items the client is
 * missing entirely.
 */
object ExpansionItems {

  data class ItemDef(
      val symbol: String,
      val name: String,
      val description: String,
      val pocket: String,
  )

  /** Keyed by full symbol, e.g. `ITEM_ICE_STONE`. */
  fun parse(expansionRoot: Path): Map<String, ItemDef> {
    val text = Files.readString(expansionRoot.resolve("src/data/items.h"))
    val result = linkedMapOf<String, ItemDef>()
    ENTRY.findAll(text).forEach { entry ->
      val symbol = "ITEM_" + entry.groupValues[1]
      val body = entry.groupValues[2]
      val name = NAME.find(body)?.groupValues?.get(1) ?: return@forEach
      val description =
          DESCRIPTION.find(body)?.groupValues?.get(1)?.let { raw ->
            QUOTED.findAll(raw).joinToString("") { it.groupValues[1] }.replace("\\n", " ")
          } ?: ""
      val pocket = POCKET.find(body)?.groupValues?.get(1) ?: "POCKET_ITEMS"
      result[symbol] = ItemDef(symbol, name, description, pocket)
    }
    return result
  }

  private val ENTRY =
      Regex(
          """\[ITEM_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[ITEM_[A-Z0-9_]+]\s*=|\Z)""",
          RegexOption.DOT_MATCHES_ALL,
      )

  private val NAME = Regex("""\.name\s*=\s*(?:ITEM_NAME|_|COMPOUND_STRING)\(\s*"([^"]*)"""")

  private val DESCRIPTION =
      Regex("""\.description\s*=\s*COMPOUND_STRING\((.*?)\)\s*,""", RegexOption.DOT_MATCHES_ALL)

  private val QUOTED = Regex(""""((?:[^"\\]|\\.)*)"""")

  private val POCKET = Regex("""\.pocket\s*=\s*(POCKET_[A-Z_]+)""")
}
