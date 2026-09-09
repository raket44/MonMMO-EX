package de.fiereu.openmmo.codegen.script

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The GBA games' scripted menus as their option texts, by the constant a script names them with:
 * FireRed's `MULTICHOICE_*` and Emerald's `MULTI_*` lists (src/script_menu.c, src/data/script_menu.h)
 * and FireRed's scrolling `LISTMENU_*` lists (src/field_specials.c `sListMenuLabels`, also keyed
 * `LISTMENU#<id>` since `special ListMenu` reads the id from VAR_0x8004). Option texts resolve
 * through the `const u8 NAME[] = _("...")` strings of the C sources under src; a label with no string keeps its
 * symbol name so the gap is visible instead of silent.
 */
object MenuIndex {
  fun build(decompDir: File, constants: Map<String, Int>): Map<String, List<String>> {
    val strings = stringTable(decompDir)
    val text =
        listOf("src/script_menu.c", "src/data/script_menu.h", "src/field_specials.c")
            .map { File(decompDir, it) }
            .filter { it.isFile }
            .joinToString("\n") { it.readText() }
            .replace(Regex("//[^\n]*"), "")
    val lists = HashMap<String, List<String>>()
    Regex("""static const struct MenuAction (\w+)\[\]\s*=\s*\{(.*?)\};""", RegexOption.DOT_MATCHES_ALL)
        .findAll(text)
        .forEach { match ->
          lists[match.groupValues[1]] =
              Regex("""\{\s*(\w+)""").findAll(match.groupValues[2]).map { strings[it.groupValues[1]] ?: it.groupValues[1] }.toList()
        }
    val menus = LinkedHashMap<String, List<String>>()
    Regex("""\[(\w+)\]\s*=\s*MULTICHOICE\((\w+)\)""").findAll(text).forEach { match ->
      lists[match.groupValues[2]]?.let { menus[match.groupValues[1]] = it }
    }
    Regex("""sListMenuLabels\[\]\[\d+\]\s*=\s*\{(.*?)\n\};""", RegexOption.DOT_MATCHES_ALL).find(text)?.let { block ->
      Regex("""\[(\w+)\]\s*=\s*\{([^}]*)\}""").findAll(block.groupValues[1]).forEach { match ->
        val name = match.groupValues[1]
        val options =
            Regex("""\b(\w+)\b""").findAll(match.groupValues[2]).map { strings[it.groupValues[1]] ?: it.groupValues[1] }.toList()
        menus[name] = options
        constants[name]?.let { menus["LISTMENU#$it"] = options }
      }
    }
    return menus
  }

  /**
   * The client's DS menu-entry bank (Platinum res/text/menu_entries.json = message bank 361, the
   * one the text-button dialog draws from): option text -> first entry index carrying it.
   */
  fun dsMenuEntries(file: File): Map<String, Int> {
    if (!file.isFile) return emptyMap()
    val messages = Json.parseToJsonElement(file.readText()).jsonObject.getValue("messages").jsonArray
    val entries = LinkedHashMap<String, Int>()
    messages.forEachIndexed { index, message ->
      // A message is one string, or a list of strings for the multi-line ones.
      val label =
          when (val en = message.jsonObject["en_US"]) {
            is kotlinx.serialization.json.JsonPrimitive -> en.content
            is kotlinx.serialization.json.JsonArray -> en.joinToString("") { (it as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "" }
            else -> return@forEachIndexed
          }
      entries.putIfAbsent(label, index)
    }
    return entries
  }

  private fun stringTable(decompDir: File): Map<String, String> {
    val table = HashMap<String, String>()
    val definition = Regex("""^(?:static )?const u8 (\w+)\[\]\s*=\s*_\("(.*)"\);""", RegexOption.MULTILINE)
    File(decompDir, "src").listFiles { file -> file.name.endsWith(".c") }?.forEach { file ->
      definition.findAll(file.readText()).forEach { table[it.groupValues[1]] = it.groupValues[2] }
    }
    return table
  }
}
