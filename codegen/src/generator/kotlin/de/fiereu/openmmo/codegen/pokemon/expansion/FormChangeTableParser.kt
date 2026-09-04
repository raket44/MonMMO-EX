package de.fiereu.openmmo.codegen.pokemon.expansion

import java.io.File

/**
 * Reads `src/data/pokemon/form_change_tables.h`: every `sXFormChangeTable[]` with its
 * `{FORM_CHANGE_KIND, SPECIES_TARGET, params...}` rows. Preprocessor guards are ignored - the
 * server decides at runtime which kinds it acts on, and a mega row it never triggers is harmless.
 */
object FormChangeTableParser {
  data class Row(val table: String, val kind: String, val target: String, val params: List<String>)

  fun parse(expansionRoot: File): List<Row> {
    val file = File(expansionRoot, "src/data/pokemon/form_change_tables.h")
    if (!file.isFile) return emptyList()
    val rows = mutableListOf<Row>()
    var table: String? = null
    file.readLines().forEach { raw ->
      val line = raw.substringBefore("//").trim()
      if (line.startsWith("static const struct FormChange ")) {
        table = line.substringAfter("struct FormChange ").substringBefore("[").trim()
        return@forEach
      }
      if (line.startsWith("};")) {
        table = null
        return@forEach
      }
      val current = table ?: return@forEach
      if (!line.startsWith("{")) return@forEach
      val parts =
          line.removePrefix("{").substringBefore("}").split(',').map { it.trim() }.filter { it.isNotEmpty() }
      if (parts.size < 2 || parts[0] == "FORM_CHANGE_TERMINATOR") return@forEach
      rows += Row(current, parts[0], parts[1], parts.drop(2))
    }
    return rows
  }
}
