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
      /** The dump's location_name_full: the short name plus a floor or zone, "Mt. Moon (B1F)". */
      val fullName: String = locationName,
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

  private val allEntries: List<Entry> by lazy {
    val stream =
        RetailEncounters::class.java.getResourceAsStream("/monmmo/retail-locations.csv")
            ?: return@lazy emptyList<Entry>().also {
              log.warn { "retail-locations.csv missing; retail encounter tables disabled" }
            }
    val entries =
        stream.bufferedReader().useLines { lines ->
          lines
              .mapNotNull { line ->
                val p = line.split(';')
                if (p.size < 14) return@mapNotNull null
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
                    fullName = p.getOrNull(14)?.takeIf { it.isNotEmpty() } ?: p[3],
                )
              }
              .toList()
        }
    log.info { "Retail encounter tables: ${entries.size} entries" }
    entries
  }

  private val byLocation: Map<String, List<Entry>> by lazy { allEntries.groupBy { normalize(it.locationName) } }

  /**
   * The retail dump's `location_id` per (region, full location name), from
   * `retail-location-ids.csv`. For the DS regions this id IS the server's map header
   * (`map shl 8 or bank`) - verified against White 2026-09-21: all 115 Unova ids resolve to a real
   * map-directory entry. That matters because the tables are split per FLOOR through the full name
   * ("Wellspring Cave (1F)" / "(B1F)", Chargestone "(1F)/(B1F)/(B2F)") while `map-directory.txt`
   * names every floor of a cave the same, so matching by name merges pools that the client and the
   * retail data keep apart.
   */
  private val locationIdByName: Map<Pair<Int, String>, Int> by lazy {
    val stream =
        RetailEncounters::class.java.getResourceAsStream("/monmmo/retail-location-ids.csv")
            ?: return@lazy emptyMap<Pair<Int, String>, Int>().also {
              log.warn { "retail-location-ids.csv missing; DS encounters fall back to name matching" }
            }
    stream.bufferedReader().useLines { lines ->
      lines
          .filterNot { it.startsWith("#") || it.isBlank() }
          .mapNotNull { line ->
            val p = line.split(';', limit = 3)
            if (p.size < 3) return@mapNotNull null
            val region = p[0].toIntOrNull() ?: return@mapNotNull null
            val id = p[1].toIntOrNull() ?: return@mapNotNull null
            (region to normalize(p[2])) to id
          }
          .toMap()
    }
  }

  /** Every entry of a DS map header, keyed exactly - no name matching, so floors stay apart. */
  private val byNdsHeader: Map<Pair<Int, Int>, List<Entry>> by lazy {
    allEntries
        .mapNotNull { e -> locationIdByName[e.regionId to normalize(e.fullName)]?.let { (e.regionId to it) to e } }
        .groupBy({ it.first }, { it.second })
  }

  /** True when this DS map has any entry of [type] at all, whatever the season or time. */
  fun ndsHeaderHasType(regionId: Int, header: Int, type: String): Boolean =
      byNdsHeader[regionId to header].orEmpty().any { it.type == type }

  /**
   * The wild pool for a DS map by its header id. Preferred over [wildPoolForNdsName]: a cave's
   * floors share one directory name but have different tables.
   */
  fun wildPoolForNdsHeader(header: Int, regionId: Int, types: Set<String>, season: Season, time: TimeOfDay): List<Slot> =
      slotsOf(byNdsHeader[regionId to header].orEmpty(), types, season, time)

  /**
   * All entries for a decomp map, matched by normalized name prefix before any floor suffix AND by
   * region: "Route 3" exists in every region, and matching by name alone put wild Blitzle - Unova's
   * Route 3 - on Kanto's. The retail dump numbers regions 0 Kanto, 1 Hoenn, 2 Unova, 3 Sinnoh, 4
   * Johto; the server's map regions share the 0/1 assignments it hosts.
   */
  fun entriesFor(sourceName: String, regionId: Int): List<Entry> =
      resolved.getOrPut(regionId to sourceName) {
        resolve(sourceName, regionId)
            ?: locationKey(sourceName)?.let { key -> byLocation[key].orEmpty().filter { it.regionId == regionId } }.orEmpty()
      }

  private val resolved = java.util.concurrent.ConcurrentHashMap<Pair<Int, String>, List<Entry>>()

  /** One retail area of a region: its short name's rows split by full name (floor / zone). */
  private class Area(
      val shortName: String,
      val shortTokens: List<String>,
      /** Full names that add a floor or zone to the short name. */
      val floors: List<FloorRows>,
      /** Rows whose full name is just the short name. */
      val unsuffixed: List<Entry>,
      val all: List<Entry>,
  )

  private class FloorRows(val tokens: List<String>, val entries: List<Entry>)

  private val areasByRegion: Map<Int, List<Area>> by lazy {
    allEntries.groupBy { it.regionId }.mapValues { (_, rows) ->
      rows.groupBy { normalize(it.locationName) }.values.map { areaRows ->
        val short = areaRows.first().locationName
        val shortTokens = tokens(short)
        val byFull = areaRows.groupBy { it.fullName }
        val floors = byFull.filterKeys { tokens(it) != shortTokens }.map { (full, e) -> FloorRows(tokens(full), e) }
        val unsuffixed = byFull.filterKeys { tokens(it) == shortTokens }.values.flatten()
        Area(short, shortTokens, floors, unsuffixed, areaRows)
      }
    }
  }

  /**
   * The rows for a map, in any region, by the words of its name against the dump's full location
   * names - one rule for FireRed's `FourIsland_IcefallCave_B1F`, Emerald's `MtPyre_Exterior` and
   * the DS directory's `Mt Coronet 4f Rooms 1 And 2` alike (2026-09-12):
   *
   *  1. the area whose short name's words all appear in the map name - the most words, then the
   *     one named latest in it (the cave inside an island beats the island);
   *  2. within it the full names whose extra words all appear ("Icefall Cave (B1F)"), the most
   *     specific ones;
   *  3. else every full name sharing a floor label with the map ("Mt. Coronet (4F North)" and
   *     "(4F South)" for a 4F room the dump does not split the same way);
   *  4. else the area's rows with no floor at all;
   *  5. else nothing when the map names a floor the dump has no rows for (Pokemon Tower 1F, where
   *     the dump starts at 3F) - and the whole area, logged, only for a map naming no floor.
   */
  private fun resolve(name: String, regionId: Int): List<Entry>? {
    val mapTokens = tokens(name)
    if (mapTokens.isEmpty()) return null
    val mapSet = mapTokens.toSet()
    val candidates = areasByRegion[regionId].orEmpty().filter { it.shortTokens.isNotEmpty() && mapSet.containsAll(it.shortTokens) }
    if (candidates.isEmpty()) return null
    val area = candidates.maxWithOrNull(compareBy<Area>({ it.shortTokens.size }, { a -> a.shortTokens.maxOf { mapTokens.lastIndexOf(it) } })) ?: return null
    val exact = area.floors.filter { mapSet.containsAll(it.tokens) }
    if (exact.isNotEmpty()) {
      val most = exact.maxOf { it.tokens.size }
      return exact.filter { it.tokens.size == most }.flatMap { it.entries }
    }
    val floorLabels = mapTokens.filter { FLOOR_LABEL.matches(it) }.toSet()
    if (floorLabels.isNotEmpty()) {
      val sharing = area.floors.filter { f -> f.tokens.any { it in floorLabels } }
      if (sharing.isNotEmpty()) return sharing.flatMap { it.entries }
    }
    if (area.unsuffixed.isNotEmpty()) return area.unsuffixed
    if (floorLabels.isNotEmpty() && area.floors.isNotEmpty()) {
      log.info { "[Retail] $name (region $regionId) names a floor '${area.shortName}' has no rows for: no encounters" }
      return emptyList()
    }
    log.info { "[Retail] $name (region $regionId) matches no floor of '${area.shortName}': using all its rows" }
    return area.all
  }

  /**
   * The words of a map or location name: ASCII, lower case, CamelCase and letter/digit boundaries
   * split, floor labels (B1F, 10F, 2R) kept whole, a few spellings unified, filler dropped.
   */
  internal fun tokens(name: String): List<String> {
    val ascii = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).filter { it.code < 128 }.replace("'s", "").replace("'S", "")
    return ascii
        .split(NON_WORD)
        .filter { it.isNotEmpty() }
        .flatMap { chunk -> WORD.findAll(chunk).map { it.value.lowercase() } }
        .map { TOKEN_ALIASES[it] ?: it }
        .filter { it !in FILLER_TOKENS }
  }

  private val NON_WORD = Regex("[^A-Za-z0-9]+")
  private val WORD = Regex("[Bb]?\\d+[FfRr](?![A-Za-z])|[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+")
  private val FLOOR_LABEL = Regex("^(b?\\d+f|\\d+r)$")
  private val TOKEN_ALIASES =
      mapOf(
          "island" to "isle",
          "exterior" to "outside",
          "inside" to "interior",
          "entryway" to "entrance",
          "rooftop" to "roof",
          "digletts" to "diglett",
      )
  // "area" and "room" are the dump's own furniture ("East Area", "Back Room", "Northern Room").
  private val FILLER_TOKENS = setOf("area", "the", "room")

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
      // Later runs first: the sub-area beats the island it sits on (FourIsland_IcefallCave_B1F is
      // "Icefall Cave", whose cave rows exist, not "Four Island", which only has sea rows).
      for (start in (parts.size - length) downTo 0) candidates += parts.subList(start, start + length).joinToString("")
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
    resolve(name, regionId)?.let { return it }
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
