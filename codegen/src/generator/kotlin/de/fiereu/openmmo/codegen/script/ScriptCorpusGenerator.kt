package de.fiereu.openmmo.codegen.script

import de.fiereu.openmmo.codegen.dialog.DialogTable
import de.fiereu.openmmo.script.PretMovementParser
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.script.TextArg
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Base64
import java.util.zip.GZIPOutputStream

data class ScriptCorpusSpec(
    val storyNamespace: String,
    val source: String,
    val gameCode: String,
    val decompDir: File,
)

data class ScriptCorpusProgramRecord(
    val label: String,
    val sourceFile: String,
    val commands: List<String>,
    val objectIds: Map<String, Int>,
)

data class ScriptCorpusMovementRecord(
    val label: String,
    val sourceFile: String,
    val actions: List<String>,
)

data class BuiltScriptCorpus(
    val spec: ScriptCorpusSpec,
    val indexedLabels: Int,
    val programs: List<ScriptCorpusProgramRecord>,
    val movements: List<ScriptCorpusMovementRecord>,
    val textIds: Map<String, Int>,
    val constants: Map<String, Int>,
    val interactableLabels: Set<String>,
    val mapEntryLabels: Set<String>,
    val unavailableDirectLabels: Set<String>,
    val parseFailureCategories: Map<String, Int>,
    val parseFailureSamples: Map<String, List<String>>,
)

class ScriptCorpusGenerator(private val dialogDataDir: File) {
  fun build(spec: ScriptCorpusSpec): BuiltScriptCorpus {
    val index = ScriptIndex.build(spec.decompDir)
    val events = MapEventIndex.build(spec.decompDir)
    val constants = ConstantsIndex.build(spec.decompDir)
    val failures = linkedMapOf<String, Int>()
    val samples = linkedMapOf<String, MutableList<String>>()
    val referencedText = linkedSetOf<String>()

    fun failure(category: String, detail: String) {
      failures[category] = failures.getOrDefault(category, 0) + 1
      samples
          .getOrPut(category) { mutableListOf() }
          .let { if (it.size < MAX_FAILURE_SAMPLES) it += detail }
    }

    val programs =
        index.scriptBodies().mapNotNull { (label, body) ->
          val sourceFile = sourceFile(spec.decompDir, body.sourceFile)
          val objectIds =
              mapName(spec.decompDir, body.sourceFile)?.let(events.objectIdsByMap::get).orEmpty()
          try {
            val parsed =
                PretScriptParser.parse(
                    id = ScriptId("gba", spec.source, spec.gameCode, label),
                    storyNamespace = spec.storyNamespace,
                    sourceFile = sourceFile,
                    lines = listOf("$label::") + body.commands,
                    objectIds = objectIds,
                    constants = constants,
                )
            parsed.instructions
                .flatMap { it.args }
                .filterIsInstance<TextArg>()
                .mapTo(referencedText) { it.token }
            ScriptCorpusProgramRecord(label, sourceFile, body.commands.toList(), objectIds)
          } catch (cause: IllegalArgumentException) {
            val category = parseFailureCategory(cause)
            failure(category, "$label: ${cause.message}")
            null
          }
        }

    val movements =
        index.movementBodies().mapNotNull { (label, body) ->
          val sourceFile = sourceFile(spec.decompDir, body.sourceFile)
          try {
            PretMovementParser.parse(
                ScriptId("gba", spec.source, spec.gameCode, label),
                sourceFile,
                listOf("$label:") + body.actions,
            )
            ScriptCorpusMovementRecord(label, sourceFile, body.actions.toList())
          } catch (cause: IllegalArgumentException) {
            failure("movement structure", "$label: ${cause.message}")
            null
          }
        }

    val dialogLines =
        DialogTable.read(DialogTable.file(dialogDataDir, spec.storyNamespace))
            ?: error("No dialog table for ${spec.storyNamespace} in $dialogDataDir")
    val allTextIds = dialogLines.associate { it.label to it.textId }
    val textIds =
        referencedText.mapNotNull { label -> allTextIds[label]?.let { label to it } }.toMap()

    val direct = events.interactableLabels + events.mapEntryLabels
    val unavailableDirectLabels = direct.filterTo(linkedSetOf()) { it !in index.scriptBodies() }

    // Ship only the constants the stored command text can actually mention.
    val tokenPattern = Regex("[A-Za-z_]\\w*")
    val referencedTokens = HashSet<String>()
    programs.forEach { program ->
      program.commands.forEach { command ->
        tokenPattern.findAll(command).forEach { referencedTokens += it.value }
      }
    }

    return BuiltScriptCorpus(
        spec = spec,
        indexedLabels = index.scriptBodies().size,
        programs = programs,
        movements = movements,
        textIds = textIds,
        constants = constants.filterKeys { it in referencedTokens },
        interactableLabels = events.interactableLabels,
        mapEntryLabels = events.mapEntryLabels,
        unavailableDirectLabels = unavailableDirectLabels,
        parseFailureCategories = failures,
        parseFailureSamples = samples.mapValues { it.value.toList() },
    )
  }

  private fun parseFailureCategory(cause: IllegalArgumentException): String =
      when {
        cause.message?.startsWith("switch expected") == true -> "switch argument shape"
        cause.message?.startsWith("case expected") == true -> "case argument shape"
        else -> "script structure"
      }

  private fun sourceFile(decompDir: File, file: String): String =
      "decomp/${decompDir.name}/${File(file).relativeTo(decompDir).invariantSeparatorsPath}"

  private fun mapName(decompDir: File, file: String): String? {
    val relative = File(file).relativeTo(decompDir).invariantSeparatorsPath
    if (!relative.startsWith("data/maps/")) return null
    return relative.removePrefix("data/maps/").substringBefore('/')
  }

  private companion object {
    const val MAX_FAILURE_SAMPLES = 5
  }
}

object ScriptCorpusBinary {
  private const val FORMAT_VERSION = 3

  fun encode(corpora: List<BuiltScriptCorpus>): String {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(GZIPOutputStream(bytes)).use { output ->
      output.writeInt(FORMAT_VERSION)
      output.writeInt(corpora.size)
      corpora.forEach { corpus ->
        output.writeUTF(corpus.spec.storyNamespace)
        output.writeUTF(corpus.spec.source)
        output.writeUTF(corpus.spec.gameCode)
        output.writeInt(corpus.indexedLabels)
        output.writeStringIntMap(corpus.parseFailureCategories)
        output.writeStringListMap(corpus.parseFailureSamples)
        output.writeStringSet(corpus.interactableLabels)
        output.writeStringSet(corpus.mapEntryLabels)
        output.writeStringSet(corpus.unavailableDirectLabels)
        output.writeStringIntMap(corpus.textIds)
        output.writeStringIntMap(corpus.constants)
        output.writeInt(corpus.programs.size)
        corpus.programs.forEach { program ->
          output.writeUTF(program.label)
          output.writeUTF(program.sourceFile)
          output.writeStringList(program.commands)
          output.writeStringIntMap(program.objectIds)
        }
        output.writeInt(corpus.movements.size)
        corpus.movements.forEach { movement ->
          output.writeUTF(movement.label)
          output.writeUTF(movement.sourceFile)
          output.writeStringList(movement.actions)
        }
      }
    }
    return Base64.getEncoder().encodeToString(bytes.toByteArray())
  }

  private fun DataOutputStream.writeStringList(values: Collection<String>) {
    writeInt(values.size)
    values.forEach(::writeUTF)
  }

  private fun DataOutputStream.writeStringSet(values: Set<String>) =
      writeStringList(values.sorted())

  private fun DataOutputStream.writeStringIntMap(values: Map<String, Int>) {
    writeInt(values.size)
    values.toSortedMap().forEach { (key, value) ->
      writeUTF(key)
      writeInt(value)
    }
  }

  private fun DataOutputStream.writeStringListMap(values: Map<String, List<String>>) {
    writeInt(values.size)
    values.toSortedMap().forEach { (key, value) ->
      writeUTF(key)
      writeStringList(value)
    }
  }
}
