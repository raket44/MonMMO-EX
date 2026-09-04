package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.EncounterMethod
import de.fiereu.openmmo.maps.MapManager
import io.kotest.core.spec.style.FunSpec

/** Prints which maps the retail encounter tables cover; a report, not a gate. */
class RetailEncounterCoverageTest :
    FunSpec({
      test("coverage report") {
        val maps = MapManager().allMaps()
        val byRegion = maps.groupBy { it.regionId.toInt() }
        val sb = StringBuilder()
        for ((region, regionMaps) in byRegion.toSortedMap()) {
          val withTable = regionMaps.filter { it.encounterTable(EncounterMethod.LAND) != null }
          val matched = withTable.filter { RetailEncounters.entriesFor(it.sourceName, region).isNotEmpty() }
          val unmatched = withTable - matched.toSet()
          sb.appendLine("REGION $region maps=${regionMaps.size} withLandTable=${withTable.size} retailMatched=${matched.size} unmatched=${unmatched.size}")
          unmatched.map { it.sourceName }.distinct().sorted().take(60).forEach { sb.appendLine("  UNMATCHED $region $it") }
        }
        val names = maps.map { it.regionId.toInt() to it.sourceName }.toSet()
        val retail = RetailEncounters.locationNames()
        val orphan = retail.filter { (region, name) -> names.none { it.first == region && RetailEncounters.sameLocation(it.second, name) } }
        sb.appendLine("RETAIL locations=${retail.size} orphan=${orphan.size}")
        orphan.sortedBy { it.first * 1000 + it.second.hashCode() % 1000 }.take(400).forEach { sb.appendLine("  ORPHAN ${it.first} ${it.second}") }
        java.io.File("build/retail-encounter-coverage.txt").writeText(sb.toString())
        println(sb.toString().lines().take(8).joinToString("\n"))
      }
    })
