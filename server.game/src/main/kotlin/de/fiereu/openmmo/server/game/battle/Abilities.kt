package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.MoveFlag
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.moves.MoveDef
import de.fiereu.openmmo.pokemon.SpeciesDef

/**
 * The ability rules the engine consults. Each helper answers one question the turn resolver asks
 * (does this ability stop this status, how much does it scale this hit, does it absorb this
 * type...), so the resolver reads as the cartridge turn and the ability table lives here.
 *
 * Abilities that depend on things the server does not model - held items, forms, terrain,
 * double battles, the party of the other side - are inert and listed in [INERT] so a reader can
 * see the gap.
 */
object Abilities {

  /** The ability a monster carries: slot from the personality's low bit, as the cartridges do. */
  fun of(species: SpeciesDef, mon: Pokemon): Ability {
    val second = species.ability2.takeIf { it != Ability.NONE && it != species.ability1 }
    return if (second != null && (mon.seed and 1) == 1) second else species.ability1
  }

  /** Mold Breaker and its two colours ignore the target's protective abilities. */
  fun ignoresTargetAbilities(attacker: BattleMonState): Boolean =
      attacker.ability in setOf(Ability.MOLD_BREAKER, Ability.TERAVOLT, Ability.TURBOBLAZE, Ability.MYCELIUM_MIGHT)

  /** Air Lock and Cloud Nine switch the weather's effects off while their holder is out. */
  fun weatherNegated(a: BattleMonState, b: BattleMonState): Boolean =
      a.ability in WEATHER_NEGATORS || b.ability in WEATHER_NEGATORS

  private val WEATHER_NEGATORS = setOf(Ability.AIR_LOCK, Ability.CLOUD_NINE)

  fun speedMultiplierPercent(mon: BattleMonState, weather: Weather?): Int =
      when (mon.ability) {
        Ability.SWIFT_SWIM -> if (weather == Weather.RAIN) 200 else 100
        Ability.CHLOROPHYLL -> if (weather == Weather.SUN) 200 else 100
        Ability.SAND_RUSH -> if (weather == Weather.SANDSTORM) 200 else 100
        Ability.SLUSH_RUSH -> if (weather == Weather.HAIL) 200 else 100
        Ability.QUICK_FEET -> if (StatusCondition.hasAny(mon.status)) 150 else 100
        Ability.SLOW_START -> if (mon.slowStartTurns > 0) 50 else 100
        Ability.PROTOSYNTHESIS -> if (weather == Weather.SUN) 150 else 100
        else -> 100
      }

  /** Paralysis quarters speed unless Quick Feet turns it into a boost instead. */
  fun paralysisSlows(mon: BattleMonState): Boolean = mon.ability != Ability.QUICK_FEET

  /** Extra priority an ability grants a move. */
  fun priorityBonus(mon: BattleMonState, move: MoveDef): Int =
      when (mon.ability) {
        Ability.PRANKSTER -> if (move.power == 0) 1 else 0
        Ability.GALE_WINGS -> if (move.type == PokemonType.FLYING && mon.currentHp == mon.maxHp) 1 else 0
        Ability.TRIAGE -> if (move.hasFlag(MoveFlag.HEALING)) 3 else 0
        Ability.STALL -> -7
        else -> 0
      }

  /** True when [status] cannot be placed on [mon] because of its ability. */
  fun blocksStatus(mon: BattleMonState, status: Int, weather: Weather?, fromMove: Boolean): Boolean {
    val a = mon.ability
    if (a == Ability.COMATOSE || a == Ability.PURIFYING_SALT || a == Ability.SHIELDS_DOWN) return true
    if (a == Ability.LEAF_GUARD && weather == Weather.SUN) return true
    if (a == Ability.GOOD_AS_GOLD && fromMove) return true
    return when {
      status and StatusCondition.PARALYSIS != 0 -> a == Ability.LIMBER
      status and (StatusCondition.POISON or StatusCondition.TOXIC) != 0 ->
          a == Ability.IMMUNITY || a == Ability.PASTEL_VEIL
      status and StatusCondition.BURN != 0 ->
          a == Ability.WATER_VEIL || a == Ability.WATER_BUBBLE || a == Ability.THERMAL_EXCHANGE
      status and StatusCondition.FREEZE != 0 -> a == Ability.MAGMA_ARMOR
      status and StatusCondition.SLEEP_MASK != 0 ->
          a == Ability.INSOMNIA || a == Ability.VITAL_SPIRIT || a == Ability.SWEET_VEIL
      else -> false
    }
  }

  fun blocksConfusion(mon: BattleMonState): Boolean = mon.ability == Ability.OWN_TEMPO

  fun blocksFlinch(mon: BattleMonState): Boolean =
      mon.ability == Ability.INNER_FOCUS || mon.ability == Ability.SHIELD_DUST

  /** Stat drops caused by the other side that the ability refuses. */
  fun blocksStatDrop(mon: BattleMonState, stat: BattleStat): Boolean =
      when (mon.ability) {
        Ability.CLEAR_BODY, Ability.WHITE_SMOKE, Ability.FULL_METAL_BODY -> true
        Ability.HYPER_CUTTER -> stat == BattleStat.ATTACK
        Ability.KEEN_EYE, Ability.MINDS_EYE -> stat == BattleStat.ACCURACY
        Ability.BIG_PECKS -> stat == BattleStat.DEFENSE
        else -> false
      }

  /** Blocks every secondary effect of a move used on the holder. */
  fun blocksSecondaryEffects(mon: BattleMonState): Boolean = mon.ability == Ability.SHIELD_DUST

  fun secondaryChanceMultiplier(attacker: BattleMonState): Int =
      if (attacker.ability == Ability.SERENE_GRACE) 2 else 1

  fun noRecoil(mon: BattleMonState): Boolean =
      mon.ability == Ability.ROCK_HEAD || mon.ability == Ability.MAGIC_GUARD

  /** Poison, burn, Leech Seed, weather, curse, trap and recoil damage all skip this holder. */
  fun noIndirectDamage(mon: BattleMonState): Boolean = mon.ability == Ability.MAGIC_GUARD

  fun immuneToSandstorm(mon: BattleMonState): Boolean =
      mon.ability in setOf(Ability.SAND_VEIL, Ability.SAND_RUSH, Ability.SAND_FORCE, Ability.OVERCOAT, Ability.MAGIC_GUARD)

  fun immuneToHail(mon: BattleMonState): Boolean =
      mon.ability in setOf(Ability.ICE_BODY, Ability.SNOW_CLOAK, Ability.SLUSH_RUSH, Ability.OVERCOAT, Ability.MAGIC_GUARD)

  fun noCrits(mon: BattleMonState): Boolean =
      mon.ability == Ability.BATTLE_ARMOR || mon.ability == Ability.SHELL_ARMOR

  /** What happens when a move of [type] meets an absorbing ability; null when nothing does. */
  fun absorb(defender: BattleMonState, type: PokemonType, move: MoveDef): Absorb? =
      when (defender.ability) {
        Ability.LEVITATE -> if (type == PokemonType.GROUND) Absorb.IMMUNE else null
        Ability.EARTH_EATER -> if (type == PokemonType.GROUND) Absorb.HEAL_QUARTER else null
        Ability.VOLT_ABSORB -> if (type == PokemonType.ELECTRIC) Absorb.HEAL_QUARTER else null
        Ability.WATER_ABSORB -> if (type == PokemonType.WATER) Absorb.HEAL_QUARTER else null
        Ability.DRY_SKIN -> if (type == PokemonType.WATER) Absorb.HEAL_QUARTER else null
        Ability.LIGHTNING_ROD -> if (type == PokemonType.ELECTRIC) Absorb.SP_ATTACK_UP else null
        Ability.STORM_DRAIN -> if (type == PokemonType.WATER) Absorb.SP_ATTACK_UP else null
        Ability.MOTOR_DRIVE -> if (type == PokemonType.ELECTRIC) Absorb.SPEED_UP else null
        Ability.SAP_SIPPER -> if (type == PokemonType.GRASS) Absorb.ATTACK_UP else null
        Ability.FLASH_FIRE -> if (type == PokemonType.FIRE) Absorb.FLASH_FIRE else null
        Ability.WELL_BAKED_BODY -> if (type == PokemonType.FIRE) Absorb.DEFENSE_UP_2 else null
        Ability.SOUNDPROOF -> if (move.hasFlag(MoveFlag.SOUND)) Absorb.IMMUNE else null
        Ability.OVERCOAT -> if (move.hasFlag(MoveFlag.POWDER)) Absorb.IMMUNE else null
        Ability.DAMP -> if (move.hasFlag(MoveFlag.EXPLOSION)) Absorb.IMMUNE else null
        Ability.GOOD_AS_GOLD -> if (move.power == 0) Absorb.IMMUNE else null
        else -> null
      }

  enum class Absorb {
    IMMUNE,
    HEAL_QUARTER,
    SP_ATTACK_UP,
    SPEED_UP,
    ATTACK_UP,
    DEFENSE_UP_2,
    FLASH_FIRE,
  }

  /** Scrappy and Mind's Eye let Normal and Fighting moves hit Ghosts. */
  fun hitsGhosts(attacker: BattleMonState): Boolean =
      attacker.ability == Ability.SCRAPPY || attacker.ability == Ability.MINDS_EYE

  /** The type a Normal move becomes under the -ate abilities (with their 20% boost). */
  fun retypedNormal(attacker: BattleMonState): PokemonType? =
      when (attacker.ability) {
        Ability.PIXILATE -> PokemonType.FAIRY
        Ability.REFRIGERATE -> PokemonType.ICE
        Ability.AERILATE -> PokemonType.FLYING
        Ability.GALVANIZE -> PokemonType.ELECTRIC
        else -> null
      }

  /** Accuracy scale in percent from the attacker's own ability. */
  fun accuracyPercent(attacker: BattleMonState, move: MoveDef, physical: Boolean): Int =
      when (attacker.ability) {
        Ability.COMPOUND_EYES -> 130
        Ability.VICTORY_STAR -> 110
        Ability.HUSTLE -> if (physical && move.power > 0) 80 else 100
        else -> 100
      }

  /** Accuracy scale in percent from the defender's ability. */
  fun evasionPercent(defender: BattleMonState, move: MoveDef, weather: Weather?): Int =
      when (defender.ability) {
        Ability.SAND_VEIL -> if (weather == Weather.SANDSTORM) 80 else 100
        Ability.SNOW_CLOAK -> if (weather == Weather.HAIL) 80 else 100
        Ability.TANGLED_FEET -> if (defender.confusionTurns > 0) 50 else 100
        Ability.WONDER_SKIN -> if (move.power == 0 && move.accuracy > 0) 50 else 100
        else -> 100
      }

  /** Extra critical stages from the attacker. */
  fun critStages(attacker: BattleMonState): Int =
      if (attacker.ability == Ability.SUPER_LUCK) 1 else 0

  /**
   * Multiplier (percent) on the damage [attacker] deals with a move of [type], from its own
   * ability, before the defender's reductions.
   */
  fun offensePercent(
      attacker: BattleMonState,
      move: MoveDef,
      type: PokemonType,
      power: Int,
      physical: Boolean,
      weather: Weather?,
      effectiveness: Int,
      movesLast: Boolean,
      defender: BattleMonState,
  ): Int {
    var pct = 100
    val hp = attacker.currentHp
    val max = attacker.maxHp.coerceAtLeast(1)
    when (attacker.ability) {
      Ability.HUGE_POWER, Ability.PURE_POWER -> if (physical) pct = pct * 2
      Ability.GUTS -> if (StatusCondition.hasAny(attacker.status) && physical) pct = pct * 3 / 2
      Ability.HUSTLE -> if (physical) pct = pct * 3 / 2
      Ability.OVERGROW -> if (type == PokemonType.GRASS && hp * 3 <= max) pct = pct * 3 / 2
      Ability.BLAZE -> if (type == PokemonType.FIRE && hp * 3 <= max) pct = pct * 3 / 2
      Ability.TORRENT -> if (type == PokemonType.WATER && hp * 3 <= max) pct = pct * 3 / 2
      Ability.SWARM -> if (type == PokemonType.BUG && hp * 3 <= max) pct = pct * 3 / 2
      Ability.TECHNICIAN -> if (power <= 60) pct = pct * 3 / 2
      Ability.IRON_FIST -> if (move.hasFlag(MoveFlag.PUNCH)) pct = pct * 6 / 5
      Ability.STRONG_JAW -> if (move.hasFlag(MoveFlag.BITE)) pct = pct * 3 / 2
      Ability.TOUGH_CLAWS -> if (move.hasFlag(MoveFlag.MAKES_CONTACT)) pct = pct * 13 / 10
      Ability.RECKLESS -> if (move.argument?.kind == "recoilPercentage") pct = pct * 6 / 5
      Ability.SHEER_FORCE -> if (move.additionalEffects.any { !it.self }) pct = pct * 13 / 10
      Ability.TINTED_LENS -> if (effectiveness in 1 until 10) pct = pct * 2
      Ability.NEUROFORCE -> if (effectiveness > 10) pct = pct * 5 / 4
      Ability.SAND_FORCE ->
          if (weather == Weather.SANDSTORM &&
              (type == PokemonType.ROCK || type == PokemonType.GROUND || type == PokemonType.STEEL))
              pct = pct * 13 / 10
      Ability.ANALYTIC -> if (movesLast) pct = pct * 13 / 10
      Ability.FLARE_BOOST -> if (!physical && StatusCondition.isBurned(attacker.status)) pct = pct * 3 / 2
      Ability.TOXIC_BOOST -> if (physical && StatusCondition.isPoisoned(attacker.status)) pct = pct * 3 / 2
      Ability.SOLAR_POWER -> if (!physical && weather == Weather.SUN) pct = pct * 3 / 2
      Ability.DEFEATIST -> if (hp * 2 <= max) pct = pct / 2
      Ability.SLOW_START -> if (attacker.slowStartTurns > 0 && physical) pct = pct / 2
      Ability.PUNK_ROCK -> if (move.hasFlag(MoveFlag.SOUND)) pct = pct * 13 / 10
      Ability.STEELWORKER, Ability.STEELY_SPIRIT -> if (type == PokemonType.STEEL) pct = pct * 3 / 2
      Ability.TRANSISTOR -> if (type == PokemonType.ELECTRIC) pct = pct * 13 / 10
      Ability.DRAGONS_MAW -> if (type == PokemonType.DRAGON) pct = pct * 3 / 2
      Ability.ROCKY_PAYLOAD -> if (type == PokemonType.ROCK) pct = pct * 3 / 2
      Ability.WATER_BUBBLE -> if (type == PokemonType.WATER) pct = pct * 2
      Ability.PIXILATE, Ability.REFRIGERATE, Ability.AERILATE, Ability.GALVANIZE, Ability.NORMALIZE ->
          if (move.type == PokemonType.NORMAL) pct = pct * 6 / 5
      Ability.RIVALRY ->
          if (attacker.gender != Gender.GENDERLESS && defender.gender != Gender.GENDERLESS) {
            pct = if (attacker.gender == defender.gender) pct * 5 / 4 else pct * 3 / 4
          }
      Ability.GORILLA_TACTICS -> if (physical) pct = pct * 3 / 2
      Ability.ORICHALCUM_PULSE -> if (weather == Weather.SUN && physical) pct = pct * 4 / 3
      Ability.PROTOSYNTHESIS -> if (weather == Weather.SUN) pct = pct * 13 / 10
      Ability.STAKEOUT, Ability.SUPREME_OVERLORD -> Unit
      else -> Unit
    }
    if (attacker.flashFire && type == PokemonType.FIRE) pct = pct * 3 / 2
    return pct
  }

  /** Multiplier (percent) the defender's ability applies to damage it takes. */
  fun defensePercent(
      defender: BattleMonState,
      attacker: BattleMonState,
      move: MoveDef,
      type: PokemonType,
      physical: Boolean,
      effectiveness: Int,
  ): Int {
    if (ignoresTargetAbilities(attacker)) return 100
    var pct = 100
    when (defender.ability) {
      Ability.THICK_FAT -> if (type == PokemonType.FIRE || type == PokemonType.ICE) pct /= 2
      Ability.HEATPROOF -> if (type == PokemonType.FIRE) pct /= 2
      Ability.WATER_BUBBLE -> if (type == PokemonType.FIRE) pct /= 2
      Ability.DRY_SKIN -> if (type == PokemonType.FIRE) pct = pct * 5 / 4
      Ability.FLUFFY -> {
        if (move.hasFlag(MoveFlag.MAKES_CONTACT)) pct /= 2
        if (type == PokemonType.FIRE) pct *= 2
      }
      Ability.FILTER, Ability.SOLID_ROCK, Ability.PRISM_ARMOR -> if (effectiveness > 10) pct = pct * 3 / 4
      Ability.MULTISCALE, Ability.SHADOW_SHIELD -> if (defender.currentHp == defender.maxHp) pct /= 2
      Ability.ICE_SCALES -> if (!physical) pct /= 2
      Ability.PUNK_ROCK -> if (move.hasFlag(MoveFlag.SOUND)) pct /= 2
      Ability.PURIFYING_SALT -> if (type == PokemonType.GHOST) pct /= 2
      Ability.THERMAL_EXCHANGE -> Unit
      else -> Unit
    }
    return pct
  }

  /** Stat multipliers (percent) an ability applies to the raw stat in the formula. */
  fun defenseStatPercent(defender: BattleMonState, attacker: BattleMonState, physical: Boolean): Int {
    if (ignoresTargetAbilities(attacker)) return 100
    return when (defender.ability) {
      Ability.MARVEL_SCALE -> if (physical && StatusCondition.hasAny(defender.status)) 150 else 100
      Ability.FUR_COAT -> if (physical) 200 else 100
      Ability.GRASS_PELT -> 100
      else -> 100
    }
  }

  /** Wonder Guard lets only super-effective damage through. */
  fun wonderGuard(defender: BattleMonState, attacker: BattleMonState, effectiveness: Int, power: Int): Boolean =
      defender.ability == Ability.WONDER_GUARD && power > 0 && effectiveness <= 10 &&
          !ignoresTargetAbilities(attacker)

  /** Sturdy shrugs off OHKO moves and, at full hp, survives any hit with 1 hp. */
  fun sturdy(defender: BattleMonState, attacker: BattleMonState): Boolean =
      defender.ability == Ability.STURDY && !ignoresTargetAbilities(attacker)

  /** Abilities without battle mechanics here (items, forms, terrain, doubles, the bag...). */
  val INERT: Set<Ability> =
      setOf(
          Ability.STICKY_HOLD, Ability.PICKUP, Ability.HONEY_GATHER, Ability.KLUTZ, Ability.UNBURDEN,
          Ability.GLUTTONY, Ability.HARVEST, Ability.CHEEK_POUCH, Ability.RIPEN, Ability.MAGICIAN,
          Ability.PICKPOCKET, Ability.SYMBIOSIS, Ability.FRISK, Ability.FORECAST, Ability.ZEN_MODE,
          Ability.STANCE_CHANGE, Ability.SCHOOLING, Ability.SHIELDS_DOWN, Ability.DISGUISE,
          Ability.ICE_FACE, Ability.POWER_CONSTRUCT, Ability.BATTLE_BOND, Ability.GULP_MISSILE,
          Ability.HUNGER_SWITCH, Ability.ZERO_TO_HERO, Ability.COMMANDER, Ability.MIMICRY,
          Ability.ILLUSION, Ability.IMPOSTER, Ability.FLOWER_GIFT, Ability.MULTITYPE,
          Ability.RKS_SYSTEM, Ability.PLUS, Ability.MINUS, Ability.FRIEND_GUARD, Ability.TELEPATHY,
          Ability.HEALER, Ability.BATTERY, Ability.POWER_SPOT, Ability.FLOWER_VEIL,
          Ability.RECEIVER, Ability.POWER_OF_ALCHEMY, Ability.ELECTRIC_SURGE, Ability.PSYCHIC_SURGE,
          Ability.MISTY_SURGE, Ability.GRASSY_SURGE, Ability.SEED_SOWER, Ability.HADRON_ENGINE,
          Ability.TOXIC_DEBRIS, Ability.WIND_POWER, Ability.ELECTROMORPHOSIS, Ability.QUARK_DRIVE,
          Ability.EMBODY_ASPECT_TEAL_MASK, Ability.EMBODY_ASPECT_HEARTHFLAME_MASK,
          Ability.EMBODY_ASPECT_WELLSPRING_MASK, Ability.EMBODY_ASPECT_CORNERSTONE_MASK,
          Ability.TERA_SHIFT, Ability.TERA_SHELL, Ability.TERAFORM_ZERO, Ability.HOSPITALITY,
          Ability.COSTAR, Ability.CUD_CHEW, Ability.CACOPHONY, Ability.ILLUMINATE, Ability.RUN_AWAY,
      )
}
