package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec

/**
 * Not an assertion - a report. Prints, per source, how many DIRECT scripts (the ones players
 * actually trigger: NPC interactions and map entries) the interpreter fully supports, and a
 * histogram of what blocks the rest. This is the work list for making trainers speak, battle, and
 * nurses heal: every rejection reason at the top of the list is the next command to support.
 */
class ScriptCoverageReportTest :
    FunSpec({
      test("print interpreter coverage report") {
        val analyzer = ScriptSupportAnalyzer()
        InterpretedScripts.sources.forEach { registration ->
          val corpus = registration.corpus
          val direct = corpus.interactableLabels + corpus.mapEntryLabels
          var complete = 0
          val reasons = mutableMapOf<String, Int>()
          direct.forEach { label ->
            val script = registration.scriptsByLabel[label] ?: return@forEach
            val support = analyzer.analyze(script)
            if (support.complete) complete++
            else {
              // Strip the per-script source line so identical causes aggregate, but keep the
              // command for the generic argument complaints - it is the actionable part.
              val raw = support.reason.orEmpty()
              val stripped = raw.substringBefore(" from `").trim()
              val reason =
                  if (stripped.startsWith("unsupported argument"))
                      "$stripped: ${raw.substringAfter(" from `", "").substringBefore(' ').trim('`')}"
                  else stripped
              reasons.merge(reason, 1, Int::plus)
              // The source line behind a reason, when asked: -Dcoverage.show=<substring>.
              System.getProperty("coverage.show")?.takeIf { it in raw }?.let { println("SOURCE $label: $raw") }
            }
          }
          println("=== ${corpus.source}: direct=${direct.size} complete=$complete ===")
          reasons.entries
              .sortedByDescending { it.value }
              .take(30)
              .forEach { println("${it.value}\t${it.key}") }
        }
      }
    })
