package de.fiereu.openmmo.codegen.script

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The interactable script labels each map references, grouped by the owning map (the map folder
 * name). Interactable object/background events remain in [byMap] for compatibility; coordinate,
 * transition, and frame entry points are indexed separately in [mapEntryByMap].
 */
class MapEventIndex
private constructor(
    val byMap: Map<String, List<String>>,
    val mapEntryByMap: Map<String, List<String>>,
    /** pret local object constants normalized to the map runtime's zero-based npc indexes. */
    val objectIdsByMap: Map<String, Map<String, Int>>,
) {

  val interactableLabels: Set<String> = byMap.values.flatten().toSet()

  val mapEntryLabels: Set<String> = mapEntryByMap.values.flatten().toSet()

  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    fun build(decompDir: File): MapEventIndex {
      val mapsDir = File(decompDir, "data/maps")
      val out = LinkedHashMap<String, List<String>>()
      val entries = LinkedHashMap<String, List<String>>()
      val objectIds = LinkedHashMap<String, Map<String, Int>>()
      if (mapsDir.isDirectory) {
        mapsDir
            .listFiles { f -> f.isDirectory }
            ?.sortedBy { it.name }
            ?.forEach { dir ->
              val mapJson = File(dir, "map.json")
              if (mapJson.isFile) {
                val parsed = parse(mapJson)
                if (parsed.labels.isNotEmpty()) out[dir.name] = parsed.labels
                val mapEntries =
                    LinkedHashSet<String>().apply {
                      addAll(parsed.coordinateLabels)
                      addAll(parseMapEntries(File(dir, "scripts.inc")))
                    }
                if (mapEntries.isNotEmpty()) entries[dir.name] = mapEntries.toList()
                if (parsed.objectIds.isNotEmpty()) objectIds[dir.name] = parsed.objectIds
              }
            }
      }
      return MapEventIndex(out, entries, objectIds)
    }

    private fun parse(mapJson: File): ParsedMapEvents {
      val root = json.parseToJsonElement(mapJson.readText()).jsonObject
      val labels = LinkedHashSet<String>()
      val coordinateLabels = LinkedHashSet<String>()
      val objectIds = linkedMapOf<String, Int>()
      root["object_events"]?.jsonArray?.forEachIndexed { index, event ->
        val objectEvent = event.jsonObject
        objectEvent["local_id"]?.jsonPrimitive?.content?.let { objectIds[it] = index }
        objectEvent["script"]?.jsonPrimitive?.content?.takeIf(::isLabel)?.let(labels::add)
      }
      root["bg_events"]?.jsonArray?.forEach { event ->
        event.jsonObject["script"]?.jsonPrimitive?.content?.takeIf(::isLabel)?.let(labels::add)
      }
      root["coord_events"]?.jsonArray?.forEach { event ->
        event.jsonObject["script"]
            ?.jsonPrimitive
            ?.content
            ?.takeIf(::isLabel)
            ?.let(coordinateLabels::add)
      }
      return ParsedMapEvents(labels.toList(), coordinateLabels.toList(), objectIds)
    }

    private fun parseMapEntries(scriptsFile: File): List<String> {
      if (!scriptsFile.isFile) return emptyList()
      val labels = LinkedHashSet<String>()
      val direct = Regex("""map_script\s+MAP_SCRIPT_[A-Z_]+\s*,\s*(\w+)""")
      val conditional = Regex("""map_script_2\s+\w+\s*,\s*[^,]+\s*,\s*(\w+)""")
      scriptsFile.forEachLine { raw ->
        direct.find(raw)?.groupValues?.get(1)?.takeIf(::isLabel)?.let(labels::add)
        conditional.find(raw)?.groupValues?.get(1)?.takeIf(::isLabel)?.let(labels::add)
      }
      return labels.toList()
    }

    private fun isLabel(value: String): Boolean =
        value.isNotBlank() && value != "0x0" && value != "NULL" && value.first().isLetter()

    private data class ParsedMapEvents(
        val labels: List<String>,
        val coordinateLabels: List<String>,
        val objectIds: Map<String, Int>,
    )
  }
}
