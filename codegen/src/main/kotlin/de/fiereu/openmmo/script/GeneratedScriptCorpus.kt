package de.fiereu.openmmo.script

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

data class ScriptCorpusDiagnostics(
    val indexedLabels: Int,
    val generatedPrograms: Int,
    val unavailableDirectLabels: Set<String>,
    val parseFailureCategories: Map<String, Int>,
    val parseFailureSamples: Map<String, List<String>>,
)

data class ScriptSourceCorpus(
    val storyNamespace: String,
    val source: String,
    val gameCode: String,
    val programs: Map<String, ScriptProgram>,
    val movements: Map<String, MovementProgram>,
    val textIds: Map<String, Int>,
    val interactableLabels: Set<String>,
    val mapEntryLabels: Set<String>,
    val diagnostics: ScriptCorpusDiagnostics,
)

object GeneratedScriptCorpus {
  val sources: List<ScriptSourceCorpus> by lazy {
    ScriptCorpusDecoder.decode(GeneratedScriptCorpusData.chunks.joinToString(separator = ""))
  }
}

private object ScriptCorpusDecoder {
  private const val FORMAT_VERSION = 3

  fun decode(encoded: String): List<ScriptSourceCorpus> {
    val bytes = Base64.getDecoder().decode(encoded)
    DataInputStream(GZIPInputStream(ByteArrayInputStream(bytes))).use { input ->
      check(input.readInt() == FORMAT_VERSION) { "Unsupported generated script corpus format" }
      return List(input.readInt()) {
        val storyNamespace = input.readUTF()
        val source = input.readUTF()
        val gameCode = input.readUTF()
        val indexed = input.readInt()
        val failureCategories = input.readStringIntMap()
        val failureSamples = input.readStringListMap()
        val interactable = input.readStringSet()
        val mapEntries = input.readStringSet()
        val unavailableDirectLabels = input.readStringSet()
        val textIds = input.readStringIntMap()
        val constants = input.readStringIntMap()

        val programCount = input.readInt()
        val programs =
            LinkedHashMap<String, ScriptProgram>(programCount).apply {
              repeat(programCount) {
                val label = input.readUTF()
                val sourceFile = input.readUTF()
                val commands = input.readStringList()
                val objectIds = input.readStringIntMap()
                val program =
                    PretScriptParser.parse(
                        id = ScriptId("gba", source, gameCode, label),
                        storyNamespace = storyNamespace,
                        sourceFile = sourceFile,
                        lines = listOf("$label::") + commands,
                        objectIds = objectIds,
                        constants = constants,
                    )
                put(label, program)
              }
            }

        val movementCount = input.readInt()
        val movements =
            LinkedHashMap<String, MovementProgram>(movementCount).apply {
              repeat(movementCount) {
                val label = input.readUTF()
                val sourceFile = input.readUTF()
                val actions = input.readStringList()
                put(
                    label,
                    PretMovementParser.parse(
                        ScriptId("gba", source, gameCode, label),
                        sourceFile,
                        listOf("$label:") + actions,
                    ),
                )
              }
            }

        ScriptSourceCorpus(
            storyNamespace = storyNamespace,
            source = source,
            gameCode = gameCode,
            programs = programs,
            movements = movements,
            textIds = textIds,
            interactableLabels = interactable,
            mapEntryLabels = mapEntries,
            diagnostics =
                ScriptCorpusDiagnostics(
                    indexedLabels = indexed,
                    generatedPrograms = programs.size,
                    unavailableDirectLabels = unavailableDirectLabels,
                    parseFailureCategories = failureCategories,
                    parseFailureSamples = failureSamples,
                ),
        )
      }
    }
  }

  private fun DataInputStream.readStringList(): List<String> = List(readInt()) { readUTF() }

  private fun DataInputStream.readStringSet(): Set<String> =
      LinkedHashSet<String>().apply { repeat(readInt()) { add(readUTF()) } }

  private fun DataInputStream.readStringIntMap(): Map<String, Int> =
      LinkedHashMap<String, Int>().apply { repeat(readInt()) { put(readUTF(), readInt()) } }

  private fun DataInputStream.readStringListMap(): Map<String, List<String>> =
      LinkedHashMap<String, List<String>>().apply {
        repeat(readInt()) { put(readUTF(), readStringList()) }
      }
}
