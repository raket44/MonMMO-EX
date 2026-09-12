package de.fiereu.openmmo.codegen.dialog

import java.io.File

/**
 * A `.string` (or `.braille`) text label found in the decomp: its label and the concatenated
 * literal content. Braille signs assemble through their own byte table ([BrailleCharmap]).
 */
data class DecompText(val label: String, val content: String, val braille: Boolean = false)

/**
 * Collects every `.string` text label from the overworld data (map and shared scripts). A label is
 * a text label when it is directly followed by one or more `.string` lines.
 */
class TextParser(private val decompDir: File) {
  private val labelLine = Regex("^(\\w+):+\\s*$")

  fun parseAll(): List<DecompText> {
    // Emerald keeps its shared text next to the scripts that use it, FireRed keeps it in its own
    // data/text folder. Reading all three roots covers both.
    val roots =
        listOf(
            File(decompDir, "data/maps"),
            File(decompDir, "data/scripts"),
            File(decompDir, "data/text"),
        )
    val out = LinkedHashMap<String, Pair<String, Boolean>>()
    for (root in roots) {
      if (!root.isDirectory) continue
      root
          .walkTopDown()
          .filter { it.isFile && it.extension == "inc" }
          .forEach { file -> parseFile(file, out) }
    }
    // Both games keep a batch of shared overworld text (Pokemon Center nurse lines included)
    // directly in the event_scripts assembly rather than any of the folders above.
    val eventScripts = File(decompDir, "data/event_scripts.s")
    if (eventScripts.isFile) parseFile(eventScripts, out)
    return out.map { DecompText(it.key, it.value.first, it.value.second) }
  }

  private fun parseFile(file: File, out: MutableMap<String, Pair<String, Boolean>>) {
    var pending: String? = null
    var active: String? = null
    var braille = false
    val buffer = StringBuilder()
    fun flush() {
      val label = active
      if (label != null && label !in out) out[label] = buffer.toString() to braille
      active = null
      braille = false
      buffer.setLength(0)
    }
    for (raw in file.readLines()) {
      val line = raw.trim()
      val m = labelLine.matchEntire(line)
      when {
        m != null -> {
          flush()
          pending = m.groupValues[1]
        }
        line.startsWith(".string") || line.startsWith(".braille") -> {
          val label = active ?: pending ?: continue
          if (active == null) {
            active = label
            braille = line.startsWith(".braille")
            buffer.setLength(0)
          }
          buffer.append(stringLiteral(line))
        }
        // Emerald keeps an RS-era brailleformat line between a braille label and its .braille
        // lines (data/text/braille.inc); it is layout data the game ignores, so is the parser.
        line.startsWith("brailleformat") -> {}
        else -> {
          flush()
          pending = null
        }
      }
    }
    flush()
  }

  private fun stringLiteral(line: String): String {
    val first = line.indexOf('"')
    val last = line.lastIndexOf('"')
    return if (first in 0 until last) line.substring(first + 1, last) else ""
  }
}
