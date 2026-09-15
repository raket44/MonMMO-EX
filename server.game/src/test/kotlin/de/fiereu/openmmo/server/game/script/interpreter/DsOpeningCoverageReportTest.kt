package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.openmmo.script.LabelArg
import de.fiereu.openmmo.script.ScriptProgram
import io.kotest.core.spec.style.FunSpec

/**
 * Not an assertion - a report. For each DS region's opening route (the maps a new game walks
 * through to its first real stretch), every script bound to those map headers (npcs, step
 * triggers, map entry) is followed through its goto/call targets, and EVERY command the interpreter
 * does not execute is listed with how often it appears and on which maps. The coverage report only
 * names each script's first blocker; this is the complete work list for getting a story started.
 */
class DsOpeningCoverageReportTest :
    FunSpec({
      /** Map header ids (DS "bank;map" = map * 256 + bank) of each opening, from the decomps / map directory. */
      val openings =
          mapOf(
              // Platinum: Twinleaf Town and its houses, Route 201, Verity Lakefront, Lake Verity, Sandgem
              // Town (lab, rival's house, Pokemon Center, mart), Route 202, Jubilife City.
              "platinum" to listOf(411, 412, 413, 414, 415, 416, 417, 342, 334, 311, 312, 418, 419, 420, 421, 422, 423, 424, 425, 343, 3, 4, 6),
              // HeartGold: New Bark Town (Elm's lab, the player's and rival's houses), Routes 29-32,
              // Cherrygrove City, Mr. Pokemon's house, Violet City and Sprout Tower.
              "heartgold" to listOf(60, 61, 62, 63, 64, 65, 66, 384, 33, 34, 35, 36, 67, 68, 69, 70, 71, 72, 143, 127, 73, 110, 155, 156),
              // White: Nuvema Town maps, Route 1, Accumula Town, Route 2, Striaton City, Dreamyard.
              "white" to (133..140).map { (1 shl 8) or it } + listOf((1 shl 8) or 61, (1 shl 8) or 63, (1 shl 8) or 64) +
                  (141..149).map { (1 shl 8) or it } + (6..15).toList() + listOf(152, 153),
          )

      test("print every unsupported command on the DS openings") {
        val analyzer = ScriptSupportAnalyzer()
        for ((source, headers) in openings) {
          val registration = InterpretedScripts.sources.firstOrNull { it.corpus.source == source } ?: continue
          val library = registration.corpus.programs
          val counts = sortedMapOf<String, Int>()
          val maps = sortedMapOf<String, MutableSet<Int>>()
          var scripts = 0
          var complete = 0
          val blockers = mutableListOf<String>()
          for (header in headers) {
            val roots = library.keys.filter { it.startsWith("NDS_${header}_") || it.startsWith("NDS_INIT_${header}_") }
            for (root in roots) {
              scripts++
              registration.scriptsByLabel[root]?.let {
                val support = analyzer.analyze(it)
                if (support.complete) complete++ else blockers += "$root: ${support.reason}"
              }
              val seen = HashSet<String>()
              val queue = ArrayDeque(listOf(root))
              while (queue.isNotEmpty()) {
                val program: ScriptProgram = library[queue.removeFirst()] ?: continue
                if (!seen.add(program.id.label)) continue
                for (instruction in program.instructions) {
                  if (!analyzer.supportsCommand(instruction.command)) {
                    counts.merge(instruction.command, 1, Int::plus)
                    maps.getOrPut(instruction.command) { sortedSetOf() } += header
                  }
                  instruction.args.filterIsInstance<LabelArg>().forEach { if (it.token in library) queue += it.token }
                }
              }
            }
          }
          println("=== $source opening: maps=${headers.size} scripts=$scripts complete=$complete missing commands=${counts.size} ===")
          counts.entries.sortedByDescending { it.value }.forEach { (command, n) ->
            println("$n\t$command\tmaps=${maps[command]}")
          }
          blockers.forEach { println("BLOCKED $it") }
        }
      }
    })
