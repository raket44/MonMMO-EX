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
      locationKey(sourceName)?.let { key -> byLocation[key].orEmpty().filter { it.regionId == regionId } }.orEmpty()

  /**
   * The retail location a decomp map name belongs to. Names are `Area_Sub_Floor` chains: the whole
   * chain, then every shorter run of its segments, is tried longest first, so `PokemonTower_3F`
   * finds "Pokémon Tower" and `SevenIsland_SevaultCanyon_Entrance` finds "Sevault Canyon" rather
   * than Seven Island.
   */
  private fun locationKey(sourceName: String): String? {
    val parts = sourceName.split('_').filter { it.isNotEmpty() }
    val candidates = mutableListOf<String>()
    for (length in parts.size downTo 1) {
      for (start in 0..parts.size - length) candidates += parts.subList(start, start + length).joinToString("")
    }
    return candidates.map(::normalize).firstOrNull { it in byLocation }
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
  ): List<Slot> = slotsOf(entriesFor(sourceName, regionId), types, season, time)

  private fun slotsOf(entries: List<Entry>, types: Set<String>, season: Season, time: TimeOfDay): List<Slot> =
      entries
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

  /**
   * The horde pool of a map: the entries flagged for three- or five-strong hordes under the given
   * terrain types (Sweet Scent brings the horde out of the ground the player stands on). [size]
   * picks the 3x or 5x flag.
   */
  fun hordePool(sourceName: String, regionId: Int, types: Set<String>, season: Season, time: TimeOfDay, size: Int): List<Slot> =
      hordeSlotsOf(entriesFor(sourceName, regionId), types, season, time, size)

  fun hordePoolForNdsName(name: String, regionId: Int, types: Set<String>, season: Season, time: TimeOfDay, size: Int): List<Slot> =
      hordeSlotsOf(entriesForNdsName(name, regionId), types, season, time, size)

  private fun hordeSlotsOf(entries: List<Entry>, types: Set<String>, season: Season, time: TimeOfDay, size: Int): List<Slot> =
      entries
          .asSequence()
          // "Sweet Scent" rows are the horde-only entries of a route (Route 22 and 197 others have
          // their 3x hordes typed that way rather than "Grass"), callable from any terrain there.
          .filter { it.type in types || it.type == "Sweet Scent" }
          .filter { it.season == "Any" || it.season == season.label }
          .filter { it.form < 0 }
          .filter { if (size >= 5) it.horde5x else it.horde3x }
          .mapNotNull { entry ->
            val weight = percentWeight(entry.rarity(time)) ?: return@mapNotNull null
            Slot(entry.dexId, entry.minLevel, entry.maxLevel, weight)
          }
          .toList()

  /** "12.5%" becomes 1250; markers like Lure, Special, -- and ??? are not ordinary spawns. */
  private fun percentWeight(rarity: String): Int? =
      rarity.removeSuffix("%").toDoubleOrNull()?.let { (it * 100).toInt() }?.takeIf { it > 0 }

  /**
   * A DS map's directory name against the dex's locations of its region: exact first, then the
   * unique dex location that starts with it ("Cianwood" -> "Cianwood City"), then the unique one
   * that contains it. Null when nothing fits or several do.
   */
  fun entriesForNdsName(name: String, regionId: Int): List<Entry> {
    val key = normalize(name)
    if (key.isEmpty()) return emptyList()
    byLocation[key]?.filter { it.regionId == regionId }?.takeIf { it.isNotEmpty() }?.let { return it }
    val regional = locationNames().filter { it.first == regionId }.map { normalize(it.second) }.distinct()
    val prefixed = regional.filter { it.startsWith(key) }
    val chosen =
        when {
          prefixed.size == 1 -> prefixed[0]
          else -> regional.filter { it.contains(key) }.singleOrNull()
        } ?: return emptyList()
    return byLocation[chosen].orEmpty().filter { it.regionId == regionId }
  }

  /** The wild pool for a DS map by directory name, see [wildPool]. */
  fun wildPoolForNdsName(name: String, regionId: Int, types: Set<String>, season: Season, time: TimeOfDay): List<Slot> =
      slotsOf(entriesForNdsName(name, regionId), types, season, time)

  /** Every (region, location name) the retail dump knows, for coverage reports. */
  fun locationNames(): Set<Pair<Int, String>> =
      byLocation.values.flatten().map { it.regionId to it.locationName }.toSet()

  fun sameLocation(sourceName: String, locationName: String): Boolean =
      locationKey(sourceName) == normalize(locationName)

  /** Lower-case ASCII letters and digits: "Pokémon Tower" and `PokemonTower` meet in the middle. */
  private fun normalize(name: String): String =
      java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
          .lowercase()
          .filter { it in 'a'..'z' || it in '0'..'9' }
          // The dex says "Three Isle Port" where the decomp says ThreeIsland_Port.
          .replace("island", "isle")
}
