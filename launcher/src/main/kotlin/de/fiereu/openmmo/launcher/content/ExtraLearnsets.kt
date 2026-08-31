package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * The learnsets that are not level-up: what a species can be taught, and what it inherits as an egg
 * move. The client keeps these in their own categorised section, separate from the level-up list,
 * so a species with only a level-up learnset is still missing most of its moveset.
 *
 * Patterns are raw strings and never match a quote character; the learnables file has its quotes
 * stripped first, which keeps every pattern free of escaping.
 */
object ExtraLearnsets {
  /** Category ids from the client's own enum, which names them outright. */
  const val EGG_MOVES = 0
  const val MOVE_LEARNER_TOOLS = 4

  private val SPECIES_BLOCK =
      Regex("""([A-Z0-9_]+)\s*:\s*\[([^\]]*)]""", RegexOption.DOT_MATCHES_ALL)
  private val MOVE = Regex("""MOVE_[A-Z0-9_]+""")
  private val EGG_ARRAY =
      Regex(
          """static\s+const\s+u16\s+(s[A-Za-z0-9_]+)\[]\s*=\s*\{([^}]*)}""",
          RegexOption.DOT_MATCHES_ALL)
  private val EGG_REFERENCE =
      Regex("""\[SPECIES_([A-Z0-9_]+)][\s\S]*?\.eggMoveLearnset\s*=\s*(s[A-Za-z0-9_]+)""")

  /** Species symbol without its prefix, to the moves it can be taught. */
  fun teachable(expansionRoot: Path, moveIds: Map<String, Int>): Map<String, List<Int>> {
    val json =
        Files.readString(expansionRoot.resolve("src/data/pokemon/all_learnables.json"))
            .replace("\"", "")
    return SPECIES_BLOCK.findAll(json).associate { match ->
      match.groupValues[1] to
          MOVE.findAll(match.groupValues[2]).mapNotNull { moveIds[it.value] }.toList()
    }
  }

  /** Species symbol without its prefix, to its egg moves. */
  fun eggMoves(expansionRoot: Path, moveIds: Map<String, Int>): Map<String, List<Int>> {
    val arrays =
        EGG_ARRAY.findAll(Files.readString(expansionRoot.resolve("src/data/pokemon/egg_moves.h")))
            .associate { match ->
              match.groupValues[1] to
                  MOVE.findAll(match.groupValues[2]).mapNotNull { moveIds[it.value] }.toList()
            }
    val references = linkedMapOf<String, List<Int>>()
    Files.list(expansionRoot.resolve("src/data/pokemon/species_info")).use { files ->
      files
          .filter { it.toString().endsWith(".h") }
          .forEach { file ->
            EGG_REFERENCE.findAll(Files.readString(file)).forEach { match ->
              arrays[match.groupValues[2]]?.let { references[match.groupValues[1]] = it }
            }
          }
    }
    return references
  }
}
