package de.fiereu.openmmo.moves

import de.fiereu.openmmo.common.enums.MoveAdditionalEffect
import de.fiereu.openmmo.common.enums.MoveEffect
import de.fiereu.openmmo.common.enums.MoveFlag
import de.fiereu.openmmo.common.enums.MoveTarget
import de.fiereu.openmmo.common.enums.PokemonType

data class MoveDef(
    val id: Int,
    val name: String,
    val effect: MoveEffect,
    val power: Int,
    val type: PokemonType,
    val accuracy: Int,
    val pp: Int,
    val secondaryEffectChance: Int,
    val target: MoveTarget,
    val priority: Int,
    val flags: Set<MoveFlag> = emptySet(),
    /**
     * The Expansion's per-move argument: which status a `NON_VOLATILE_STATUS` move inflicts, the
     * recoil or absorb percentage, the weather a `WEATHER` move sets, and so on. [MoveArgument.kind]
     * is the union member's name, [MoveArgument.value] the token or number as written.
     */
    val argument: MoveArgument? = null,
    /** What the move does on top of its effect, each with its own chance (100 = always). */
    val additionalEffects: List<AdditionalEffect> = emptyList(),
    /** Fixed hit count (2 Double Kick, 3 Triple Kick, 10 Population Bomb); 0 = one hit. */
    val strikeCount: Int = 0,
    /** Extra critical-hit stages (Slash 1); 0 for an ordinary move. */
    val criticalHitStage: Int = 0,
) {
  fun hasFlag(flag: MoveFlag): Boolean = flag in flags

  /** The argument as a number, for percentages and fixed damage. */
  val argumentValue: Int?
    get() = argument?.value?.toIntOrNull()

  /** The argument as a `MOVE_EFFECT_*` status token. */
  val argumentEffect: MoveAdditionalEffect?
    get() =
        argument?.takeIf { it.value.startsWith("MOVE_EFFECT_") }?.let {
          MoveAdditionalEffect.byConstant(it.value)
        }
}

data class MoveArgument(val kind: String, val value: String)

data class AdditionalEffect(
    val effect: MoveAdditionalEffect,
    val chance: Int,
    val self: Boolean,
    /** For STAT_PLUS / STAT_MINUS: every stat moved (server stat name) with its stage count. */
    val stats: List<Pair<String, Int>> = emptyList(),
)
