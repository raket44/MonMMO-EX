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
) {
  companion object {
    const val EGG_GROUPS = 0x001
    /**
     * Hides the species from the Pokedex. The screen reads it as `zK0.JI` and skips the entry
     * outright, which is how the client keeps its own reserved 1000-1052 block out of the dex.
     *
     * Section 10 sets it on everything it creates, so a species we add is hidden unless this bit is
     * left clear here - section 6 runs afterwards and assigns the field from it either way.
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
