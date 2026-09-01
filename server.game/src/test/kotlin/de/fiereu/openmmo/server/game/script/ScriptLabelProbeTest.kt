package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec

/**
 * Not an assertion - a diagnostic. Prints the analyzer's verdict and full reason for a handful of
 * labels under investigation, so a "this npc does nothing" report can be pinned to the exact
 * unsupported command without booting the server.
 */
class ScriptLabelProbeTest :
    FunSpec({
      test("print analyzer verdicts for probe labels") {
        val labels =
            listOf(
                "PewterCity_Gym_EventScript_Brock",
                "CeruleanCity_Gym_EventScript_Misty",
                "VermilionCity_Gym_EventScript_LtSurge",
                "PewterCity_Gym_EventScript_JrTrainer",
            )
        val analyzer = ScriptSupportAnalyzer()
        InterpretedScripts.sources.forEach { registration ->
          labels.forEach { label ->
            val script = registration.scriptsByLabel[label] ?: return@forEach
            val support = analyzer.analyze(script)
            println(
                "[probe] ${registration.corpus.source} $label complete=${support.complete} " +
                    "reason=${support.reason}")
          }
        }
      }
    })
