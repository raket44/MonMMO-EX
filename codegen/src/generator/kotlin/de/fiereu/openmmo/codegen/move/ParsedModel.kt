package de.fiereu.openmmo.codegen.move

data class ParsedMove(
    val id: Int,
    val name: String,
    val effect: String,
    val power: Int,
    val type: String,
    val accuracy: Int,
    val pp: Int,
    val secondaryEffectChance: Int,
    val target: String,
    val priority: Int,
    val flags: List<String>,
    /** The `.argument = { .kind = value }` pair, tokens as written (ternaries resolved). */
    val argumentKind: String? = null,
    val argument: String? = null,
    val additionalEffects: List<ParsedAdditionalEffect> = emptyList(),
    /** Fixed hit count (2 Double Kick, 3 Triple Kick, 10 Population Bomb); 0 = one hit. */
    val strikeCount: Int = 0,
    /** Extra critical-hit stages (Slash 1); 0 for an ordinary move. */
    val criticalHitStage: Int = 0,
    /** PHYSICAL, SPECIAL or STATUS, from `.category = DAMAGE_CATEGORY_*`. */
    val category: String = "STATUS",
)

/**
 * One `ADDITIONAL_EFFECTS` entry: the `MOVE_EFFECT_*` token, its chance, self flag, and for the
 * stat effects every stat it moves with its stage count (Bulk Up lists two, Ancient Power five).
 */
data class ParsedAdditionalEffect(
    val effect: String,
    val chance: Int,
    val self: Boolean,
    val stats: List<Pair<String, Int>> = emptyList(),
)
