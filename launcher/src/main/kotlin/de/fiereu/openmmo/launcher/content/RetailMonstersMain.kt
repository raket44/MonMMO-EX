@file:JvmName("RetailMonstersMain")

package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Compacts the retail client's monsters.json - the dump of PokeMMO's own species database - into
 * the two tables the server actually consumes:
 * - `retail-locations.csv`: every wild encounter entry with its region, location, encounter type,
 *   level range, season, horde flags and per-time-of-day rarity. This is what makes encounter
 *   tables retail-accurate, seasonal, and time-aware, and what a dex "where to find" view reads.
 * - `retail-abilities.csv`: the three ability slots per species, retail ids - the reference the
 *   ability pill switches between.
 *
 * The source file is 19MB of JSON; the compacted tables ride the codegen resources like the other
 * calibration files, so the server needs no JSON parsing at runtime.
 */
fun main(args: Array<String>) {
  require(args.size == 2) { "Usage: <monsters-retail.json> <output-resource-dir>" }
  val source = Path.of(args[0])
  val outputDir = Path.of(args[1])
  require(Files.isRegularFile(source)) { "Retail monsters file not found: $source" }
  Files.createDirectories(outputDir)

  val root = Json.parseToJsonElement(Files.readString(source)).jsonArray
  var locationCount = 0
  var abilityCount = 0
  var heldItemCount = 0
  Files.newBufferedWriter(outputDir.resolve("retail-locations.csv")).use { locations ->
    Files.newBufferedWriter(outputDir.resolve("retail-abilities.csv")).use { abilities ->
     Files.newBufferedWriter(outputDir.resolve("retail-held-items.csv")).use { heldItems ->
      root.forEach { element ->
        val mon = element.jsonObject
        val dexId = mon.getValue("id").jsonPrimitive.content
        // The wild held items the dex lists, in its order: species;slot;client item id;name.
        mon["held_items"]?.jsonArray?.forEachIndexed { slot, item ->
          val body = item.jsonObject
          heldItems.appendLine(
              "$dexId;$slot;${body.getValue("id").jsonPrimitive.content};" +
                  body.getValue("name").jsonPrimitive.content.replace(';', ','))
          heldItemCount++
        }
        mon["abilities"]?.jsonArray?.forEachIndexed { slot, ability ->
          val body = ability.jsonObject
          abilities.appendLine(
              "$dexId;$slot;${body.getValue("id").jsonPrimitive.content};" +
                  body.getValue("name").jsonPrimitive.content)
          abilityCount++
        }
        mon["locations"]?.jsonArray?.forEach { location ->
          val entry = location.jsonObject
          fun text(key: String) = entry[key]?.jsonPrimitive?.content.orEmpty()
          // Semicolons separate fields, so any inside a name would shear the row.
          val name = text("location_name").replace(';', ',')
          locations.appendLine(
              listOf(
                      dexId,
                      text("form"),
                      text("region_id"),
                      name,
                      text("type"),
                      text("min_level"),
                      text("max_level"),
                      text("season"),
                      text("rarity_flags"),
                      text("is_horde_3x"),
                      text("is_horde_5x"),
                      text("rarity_morning"),
                      text("rarity_day"),
                      text("rarity_night"),
                      // The floor or zone the dump splits an area into ("Mt. Moon (B1F)").
                      text("location_name_full").replace(';', ','),
                  )
                  .joinToString(";"))
          locationCount++
        }
      }
     }
    }
  }
  println(
      "[retail-monsters] species=${root.size} locations=$locationCount abilities=$abilityCount " +
          "heldItems=$heldItemCount")
}
