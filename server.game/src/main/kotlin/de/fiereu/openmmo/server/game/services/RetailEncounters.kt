package de.fiereu.openmmo.server.game.services

import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * The retail client's own wild encounter tables, compacted from its monsters.json dump.
 *
 * Every entry names a location ("Viridian Forest"), an encounter type (Grass, Cave, Water, rods,
 * Dark Grass...), a level range, a season, horde flags, and a separate rarity for morning, day and
 * night. Locations are matched to decomp maps by normalized name - `CeruleanCave_1F` matches
 * "Cerulean Cave" by its prefix - so one retail location covers all of a dungeon's floors, exactly
 * as the retail dex presents it.
 *
 * Rarity strings are percentages for ordinary slots; "Lure", "Special", "--" and "???" mark spawns
 * that ordinary walking never rolls, so they are excluded from the wild pool but kept for the dex.
 */
object RetailEncounters {

  data class Entry(
      val dexId: Int,
      val form: Int,
      val regionId: Int,
      val locationName: String,
      val type: String,
      val minLevel: Int,
      val maxLevel: Int,
      val season: String,
      val horde3x: Boolean,
      val horde5x: Boolean,
      val rarityMorning: String,
      val rarityDay: String,
      val rarityNight: String,
  ) {
    fun rarity(time: TimeOfDay): String =
        when (time) {
          TimeOfDay.MORNING -> rarityMorning
          TimeOfDay.DAY -> rarityDay
          TimeOfDay.NIGHT -> rarityNight
        }
  }

  /** One rollable slot: a species with a weight in hundredths of a percent. */
  data class Slot(val dexId: Int, val minLevel: Int, val maxLevel: Int, val weight: Int)

  private val byLocation: Map<String, List<Entry>> by lazy {
    val stream =
        RetailEncounters::class.java.getResourceAsStream("/monmmo/retail-locations.csv")
            ?: return@lazy emptyMap<String, List<Entry>>().also {
              log.warn { "retail-locations.csv missing; retail encounter tables disabled" }
            }
    val entries =
        stream.bufferedReader().useLines { lines ->
          lines
              .mapNotNull { line ->
                val p = line.split(';')
                if (p.size != 14) return@mapNotNull null
                Entry(
                    dexId = p[0].toIntOrNull() ?: return@mapNotNull null,
                    form = p[1].toIntOrNull() ?: -1,
                    regionId = p[2].toIntOrNull() ?: return@mapNotNull null,
                    locationName = p[3],
                    type = p[4],
                    minLevel = p[5].toIntOrNull() ?: 1,
                    maxLevel = p[6].toIntOrNull() ?: 100,
                    season = p[7],
                    horde3x = p[9] == "true",
                    horde5x = p[10] == "true",
                    rarityMorning = p[11],
                    rarityDay = p[12],
                    rarityNight = p[13],
                )
              }
              .toList()
        }
    log.info { "Retail encounter tables: ${entries.size} entries" }
    entries.groupBy { normalize(it.locationName) }
  }

  /**
   * All entries for a decomp map, matched by normalized name prefix before any floor suffix AND by
   * region: "Route 3" exists in every region, and matching by name alone put wild Blitzle - Unova's
   * Route 3 - on Kanto's. The retail dump numbers regions 0 Kanto, 1 Hoenn, 2 Unova, 3 Sinnoh, 4
   * Johto; the server's map regions share the 0/1 assignments it hosts.
   */
  fun entriesFor(sourceName: String, regionId: Int): List<Entry> =
      byLocation[normalize(sourceName.substringBefore('_'))].orEmpty().filter {
        it.regionId == regionId
      }

  /**
   * The rollable wild pool for a map under the given conditions. Base forms only - a form entry
   * (form >= 0) duplicates its base species row for dex display purposes.
   */
  fun wildPool(
      sourceName: String,
      regionId: Int,
      types: Set<String>,
      season: Season,
      time: TimeOfDay,
  ): List<Slot> =
      entriesFor(sourceName, regionId)
          .asSequence()
          .filter { it.type in types }
          .filter { it.season == "Any" || it.season == season.label }
          .filter { it.form < 0 }
          .filter { !it.horde3x && !it.horde5x }
          .mapNotNull { entry ->
            val weight = percentWeight(entry.rarity(time)) ?: return@mapNotNull null
            Slot(entry.dexId, entry.minLevel, entry.maxLevel, weight)
          }
          .toList()

  /** "12.5%" becomes 1250; markers like Lure, Special, -- and ??? are not ordinary spawns. */
  private fun percentWeight(rarity: String): Int? =
      rarity.removeSuffix("%").toDoubleOrNull()?.let { (it * 100).toInt() }?.takeIf { it > 0 }

  private fun normalize(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }
}
