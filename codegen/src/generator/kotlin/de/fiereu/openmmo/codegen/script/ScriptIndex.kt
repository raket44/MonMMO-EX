package de.fiereu.openmmo.codegen.script

import java.io.File

/**
 * Every executable script in the decomp keyed by its label, with the command lines that make up its
 * body. Scripts live in the map scripts and in the shared script files. A label ending in `::`
 * opens a script, a label ending in a single `:` opens data such as text and ends the current
 * script.
 */
data class ScriptBody(val sourceFile: String, val commands: MutableList<String>)

data class MovementBody(val sourceFile: String, val actions: MutableList<String>)

class ScriptIndex
private constructor(
    private val scripts: Map<String, ScriptBody>,
    private val movements: Map<String, MovementBody>,
) {

  fun commandsFor(label: String): List<String>? = scripts[label]?.commands

  fun bodyFor(label: String): ScriptBody? = scripts[label]

  fun movementBodyFor(label: String): MovementBody? = movements[label]

  fun scriptBodies(): Map<String, ScriptBody> = scripts

  fun movementBodies(): Map<String, MovementBody> = movements

  companion object {
    private val labelLine = Regex("^(\\w+)::?\\s*$")
    private val aliasLine = Regex("^\\.(?:equ|set)\\s+(\\w+)\\s*,\\s*(.+)$")

    private fun applyAliases(line: String, aliases: Map<String, String>): String {
      var out = line
      for ((name, value) in aliases) out = out.replace(Regex("\\b" + Regex.escape(name) + "\\b"), value)
      return out
    }

    fun build(decompDir: File): ScriptIndex {
      val roots = listOf(File(decompDir, "data/maps"), File(decompDir, "data/scripts"))
      val out = LinkedHashMap<String, ScriptBody>()
      val movements = LinkedHashMap<String, MovementBody>()
      for (root in roots) {
        if (!root.isDirectory) continue
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == "inc" }
            .forEach {
              val lines = expandLocalMacros(it.readLines())
              parseFile(it, lines, out)
              parseMovementFile(it, lines, movements)
            }
      }
      File(decompDir, "data/event_scripts.s").takeIf(File::isFile)?.let {
        val lines = expandLocalMacros(it.readLines())
        parseFile(it, lines, out)
        parseMovementFile(it, lines, movements)
      }
      return ScriptIndex(out, movements)
    }

    /**
     * Inlines a file's own argument-less `.macro`/`.endm` blocks (Pallet Town's walk_to_lab,
     * Pewter's walk_to_gym) so movement lists read as plain steps. Parameterised macros are dropped
     * whole - nothing expands them, so their call sites stay visibly unsupported.
     */
    private fun expandLocalMacros(raw: List<String>): List<String> {
      val macros = HashMap<String, List<String>>()
      val body = mutableListOf<String>()
      var recording = false
      var name: String? = null
      val withoutDefinitions = mutableListOf<String>()
      for (line in raw) {
        val trimmed = line.trim()
        when {
          trimmed.startsWith(".macro") -> {
            recording = true
            body.clear()
            name =
                trimmed
                    .removePrefix(".macro")
                    .trim()
                    .split(Regex("\\s+"))
                    .takeIf { it.size == 1 }
                    ?.single()
          }
          trimmed == ".endm" -> {
            name?.let { macros[it] = body.toList() }
            recording = false
            name = null
          }
          recording -> body.add(line)
          else -> withoutDefinitions.add(line)
        }
      }
      if (macros.isEmpty()) return withoutDefinitions

      fun expand(line: String, depth: Int): List<String> {
        val invocation = line.substringBefore('@').trim()
        val macro = macros[invocation] ?: return listOf(line)
        if (depth > 4) return listOf(line)
        return macro.flatMap { expand(it, depth + 1) }
      }
      return withoutDefinitions.flatMap { expand(it, 0) }
    }

    private fun parseFile(file: File, lines: List<String>, out: MutableMap<String, ScriptBody>) {
      // Labels stacked with no commands between them are aliases for the same body.
      val pending = mutableListOf<String>()
      // A single-colon label whose block is `.2byte` rows - Emerald's mart shelves. FireRed
      // declares its shelves with `::` and they come through the ordinary path.
      var dataLabel: String? = null
      // File-local assembler aliases (`.equ SWITCH2_ID, VAR_0x8005` in the Vermilion Gym) are
      // substituted into every later line, so scripts read the var they actually mean.
      val aliases = LinkedHashMap<String, String>()
      for (rawLine in lines) {
        val aliasMatch = aliasLine.matchEntire(rawLine.trim())
        if (aliasMatch != null) {
          aliases[aliasMatch.groupValues[1]] = aliasMatch.groupValues[2].trim()
          continue
        }
        val raw = if (aliases.isEmpty()) rawLine else applyAliases(rawLine, aliases)
        val line = raw.trim()
        // A label may carry a trailing `@ comment` (silphco_doors.inc: EventScript_Close5FDoor1);
        // the comment is not part of the label.
        val labelText = raw.substringBefore('@').trim()
        val match = labelLine.matchEntire(labelText)
        if (match != null) {
          dataLabel = null
          if (labelText.endsWith("::")) {
            val label = match.groupValues[1]
            out.getOrPut(label) { ScriptBody(file.path, mutableListOf()) }
            pending.add(label)
          } else if (pending.isNotEmpty()) {
            // A single-colon label INSIDE a script is a local jump target the script falls through
            // into (field_moves.inc: EventScript_WaterCrashingDown, EventScript_EndSurface). It
            // opens a body of its own for the gotos while the enclosing bodies keep collecting.
            val label = match.groupValues[1]
            out.getOrPut(label) { ScriptBody(file.path, mutableListOf()) }
            pending.add(label)
          } else {
            // A data label ends the current script body.
            pending.clear()
            dataLabel = match.groupValues[1]
          }
          continue
        }
        if (line.isEmpty()) {
          // A blank line separates one script from the next.
          pending.clear()
          dataLabel = null
          continue
        }
        if (line.startsWith("@")) continue
        if (dataLabel != null) {
          if (line.startsWith(".2byte")) {
            out.getOrPut(dataLabel) { ScriptBody(file.path, mutableListOf()) }.commands.add(line)
            continue
          }
          if (line.startsWith(".")) {
            // Anything else under a data label is text or tables the server does not read.
            dataLabel = null
            continue
          }
          // A single-colon label after a blank line whose block is script commands is a local
          // script after all (mystery_event_club.inc: EventScript_AlreadyGaveProfile and the
          // rest of the Pewter Center woman's branches); the gotos into it must resolve.
          out.getOrPut(dataLabel) { ScriptBody(file.path, mutableListOf()) }
          pending.add(dataLabel)
          dataLabel = null
        }
        for (label in pending) out.getValue(label).commands.add(line)
      }
    }

    /** Movement lists are data, and Emerald therefore declares nearly all of them with `:`. */
    private fun parseMovementFile(
        file: File,
        lines: List<String>,
        out: MutableMap<String, MovementBody>,
    ) {
      val pendingLabels = mutableListOf<String>()
      val actions = mutableListOf<String>()

      fun flush() {
        if (actions.any { it.substringBefore('@').trim() == "step_end" }) {
          for (label in pendingLabels) {
            out.putIfAbsent(label, MovementBody(file.path, actions.toMutableList()))
          }
        }
        pendingLabels.clear()
        actions.clear()
      }

      for (raw in lines) {
        val line = raw.trim()
        val match = labelLine.matchEntire(line)
        if (match != null) {
          if (actions.isNotEmpty()) flush()
          pendingLabels.add(match.groupValues[1])
          continue
        }
        if (line.isEmpty()) {
          flush()
          continue
        }
        if (!line.startsWith("@") && pendingLabels.isNotEmpty()) actions.add(line)
      }
      flush()
    }
  }
}
