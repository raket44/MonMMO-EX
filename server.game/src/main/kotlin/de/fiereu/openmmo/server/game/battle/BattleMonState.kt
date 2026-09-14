package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.net.game.packets.battle.BattleMonBlock
import de.fiereu.openmmo.net.game.packets.battle.BattleOpponentBlock
import de.fiereu.openmmo.pokemon.SpeciesDef
import java.util.EnumMap

/**
 * One monster's live state inside a battle. [source] is the snapshot the battle started from.
 * [currentHp] and the move PP are the live values written back when the battle ends.
 */
class BattleMonState(
    val entityId: Long,
    /** The species in effect; form changes swap it for the battle. */
    var species: SpeciesDef,
    val partyIndex: Int?,
    // Both move on when a reward lands, so a second reward in the same battle builds on the first.
    var source: Pokemon,
    var stats: ComputedStats,
    /** 0 male, 1 female, -1 genderless - the client's codes, derived the client's way. */
    val gender: Byte = Gender.of(species.genderRatio, source.seed),
) {
  /** The species the battle started with; forms revert to it on faint and at the end. */
  val originalSpecies: SpeciesDef = species

  var currentHp: Int = source.hp.toInt().coerceIn(0, stats.hp)

  /** Non-volatile status in the record's bit layout; carried in and out of the battle. */
  var status: Int = source.status and 0xFF

  /** The ability in effect; Trace, Skill Swap and the like change it for the battle. */
  var ability: de.fiereu.openmmo.common.enums.Ability = Abilities.of(species, source)

  // Ability bookkeeping.
  var truantLoafs: Boolean = false
  var slowStartTurns: Int = 0
  var flashFire: Boolean = false

  /** Client item id of the held item; consumed, stolen and swapped in battle, written back after. */
  var heldItem: Int = source.heldItem
  /** The last item this monster used up, for Harvest, Recycle and Unburden. */
  var consumedItem: Int = 0
  var unburdened: Boolean = false
  var airBalloonPopped: Boolean = false
  /** The move a Choice item locked this monster into (0 = free). */
  var choiceLockedMove: Int = 0
  var custapReady: Boolean = false
  var micleBoost: Boolean = false
  /** Item id that let it move first this turn (Quick Claw, Custap Berry), for the line. */
  var movedFirstByItem: Int = 0
  /** Trapping move bookkeeping set by the trapper's item. */
  var trapDamageDivisor: Int = 8
  /** Illusion: the party member this monster is showing itself as, until it takes a hit. */
  var illusionOf: BattleMonState? = null
  /** Transform and Imposter: the moves it had before copying its target's, back when it leaves the field. */
  var transformedFrom: List<PokemonMove>? = null
  val transformed: Boolean
    get() = transformedFrom != null

  // Move-made volatile state (the Expansion's volatiles), cleared when the monster leaves the field.
  /** Hp left in a Substitute; 0 = none. */
  var substituteHp: Int = 0
  var tauntTurns: Int = 0
  var encoreTurns: Int = 0
  var encoreMoveId: Int = 0
  var disableTurns: Int = 0
  var disabledMoveId: Int = 0
  var tormented: Boolean = false
  /** Attract: the monster it fell for. */
  var infatuatedWith: BattleMonState? = null
  var destinyBond: Boolean = false
  /** Perish Song: end-of-turns left, fainting when it reaches 0; 0 = not counting. */
  var perishCount: Int = 0
  var aquaRing: Boolean = false
  var magnetRiseTurns: Int = 0
  var healBlockTurns: Int = 0
  /** Smack Down pulled it to the ground. */
  var grounded: Boolean = false
  /** Stockpile count and the stages it actually added, which Spit Up and Swallow take back. */
  var stockpile: Int = 0
  var stockpileDef: Int = 0
  var stockpileSpDef: Int = 0
  /** The last move it used and whether it failed (Encore, Disable, Torment, Stomping Tantrum). */
  var lastMoveId: Int = 0
  var lastMoveFailed: Boolean = false
  /** Successful uses of [lastMoveId] in a row (Fury Cutter, Rollout). */
  var consecutive: Int = 0
  /** Every move used since it came in (Last Resort). */
  val usedMoves: MutableSet<Int> = mutableSetOf()
  /** Full turns spent on the field; Fake Out only works on the first. */
  var turnsOnField: Int = 0
  /** Healing Wish and the entry hazards have already met it this stay. */
  var arrived: Boolean = false
  // Per-turn move state.
  var helpingHand: Boolean = false
  var centerOfAttention: Boolean = false
  /** Soak, Conversion, Reflect Type, Burn Up: the types it has for the rest of its stay, over its species'. */
  var typeOverride: Pair<de.fiereu.openmmo.common.enums.PokemonType, de.fiereu.openmmo.common.enums.PokemonType>? = null
  val type1: de.fiereu.openmmo.common.enums.PokemonType
    get() = typeOverride?.first ?: species.type1
  val type2: de.fiereu.openmmo.common.enums.PokemonType
    get() = typeOverride?.second ?: species.type2

  fun hasType(type: de.fiereu.openmmo.common.enums.PokemonType): Boolean = type1 == type || type2 == type || thirdType == type

  /** Charge: the next Electric attack hits twice as hard. */
  var charged: Boolean = false
  /** No Retreat was used: it cannot be used again, nor can the monster flee. */
  var noRetreat: Boolean = false
  /** Embargo: its held item does nothing for these many more turns. */
  var embargoTurns: Int = 0
  /** Magic Room is up: no held item works (kept in step with the field). */
  var inMagicRoom: Boolean = false
  /** Forest's Curse and Trick-or-Treat: a type added on top of the other two. */
  var thirdType: de.fiereu.openmmo.common.enums.PokemonType? = null
  /** Hits taken this battle, switches included (Rage Fist). */
  var timesHit: Int = 0
  /** A stat of it was lowered this turn (Lash Out). */
  var statLoweredThisTurn: Boolean = false
  /** Heating its beak for Beak Blast this turn: contact burns the attacker. */
  var beakBlast: Boolean = false
  /** Which party member's strike Beat Up is on. */
  var beatUpIndex: Int = 0
  /** Mimic: the slot it copied into and the move that sat there, back when it leaves the field. */
  var mimicked: Pair<Int, PokemonMove>? = null
  /** Imprison: foes cannot use any move this monster knows. */
  var imprisoning: Boolean = false
  /** Grudge: the move that knocks it out loses all its pp; holds until it moves again. */
  var grudge: Boolean = false
  /** Octolock: the monster holding it, lowering its defenses each turn. */
  var octolockedBy: BattleMonState? = null
  var telekinesisTurns: Int = 0
  /** Dragon Cheer: extra critical-hit stages. */
  var critBoost: Int = 0
  // Per-turn move state.
  var snatching: Boolean = false
  var magicCoat: Boolean = false
  var electrified: Boolean = false
  var powdered: Boolean = false
  var meFirst: Boolean = false
  /** Pursuit is catching it on its way out. */
  var pursued: Boolean = false

  /** Gives Mimic's slot its own move back. */
  fun restoreMimic() {
    mimicked?.let { (slot, original) -> if (slot < moves.size) moves[slot] = PokemonMove(original.id, original.pp) }
    mimicked = null
  }

  /** Its own moves as the store keeps them: before Transform, with Mimic's slot given back. */
  fun ownMoves(): List<PokemonMove> {
    val own = (transformedFrom ?: moves).map { PokemonMove(it.id, it.pp) }.toMutableList()
    mimicked?.let { (slot, original) -> if (transformedFrom == null && slot < own.size) own[slot] = PokemonMove(original.id, original.pp) }
    return own
  }

  /** Turns of Toxic so far, which scales its damage; resets when the monster leaves the field. */
  var toxicCounter: Int = 0

  /** Gained at least one level in this battle - the cartridge gate for a level evolution. */
  var leveledThisBattle: Boolean = false
  val moves: MutableList<PokemonMove> =
      source.moves.map { PokemonMove(it.id, it.pp) }.toMutableList()

  private val stages = EnumMap<BattleStat, Int>(BattleStat::class.java)

  // Volatile state, cleared when the monster leaves the field.
  var confusionTurns: Int = 0
  var flinched: Boolean = false
  var protectedThisTurn: Boolean = false
  var protectStreak: Int = 0
  var enduring: Boolean = false
  var leechSeeded: Boolean = false
  var focusEnergy: Boolean = false
  var minimized: Boolean = false
  var defenseCurled: Boolean = false
  var identified: Boolean = false
  var lockedOn: Boolean = false
  var cursed: Boolean = false
  var nightmare: Boolean = false
  var ingrained: Boolean = false
  var mustRecharge: Boolean = false
  var trappedTurns: Int = 0
  /** The move that trapped it, named in the "hurt by" and "freed from" lines. */
  var trappingMoveId: Int = 0
  var drowsyTurns: Int = 0
  var wishTurns: Int = 0
  /** A two-turn move in progress: the first half was used, the second executes next turn. */
  var chargingMoveId: Int = 0
  var semiInvulnerable: Boolean = false
  /** Damage taken this turn, for Counter / Mirror Coat and Revenge. */
  var lastDamageTaken: Int = 0
  var lastDamagePhysical: Boolean = true
  var movedThisTurn: Boolean = false
  /** This turn's move checks (sleep, paralysis, flinch...) ran for the first target of a use. */
  var leadChecked: Boolean = false
  /** ...and the monster lost its action there, so a spread's later targets are not hit either. */
  var leadLost: Boolean = false
  /** Turns ended asleep in a row on the field, for the Dream Ball; 0 while awake. */
  var sleepTurns: Int = 0

  /** Everything a switch or a faint forgets: stages and the per-battle flags. */
  fun resetVolatile() {
    transformedFrom?.let { own ->
      moves.clear()
      moves += own
    }
    transformedFrom = null
    restoreMimic()
    stages.clear()
    toxicCounter = 0
    ability = Abilities.of(species, source)
    truantLoafs = false
    slowStartTurns = 0
    flashFire = false
    unburdened = false
    choiceLockedMove = 0
    custapReady = false
    micleBoost = false
    illusionOf = null
    confusionTurns = 0
    flinched = false
    protectedThisTurn = false
    protectStreak = 0
    enduring = false
    leechSeeded = false
    focusEnergy = false
    minimized = false
    defenseCurled = false
    identified = false
    lockedOn = false
    cursed = false
    nightmare = false
    ingrained = false
    mustRecharge = false
    trappedTurns = 0
    trappingMoveId = 0
    drowsyTurns = 0
    wishTurns = 0
    chargingMoveId = 0
    semiInvulnerable = false
    lastDamageTaken = 0
    movedThisTurn = false
    sleepTurns = 0
    substituteHp = 0
    tauntTurns = 0
    encoreTurns = 0
    encoreMoveId = 0
    disableTurns = 0
    disabledMoveId = 0
    tormented = false
    infatuatedWith = null
    destinyBond = false
    perishCount = 0
    aquaRing = false
    magnetRiseTurns = 0
    healBlockTurns = 0
    grounded = false
    stockpile = 0
    stockpileDef = 0
    stockpileSpDef = 0
    lastMoveId = 0
    lastMoveFailed = false
    consecutive = 0
    usedMoves.clear()
    turnsOnField = 0
    arrived = false
    helpingHand = false
    centerOfAttention = false
    charged = false
    noRetreat = false
    typeOverride = null
    thirdType = null
    embargoTurns = 0
    imprisoning = false
    grudge = false
    octolockedBy = null
    telekinesisTurns = 0
    critBoost = 0
    // Power Trick, Speed Swap and the splits change the stats themselves; they end with the stay.
    stats = StatCalculator.computeAll(species, source).copy(hp = stats.hp)
  }

  /** Per-turn flags, cleared at the end of every turn. */
  fun endTurn() {
    leadChecked = false
    leadLost = false
    flinched = false
    protectedThisTurn = false
    enduring = false
    lastDamageTaken = 0
    movedThisTurn = false
    movedFirstByItem = 0
    custapReady = false
    helpingHand = false
    centerOfAttention = false
    statLoweredThisTurn = false
    beakBlast = false
    snatching = false
    magicCoat = false
    electrified = false
    powdered = false
    meFirst = false
    turnsOnField++
    sleepTurns = if (de.fiereu.openmmo.common.StatusCondition.isAsleep(status)) sleepTurns + 1 else 0
  }

  val level: Int
    get() = source.level.toInt()

  val fainted: Boolean
    get() = currentHp <= 0

  val maxHp: Int
    get() = stats.hp

  /** What Baton Pass hands to the monster coming in: the stages and the effects that go with them. */
  class Passed(
      val stages: Map<BattleStat, Int>,
      val substituteHp: Int,
      val confusionTurns: Int,
      val focusEnergy: Boolean,
      val leechSeeded: Boolean,
      val perishCount: Int,
      val aquaRing: Boolean,
      val magnetRiseTurns: Int,
      val embargoTurns: Int,
      val healBlockTurns: Int,
      val ingrained: Boolean,
      val cursed: Boolean,
  )

  fun batonPass(): Passed =
      Passed(stages.toMap(), substituteHp, confusionTurns, focusEnergy, leechSeeded, perishCount, aquaRing, magnetRiseTurns,
          embargoTurns, healBlockTurns, ingrained, cursed)

  fun receive(passed: Passed) {
    stages.putAll(passed.stages)
    substituteHp = passed.substituteHp
    confusionTurns = passed.confusionTurns
    focusEnergy = passed.focusEnergy
    leechSeeded = passed.leechSeeded
    perishCount = passed.perishCount
    aquaRing = passed.aquaRing
    magnetRiseTurns = passed.magnetRiseTurns
    embargoTurns = passed.embargoTurns
    healBlockTurns = passed.healBlockTurns
    ingrained = passed.ingrained
    cursed = passed.cursed
  }

  fun stage(stat: BattleStat): Int = stages[stat] ?: 0

  /** Clamp to the stage limits and return the delta that was actually applied. */
  fun changeStage(stat: BattleStat, delta: Int): Int {
    val old = stage(stat)
    val new = (old + delta).coerceIn(StatStages.MIN, StatStages.MAX)
    stages[stat] = new
    if (new < old) statLoweredThisTurn = true
    return new - old
  }

  fun unstaged(stat: BattleStat): Int =
      when (stat) {
        BattleStat.ATTACK -> stats.atk
        BattleStat.DEFENSE -> stats.def
        BattleStat.SP_ATTACK -> stats.spAtk
        BattleStat.SP_DEFENSE -> stats.spDef
        BattleStat.SPEED -> stats.spd
        BattleStat.ACCURACY,
        BattleStat.EVASION -> error("$this has no base stat to stage")
      }

  fun effective(stat: BattleStat): Int = StatStages.scaleStat(unstaged(stat), stage(stat))

  fun toOpponentBlock(slot: Int): BattleOpponentBlock =
      BattleOpponentBlock(
          slot = slot,
          revealed = true,
          entityId = entityId,
          species = shownSpeciesId(),
          level = source.level,
          gender = illusionOf?.gender ?: gender,
          maxHp = stats.hp.toShort(),
          currentHp = currentHp.toShort(),
      )

  fun toBlock(slot: Int, movesPresent: Boolean): BattleMonBlock =
      BattleMonBlock(
          slot = slot,
          entityId = entityId,
          species = shownSpeciesId(),
          level = source.level,
          gender = illusionOf?.gender ?: gender,
          abilityId = de.fiereu.openmmo.pokemon.AbilityWireIds.of(ability).toShort(),
          maxHp = stats.hp.toShort(),
          currentHp = currentHp.toShort(),
          movesPresent = movesPresent,
          moveIds = List(BattleMonBlock.MOVE_SLOTS) { moves.getOrNull(it)?.id ?: 0 },
          shiny = illusionOf?.source?.isShiny ?: source.isShiny,
      )

  /** The species the client is shown: the Illusion disguise while it holds, else the real one. */
  fun shownSpeciesId(): Short = clientSpeciesId((illusionOf ?: this).species.id).toShort()

  fun wireSpeciesId(): Short = clientSpeciesId(species.id).toShort()
}
