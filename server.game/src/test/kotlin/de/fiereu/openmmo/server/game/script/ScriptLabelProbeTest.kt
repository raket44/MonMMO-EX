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
                "OldaleTown_Mart_EventScript_Clerk",
                "SlateportCity_Mart_EventScript_Clerk",
                "ViridianCity_Mart_EventScript_Clerk",
                "CeladonCity_DepartmentStore_2F_EventScript_Clerk1",
                "PewterCity_Gym_EventScript_Brock",
                "CeruleanCity_Gym_EventScript_Misty",
                "VermilionCity_Gym_EventScript_LtSurge",
                "CeladonCity_Gym_EventScript_Erika",
                "CeladonCity_GameCorner_OnLoad",
                "RocketHideout_Elevator_EventScript_FloorSelect",
                "CeladonCity_DepartmentStore_Elevator_EventScript_FloorSelect",
                "SilphCo_Elevator_EventScript_FloorSelect",
                "PewterCity_Gym_EventScript_JrTrainer",
                "Route2_EastBuilding_EventScript_Aide",
                "EventScript_FldEffFlash",
                "UTR_0",
                "NDS_CHUNK_3001",
                "NDS_CHUNK_3002",
                "NDS_CHUNK_3043",
            )
        val analyzer = ScriptSupportAnalyzer()
        println("[probe] DS menu-entry bank: ${de.fiereu.openmmo.script.GeneratedScriptCorpus.dsMenuEntries.size} labels")
        InterpretedScripts.sources.forEach { registration ->
          println("[probe] ${registration.corpus.source} menus=${registration.corpus.menus.size} dynamicExits=${registration.corpus.dynamicExits.size} headerFloors=${registration.corpus.headerFloors.size}")
          registration.scriptsByLabel.values
              .filter { it.program.id.label.contains("Elevator", ignoreCase = true) || it.program.sourceFile.contains("T07R0206") || it.program.sourceFile.contains("T25R1007") || it.program.sourceFile.contains("veilstone_store_elevator") }
              .forEach { script ->
                val support = analyzer.analyze(script)
                println("[probe] ${registration.corpus.source} ${script.program.id.label} (${script.program.sourceFile.substringAfterLast('/')}) complete=${support.complete} reason=${support.reason}")
                script.program.instructions.filter { it.command.startsWith("ds_menu") || it.command.startsWith("ds_setdynamicwarp") || it.command.startsWith("ds_dynamicwarpfloor") || it.command == "message" }.forEach { println("[probe]     ${it.sourceLine}") }
              }
          listOf("MULTICHOICE_ROCKET_HIDEOUT_ELEVATOR", "MULTICHOICE_DEPT_STORE_ELEVATOR", "LISTMENU_SILPHCO_FLOORS", "LISTMENU#1", "MULTI_PC", "MULTICHOICE_YES_NO").forEach { menu ->
            val options = registration.corpus.menus[menu] ?: return@forEach
            println("[probe] ${registration.corpus.source} $menu options=$options ds=${de.fiereu.openmmo.server.game.script.interpreter.InterpreterSupport.dsTextList(options)}")
          }
          labels.forEach { label ->
            val script = registration.scriptsByLabel[label] ?: return@forEach
            val support = analyzer.analyze(script)
            println(
                "[probe] ${registration.corpus.source} $label complete=${support.complete} " +
                    "reason=${support.reason}")
            if (label.startsWith("UTR_") || label.startsWith("NDS_CHUNK_30"))
                script.program.instructions.forEach { println("[probe]     ${it.sourceLine}") }
          }
        }
      }
    })
