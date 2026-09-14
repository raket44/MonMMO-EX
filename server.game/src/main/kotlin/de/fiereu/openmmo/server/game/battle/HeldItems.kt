package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.MoveFlag
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.items.generated.Items
import de.fiereu.openmmo.moves.MoveDef

/**
 * The held-item rules the engine consults. Items are matched by the catalogue's own instances
 * (`Items.LEFTOVERS` and friends) after the monster's client item id is resolved through the
 * registry, so every id a name answers to behaves the same.
 */
class HeldItems(private val registry: ItemRegistry) {

  /** The item [mon] gets the effect of: none under Klutz, Embargo or Magic Room, none once consumed. */
  fun of(mon: BattleMonState): ItemDef? {
    if (mon.heldItem == 0 || mon.ability == Ability.KLUTZ || mon.embargoTurns > 0 || mon.inMagicRoom) return null
    return registry.get(mon.heldItem)
  }

  private class ItemBattleData(val flingPower: Int, val giftType: PokemonType?, val giftPower: Int)

  /** Fling power and Natural Gift type/power by item name, extracted from the Expansion (monmmo/item-battle-data.csv). */
  private val battleData: Map<String, ItemBattleData> by lazy {
    val stream = HeldItems::class.java.getResourceAsStream("/monmmo/item-battle-data.csv") ?: return@lazy emptyMap()
    stream.bufferedReader().useLines { lines ->
      lines
          .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("name,") }
          .associate { line ->
            val cols = line.split(",")
            val type = cols.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { runCatching { PokemonType.valueOf(it) }.getOrNull() }
            cols[0] to ItemBattleData(cols.getOrNull(1)?.toIntOrNull() ?: 0, type, cols.getOrNull(3)?.toIntOrNull() ?: 0)
          }
    }
  }

  /** Fling's power with [item]; 0 when it cannot be flung. */
  fun flingPower(item: ItemDef?): Int = item?.let { battleData[it.name]?.flingPower } ?: 0

  /** Natural Gift's type and power with the berry [item], or null for anything else. */
  fun naturalGift(item: ItemDef?): Pair<PokemonType, Int>? =
      item?.let { battleData[it.name] }?.let { data -> data.giftType?.let { it to data.giftPower } }

  /** Judgment's type: the held plate's. */
  fun plateType(item: ItemDef?): PokemonType? = if (item != null && item.name.endsWith(" Plate")) boostedType(item) else null

  /** Techno Blast's type: the held drive's. */
  fun driveType(item: ItemDef?): PokemonType? =
      when (item) {
        Items.DOUSE_DRIVE -> PokemonType.WATER
        Items.SHOCK_DRIVE -> PokemonType.ELECTRIC
        Items.BURN_DRIVE -> PokemonType.FIRE
        Items.CHILL_DRIVE -> PokemonType.ICE
        else -> null
      }

  fun isBerry(item: ItemDef?): Boolean = item != null && item.name.endsWith(" Berry")

  /** Type-boosting held items: the type they boost (20%). */
  fun boostedType(item: ItemDef?): PokemonType? =
      when (item) {
        Items.CHARCOAL, Items.FLAME_PLATE -> PokemonType.FIRE
        Items.MYSTIC_WATER, Items.SPLASH_PLATE, Items.SEA_INCENSE, Items.WAVE_INCENSE -> PokemonType.WATER
        Items.MAGNET, Items.ZAP_PLATE -> PokemonType.ELECTRIC
        Items.MIRACLE_SEED, Items.MEADOW_PLATE, Items.ROSE_INCENSE -> PokemonType.GRASS
        Items.NEVERMELTICE, Items.ICICLE_PLATE -> PokemonType.ICE
        Items.BLACK_BELT, Items.FIST_PLATE -> PokemonType.FIGHTING
        Items.POISON_BARB, Items.TOXIC_PLATE -> PokemonType.POISON
        Items.SOFT_SAND, Items.EARTH_PLATE -> PokemonType.GROUND
        Items.SHARP_BEAK, Items.SKY_PLATE -> PokemonType.FLYING
        Items.TWISTEDSPOON, Items.MIND_PLATE, Items.ODD_INCENSE -> PokemonType.PSYCHIC
        Items.SILVERPOWDER, Items.INSECT_PLATE -> PokemonType.BUG
        Items.HARD_STONE, Items.STONE_PLATE, Items.ROCK_INCENSE -> PokemonType.ROCK
        Items.SPELL_TAG, Items.SPOOKY_PLATE -> PokemonType.GHOST
        Items.DRAGON_FANG, Items.DRACO_PLATE -> PokemonType.DRAGON
        Items.BLACKGLASSES, Items.DREAD_PLATE -> PokemonType.DARK
        Items.METAL_COAT, Items.IRON_PLATE -> PokemonType.STEEL
        Items.SILK_SCARF -> PokemonType.NORMAL
        else -> null
      }

  /** The type a resist berry halves a super-effective hit of; Chilan halves Normal outright. */
  fun resistBerryType(item: ItemDef?): PokemonType? =
      when (item) {
        Items.OCCA_BERRY -> PokemonType.FIRE
        Items.PASSHO_BERRY -> PokemonType.WATER
        Items.WACAN_BERRY -> PokemonType.ELECTRIC
        Items.RINDO_BERRY -> PokemonType.GRASS
        Items.YACHE_BERRY -> PokemonType.ICE
        Items.CHOPLE_BERRY -> PokemonType.FIGHTING
        Items.KEBIA_BERRY -> PokemonType.POISON
        Items.SHUCA_BERRY -> PokemonType.GROUND
        Items.COBA_BERRY -> PokemonType.FLYING
        Items.PAYAPA_BERRY -> PokemonType.PSYCHIC
        Items.TANGA_BERRY -> PokemonType.BUG
        Items.CHARTI_BERRY -> PokemonType.ROCK
        Items.KASIB_BERRY -> PokemonType.GHOST
        Items.HABAN_BERRY -> PokemonType.DRAGON
        Items.COLBUR_BERRY -> PokemonType.DARK
        Items.BABIRI_BERRY -> PokemonType.STEEL
        Items.CHILAN_BERRY -> PokemonType.NORMAL
        else -> null
      }

  /** The status bits a berry cures on the spot; Lum cures everything (and confusion). */
  fun curedStatus(item: ItemDef?): Int =
      when (item) {
        Items.CHERI_BERRY -> StatusCondition.PARALYSIS
        Items.CHESTO_BERRY -> StatusCondition.SLEEP_MASK
        Items.PECHA_BERRY -> StatusCondition.POISON or StatusCondition.TOXIC
        Items.RAWST_BERRY -> StatusCondition.BURN
        Items.ASPEAR_BERRY -> StatusCondition.FREEZE
        Items.LUM_BERRY -> 0xFF
        else -> 0
      }

  fun curesConfusion(item: ItemDef?): Boolean = item == Items.PERSIM_BERRY || item == Items.LUM_BERRY

  /** Pinch berries: the stat they raise by one stage at a quarter hp. */
  fun pinchStat(item: ItemDef?): BattleStat? =
      when (item) {
        Items.LIECHI_BERRY -> BattleStat.ATTACK
        Items.GANLON_BERRY -> BattleStat.DEFENSE
        Items.SALAC_BERRY -> BattleStat.SPEED
        Items.PETAYA_BERRY -> BattleStat.SP_ATTACK
        Items.APICOT_BERRY -> BattleStat.SP_DEFENSE
        else -> null
      }

  /** Healing berries and Berry Juice at half hp: the fraction (of max hp) or flat amount. */
  fun halfHpHeal(item: ItemDef?, maxHp: Int): Int =
      when (item) {
        Items.ORAN_BERRY -> 10
        Items.BERRY_JUICE -> 20
        Items.SITRUS_BERRY -> maxHp / 4
        else -> 0
      }

  /** The flavour berries heal a third at a quarter hp (confusing the wrong natures is not modelled). */
  fun quarterHpHeal(item: ItemDef?, maxHp: Int): Int =
      when (item) {
        Items.FIGY_BERRY, Items.WIKI_BERRY, Items.MAGO_BERRY, Items.AGUAV_BERRY, Items.IAPAPA_BERRY -> maxHp / 3
        else -> 0
      }

  fun speedPercent(mon: BattleMonState): Int {
    var pct = if (mon.unburdened) 200 else 100
    when (of(mon)) {
      Items.CHOICE_SCARF -> pct = pct * 3 / 2
      Items.IRON_BALL, Items.MACHO_BRACE, Items.POWER_BRACER, Items.POWER_BELT, Items.POWER_LENS,
      Items.POWER_BAND, Items.POWER_ANKLET, Items.POWER_WEIGHT -> pct /= 2
      else -> Unit
    }
    return pct
  }

  fun accuracyPercent(attacker: BattleMonState, defender: BattleMonState, slower: Boolean): Int {
    var pct = 100
    when (of(attacker)) {
      Items.WIDE_LENS -> pct = pct * 11 / 10
      Items.ZOOM_LENS -> if (slower) pct = pct * 6 / 5
      else -> Unit
    }
    if (attacker.micleBoost) pct = pct * 6 / 5
    when (of(defender)) {
      Items.BRIGHTPOWDER, Items.LAX_INCENSE -> pct = pct * 9 / 10
      else -> Unit
    }
    return pct
  }

  fun critStages(attacker: BattleMonState): Int =
      when (of(attacker)) {
        Items.SCOPE_LENS, Items.RAZOR_CLAW -> 1
        Items.LANSAT_BERRY -> 0
        else -> 0
      }

  /** Percent scale on damage dealt from the attacker's item. */
  fun offensePercent(
      attacker: BattleMonState,
      move: MoveDef,
      type: PokemonType,
      physical: Boolean,
      effectiveness: Int,
  ): Int {
    val item = of(attacker) ?: return 100
    var pct = 100
    if (boostedType(item) == type) pct = pct * 6 / 5
    when (item) {
      Items.CHOICE_BAND -> if (physical) pct = pct * 3 / 2
      Items.CHOICE_SPECS -> if (!physical) pct = pct * 3 / 2
      Items.LIFE_ORB -> pct = pct * 13 / 10
      Items.EXPERT_BELT -> if (effectiveness > 10) pct = pct * 6 / 5
      Items.MUSCLE_BAND -> if (physical) pct = pct * 11 / 10
      Items.WISE_GLASSES -> if (!physical) pct = pct * 11 / 10
      Items.LIGHT_BALL -> if (attacker.species.id == PIKACHU) pct *= 2
      Items.THICK_CLUB -> if (physical && (attacker.species.id == CUBONE || attacker.species.id == MAROWAK)) pct *= 2
      Items.DEEPSEATOOTH -> if (!physical && attacker.species.id == CLAMPERL) pct *= 2
      Items.SOUL_DEW -> if (!physical && (attacker.species.id == LATIAS || attacker.species.id == LATIOS)) pct = pct * 3 / 2
      else -> Unit
    }
    return pct
  }

  /** Percent scale on damage taken from the defender's item (resist berries are handled apart). */
  fun defensePercent(defender: BattleMonState, physical: Boolean, canEvolve: Boolean): Int {
    val item = of(defender) ?: return 100
    return when (item) {
      Items.EVIOLITE -> if (canEvolve) 100 * 2 / 3 else 100
      Items.METAL_POWDER -> if (defender.species.id == DITTO) 100 * 2 / 3 else 100
      Items.DEEPSEASCALE -> if (!physical && defender.species.id == CLAMPERL) 50 else 100
      Items.SOUL_DEW -> if (!physical && (defender.species.id == LATIAS || defender.species.id == LATIOS)) 100 * 2 / 3 else 100
      else -> 100
    }
  }

  fun blocksContactEffects(attacker: BattleMonState): Boolean = false

  fun flinchChance(attacker: BattleMonState): Int =
      when (of(attacker)) {
        Items.KING_S_ROCK, Items.RAZOR_FANG -> 10
        else -> 0
      }

  fun drainPercent(attacker: BattleMonState): Int = if (of(attacker) == Items.BIG_ROOT) 130 else 100

  fun trapDamageDivisor(attacker: BattleMonState): Int = if (of(attacker) == Items.BINDING_BAND) 6 else 8

  fun trapTurns(attacker: BattleMonState, rolled: Int): Int = if (of(attacker) == Items.GRIP_CLAW) 7 else rolled

  fun screenTurns(user: BattleMonState): Int = if (of(user) == Items.LIGHT_CLAY) 8 else 5

  fun weatherTurns(user: BattleMonState, weather: Weather): Int =
      when (weather) {
        Weather.RAIN -> if (of(user) == Items.DAMP_ROCK) 8 else 5
        Weather.SUN -> if (of(user) == Items.HEAT_ROCK) 8 else 5
        Weather.SANDSTORM -> if (of(user) == Items.SMOOTH_ROCK) 8 else 5
        Weather.HAIL -> if (of(user) == Items.ICY_ROCK) 8 else 5
      }

  /** Whether a Choice item locks the holder into its first move. */
  fun isChoice(item: ItemDef?): Boolean =
      item == Items.CHOICE_BAND || item == Items.CHOICE_SPECS || item == Items.CHOICE_SCARF

  fun canBeTaken(holder: BattleMonState): Boolean =
      holder.heldItem != 0 && holder.ability != Ability.STICKY_HOLD

  /** Rewards from the item catalogue that should not travel between monsters (mail, key items). */
  fun isTradable(item: ItemDef?): Boolean = item != null

  fun moveIsContact(move: MoveDef, attacker: BattleMonState): Boolean =
      move.hasFlag(MoveFlag.MAKES_CONTACT) && attacker.ability != Ability.LONG_REACH

  fun idOf(item: ItemDef): Int = registry.idsOf(item).firstOrNull() ?: 0

  fun get(id: Int): ItemDef? = registry.get(id)

  private companion object {
    const val PIKACHU = 25
    const val CUBONE = 104
    const val MAROWAK = 105
    const val CLAMPERL = 366
    const val DITTO = 132
    const val LATIAS = 380
    const val LATIOS = 381
  }
}
