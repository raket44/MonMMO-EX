package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * Ability names and descriptions, read from the Expansion's own ability table.
 *
 * The client resolves an ability's name from string id `210000 + abilityId` and its description
 * from `220000 + abilityId`. Ids the ROM covers are named by the ROM, but the string table is
 * consulted as well - PokeMMO's own abilities sit at 210500 and up - so the range the Expansion's
 * post-Gen-3 ids occupy is free for imported species to name their abilities the same way.
 */
object AbilityText {
  const val NAME_BASE = 210000
  const val DESCRIPTION_BASE = 220000

  data class Ability(val id: Int, val name: String, val description: String)

  private val ENTRY =
      Regex("""\[ABILITY_([A-Z0-9_]+)]\s*=\s*\{(.*?)\n    },""", RegexOption.DOT_MATCHES_ALL)
  private val NAME = Regex("""\.name\s*=\s*(?:COMPOUND_STRING|_)\("([^"]*)"\)""")
  private val DESCRIPTION =
      Regex(
          """\.description\s*=\s*(?:COMPOUND_STRING|_)\((.*?)\),\n""", RegexOption.DOT_MATCHES_ALL)
  private val LITERAL = Regex("\"([^\"]*)\"")
  private val DECLARATION =
      Regex("""^\s*ABILITY_([A-Z][A-Z0-9_]*)\s*(?:=\s*([^,]+))?,""", RegexOption.MULTILINE)

  fun parse(expansionRoot: Path, ids: Map<String, Int>): List<Ability> {
    val text = Files.readString(expansionRoot.resolve("src/data/abilities.h"))
    return ENTRY.findAll(text)
        .mapNotNull { match ->
          val id = ids["ABILITY_" + match.groupValues[1]] ?: return@mapNotNull null
          val body = match.groupValues[2]
          val name = NAME.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
          // A description is sometimes split across adjacent literals, which C concatenates.
          val description =
              DESCRIPTION.find(body)?.groupValues?.get(1)?.let { raw ->
                LITERAL.findAll(raw).joinToString("") { it.groupValues[1] }
              }
          Ability(id, clean(name), clean(description.orEmpty()).ifEmpty { "--" })
        }
        .toList()
  }

  /** Ability symbol to numeric id, matching what the species records carry. */
  fun ids(expansionRoot: Path): Map<String, Int> {
    val header = Files.readString(expansionRoot.resolve("include/constants/abilities.h"))
    var next = 0
    val ids = linkedMapOf<String, Int>()
    DECLARATION.findAll(header).forEach { match ->
      val id = match.groupValues[2].trim().toIntOrNull() ?: next
      ids["ABILITY_" + match.groupValues[1]] = id
      next = id + 1
    }
    return ids
  }

  /** The decomp wraps text for the GBA screen with escape codes; the client lays out its own. */
  private val CONTROL = Regex("""\\+[nlp]""")
  private val WHITESPACE = Regex("\\s+")

  private fun clean(text: String): String =
      WHITESPACE.replace(CONTROL.replace(text, " ").replace("\\", ""), " ").trim()
}
