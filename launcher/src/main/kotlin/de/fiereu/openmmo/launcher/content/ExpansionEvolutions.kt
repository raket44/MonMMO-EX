package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Evolution data read from the Expansion's species tables, converted to the client's model.
 *
 * The client stores one evolution as `(method, param, target)`. The method is the client enum's
 * ROM key (`f/vj3.v9`, the gen-5 method numbers: 1 happiness, 4 level, 8 item, 21 level knowing a
 * move, 22 level with a species in the party...), read straight off the bytecode on 2026-09-13.
 *
 * The Expansion writes most special evolutions as `EVO_LEVEL` plus `CONDITIONS(...)`, and the
 * conditions carry the meaning: Sylveon is level plus friendship plus a known Fairy move, Kingambit
 * is level after defeating Bisharp holding a Leader's Crest. The conditions choose the client method
 * that says the same thing, and the parameter moves with it (a move id, an item id, a species).
 *
 * A time of day the method key cannot say (a stone at night) rides in [Evolution.time] for the
 * server; region-gated evolutions become night-time ones (see [convert]). A condition no client
 * method describes (recoil damage, steps walked, critical hits in one battle...) gets method 0,
 * which the client draws as a bare arrow and no level check matches, instead of a "Lv. 0" that the
 * server would read as evolving on any level-up.
 */
object ExpansionEvolutions {

  /** Expansion item symbols seen in evolutions with no client item id yet. */
  val unmappedItems = sortedSetOf<String>()

  /**
   * One evolution. [param] carries the ROM value: an item before the client's +5000 shift, a move
   * or a level. When the parameter is a species, [speciesParamSymbol] names it (bare symbol) and
   * [param] is 0 until the caller resolves the client id.
   */
  data class Evolution(
      val method: Int,
      val param: Int,
      val targetSymbol: String,
      val speciesParamSymbol: String? = null,
      /** "day", "night" or null: a time condition the method key does not carry (server only). */
      val time: String? = null,
  )

  /** Parses every species-info file, keyed by bare species symbol. */
  fun parse(expansionRoot: Path): Map<String, List<Evolution>> {
    val moveIds = MoveText.ids(expansionRoot)
    // Items the full import creates (Leader's Crest), by symbol, as ROM values before the shift.
    val importedItems =
        ItemImportPlan.compute(expansionRoot).associate { it.symbol to it.itemId - ROM_ITEM_SHIFT }
    val speciesInfo = expansionRoot.resolve("src/data/pokemon/species_info")
    val result = linkedMapOf<String, List<Evolution>>()
    Files.list(speciesInfo).use { files ->
      files
          .filter { it.name.endsWith(".h") }
          .sorted()
          .forEach { file -> parseFile(Files.readString(file), moveIds, importedItems, result) }
    }
    return result
  }

  private fun parseFile(
      text: String,
      moveIds: Map<String, Int>,
      importedItems: Map<String, Int>,
      into: MutableMap<String, List<Evolution>>,
  ) {
    ENTRY.findAll(text).forEach { entry ->
      val symbol = entry.groupValues[1]
      val block = EVOLUTIONS.find(entry.groupValues[2])?.groupValues?.get(1) ?: return@forEach
      val evolutions = entries(block).mapNotNull { tuple -> convert(tuple, moveIds, importedItems) }
      if (evolutions.isNotEmpty()) into[symbol] = evolutions
    }
  }

  /**
   * One `{EVO_x, param, SPECIES_y, CONDITIONS(...)}` entry in the client's terms, or null.
   *
   * An evolution the Expansion gates on a region happens at night here (project owner, 2026-09-13):
   * this game has no Alola, and night splits Pikachu's Thunder Stone between Raichu and Alolan Raichu,
   * Cubone's level 28 between Marowak and Alolan Marowak, Quilava's 36 into Hisuian Typhlosion.
   */
  internal fun convert(
      tuple: String,
      moveIds: Map<String, Int>,
      importedItems: Map<String, Int> = emptyMap(),
  ): Evolution? {
    val head = EVOLUTION_TUPLE.find(tuple) ?: return null
    val methodSymbol = head.groupValues[1]
    val rawParam = head.groupValues[2].trim()
    val target = head.groupValues[3]
    val conditions = CONDITION.findAll(tuple).associate { match ->
      match.groupValues[1] to match.groupValues[2].split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
    fun argument(name: String, index: Int = 0) = conditions[name]?.getOrNull(index)
    val level = rawParam.toIntOrNull() ?: 0
    val night = argument("IF_TIME") == "TIME_NIGHT"
    val day = argument("IF_TIME") == "TIME_DAY" || argument("IF_NOT_TIME") == "TIME_NIGHT"
    val female = argument("IF_GENDER") == "MON_FEMALE"
    val male = argument("IF_GENDER") == "MON_MALE"

    fun special() = Evolution(SPECIAL, 0, target)
    fun item(itemSymbol: String?, method: Int): Evolution {
      val id = itemSymbol?.let { ITEM_IDS[it] ?: EvoItemPlan.evolutionParam(it) ?: importedItems[it] }
      if (id == null) {
        itemSymbol?.let(unmappedItems::add)
        return special()
      }
      return Evolution(method, id, target)
    }
    fun move(moveSymbol: String?): Evolution {
      val id = moveSymbol?.let(moveIds::get) ?: return special()
      return Evolution(LEVEL_WITH_MOVE, CANONICAL_MOVE_IDS[id] ?: id, target)
    }

    return when (methodSymbol) {
      "EVO_NONE" -> null
      // An item that is never created stands in for something the game does natively (the Linking
      // Cord for a trade): the species' own entry for that already exists, so this one is dropped.
      "EVO_ITEM" ->
          if (rawParam in EvoItemPlan.NOT_CREATED) null
          else item(rawParam, if (male) ITEM_MALE else if (female) ITEM_FEMALE else ITEM)
      "EVO_TRADE" ->
          when {
            "IF_HOLD_ITEM" in conditions -> item(argument("IF_HOLD_ITEM"), TRADE_WITH_ITEM)
            "IF_TRADE_PARTNER_SPECIES" in conditions ->
                Evolution(
                    TRADE_FOR_SPECIES, 0, target,
                    speciesParamSymbol = argument("IF_TRADE_PARTNER_SPECIES")?.removePrefix("SPECIES_"))
            else -> Evolution(TRADE, 0, target)
          }
      "EVO_SPLIT_FROM_EVO" -> Evolution(SHEDINJA, 0, target)
      "EVO_LEVEL", "EVO_LEVEL_BATTLE_ONLY", "EVO_BATTLE_END" ->
          when {
            "IF_MIN_FRIENDSHIP" in conditions ->
                Evolution(if (night) HAPPINESS_NIGHT else if (day) HAPPINESS_DAY else HAPPINESS, 0, target)
            "IF_HOLD_ITEM" in conditions ->
                item(argument("IF_HOLD_ITEM"), if (night) LEVEL_ITEM_NIGHT else LEVEL_ITEM_DAY)
            "IF_DEFEAT_X_WITH_ITEMS" in conditions ->
                item(argument("IF_DEFEAT_X_WITH_ITEMS", 1), LEVEL_ITEM_DAY)
            "IF_KNOWS_MOVE" in conditions -> move(argument("IF_KNOWS_MOVE"))
            "IF_USED_MOVE_X_TIMES" in conditions -> move(argument("IF_USED_MOVE_X_TIMES"))
            "IF_SPECIES_IN_PARTY" in conditions ->
                Evolution(
                    LEVEL_WITH_SPECIES, 0, target,
                    speciesParamSymbol = argument("IF_SPECIES_IN_PARTY")?.removePrefix("SPECIES_"))
            "IF_ATK_GT_DEF" in conditions -> Evolution(ATK_GT_DEF, level, target)
            "IF_ATK_EQ_DEF" in conditions -> Evolution(ATK_EQ_DEF, level, target)
            "IF_ATK_LT_DEF" in conditions -> Evolution(ATK_LT_DEF, level, target)
            "IF_PID_UPPER_MODULO_10_GT" in conditions -> Evolution(PERSONALITY_HIGH, level, target)
            "IF_PID_UPPER_MODULO_10_LT" in conditions || "IF_PID_UPPER_MODULO_10_EQ" in conditions ->
                Evolution(PERSONALITY_LOW, level, target)
            "IF_MIN_BEAUTY" in conditions -> Evolution(BEAUTY, 0, target)
            "IF_IN_MAP" in conditions || "IF_IN_MAPSEC" in conditions -> {
              val place = (argument("IF_IN_MAP") ?: argument("IF_IN_MAPSEC")).orEmpty()
              val method =
                  when {
                    MAGNETIC_PLACES.any { it in place } -> LOCATION_MAGNETIC
                    "ICE" in place || "SNOW" in place -> LOCATION_ICE
                    else -> LOCATION_MOSS
                  }
              Evolution(method, 0, target)
            }
            male && level > 0 -> Evolution(LEVEL_MALE, level, target)
            female && level > 0 -> Evolution(LEVEL_FEMALE, level, target)
            // A plain level, or a level with a condition the client has no word for (time of
            // day, weather, a type in the party): the level is still the rule players meet.
            level > 0 -> Evolution(LEVEL, level, target)
            else -> special()
          }
      else -> special()
    }?.let { evolution ->
      val time =
          when {
            evolution.method in TIMED_METHODS -> null
            "IF_REGION" in conditions || night -> "night"
            day -> "day"
            else -> null
          }
      evolution.copy(time = time)
    }
  }

  /** Methods whose key already says the time of day. */
  private val TIMED_METHODS = setOf(HAPPINESS_DAY, HAPPINESS_NIGHT, LEVEL_ITEM_DAY, LEVEL_ITEM_NIGHT)

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

  /** The ROM loader adds this to item parameters; the evolution tables carry the value before it. */
  private const val ROM_ITEM_SHIFT = 5000

  /** One `{IF_x, arg, ...}` inside CONDITIONS(...). */
  private val CONDITION = Regex("""\{\s*(IF_[A-Z0-9_]+)\s*(?:,([^{}]*))?\}""")

  /** Map names the Expansion uses for its magnetic-field evolutions (Magneton, Nosepass). */
  private val MAGNETIC_PLACES = listOf("MAGNET", "CORONET", "CHARGESTONE", "NEW_MAUVILLE", "VAST_POKE")

  /** The client move ids that stand for Expansion duplicates (see PorymovesLearnsets). */
  private val CANONICAL_MOVE_IDS = mapOf(809 to 258, 567 to 1000, 680 to 1019)

  // The client's method keys (f/vj3: ordinal == ROM key for every constant).
  const val SPECIAL = 0
  const val HAPPINESS = 1
  const val HAPPINESS_DAY = 2
  const val HAPPINESS_NIGHT = 3
  const val LEVEL = 4
  const val TRADE = 5
  const val TRADE_WITH_ITEM = 6
  const val TRADE_FOR_SPECIES = 7
  const val ITEM = 8
  const val ATK_GT_DEF = 9
  const val ATK_EQ_DEF = 10
  const val ATK_LT_DEF = 11
  const val PERSONALITY_HIGH = 12
  const val PERSONALITY_LOW = 13
  const val SHEDINJA = 15
  const val BEAUTY = 16
  const val ITEM_MALE = 17
  const val ITEM_FEMALE = 18
  const val LEVEL_ITEM_DAY = 19
  const val LEVEL_ITEM_NIGHT = 20
  const val LEVEL_WITH_MOVE = 21
  const val LEVEL_WITH_SPECIES = 22
  const val LEVEL_MALE = 23
  const val LEVEL_FEMALE = 24
  const val LOCATION_MAGNETIC = 25
  const val LOCATION_MOSS = 26
  const val LOCATION_ICE = 27

  /**
   * Methods whose parameter is an item: the ROM loader (v67.SB) adds 5000 to exactly these, so a
   * record written as data must carry the shifted value itself.
   */
  val ITEM_METHODS = setOf(TRADE_WITH_ITEM, ITEM, ITEM_MALE, ITEM_FEMALE, LEVEL_ITEM_DAY, LEVEL_ITEM_NIGHT)

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
