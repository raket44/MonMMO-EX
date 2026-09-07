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

  /** Everything a switch or a faint forgets: stages and the per-battle flags. */
  fun resetVolatile() {
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
  }

  /** Per-turn flags, cleared at the end of every turn. */
  fun endTurn() {
    flinched = false
    protectedThisTurn = false
    enduring = false
    lastDamageTaken = 0
    movedThisTurn = false
    movedFirstByItem = 0
    custapReady = false
  }

  val level: Int
    get() = source.level.toInt()

  val fainted: Boolean
    get() = currentHp <= 0

  val maxHp: Int
    get() = stats.hp

  fun stage(stat: BattleStat): Int = stages[stat] ?: 0

  /** Clamp to the stage limits and return the delta that was actually applied. */
  fun changeStage(stat: BattleStat, delta: Int): Int {
    val old = stage(stat)
    val new = (old + delta).coerceIn(StatStages.MIN, StatStages.MAX)
    stages[stat] = new
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
