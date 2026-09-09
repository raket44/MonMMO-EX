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
    /** The game's scripted menus (MULTICHOICE_*, MULTI_*, LISTMENU_*) as option texts. */
    val menus: Map<String, List<String>> = emptyMap(),
)

/** [dsMenuEntries]: the client's DS menu-entry bank (Platinum bank 361), option text -> entry. */
data class DecodedScriptCorpus(val sources: List<ScriptSourceCorpus>, val dsMenuEntries: Map<String, Int>)

object GeneratedScriptCorpus {
  private val decoded: DecodedScriptCorpus by lazy {
    ScriptCorpusDecoder.decode(GeneratedScriptCorpusData.chunks.joinToString(separator = ""))
  }
  val sources: List<ScriptSourceCorpus>
    get() = decoded.sources
  val dsMenuEntries: Map<String, Int>
    get() = decoded.dsMenuEntries
}

private object ScriptCorpusDecoder {
  private const val FORMAT_VERSION = 4

  fun decode(encoded: String): DecodedScriptCorpus {
    val bytes = Base64.getDecoder().decode(encoded)
    DataInputStream(GZIPInputStream(ByteArrayInputStream(bytes))).use { input ->
      check(input.readInt() == FORMAT_VERSION) { "Unsupported generated script corpus format" }
      val dsMenuEntries = input.readStringIntMap()
      val sources = List(input.readInt()) {
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
        val menus = input.readStringListMap()

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
            menus = menus,
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
      return DecodedScriptCorpus(sources, dsMenuEntries)
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
