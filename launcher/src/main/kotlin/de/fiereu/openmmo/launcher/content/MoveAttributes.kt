package de.fiereu.openmmo.launcher.content

/**
 * The lines the client prints under a move: "Makes contact with the foe.", "Can be prevented with
 * Protect/Detect", "10% chance to Burn the foe".
 *
 * Each is a string id carried in the move's effect list, with the percentages and turn counts as
 * parameters. They are display only - the battle itself is the server's business - but a move
 * without them reads as blank next to every move the client shipped with.
 *
 * The mapping is checked rather than assumed: the client already ships these lines for moves 1-559,
 * so deriving them from the Expansion and diffing against what is in the file measures the mapping
 * directly. See `MoveAttributesTest`.
 */
object MoveAttributes {
  const val MAKES_CONTACT = 3400
  const val PROTECT = 3401
  const val MAGIC_COAT = 3402
  const val SNATCH = 3403
  const val MIRROR_MOVE = 3404
  const val SOUND = 3406
  const val BYPASSES_SUBSTITUTE = 3407
  const val FLINCH = 3500
  const val BURN = 3501
  const val PARALYZE = 3502
  const val FREEZE = 3503
  const val POISON = 3504
  const val BADLY_POISON = 3505
  const val CONFUSE = 3506
  const val SLEEP_RANGE = 3508
  const val RECOIL = 3521
  const val THAWS_USER = 3541
  const val SLICING = 3544
  const val WIND = 3549
  const val MINIMIZE_DOUBLE = 3524
  const val NEVER_MISS_SNOW = 3535
  const val ACCURACY_IN_RAIN = 3536
  const val HITS_UNDERGROUND = 3528
  const val HITS_AIRBORNE_DOUBLE = 3526
  const val HITS_UNDERWATER_DOUBLE = 3527
  const val NEVER_MISS_SAME_TYPE = 3539
  const val DRAIN = 3522

  /** Sleep from a secondary effect always runs one to three turns. */
  private const val CERTAIN = 100

  private const val SLEEP_MIN_TURNS = 1
  private const val SLEEP_MAX_TURNS = 3

  /**
   * The order the client lists attributes in, which is not ascending by id: Snatch precedes
   * Protect, and "Makes contact" sits between the special mechanics and the newer flags.
   *
   * Derived from the client's own 559 move records rather than guessed. Every pair of attributes
   * that appears together does so in a consistent order across all of them - 138 ordered pairs, no
   * contradictions - so a single sequence explains the whole file. `MoveAttributesTest` re-checks
   * that against the shipped data.
   */
  val ORDER =
      listOf(
          3500,
          3504,
          3505,
          3506,
          3507,
          3508,
          3509,
          3521,
          3501,
          3502,
          3503,
          3522,
          3524,
          3525,
          3526,
          3527,
          3528,
          3529,
          3530,
          3531,
          3532,
          3533,
          3535,
          3536,
          3537,
          3538,
          3539,
          3540,
          3541,
          3543,
          3400,
          3544,
          3549,
          3551,
          3542,
          3550,
          3403,
          3552,
          3401,
          3402,
          3404,
          3406,
          3407,
      )

  private val RANK = ORDER.withIndex().associate { (index, id) -> id to index }

  private val STATUS_EFFECTS =
      mapOf(
          "MOVE_EFFECT_FLINCH" to FLINCH,
          "MOVE_EFFECT_BURN" to BURN,
          "MOVE_EFFECT_PARALYSIS" to PARALYZE,
          "MOVE_EFFECT_FREEZE" to FREEZE,
          "MOVE_EFFECT_FREEZE_OR_FROSTBITE" to FREEZE,
          "MOVE_EFFECT_POISON" to POISON,
          "MOVE_EFFECT_TOXIC" to BADLY_POISON,
          "MOVE_EFFECT_CONFUSION" to CONFUSE,
          "MOVE_EFFECT_SLEEP" to SLEEP_RANGE,
      )

  /** Derives a move's attribute lines, ordered by string id as the client's own records are. */
  fun of(move: MoveText.Move): List<MoveEffect> {
    val lines = mutableListOf<MoveEffect>()
    fun plain(id: Int) = lines.add(MoveEffect(id, emptyList()))
    fun withNumbers(id: Int, vararg values: Int) =
        lines.add(
            MoveEffect(
                id,
                values.mapIndexed { slot, value ->
                  MoveEffectParam(slot, MoveEffectParam.SHORTS, shorts = listOf(value))
                },
            ))

    if ("makesContact" in move.flags) plain(MAKES_CONTACT)
    if ("ignoresProtect" !in move.flags) plain(PROTECT)
    if ("magicCoatAffected" in move.flags) plain(MAGIC_COAT)
    if ("snatchAffected" in move.flags) plain(SNATCH)
    if ("mirrorMoveBanned" !in move.flags) plain(MIRROR_MOVE)
    if ("soundMove" in move.flags) plain(SOUND)
    if ("ignoresSubstitute" in move.flags) plain(BYPASSES_SUBSTITUTE)
    if ("thawsUser" in move.flags) plain(THAWS_USER)
    if ("slicingMove" in move.flags) plain(SLICING)
    if ("windMove" in move.flags) plain(WIND)
    if ("minimizeDoubleDamage" in move.flags) plain(MINIMIZE_DOUBLE)
    if ("alwaysHitsInHailSnow" in move.flags) plain(NEVER_MISS_SNOW)
    if ("alwaysHitsInRain" in move.flags) plain(ACCURACY_IN_RAIN)
    if ("damagesUnderground" in move.flags) plain(HITS_UNDERGROUND)
    // The client uses one line whether the move merely reaches a Flying target or doubles on it.
    if ("damagesAirborneDoubleDamage" in move.flags || "damagesAirborne" in move.flags)
        plain(HITS_AIRBORNE_DOUBLE)
    if ("damagesUnderwater" in move.flags) plain(HITS_UNDERWATER_DOUBLE)
    if ("alwaysHitsOnSameType" in move.flags) plain(NEVER_MISS_SAME_TYPE)
    move.recoilPercent?.let { withNumbers(RECOIL, it) }
    // A status move carries its status as the primary effect rather than a chance-based one, so
    // it lands every time. The client spells that 100%.
    if (move.effect == "EFFECT_CONFUSE") withNumbers(CONFUSE, CERTAIN)
    move.nonVolatileStatus?.let { status ->
      when (val id = STATUS_EFFECTS[status]) {
        null -> Unit
        SLEEP_RANGE -> withNumbers(id, CERTAIN, SLEEP_MIN_TURNS, SLEEP_MAX_TURNS)
        else -> withNumbers(id, CERTAIN)
      }
    }
    move.absorbPercent?.let { withNumbers(DRAIN, it) }
    move.additionalEffects.forEach { effect ->
      when (val id = STATUS_EFFECTS[effect.effect]) {
        null -> Unit
        SLEEP_RANGE -> withNumbers(id, effect.chance, SLEEP_MIN_TURNS, SLEEP_MAX_TURNS)
        else -> withNumbers(id, effect.chance)
      }
    }
    return lines.sortedBy { RANK[it.effectId] ?: ORDER.size }
  }
}
