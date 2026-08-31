package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.pokemon.SpeciesDef
import kotlin.math.sqrt

/** Gen 1 stat formulas: DVs (IV/2, 0-15), Stat Exp (sqrt-scaled), no natures. */
object Gen1StatCalculator {

  private fun statExp(ev: Int): Int = sqrt(ev * 65535.0 / 252).toInt().coerceAtMost(255)

  fun maxHp(base: Int, iv: Int, ev: Int, level: Int): Int {
    val dv = iv / 2
    return (base + dv) * 2 * level / 100 + statExp(ev) * level / 100 + level + 10
  }

  fun stat(base: Int, iv: Int, ev: Int, level: Int): Int {
    val dv = iv / 2
    return (base + dv) * 2 * level / 100 + statExp(ev) * level / 100 + 5
  }

  /**
   * @param splitSpecial true for Gen 2 (separate SpAtk/SpDef), false for Gen 1 (Special = both).
   */
  fun computeAll(
      species: SpeciesDef,
      pokemon: Pokemon,
      splitSpecial: Boolean = false
  ): ComputedStats {
    val level = pokemon.level.toInt()
    val ivs = pokemon.iVs
    val evs = pokemon.eVs
    val special = stat(species.baseSpAttack, ivs.spAtk, evs.spAtk, level)
    return ComputedStats(
        hp = maxHp(species.baseHp, ivs.hp, evs.hp, level),
        atk = stat(species.baseAttack, ivs.atk, evs.atk, level),
        def = stat(species.baseDefense, ivs.def, evs.def, level),
        spAtk = special,
        spDef =
            if (splitSpecial) stat(species.baseSpDefense, ivs.spDef, evs.spDef, level) else special,
        spd = stat(species.baseSpeed, ivs.spd, evs.spd, level),
    )
  }
}

/** Gen 1 critical hit: roll < speed/2 (capped at 255). */
object Gen1CritRate {
  fun threshold(speed: Int): Int = (speed / 2).coerceAtMost(255)

  fun isCrit(roll: Int, speed: Int): Boolean = roll < threshold(speed)
}
