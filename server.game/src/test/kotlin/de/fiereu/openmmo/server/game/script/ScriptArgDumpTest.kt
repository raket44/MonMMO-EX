package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec

class ScriptArgDumpTest :
    FunSpec({
      test("dump unsupported argument instances") {
        val analyzer = ScriptSupportAnalyzer()
        InterpretedScripts.sources.forEach { registration ->
          val corpus = registration.corpus
          val withIds = registration.scriptsByLabel.values.count { it.program.objectIds.isNotEmpty() }
          println("DUMP ${corpus.source} programsWithObjectIds=$withIds of ${registration.scriptsByLabel.size}")
          listOf("VermilionCity_EventScript_CheckTicketRight", "VermilionCity_EventScript_CheckTicket", "VermilionCity_EventScript_FerrySailor", "PalletTown_ProfessorOaksLab_EventScript_Oak").forEach { l ->
            val s = registration.scriptsByLabel[l]
            println("DUMP ${corpus.source} $l objectIds=${s?.program?.objectIds?.keys?.take(6)} lib=${s?.programLibrary?.get("VermilionCity_EventScript_CheckTicket")?.objectIds?.size} src=${s?.program?.sourceFile}")
          }
          registration.scriptsByLabel["VermilionCity_EventScript_CheckTicketRight"]?.let { s ->
            s.programLibrary.filterKeys { it.startsWith("VermilionCity_EventScript_") || it.startsWith("Common_") }.forEach { (k, p) -> println("DUMP LIB $k ids=${p.objectIds.size} src=${p.sourceFile}") }
          }
          (corpus.interactableLabels + corpus.mapEntryLabels).forEach { label ->
            val script = registration.scriptsByLabel[label] ?: return@forEach
            val support = analyzer.analyze(script)
            val reason = support.reason.orEmpty()
            if (reason.startsWith("unsupported argument") || reason.startsWith("unresolved object"))
                println("DUMP ${corpus.source} $label :: $reason")
          }
        }
      }
    })
