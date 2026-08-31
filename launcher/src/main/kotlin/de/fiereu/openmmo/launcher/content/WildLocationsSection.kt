package de.fiereu.openmmo.launcher.content

/**
 * Builds data.pak section 5 - the Pokedex "Wild Locations" tab - from the retail client's own
 * encounter dump.
 *
 * The section shape, decoded from the client's loader (`f/uh`, section id 5) and the dex row
 * renderer (`f/C6.Ey`): `u16 speciesCount`, then per species `u16 id, u16 entryCount` and 12-byte
 * entries: `u8 region, u16 ignored, u8 locationIndex, u8 type, u8 variant, u8 timeMask, u8
 * regionFilter, u16 rarityMask, u8 minLevel, u8 maxLevel`. The row's location label is string
 * `140000 + region * 1000 + locationIndex`, which the stock client barely populates - the strings
 * are ours to define, and that is also how seasons work on this client generation: a
 * season-restricted spawn becomes its own labelled row, "Viridian Forest (Winter)", since the 31914
 * entry format carries no season field. Hordes and lures are native: rarity-mask bits the client
 * already renders with its own Horde and Lure labels.
 *
 * Types: Grass(0) carries Cave and Inside as variants 1 and 2; Fishing(3) carries the rod as its
 * variant; Water(1), Rocks(2), Dark Grass(4) and Headbutt(10) stand alone.
 */
object WildLocationsSection {

  data class Output(
      val payload: ByteArray,
      /** String id to label, for every location name the section references. */
      val strings: List<Pair<Int, String>>,
      val species: Int,
      val entries: Int,
      val skipped: Int,
  )

  private data class TypeKey(val type: Int, val variant: Int)

  private val TYPES =
      mapOf(
          "Grass" to TypeKey(0, 0),
          "Cave" to TypeKey(0, 1),
          "Inside" to TypeKey(0, 2),
          "Water" to TypeKey(1, 0),
          "Rocks" to TypeKey(2, 0),
          "Old Rod" to TypeKey(3, 0),
          "Good Rod" to TypeKey(3, 1),
          "Super Rod" to TypeKey(3, 2),
          "Fishing" to TypeKey(3, 2),
          "Dark Grass" to TypeKey(4, 0),
          "Headbutt" to TypeKey(10, 0),
      )

  /** Rarity-mask bits the dex renders, from most common tier to the special markers. */
  private const val VERY_COMMON = 1
  private const val COMMON = 2
  private const val UNCOMMON = 4
  private const val RARE = 8
  private const val VERY_RARE = 16
  private const val SPECIAL = 64
  private const val HORDE = 128
  private const val LURE = 256

  /**
   * The season a build targets. The 31914 row format has no season field, so one section can only
   * describe one season - the dex's season toggle is served by rebuilding the section, which is why
   * the retail rows are filtered here rather than folded into labels. Rows marked "Any" appear in
   * every season's build.
   */
  enum class SeasonFilter(val label: String) {
    ANY("Any"),
    SPRING("Spring"),
    SUMMER("Summer"),
    AUTUMN("Autumn"),
    WINTER("Winter"),
  }

  fun build(season: SeasonFilter = SeasonFilter.ANY): Output {
    val stream =
        javaClass.getResourceAsStream("/monmmo/retail-locations.csv")
            ?: error("retail-locations.csv is not on the classpath; run stageRetailData first")
    // (region, label) to index; each region has 256 addressable names.
    val locationIndex = linkedMapOf<Pair<Int, String>, Int>()
    val perRegionCounts = mutableMapOf<Int, Int>()
    val bySpecies = sortedMapOf<Int, MutableList<IntArray>>()
    var skipped = 0
    var entries = 0

    stream.bufferedReader().useLines { lines ->
      lines.forEach { line ->
        val p = line.split(';')
        if (p.size != 14) return@forEach
        val dexId = p[0].toIntOrNull() ?: return@forEach
        // Ids past 649 are the retail client's own form numbering, which collides with the wire
        // ids our imported species hold; base species only.
        if (dexId !in 1..649) return@forEach
        if ((p[1].toIntOrNull() ?: -1) >= 0) return@forEach
        val region = p[2].toIntOrNull() ?: return@forEach
        val type =
            TYPES[p[4]]
                ?: run {
                  skipped++
                  return@forEach
                }
        val minLevel = (p[5].toIntOrNull() ?: 1).coerceIn(1, 100)
        val maxLevel = (p[6].toIntOrNull() ?: minLevel).coerceIn(minLevel, 100)
        val rowSeason = p[7]
        // A season build keeps that season's rows plus the season-agnostic ones; the ANY build
        // keeps everything, which is what a client with no season control should see.
        if (season != SeasonFilter.ANY && rowSeason != "Any" && rowSeason != season.label) {
          return@forEach
        }
        val horde = p[9] == "true" || p[10] == "true"
        val rarities = listOf(p[11], p[12], p[13])

        var timeMask = 0
        var best = 0.0
        var lure = false
        var special = false
        rarities.forEachIndexed { index, rarity ->
          val percent = rarity.removeSuffix("%").toDoubleOrNull()
          when {
            percent != null && percent > 0 -> {
              timeMask = timeMask or (1 shl index)
              if (percent > best) best = percent
            }
            rarity == "Lure" -> {
              timeMask = timeMask or (1 shl index)
              lure = true
            }
            rarity == "Special" || rarity == "???" -> {
              timeMask = timeMask or (1 shl index)
              special = true
            }
          }
        }
        if (timeMask == 0) return@forEach
        var rarityMask =
            when {
              lure -> LURE
              special -> SPECIAL
              best >= 40 -> VERY_COMMON
              best >= 20 -> COMMON
              best >= 10 -> UNCOMMON
              best >= 5 -> RARE
              else -> VERY_RARE
            }
        if (horde) rarityMask = HORDE

        // A region has 256 addressable labels. Season-tagged labels are the first to concede:
        // when the region is full, the row keeps its plain location name rather than vanishing.
        fun indexFor(label: String): Int? {
          locationIndex[region to label]?.let {
            return it
          }
          val next = perRegionCounts.getOrDefault(region, 0)
          if (next > 255) return null
          perRegionCounts[region] = next + 1
          locationIndex[region to label] = next
          return next
        }
        // Plain location names only: seasons are a property of the build, not of the label, so
        // "Route 4 (Winter)" rows never appear in the list again.
        val index =
            indexFor(p[3])
                ?: run {
                  skipped++
                  return@forEach
                }
        bySpecies
            .getOrPut(dexId, ::mutableListOf)
            .add(
                intArrayOf(
                    region,
                    index,
                    type.type,
                    type.variant,
                    timeMask,
                    rarityMask,
                    minLevel,
                    maxLevel))
        entries++
      }
    }

    val writer = Writer()
    writer.short(bySpecies.size)
    bySpecies.forEach { (dexId, rows) ->
      writer.short(dexId)
      writer.short(rows.size)
      rows.forEach { row ->
        writer.byte(row[0]) // region
        writer.short(0) // consumed and discarded by the client
        writer.byte(row[1]) // location index
        writer.byte(row[2]) // type
        writer.byte(row[3]) // variant
        writer.byte(row[4]) // time-of-day mask
        writer.byte(row[0]) // region filter
        writer.short(row[5]) // rarity mask
        writer.byte(row[6]) // min level
        writer.byte(row[7]) // max level
      }
    }
    val strings =
        locationIndex.map { (key, index) -> (140000 + key.first * 1000 + index) to key.second }
    return Output(writer.bytes(), strings, bySpecies.size, entries, skipped)
  }
}

/** Reports what each season's section build would contain, without installing anything. */
fun main() {
  WildLocationsSection.SeasonFilter.entries.forEach { season ->
    val output = WildLocationsSection.build(season)
    println(
        "[wild-locations] ${season.label}: species=${output.species} entries=${output.entries} " +
            "names=${output.strings.size} skipped=${output.skipped} bytes=${output.payload.size}")
  }
}
