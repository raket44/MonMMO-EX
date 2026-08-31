package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.script.GeneratedScriptCorpus
import de.fiereu.openmmo.script.ScriptSourceCorpus
import de.fiereu.openmmo.server.game.script.Script

internal data class InterpretedSourceRegistration(
    val corpus: ScriptSourceCorpus,
    val scriptsByLabel: Map<String, InterpretedScript>,
)

internal object InterpretedScripts {
  val sources: List<InterpretedSourceRegistration> =
      GeneratedScriptCorpus.sources.map { corpus ->
        val text = corpus.textIds.mapValues { CorpusDialogLine(it.value) }
        val scripts =
            corpus.programs.mapValues { (_, program) ->
              InterpretedScript(
                  program = program,
                  textBindings = text,
                  movementPrograms = corpus.movements,
                  programLibrary = corpus.programs,
              )
            }
        InterpretedSourceRegistration(corpus, scripts)
      }

  val byId: Map<String, Script> =
      sources.flatMap { it.scriptsByLabel.values }.associateBy { it.program.id.stable }

  val candidatesByBareLabel: Map<String, List<InterpretedScript>> =
      sources.flatMap { it.scriptsByLabel.values }.groupBy { it.program.id.label }

  /** Compatibility aliases exist only where a label belongs to one game source. */
  val byBareLabel: Map<String, Script> =
      candidatesByBareLabel
          .mapNotNull { (label, candidates) -> candidates.singleOrNull()?.let { label to it } }
          .toMap()

  private data class CorpusDialogLine(override val textId: Int) : DialogLine
}
