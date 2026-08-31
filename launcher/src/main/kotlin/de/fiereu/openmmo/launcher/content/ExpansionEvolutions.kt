package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Evolution data read from the Expansion's species tables, converted to the client's model.
 *
 * The client stores one evolution as `(method, param, target)` where the method is a **gen-5 ROM
 * method id** - the loader looks the byte up by value in its method enum and, for the item-based
 * ordinals, shifts the param into PokeMMO's item space. Injected entries go through the exact same
 * enum lookup, so matching the ROM ids is the whole contract.
 *
 * The Expansion writes `.evolutions = EVOLUTION({EVO_ITEM, ITEM_WATER_STONE, SPECIES_VAPOREON})`,
 * with its own method vocabulary and item ids. Methods the ROM never had degrade to a plain
 * level-up entry: the tree still links and renders, only the condition label is approximate. Item
 * params are translated to gen-5 item ids where one exists.
 */
object ExpansionEvolutions {

  /** Expansion item symbols seen in evolutions with no client item id yet. */
  val unmappedItems = sortedSetOf<String>()

  /**
   * Items imported by the overlay get their evolution params here - the imported id minus the
   * client loader..s +5000 item shift, so the stored value resolves to the imported item.
   */
  private fun importedParam(symbol: String): Int? = EvoItemPlan.evolutionParam(symbol)

  data class Evolution(val method: Int, val param: Int, val targetSymbol: String)

  /** Parses every species-info file, keyed by bare species symbol. */
  fun parse(expansionRoot: Path): Map<String, List<Evolution>> {
    val speciesInfo = expansionRoot.resolve("src/data/pokemon/species_info")
    val result = linkedMapOf<String, List<Evolution>>()
    Files.list(speciesInfo).use { files ->
      files
          .filter { it.name.endsWith(".h") }
          .sorted()
          .forEach { file -> parseFile(Files.readString(file), result) }
    }
    return result
  }

  private fun parseFile(text: String, into: MutableMap<String, List<Evolution>>) {
    ENTRY.findAll(text).forEach { entry ->
      val symbol = entry.groupValues[1]
      val block = EVOLUTIONS.find(entry.groupValues[2])?.groupValues?.get(1) ?: return@forEach
      val evolutions =
          entries(block).mapNotNull { tuple ->
            val head = EVOLUTION_TUPLE.find(tuple) ?: return@mapNotNull null
            val method = resolveMethod(head.groupValues[1], tuple)
            val rawParam = head.groupValues[2].trim()
            val mappedParam =
                rawParam.toIntOrNull() ?: ITEM_IDS[rawParam] ?: importedParam(rawParam)
            if (mappedParam == null && method == USE_ITEM) {
              // An Expansion item the client has no id for. Dropping the entry breaks the chain,
              // so it degrades to a plain level evolution and is counted for the day the items
              // themselves are imported.
              unmappedItems.add(rawParam)
              return@mapNotNull Evolution(FALLBACK_METHOD, 0, head.groupValues[3])
            }
            Evolution(method, mappedParam ?: 0, head.groupValues[3])
          }
      if (evolutions.isNotEmpty()) into[symbol] = evolutions
    }
  }

  /** Splits an EVOLUTION(...) block into balanced top-level `{...}` entries. */
  private fun entries(block: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var start = -1
    block.forEachIndexed { index, c ->
      when (c) {
        '{' -> {
          if (depth == 0) start = index
          depth++
        }
        '}' -> {
          depth--
          if (depth == 0 && start >= 0) {
            result.add(block.substring(start, index + 1))
            start = -1
          }
        }
      }
    }
    return result
  }

  /**
   * The modern Expansion writes most special evolutions as `EVO_LEVEL` plus a `CONDITIONS(...)`
   * suffix, which carries the meaning: Sylveon is level plus minimum friendship plus a known Fairy
   * move, Leafeon is level inside a forest map. The client has native methods for exactly these
   * shapes - friendship with day and night variants, and the two location-based methods its own
   * Eevee entries use - so the conditions choose the method.
   */
  private fun resolveMethod(methodSymbol: String, tuple: String): Int {
    val base = METHODS[methodSymbol] ?: FALLBACK_METHOD
    return when {
      "IF_MIN_FRIENDSHIP" in tuple ->
          when {
            "IF_TIME, TIME_NIGHT" in tuple -> 3
            "IF_NOT_TIME, TIME_NIGHT" in tuple -> 2
            else -> 1
          }
      "IF_IN_MAP" in tuple -> if ("ICE" in tuple) LOCATION_ICE else LOCATION_FOREST
      else -> base
    }
  }

  /** Species headers in the info files; the body runs to the next header. */
  private val ENTRY =
      Regex(
          """\[SPECIES_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[SPECIES_[A-Z0-9_]+]\s*=|\Z)""",
          RegexOption.DOT_MATCHES_ALL,
      )

  private val EVOLUTIONS =
      Regex("""\.evolutions\s*=\s*EVOLUTION\((.*?)\)\s*,\s*\n""", RegexOption.DOT_MATCHES_ALL)

  private val EVOLUTION_TUPLE =
      Regex("""\{\s*(EVO_[A-Z0-9_]+)\s*,\s*([A-Z0-9_]+)\s*,\s*SPECIES_([A-Z0-9_]+)""")

  /** The two location-based methods the client itself uses for Leafeon and Glaceon. */
  private const val LOCATION_FOREST = 26
  private const val LOCATION_ICE = 27

  /** Use-item, confirmed by the running client: Eevee stones are ITEM#8. */
  private const val USE_ITEM = 8

  /** Methods with no gen-5 counterpart render as a plain level evolution. */
  private const val FALLBACK_METHOD = 4

  /**
   * Expansion method names to gen-5 ROM method ids. The dump the fixup helper writes on load is the
   * calibration for this table: it prints the client's own value-to-ordinal map next to a known
   * species' entries.
   */
  private val METHODS =
      mapOf(
          "EVO_FRIENDSHIP" to 1,
          "EVO_FRIENDSHIP_DAY" to 2,
          "EVO_FRIENDSHIP_NIGHT" to 3,
          "EVO_LEVEL" to 4,
          "EVO_TRADE" to 5,
          "EVO_TRADE_ITEM" to 6,
          "EVO_ITEM" to 8,
          "EVO_LEVEL_ATK_GT_DEF" to 8,
          "EVO_LEVEL_ATK_EQ_DEF" to 9,
          "EVO_LEVEL_ATK_LT_DEF" to 10,
          "EVO_LEVEL_SILCOON" to 11,
          "EVO_LEVEL_CASCOON" to 12,
          "EVO_LEVEL_NINJASK" to 13,
          "EVO_LEVEL_SHEDINJA" to 14,
          "EVO_BEAUTY" to 15,
          "EVO_ITEM_MALE" to 16,
          "EVO_ITEM_FEMALE" to 17,
          "EVO_LEVEL_DAY" to 4,
          "EVO_LEVEL_NIGHT" to 4,
          "EVO_HOLD_ITEM_DAY" to 18,
          "EVO_HOLD_ITEM_NIGHT" to 19,
          "EVO_MOVE" to 20,
          "EVO_OTHER_PARTY_MON" to 21,
          "EVO_LEVEL_MALE" to 22,
          "EVO_LEVEL_FEMALE" to 23,
          // Fairy-move affection has no gen-5 method; friendship is the chosen stand-in until a
          // Sylveon location exists, matching the friendship-plus-place design for Eevee.
          "EVO_MOVE_TYPE" to 1,
      )

  /** Expansion item symbols to gen-5 item ids, for the item-based methods. */
  private val ITEM_IDS =
      mapOf(
          "ITEM_SUN_STONE" to 80,
          "ITEM_MOON_STONE" to 81,
          "ITEM_FIRE_STONE" to 82,
          "ITEM_THUNDER_STONE" to 83,
          "ITEM_WATER_STONE" to 84,
          "ITEM_LEAF_STONE" to 85,
          "ITEM_SHINY_STONE" to 107,
          "ITEM_DUSK_STONE" to 108,
          "ITEM_DAWN_STONE" to 109,
          "ITEM_OVAL_STONE" to 110,
          "ITEM_KINGS_ROCK" to 221,
          "ITEM_DEEP_SEA_TOOTH" to 226,
          "ITEM_DEEP_SEA_SCALE" to 227,
          "ITEM_METAL_COAT" to 233,
          "ITEM_DRAGON_SCALE" to 235,
          "ITEM_UPGRADE" to 252,
          "ITEM_PROTECTOR" to 321,
          "ITEM_ELECTIRIZER" to 322,
          "ITEM_MAGMARIZER" to 323,
          "ITEM_DUBIOUS_DISC" to 324,
          "ITEM_REAPER_CLOTH" to 325,
          "ITEM_RAZOR_CLAW" to 326,
          "ITEM_RAZOR_FANG" to 327,
          "ITEM_PRISM_SCALE" to 537,
      )
}
