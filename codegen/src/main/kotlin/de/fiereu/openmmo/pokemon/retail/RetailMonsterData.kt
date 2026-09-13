package de.fiereu.openmmo.pokemon.retail

import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.GrowthRate
import de.fiereu.openmmo.pokemon.LevelUpMove
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The client's own DUMP DEX output (`data/pokemmo/monsters.json`, operator-supplied): one record
 * per RETAIL species - complete stats, types, learnsets (outdated, pre-expansion), egg groups and
 * yields, but NO expansion species - plus every wild-encounter row across the five regions. The
 * encounter rows are the operator-designated single source of truth for how and when anything
 * spawns; the species data ranks BELOW the expansion catalogue (the modern truth - Fairy retypes
 * and current tables live there) and above the decomp. Species ids 1-649 are national dex numbers
 * (the same as canonical server ids); 1000+ are the client's form records, kept parsed but not yet
 * applied.
 *
 * Loaded lazily from the working directory (the server launches with CWD = the project dir, same
 * convention as warp-rules.txt); a missing file degrades to "no overrides" so tests and tools
 * without the data keep working on the decomp tables. Parsed through the JsonElement tree API
 * because no module applies the kotlinx-serialization compiler plugin.
 */
object RetailMonsterData {

  data class RetailStats(
      val hp: Int,
      val attack: Int,
      val defense: Int,
      val speed: Int,
      val spAttack: Int,
      val spDefense: Int,
  )

  data class RetailYields(
      val exp: Int,
      val evHp: Int,
      val evAttack: Int,
      val evDefense: Int,
      val evSpeed: Int,
      val evSpAttack: Int,
      val evSpDefense: Int,
  )

  data class RetailEncounter(
      val speciesId: Int,
      val form: Int,
      val type: String,
      val regionId: Int,
      val locationId: Int,
      val locationName: String,
      val minLevel: Int,
      val maxLevel: Int,
      val season: String,
      val isHorde3x: Boolean,
      val isHorde5x: Boolean,
      val rarityFlags: Int,
      val rarityMorning: String,
      val rarityDay: String,
      val rarityNight: String,
  )

  /**
   * One evolution row exactly as the dump spells it: the client evolution-method word (LEVEL, ITEM,
   * HAPPINESS, ATK_GREATER_THAN_DEF...), its parameter (a level, or a CLIENT item id for the item
   * methods), and the target species wire id.
   */
  data class RetailEvolution(val method: String, val value: Int, val toId: Int)

  data class RetailMonster(
      val id: Int,
      val name: String,
      val growthRate: GrowthRate,
      val catchRate: Int,
      val obtainable: Boolean,
      val genderRatio: Int,
      val eggGroups: List<EggGroup>,
      /** [primary, secondary, hidden] ability ids; secondary repeats primary when absent. */
      val primaryAbilityId: Int,
      val secondaryAbilityId: Int,
      val hiddenAbilityId: Int,
      /** Type names in the server's PokemonType enum spelling. */
      val typeNames: List<String>,
      val stats: RetailStats,
      val yields: RetailYields,
      /** Level-up learnset, sorted and deduplicated. */
      val levelUpLearnset: List<LevelUpMove>,
      val encounters: List<RetailEncounter>,
      val evolutions: List<RetailEvolution>,
      /** Hectograms; form records carry their own (Giratina Origin 6500 against Altered 7500). */
      val weight: Int = 0,
  )

  /**
   * Dump egg-group names to the server enum. "genderless" marks the Ditto-only breeders and maps to
   * NONE: the compatibility check strips NONE from group sets, so two of them never intersect
   * (correct - they cannot pair with each other) while the Ditto path ignores groups entirely.
   */
  private val EGG_GROUP_NAMES =
      mapOf(
          "monster" to EggGroup.MONSTER,
          "plant" to EggGroup.GRASS,
          "water a" to EggGroup.WATER_1,
          "water b" to EggGroup.WATER_2,
          "water c" to EggGroup.WATER_3,
          "bug" to EggGroup.BUG,
          "flying" to EggGroup.FLYING,
          "field" to EggGroup.FIELD,
          "fairy" to EggGroup.FAIRY,
          "humanoid" to EggGroup.HUMAN_LIKE,
          "mineral" to EggGroup.MINERAL,
          "chaos" to EggGroup.AMORPHOUS,
          "ditto" to EggGroup.DITTO,
          "dragon" to EggGroup.DRAGON,
          "cannot breed" to EggGroup.NO_EGGS_DISCOVERED,
          "genderless" to EggGroup.NONE,
      )

  private val monsters: Map<Int, RetailMonster> by lazy {
    val path = Path.of(System.getProperty("monmmo.retailData", "data/pokemmo/monsters.json"))
    if (!Files.isRegularFile(path)) {
      System.err.println("[RetailMonsterData] $path not found - decomp tables stay authoritative")
      return@lazy emptyMap()
    }
    val root = Json.parseToJsonElement(Files.readString(path)).jsonArray
    val parsed = root.map { parseMonster(it.jsonObject) }
    System.err.println(
        "[RetailMonsterData] loaded ${parsed.size} species, " +
            "${parsed.sumOf { it.encounters.size }} encounter rows from $path")
    parsed.associateBy { it.id }
  }

  private fun JsonObject.int(key: String, default: Int = 0): Int =
      this[key]?.jsonPrimitive?.content?.toIntOrNull() ?: default

  private fun JsonObject.str(key: String, default: String = ""): String =
      this[key]?.jsonPrimitive?.content ?: default

  private fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
      this[key]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: default

  private fun parseMonster(m: JsonObject): RetailMonster {
    val id = m.int("id")
    val abilities = m["abilities"]?.jsonArray?.map { it.jsonObject.int("id") } ?: emptyList()
    val stats = m["stats"]?.jsonObject
    val yields = m["yields"]?.jsonObject
    val moves =
        m["moves"]?.jsonArray?.map { it.jsonObject }?.filter { it.str("type") == "level" }
            ?: emptyList()
    return RetailMonster(
        id = id,
        name = m.str("name"),
        growthRate = GrowthRate.entries.getOrElse(m.int("exp_type")) { GrowthRate.MEDIUM_FAST },
        catchRate = m.int("catch_rate"),
        obtainable = m.bool("obtainable"),
        genderRatio = m.int("gender_ratio"),
        weight = m.int("weight"),
        eggGroups =
            m["egg_groups"]?.jsonArray?.mapNotNull { EGG_GROUP_NAMES[it.jsonPrimitive.content] }
                ?: emptyList(),
        primaryAbilityId = abilities.getOrElse(0) { 0 },
        secondaryAbilityId = abilities.getOrElse(1) { abilities.getOrElse(0) { 0 } },
        hiddenAbilityId = abilities.getOrElse(2) { 0 },
        typeNames = m["types"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        stats =
            RetailStats(
                hp = stats?.int("hp") ?: 0,
                attack = stats?.int("attack") ?: 0,
                defense = stats?.int("defense") ?: 0,
                speed = stats?.int("speed") ?: 0,
                spAttack = stats?.int("sp_attack") ?: 0,
                spDefense = stats?.int("sp_defense") ?: 0,
            ),
        yields =
            RetailYields(
                exp = yields?.int("exp") ?: 0,
                evHp = yields?.int("ev_hp") ?: 0,
                evAttack = yields?.int("ev_attack") ?: 0,
                evDefense = yields?.int("ev_defense") ?: 0,
                evSpeed = yields?.int("ev_speed") ?: 0,
                evSpAttack = yields?.int("ev_sp_attack") ?: 0,
                evSpDefense = yields?.int("ev_sp_defense") ?: 0,
            ),
        levelUpLearnset =
            moves
                .map { LevelUpMove(it.int("level"), it.int("id")) }
                .distinct()
                .sortedBy { it.level },
        evolutions =
            m["evolutions"]?.jsonArray?.mapNotNull { row ->
              val evo = row.jsonObject
              val method = evo["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
              val target =
                  evo["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
              RetailEvolution(
                  method, evo["val"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0, target)
            } ?: emptyList(),
        encounters =
            m["locations"]?.jsonArray?.map { loc ->
              val l = loc.jsonObject
              RetailEncounter(
                  speciesId = id,
                  form = l.int("form", -1),
                  type = l.str("type"),
                  regionId = l.int("region_id", -1),
                  locationId = l.int("location_id"),
                  locationName = l.str("location_name_full", l.str("location_name")),
                  minLevel = l.int("min_level"),
                  maxLevel = l.int("max_level"),
                  season = l.str("season", "Any"),
                  isHorde3x = l.bool("is_horde_3x"),
                  isHorde5x = l.bool("is_horde_5x"),
                  rarityFlags = l.int("rarity_flags"),
                  rarityMorning = l.str("rarity_morning", "--"),
                  rarityDay = l.str("rarity_day", "--"),
                  rarityNight = l.str("rarity_night", "--"),
              )
            } ?: emptyList(),
    )
  }

  /** The dump record for a canonical species id, or null (missing file, forms, expansion ids). */
  fun get(id: Int): RetailMonster? = monsters[id]

  fun all(): Collection<RetailMonster> = monsters.values

  /** Every wild-encounter row in the dump. */
  fun encounters(): Sequence<RetailEncounter> =
      monsters.values.asSequence().flatMap { it.encounters.asSequence() }

  fun isLoaded(): Boolean = monsters.isNotEmpty()
}
