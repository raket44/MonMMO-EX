@file:JvmName("TutorLearnsetsMain")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.nio.file.Files
import java.nio.file.Path

/**
 * Writes the game server's move tutor compatibility for Expansion species
 * (`tutor-learnsets-expansion.csv`: server species id, then the tutor moves it can be taught).
 *
 * The retail tables come from each ROM's tutor_learnsets.h and are keyed by National Dex number,
 * which no Expansion species carries on the server. Their compatibility is the Expansion's own
 * taught list instead - the same porymoves union and family walk that fill the client dex's tools
 * tab - cut down to the moves the FRLG and Emerald tutors actually teach.
 *
 * Usage: <expansion-root> <output.csv>
 */
fun main(args: Array<String>) {
  require(args.size >= 2) { "Usage: <expansion-root> <output.csv>" }
  val expansionRoot = Path.of(args[0])
  val output = Path.of(args[1])
  require(Files.isDirectory(expansionRoot)) { "Expansion root not found: $expansionRoot" }

  val taught = PorymovesLearnsets.parse(expansionRoot, MoveText.ids(expansionRoot)).taught
  val rows = TutorLearnsets.rows(ExpansionSpeciesRegistry().all(), taught)
  Files.createDirectories(output.parent)
  Files.write(output, listOf(TutorLearnsets.HEADER) + rows)
  println("[tutor-learnsets] ${rows.size} Expansion species -> $output")
}

object TutorLearnsets {
  const val HEADER =
      "# Move tutor compatibility for Expansion species from the Expansion's porymoves taught lists: server species id, then the tutor moves it can be taught."

  /**
   * Every move a ROM tutor teaches: pokefirered MOVETUTOR_* and pokeemerald TUTOR_MOVE_*, the same
   * lists as the server's MoveTutorService. Frenzy Plant, Blast Burn and Hydro Cannon are left out -
   * the server gates those on the Kanto starters' final evolutions alone.
   */
  val TUTOR_MOVES =
      setOf(5, 14, 25, 34, 38, 68, 69, 102, 118, 135, 138, 86, 153, 157, 164) +
          setOf(223, 205, 244, 173, 196, 203, 189, 8, 207, 214, 129, 111, 9, 7, 210)

  fun rows(catalog: List<ExpansionSpeciesDef>, taught: Map<String, List<Int>>): List<String> =
      catalog
          .sortedBy { it.serverId }
          .mapNotNull { entry ->
            val moves = familyLookup(taught, entry).filter { it in TUTOR_MOVES }.distinct().sorted()
            if (moves.isEmpty()) null else (listOf(entry.serverId) + moves).joinToString(",")
          }

  /**
   * Taught lists are keyed by family names (XERNEAS, RATTATA_ALOLA) while a species symbol may carry
   * form suffixes, so the name is walked back one underscore at a time - the symbol first, then its
   * base species - exactly as the client content staging resolves the tools tab.
   */
  internal fun familyLookup(table: Map<String, List<Int>>, entry: ExpansionSpeciesDef): List<Int> {
    val candidates =
        listOf(
            entry.symbol.removePrefix("SPECIES_"),
            entry.baseSpeciesStableId.substringAfter("SPECIES_"),
        )
    candidates.forEach { name ->
      var current = name
      while (current.isNotEmpty()) {
        table[current]?.let {
          return it
        }
        val cut = current.lastIndexOf('_')
        if (cut <= 0) break
        current = current.substring(0, cut)
      }
    }
    return emptyList()
  }
}
