package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.script.generated.GeneratedScripts
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

internal fun gbaScriptSource(regionId: Int): String? =
    when (Region.byId(regionId)) {
      Region.KANTO -> "firered"
      Region.HOENN -> "emerald"
      Region.SINNOH -> "platinum"
      Region.JOHTO -> "heartgold"
      Region.UNOVA -> "white"
      else -> null
    }

class ScriptResolutionException(message: String) : IllegalStateException(message)

/**
 * The decomp writes object events with no script as literal `0x0`; the map generator passes it
 * through. It is a first-class "this npc has nothing to say", not a resolution failure.
 */
internal fun isNoScript(label: String): Boolean =
    label.isEmpty() || label == "0x0" || label == "0" || label == "NULL"

/** Resolves a decomp label through developer forcing, support analysis, and Kotlin fallback. */
class ScriptRegistry(
    private val byLabel: Map<String, Script>,
    private val interpretedById: Map<String, Script> = emptyMap(),
    private val interpretedByBareLabel: Map<String, Script> = emptyMap(),
    private val developerTools: DeveloperTools? = null,
    private val supportAnalyzer: ScriptSupportAnalyzer = ScriptSupportAnalyzer(),
    private val interpretedCandidates: Map<String, List<InterpretedScript>> = emptyMap(),
) {
  fun forLabel(scriptLabel: String, source: String? = null): Script? {
    if (isNoScript(scriptLabel)) return null
    val candidates = interpretedCandidates[scriptLabel].orEmpty()
    val interpreted =
        if (source == null) {
          interpretedByBareLabel[scriptLabel] ?: candidates.singleOrNull()
        } else {
          candidates.singleOrNull { it.program.id.source == source }
        }
    val kotlin = byLabel[scriptLabel]
    val scriptId =
        (interpreted as? InterpretedScript)?.program?.id?.stable
            ?: candidates.singleOrNull()?.program?.id?.stable
            ?: scriptLabel

    val interpretedOverride = developerTools?.matchingOverride(scriptLabel)
    if (interpretedOverride != null) {
      val forced =
          if (interpretedOverride == scriptLabel) interpreted
          else interpretedById[interpretedOverride]
      if (forced != null) {
        log.info { "[ScriptResolver] INTERPRETER $scriptId" }
        log.warn {
          "[Developer Override] Using interpreted $interpretedOverride for script label $scriptLabel"
        }
        return forced
      }
      throw ScriptResolutionException(
          "No interpreted script resolves for forced override $interpretedOverride " +
              "(label $scriptLabel)")
    }

    val kotlinOverride = developerTools?.matchingKotlinOverride(scriptLabel)
    if (kotlinOverride != null) {
      if (kotlin != null) {
        log.info { "[ScriptResolver] KOTLIN_FALLBACK $scriptId reason=developer force-Kotlin" }
        return kotlin
      }
      throw ScriptResolutionException(
          "No Kotlin script resolves for forced override $kotlinOverride (label $scriptLabel)")
    }

    val support =
        if (interpreted is InterpretedScript) supportAnalyzer.analyze(interpreted) else null
    if (support?.complete == true) {
      log.info { "[ScriptResolver] INTERPRETER $scriptId" }
      developerTools?.trace { "Selected complete interpreted script $scriptId" }
      return interpreted
    }

    val reason =
        when {
          interpreted == null && candidates.size > 1 ->
              "ambiguous interpreted label across sources: " +
                  candidates.joinToString { it.program.id.stable }
          interpreted == null -> "no interpreted program registered"
          support == null -> "interpreted entry has no structured support metadata"
          else -> checkNotNull(support.reason)
        }
    if (kotlin != null) {
      log.info { "[ScriptResolver] KOTLIN_FALLBACK $scriptId reason=$reason" }
      developerTools?.trace { "Selected Kotlin fallback $scriptId: $reason" }
      return kotlin
    }

    throw ScriptResolutionException(
        "No usable script for $scriptId: interpreter=$reason; Kotlin script is not registered")
  }

  fun forId(scriptId: String): Script? = interpretedById[scriptId]

  /**
   * The first trainer constant a label's interpreted program battles, or null when the label has no
   * interpreted program or no trainerbattle. The line-of-sight engine uses it to check the defeated
   * flag BEFORE approaching - a beaten trainer must neither approach nor auto-talk.
   */
  fun trainerConstant(scriptLabel: String, source: String? = null): String? {
    val candidates = interpretedCandidates[scriptLabel].orEmpty()
    val interpreted =
        if (source == null) {
          interpretedByBareLabel[scriptLabel] ?: candidates.singleOrNull()
        } else {
          candidates.singleOrNull { it.program.id.source == source }
        }
    val program = (interpreted as? InterpretedScript)?.program ?: return null
    return program.instructions.firstNotNullOfOrNull { instruction ->
      if (!instruction.command.startsWith("trainerbattle")) null
      else instruction.args.firstOrNull()?.token
    }
  }

  fun hasInterpreted(scriptIdOrLabel: String): Boolean =
      if (':' in scriptIdOrLabel) interpretedById.containsKey(scriptIdOrLabel)
      else
          interpretedByBareLabel.containsKey(scriptIdOrLabel) ||
              interpretedCandidates.containsKey(scriptIdOrLabel)

  private fun logCorpusSummary() {
    InterpretedScripts.sources.forEach { registration ->
      val corpus = registration.corpus
      val support =
          registration.scriptsByLabel.mapValues { (_, script) -> supportAnalyzer.analyze(script) }
      val fullySupported = support.count { it.value.complete }
      val unsupported = support.size - fullySupported
      val interactable = directCoverage(corpus.interactableLabels, support)
      val mapEntry = directCoverage(corpus.mapEntryLabels, support)
      val direct = directCoverage(corpus.interactableLabels + corpus.mapEntryLabels, support)
      val display = if (corpus.source == "firered") "FireRed" else "Emerald"
      log.info {
        "[ScriptCorpus] $display indexed=${corpus.diagnostics.indexedLabels} " +
            "programs=${corpus.programs.size} parseFailures=" +
            "${corpus.diagnostics.parseFailureCategories.values.sum()} " +
            "unavailableDirect=${corpus.diagnostics.unavailableDirectLabels.size} " +
            "fullySupported=$fullySupported unsupported=$unsupported " +
            "direct=${direct.summary} interactable=${interactable.summary} " +
            "mapEntry=${mapEntry.summary}"
      }
      if (corpus.diagnostics.parseFailureCategories.isNotEmpty()) {
        log.warn {
          "[ScriptCorpus] $display parse failures: " +
              corpus.diagnostics.parseFailureCategories.toSortedMap()
        }
      }
    }
  }

  private fun directCoverage(
      labels: Set<String>,
      support: Map<String, de.fiereu.openmmo.server.game.script.interpreter.ScriptSupport>,
  ): DirectCoverage {
    var complete = 0
    var kotlinFallback = 0
    var unavailable = 0
    labels.forEach { label ->
      val result = support[label]
      when {
        result?.complete == true -> complete++
        label in byLabel -> kotlinFallback++
        else -> unavailable++
      }
    }
    return DirectCoverage(labels.size, complete, kotlinFallback, unavailable)
  }

  private data class DirectCoverage(
      val total: Int,
      val complete: Int,
      val kotlinFallback: Int,
      val unavailable: Int,
  ) {
    val summary: String
      get() = "$complete/$total complete, $kotlinFallback Kotlin, $unavailable unavailable"
  }

  companion object {
    fun generated(
        developerTools: DeveloperTools? = null,
        supportAnalyzer: ScriptSupportAnalyzer = ScriptSupportAnalyzer(),
    ): ScriptRegistry {
      val registry =
          ScriptRegistry(
              GeneratedScripts.byLabel,
              InterpretedScripts.byId,
              InterpretedScripts.byBareLabel,
              developerTools,
              supportAnalyzer,
              InterpretedScripts.candidatesByBareLabel,
          )
      registry.logCorpusSummary()
      return registry
    }
  }
}
