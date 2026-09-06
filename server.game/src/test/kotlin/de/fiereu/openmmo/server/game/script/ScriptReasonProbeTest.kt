package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec

/** Prints why the story-critical labels below are (or are not) interpreter-complete. */
class ScriptReasonProbeTest :
    FunSpec({
      test("gift monster scripts") {
        val analyzer = ScriptSupportAnalyzer()
        val gifts = setOf("givemon", "giveegg")
        InterpretedScripts.sources.forEach { reg ->
          reg.scriptsByLabel.forEach { (label, s) ->
            if (s.program.instructions.none { it.command in gifts }) return@forEach
            println("GIFT ${reg.corpus.source} $label -> ${analyzer.analyze(s).reason ?: "COMPLETE"}")
          }
        }
      }

      test("map sweep") {
        val prefix = System.getenv("MONMMO_PROBE_MAP") ?: return@test
        val analyzer = ScriptSupportAnalyzer()
        InterpretedScripts.sources.forEach { reg ->
          reg.scriptsByLabel.forEach { (label, s) ->
            if (!label.startsWith(prefix)) return@forEach
            println("MAP ${reg.corpus.source} $label -> ${analyzer.analyze(s).reason ?: "COMPLETE"}")
          }
        }
      }

      test("story labels") {
        val analyzer = ScriptSupportAnalyzer()
        val labels =
            listOf(
                "VermilionCity_EventScript_FerrySailor",
                "VermilionCity_EventScript_CheckTicketRight",
                "Route25_SeaCottage_EventScript_Bill",
                "Route25_SeaCottage_EventScript_Computer",
                "Route25_SeaCottage_EventScript_BillPokemon",
                "SSAnne_2F_Corridor_EventScript_Rival",
                "SSAnne_CaptainsOffice_EventScript_Captain",
                "PalletTown_ProfessorOaksLab_EventScript_Oak",
                "Route22_EventScript_RivalTrigger",
                "CeruleanCity_EventScript_Rival",
                "CeladonCity_Condominiums_RoofRoom_EventScript_EeveeBall",
                "SilphCo_7F_EventScript_LaprasGuy",
            )
        InterpretedScripts.sources.forEach { reg ->
          labels.forEach { label ->
            val s = reg.scriptsByLabel[label] ?: return@forEach
            println("PROBE ${reg.corpus.source} $label -> ${analyzer.analyze(s).reason ?: "COMPLETE"}")
          }
        }
      }
    })
