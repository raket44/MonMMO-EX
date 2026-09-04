package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.MapType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger {}

/**
 * The map type the client is told for an NDS map (Johto, Sinnoh, Unova render from the ROM; the
 * server only names the type). Every NDS map used to be sent as ROUTE, which the client echoes
 * beside the location name ("Cianwood City/Route"). The type is derived from the map's directory
 * name against the retail dex's location list: a name the dex spells "... City" is a CITY, "...
 * Town" a town, obvious interiors are INSIDE, the rest stay ROUTE.
 */
object NdsMapTypes {
  private val names: Map<Triple<Int, Int, Int>, String> by lazy {
    // The dist runs from the repo root, the gradle run task from server.game/.
    val file = listOf(File("map-directory.txt"), File("server.game/map-directory.txt")).firstOrNull { it.isFile } ?: File("map-directory.txt")
    if (!file.isFile) {
      log.warn { "map-directory.txt missing; NDS maps are all sent as ROUTE" }
      return@lazy emptyMap()
    }
    file.readLines().mapNotNull { line ->
      val p = line.split(';')
      if (p.size < 4) return@mapNotNull null
      val region = p[0].toIntOrNull() ?: return@mapNotNull null
      val bank = p[1].toIntOrNull() ?: return@mapNotNull null
      val map = p[2].toIntOrNull() ?: return@mapNotNull null
      Triple(region, bank, map) to p[3].trim()
    }.toMap()
  }

  private val cities: Set<Pair<Int, String>> by lazy { retailPlaces(" City") }
  private val towns: Set<Pair<Int, String>> by lazy { retailPlaces(" Town") + retailPlaces(" Village") }

  private fun retailPlaces(suffix: String): Set<Pair<Int, String>> =
      RetailEncounters.locationNames()
          .filter { (_, name) -> name.endsWith(suffix) }
          .map { (region, name) -> region to key(name.removeSuffix(suffix)) }
          .toSet()

  private fun key(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }

  fun nameOf(region: Int, bank: Int, map: Int): String? = names[Triple(region, bank, map)]

  fun typeOf(region: Int, bank: Int, map: Int): MapType {
    val name = nameOf(region, bank, map) ?: return MapType.ROUTE
    val k = key(name)
    if (INTERIOR_WORDS.any { name.contains(it, ignoreCase = true) }) return MapType.INSIDE
    if (region to k in cities || name.endsWith(" City")) return MapType.CITY
    if (region to k in towns || name.endsWith(" Town")) return MapType.VILLAGE
    return MapType.ROUTE
  }

  private val INTERIOR_WORDS =
      listOf("House", "Gym", "Center", "Mart", "Lab", "Shop", "Store", "Room", "Home", "Office", "Hall", "Inside", "Floor", " F ", "B1", "Building", "Tower", "Cafe", "Salon", "Company", "School", "Museum", "Station", "Gate")
}
