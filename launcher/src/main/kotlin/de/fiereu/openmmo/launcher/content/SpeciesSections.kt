package de.fiereu.openmmo.launcher.content

/** A base species record: identity, typing, stats and abilities. Fixed width. */
data class SpeciesRecord(
    val speciesId: Int,
    val type1: Int,
    val type2: Int,
    val stats: List<Int>,
    val abilities: List<Int>,
    val trailer: Int,
) {
  init {
    require(stats.size == STAT_COUNT) { "A species record carries $STAT_COUNT stats" }
    require(abilities.size == ABILITY_COUNT) { "A species record carries $ABILITY_COUNT abilities" }
  }

  companion object {
    const val STAT_COUNT = 6
    const val ABILITY_COUNT = 3
  }
}

object SpeciesCodec : SectionCodec<SpeciesRecord> {
  override val type = 10
  override val name = "species"

  override fun decode(payload: ByteArray): List<SpeciesRecord> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          SpeciesRecord(
              speciesId = reader.short(),
              type1 = reader.byte(),
              type2 = reader.byte(),
              stats = List(SpeciesRecord.STAT_COUNT) { reader.short() },
              abilities = List(SpeciesRecord.ABILITY_COUNT) { reader.short() },
              trailer = reader.short(),
          )
        }
    check(reader.exhausted()) { "species section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<SpeciesRecord>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.speciesId)
              byte(record.type1)
              byte(record.type2)
              record.stats.forEach(::short)
              record.abilities.forEach(::short)
              short(record.trailer)
            }
          }
          .bytes()
}

/** One level-up move: which move, and the level it arrives at. */
data class LevelUpMove(val moveId: Int, val level: Int)

data class LevelUpLearnset(val speciesId: Int, val moves: List<LevelUpMove>) {
  init {
    require(moves.size <= MAX_MOVES) { "A level-up learnset holds at most $MAX_MOVES moves" }
  }

  companion object {
    /** The move count is a single byte. */
    const val MAX_MOVES = 255
  }
}

object LevelUpLearnsetCodec : SectionCodec<LevelUpLearnset> {
  override val type = 1
  override val name = "level-up learnsets"

  override fun decode(payload: ByteArray): List<LevelUpLearnset> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          val speciesId = reader.short()
          val moves = List(reader.byte()) { LevelUpMove(reader.short(), reader.byte()) }
          LevelUpLearnset(speciesId, moves)
        }
    check(reader.exhausted()) { "level-up section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<LevelUpLearnset>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.speciesId)
              byte(record.moves.size)
              record.moves.forEach { move ->
                short(move.moveId)
                byte(move.level)
              }
            }
          }
          .bytes()
}

/**
 * A learnset that is not level-up. The categories are named by the enum inside the client, so these
 * are its terms rather than ours.
 */
data class ExtraLearnset(val speciesId: Int, val category: Int, val moves: List<Int>) {
  companion object {
    const val EGG_MOVES = 0
    const val MOVE_TUTOR = 1
    const val SPECIAL_MOVES = 2
    const val PREVO_MOVES = 3
    const val MOVE_LEARNER_TOOLS = 4
    const val SPECIAL_EGG = 5
    const val ON_EVOLUTION = 6
  }
}

object ExtraLearnsetCodec : SectionCodec<ExtraLearnset> {
  override val type = 2
  override val name = "egg and teachable learnsets"

  override fun decode(payload: ByteArray): List<ExtraLearnset> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          val speciesId = reader.short()
          val category = reader.byte()
          ExtraLearnset(speciesId, category, List(reader.short()) { reader.short() })
        }
    check(reader.exhausted()) { "extra learnset section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<ExtraLearnset>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.speciesId)
              byte(record.category)
              short(record.moves.size)
              record.moves.forEach(::short)
            }
          }
          .bytes()
}

/**
 * The optional-field species record: a flag word followed by one payload per set bit, in bit order.
 * Bits with no payload are pure booleans. Fields that are decodable but not yet understood are kept
 * verbatim so the section still round-trips.
 */
data class SpeciesDetail(
    val speciesId: Int,
    val flags: Int,
    val eggGroups: Pair<Int, Int>? = null,
    val stats: List<Int>? = null,
    val abilities: List<Int>? = null,
    val field010: Int? = null,
    val heldItems: List<Int>? = null,
    val field080: Int? = null,
    val rarity: Int? = null,
    /**
     * Bit 0x400, new in r32645: a count byte then [SPECIAL_VARIANT_BYTES] bytes per entry, kept
     * verbatim. Retail uses it for the Charmander line ("Royal") and Bidoof/Bibarel ("Almighty"):
     * `u8, u16 speciesId, u16 formId, u16, i32 descString, i32 nameString, u8, u8`.
     */
    val specialVariants: List<Int>? = null,
    /**
     * Bit 0x800, read by MonMMO-EX's client code (f/fi7): the ROM personal fields data never
     * carried, so a species data adds has an exp curve, a base exp yield, a height and a weight.
     */
    val romScalars: RomScalars? = null,
    /** Bit 0x1000, MonMMO-EX client code: evolutions appended to the species' list, linked. */
    val evolutions: List<ClientEvolution>? = null,
    /** Bit 0x2000, MonMMO-EX client code: both types, in place (the Fairy retypes). */
    val types: Pair<Int, Int>? = null,
    /**
     * Bit 0x4000, MonMMO-EX client code: u8 dex listing for a species data adds - [LISTED], or
     * [LISTED_OUTSIDE_NATIONAL] for a regional form. Without it the record keeps section 10's hide.
     */
    val dexListing: Int? = null,
) {
  companion object {
    const val ROM_SCALARS = 0x800
    const val EVOLUTIONS = 0x1000
    const val TYPES = 0x2000
    const val DEX_LISTING = 0x4000
    const val LISTED = 1
    const val LISTED_OUTSIDE_NATIONAL = 2
    /** [FIELD_010]'s meaning: the ROM's EV yield word, two bits per stat (f/zp3.mt, decoded by LU). */
    const val EV_YIELD = 0x010
    /** [FIELD_080]'s meaning: the catch rate (f/zp3.bj, the ROM byte after the types). */
    const val CATCH_RATE = 0x080
    const val SPECIAL_VARIANTS = 0x400
    const val SPECIAL_VARIANT_BYTES = 17
    const val EGG_GROUPS = 0x001
    /**
     * Hides the species from the Pokedex (r32645 `zp3.Nm1`): the screen drops a hidden species while
     * it is unseen. Section 10 already hides everything it creates - retail's reserved 1000-1052
     * block stays out of the lists that way - and this bit only ever sets the field, never clears
     * it, so a species we add is listed through [DEX_LISTING].
     */
    const val HIDDEN_FROM_DEX = 0x002
    const val STATS = 0x004
    const val ABILITIES = 0x008
    const val FIELD_010 = 0x010
    const val HELD_ITEMS = 0x020
    /**
     * The second flag that hides a species from the Pokedex, read as `zK0.C30`. The screen skips an
     * entry when either this or [HIDDEN_FROM_DEX] is set, so both have to stay clear.
     */
    const val EXCLUDED_FROM_DEX = 0x040
    const val FIELD_080 = 0x080
    const val RARITY = 0x100
    const val FLAG_200 = 0x200

    const val RARITY_MYTHICAL = 1
    const val RARITY_LEGENDARY = 2
  }
}

/** Growth rate (the ROM's byte, f/o9 key), base exp yield, height in dm and weight in hg. */
data class RomScalars(val growthRate: Int, val baseExp: Int, val height: Int, val weight: Int)

/** One evolution: the client method's ROM key, its parameter as stored and the target species id. */
data class ClientEvolution(
    val method: Int,
    val param: Int,
    val targetId: Int,
    /**
     * The badge byte after the target (see [evolutionTimeCode]): "day" or "night", a time the method
     * key cannot carry (a stone at night), drawn as the friendship methods' sun/moon badge; or
     * "mega", "alpha", "omega" for a Mega or Primal on the evolution tab, drawn as its symbol.
     */
    val time: String? = null,
)

/**
 * The badge byte of a data evolution (MonMMO-EX client code, f/fi7 0x1000; drawn by f/j67): 0 none,
 * 1 day, 2 night, and for the battle form changes on the evolution tab 3 Mega Evolution, 4 alpha
 * (Primal Kyogre), 5 omega (Primal Groudon), which also relabel the entry.
 */
internal fun evolutionTimeCode(time: String?): Int =
    when (time) {
      null -> 0
      "day" -> 1
      "night" -> 2
      "mega" -> 3
      "alpha" -> 4
      "omega" -> 5
      else -> error("Unknown evolution time $time")
    }

internal fun evolutionTime(code: Int): String? =
    when (code) {
      1 -> "day"
      2 -> "night"
      3 -> "mega"
      4 -> "alpha"
      5 -> "omega"
      else -> null
    }

object SpeciesDetailCodec : SectionCodec<SpeciesDetail> {
  override val type = 6
  override val name = "species detail"

  override fun decode(payload: ByteArray): List<SpeciesDetail> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          val speciesId = reader.short()
          val flags = reader.short()
          SpeciesDetail(
              speciesId = speciesId,
              flags = flags,
              eggGroups =
                  if (flags and SpeciesDetail.EGG_GROUPS != 0) reader.byte() to reader.byte()
                  else null,
              stats =
                  if (flags and SpeciesDetail.STATS != 0)
                      List(SpeciesRecord.STAT_COUNT) { reader.short() }
                  else null,
              abilities =
                  if (flags and SpeciesDetail.ABILITIES != 0)
                      List(SpeciesRecord.ABILITY_COUNT) { reader.short() }
                  else null,
              field010 = if (flags and SpeciesDetail.FIELD_010 != 0) reader.short() else null,
              heldItems =
                  if (flags and SpeciesDetail.HELD_ITEMS != 0)
                      List(reader.byte()) { reader.short() }
                  else null,
              field080 = if (flags and SpeciesDetail.FIELD_080 != 0) reader.short() else null,
              rarity = if (flags and SpeciesDetail.RARITY != 0) reader.byte() else null,
              specialVariants =
                  if (flags and SpeciesDetail.SPECIAL_VARIANTS != 0) {
                    val count = reader.byte()
                    listOf(count) +
                        List(count * SpeciesDetail.SPECIAL_VARIANT_BYTES) { reader.byte() }
                  } else null,
              romScalars =
                  if (flags and SpeciesDetail.ROM_SCALARS != 0)
                      RomScalars(reader.byte(), reader.short(), reader.short(), reader.short())
                  else null,
              evolutions =
                  if (flags and SpeciesDetail.EVOLUTIONS != 0)
                      List(reader.byte()) {
                        ClientEvolution(
                            reader.byte(), reader.short(), reader.short(), evolutionTime(reader.byte()))
                      }
                  else null,
              types =
                  if (flags and SpeciesDetail.TYPES != 0) reader.byte() to reader.byte() else null,
              dexListing = if (flags and SpeciesDetail.DEX_LISTING != 0) reader.byte() else null,
          )
        }
    check(reader.exhausted()) { "species detail section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<SpeciesDetail>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.speciesId)
              short(record.flags)
              record.eggGroups?.let {
                byte(it.first)
                byte(it.second)
              }
              record.stats?.forEach(::short)
              record.abilities?.forEach(::short)
              record.field010?.let(::short)
              record.heldItems?.let { items ->
                byte(items.size)
                items.forEach(::short)
              }
              record.field080?.let(::short)
              record.rarity?.let(::byte)
              record.specialVariants?.forEach(::byte)
              record.romScalars?.let {
                byte(it.growthRate)
                short(it.baseExp)
                short(it.height)
                short(it.weight)
              }
              record.evolutions?.let { evolutions ->
                byte(evolutions.size)
                evolutions.forEach {
                  byte(it.method)
                  short(it.param)
                  short(it.targetId)
                  byte(evolutionTimeCode(it.time))
                }
              }
              record.types?.let {
                byte(it.first)
                byte(it.second)
              }
              record.dexListing?.let(::byte)
            }
          }
          .bytes()
}

/** One region Pokedex list. The client stores each species position as its number. */
data class RegionalDex(val regionId: Int, val speciesIds: List<Int>) {
  companion object {
    /** The per-species array of regional numbers is six wide. */
    const val MAX_REGIONS = 6
  }
}

object RegionalDexCodec : SectionCodec<RegionalDex> {
  override val type = 11
  override val name = "regional dex"

  override fun decode(payload: ByteArray): List<RegionalDex> {
    val reader = Reader(payload)
    val records =
        List(reader.byte()) {
          val regionId = reader.byte()
          RegionalDex(regionId, List(reader.short()) { reader.short() })
        }
    check(reader.exhausted()) { "regional dex section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<RegionalDex>): ByteArray =
      Writer()
          .apply {
            byte(records.size)
            records.forEach { record ->
              byte(record.regionId)
              short(record.speciesIds.size)
              record.speciesIds.forEach(::short)
            }
          }
          .bytes()
}
