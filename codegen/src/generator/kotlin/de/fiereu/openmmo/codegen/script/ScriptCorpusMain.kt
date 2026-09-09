@file:JvmName("ScriptCorpusMain")

package de.fiereu.openmmo.codegen.script

import de.fiereu.openmmo.codegen.script.nds.NdsScriptCorpusGenerator

import java.io.File

fun main(args: Array<String>) {
  require(args.size >= 5) {
    "Usage: <output> <templates> <cache> <dialog-data> " +
        "<namespace|source|gameCode|decomp>... got ${args.toList()}"
  }
  val outputDir = File(args[0])
  val dialogDataDir = File(args[3])
  val generator = ScriptCorpusGenerator(dialogDataDir)
  val corpora =
      args.drop(4).map { encoded ->
        val parts = encoded.split('|', limit = 4)
        require(parts.size == 4) { "Invalid script corpus spec $encoded" }
        buildCorpus(
            generator,
            ScriptCorpusSpec(
                storyNamespace = parts[0],
                source = parts[1],
                gameCode = parts[2],
                decompDir = File(parts[3]),
            ))
      }

  corpora.forEach { corpus ->
    val display = corpus.spec.source.replaceFirstChar { it.uppercase() }
    println(
        "[script-corpus] $display: indexed=${corpus.indexedLabels}, " +
            "programs=${corpus.programs.size}, parseFailures=" +
            "${corpus.parseFailureCategories.values.sum()}, " +
            "unavailableDirect=${corpus.unavailableDirectLabels.size}, " +
            "interactable=${corpus.interactableLabels.size}, " +
            "mapEntry=${corpus.mapEntryLabels.size}, movements=${corpus.movements.size}")
    if (corpus.parseFailureCategories.isNotEmpty()) {
      println("[script-corpus] $display parse failures: ${corpus.parseFailureCategories}")
    }
  }

  // The text-button dialog draws its labels from Platinum's menu-entry bank (client bank 361).
  val dsMenuEntries =
      corpora.firstOrNull { it.spec.source == "platinum" }?.spec?.decompDir?.let {
        MenuIndex.dsMenuEntries(File(it, "res/text/menu_entries.json"))
      } ?: emptyMap()
  println("[script-corpus] DS menu-entry bank: ${dsMenuEntries.size} labels")
  val encoded = ScriptCorpusBinary.encode(corpora, dsMenuEntries)
  val chunks = encoded.chunked(12_000)
  val target =
      File(
          outputDir,
          "de/fiereu/openmmo/script/GeneratedScriptCorpusData.kt",
      )
  target.parentFile.mkdirs()
  target.writeText(
      buildString {
        appendLine("package de.fiereu.openmmo.script")
        appendLine()
        appendLine("internal object GeneratedScriptCorpusData {")
        appendLine("  val chunks: Array<String> =")
        appendLine("      arrayOf(")
        chunks.forEach { appendLine("          \"$it\",") }
        appendLine("      )")
        appendLine("}")
      })
}

private fun buildCorpus(generator: ScriptCorpusGenerator, spec: ScriptCorpusSpec): BuiltScriptCorpus =
    if (spec.source in setOf("platinum", "heartgold", "white")) NdsScriptCorpusGenerator().build(spec)
    else generator.build(spec)
