package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * The Expansion type chart, read from src/data/types_info.h rather than restated here, so the
 * matchups the client uses are the ones the source tree actually configures.
 *
 * The table is written attacker-major with a defender column per type. Cells are either the
 * `______` shorthand for neutral or `X(n)`; a handful are named macros whose value depends on the
 * configured matchup generation, and those are resolved from the same header.
 */
object TypeChart {
  /** Expansion's own order, which the table's columns follow. */
  val EXPANSION_TYPES =
      listOf(
          "NONE",
          "NORMAL",
          "FIGHTING",
          "FLYING",
          "POISON",
          "GROUND",
          "ROCK",
          "BUG",
          "GHOST",
          "STEEL",
          "MYSTERY",
          "FIRE",
          "WATER",
          "GRASS",
          "ELECTRIC",
          "PSYCHIC",
          "ICE",
          "DRAGON",
          "DARK",
          "FAIRY",
          "STELLAR")

  private val ROW = Regex("""\[TYPE_([A-Z_]+)]\s*=\s*\{([^}]*)}""")
  private val MACRO =
      Regex(
          """#define\s+([A-Z_]+)\s+\(B_UPDATED_TYPE_MATCHUPS\s*>=\s*GEN_(\d+)\s*\?\s*X\(([0-9.]+)\)\s*:\s*X\(([0-9.]+)\)\)""")
  private val GEN = Regex("""#define\s+B_UPDATED_TYPE_MATCHUPS\s+GEN_(\d+)""")

  fun parse(expansionRoot: Path): Map<String, Map<String, Double>> {
    val text = Files.readString(expansionRoot.resolve("src/data/types_info.h"))
    val configuredGen = configuredGeneration(expansionRoot)
    val macros =
        MACRO.findAll(text).associate { match ->
          val (name, gen, whenNewer, whenOlder) = match.destructured
          name to if (configuredGen >= gen.toInt()) whenNewer.toDouble() else whenOlder.toDouble()
        }

    val table = text.substringAfter("gTypeEffectivenessTable").substringBefore("\n};")
    return ROW.findAll(table).associate { row ->
      val attacker = row.groupValues[1]
      val cells = row.groupValues[2].split(",").map(String::trim).filter(String::isNotEmpty)
      require(cells.size == EXPANSION_TYPES.size) {
        "TYPE_$attacker has ${cells.size} columns, expected ${EXPANSION_TYPES.size}"
      }
      attacker to EXPANSION_TYPES.zip(cells.map { cell(it, macros) }).toMap()
    }
  }

  private fun cell(token: String, macros: Map<String, Double>): Double =
      when {
        token == "______" -> 1.0
        token.startsWith("X(") -> token.removePrefix("X(").removeSuffix(")").toDouble()
        else -> macros[token] ?: error("Unknown type chart cell: $token")
      }

  /** Defaults to the modern chart, which is what the header itself falls back to. */
  private fun configuredGeneration(root: Path): Int {
    val config = root.resolve("include/config/battle.h")
    if (!Files.isRegularFile(config)) return LATEST_GENERATION
    return GEN.find(Files.readString(config))?.groupValues?.get(1)?.toInt() ?: LATEST_GENERATION
  }

  private const val LATEST_GENERATION = 9
}
