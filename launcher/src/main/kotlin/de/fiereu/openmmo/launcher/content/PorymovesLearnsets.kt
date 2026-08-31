package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Taught, egg and pre-evolution move lists from the Expansion's own per-game dumps.
 *
 * The previous source was a single `all_learnables.json` that lumped every way of learning a move
 * into one list, which put Growl - a pre-evolution level-up move - into Sylveon's tools tab as a
 * nameless TM. The `porymoves` files the Expansion generates its learnsets from keep the methods
 * apart per game, so each list can land in the category the client actually means:
 * - `TMMoves` and `TutorMoves` are things a tool teaches - the tools tab.
 * - `EggMoves` are the egg tab.
 * - `PreEvoMoves` are exactly the "evolved too early" set the tutor tab exists for.
 *
 * Lists are unioned across every game dump, matching the goal of the most complete data available.
 */
object PorymovesLearnsets {

  data class Learnsets(
      val taught: Map<String, List<Int>>,
      val egg: Map<String, List<Int>>,
      val prevo: Map<String, List<Int>>,
  )

  fun parse(expansionRoot: Path, moveIds: Map<String, Int>): Learnsets {
    val directory = expansionRoot.resolve("tools/learnset_helpers/porymoves_files")
    val taught = mutableMapOf<String, MutableSet<Int>>()
    val egg = mutableMapOf<String, MutableSet<Int>>()
    val prevo = mutableMapOf<String, MutableSet<Int>>()
    Files.list(directory).use { files ->
      files
          .filter { it.name.endsWith(".json") }
          .sorted()
          .forEach { file ->
            val root = Json.parseToJsonElement(Files.readString(file)).jsonObject
            root.forEach { (species, entry) ->
              val lists = entry.jsonObject
              collect(lists, "TMMoves", moveIds, taught.getOrPut(species, ::mutableSetOf))
              collect(lists, "TutorMoves", moveIds, taught.getOrPut(species, ::mutableSetOf))
              collect(lists, "EggMoves", moveIds, egg.getOrPut(species, ::mutableSetOf))
              collect(lists, "PreEvoMoves", moveIds, prevo.getOrPut(species, ::mutableSetOf))
            }
          }
    }
    fun finish(map: Map<String, MutableSet<Int>>) =
        map.filterValues { it.isNotEmpty() }.mapValues { (_, moves) -> moves.sorted() }
    return Learnsets(finish(taught), finish(egg), finish(prevo))
  }

  private fun collect(
      entry: JsonObject,
      key: String,
      moveIds: Map<String, Int>,
      into: MutableSet<Int>,
  ) {
    val list = entry[key] ?: return
    list.jsonArray.forEach { element ->
      val symbol = element.jsonPrimitive.content
      moveIds[symbol]?.let(into::add)
    }
  }
}
