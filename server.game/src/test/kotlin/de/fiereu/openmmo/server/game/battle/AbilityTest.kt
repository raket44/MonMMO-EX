package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private const val PLAYER_ID = 0x1C000L
private const val WILD_ID = 0x3C000L

private const val TACKLE: Short = 33
private const val SPLASH: Short = 150
private const val EARTHQUAKE: Short = 89
private const val THUNDERBOLT: Short = 85
private const val FISSURE: Short = 90
private const val THUNDER_WAVE: Short = 86

private const val PIDGEOT = 18
private const val SNORLAX = 143
private const val GENGAR = 94
private const val JOLTEON = 135
private const val GYARADOS = 130
private const val YANMA = 193
private const val SKARMORY = 227
private const val ELECTRODE = 101

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()

private fun pokemon(dexId: Int, level: Int, moves: List<Short>, id: Long): Pokemon {
  val padded = List(4) { i -> moves.getOrNull(i) ?: 0 }
  return Pokemon(
      id = id,
      ownerId = 0,
      container = PokemonContainer.PARTY,
      containerSlot = 0,
      dexId = dexId,
      seed = 0,
      ot = "",
      nickname = "",
      level = level.toByte(),
      hp = Short.MAX_VALUE,
      xp = 0,
      eVs = EVs(),
      iVs = IVs(),
      moves = padded.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) },
      isShiny = false,
      hasHiddenAbility = false,
      isAlpha = false,
      isSecret = false,
      isFatefulEncounter = false,
      isRaidEncounter = false,
      caughtAt = LocalDateTime.now(),
  )
}

private fun state(dexId: Int, level: Int, moves: List<Short>, id: Long, ability: Ability): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val mon = pokemon(dexId, level, moves, id)
  val state = BattleMonState(id, def, if (id == PLAYER_ID) 0 else null, mon, StatCalculator.computeAll(def, mon))
  state.ability = ability
  return state
}

private fun battle(player: BattleMonState, wild: BattleMonState, seed: Long): BattleInstance =
    BattleInstance(1L, 100L, FakeSession(100L), listOf(player), listOf(wild), BattleRng(seed))

private val engine = TurnEngine(MoveRegistry(), TypeChart())

class AbilityTest :
    FunSpec({
      test("Levitate makes Earthquake miss entirely") {
        val player = state(SNORLAX, 60, listOf(EARTHQUAKE), PLAYER_ID, Ability.THICK_FAT)
        val wild = state(GENGAR, 20, listOf(SPLASH), WILD_ID, Ability.LEVITATE)
        val events = engine.resolveTurn(battle(player, wild, 1), EARTHQUAKE)
        events.filterIsInstance<BattleEvent.AbilityShown>().first { it.targetId == WILD_ID }.ability shouldBe Ability.LEVITATE
        events.filterIsInstance<BattleEvent.Immune>().first().targetId shouldBe WILD_ID
        wild.currentHp shouldBe wild.maxHp
      }

      test("Volt Absorb turns an Electric hit into healing") {
        val player = state(PIDGEOT, 60, listOf(THUNDERBOLT), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(JOLTEON, 30, listOf(SPLASH), WILD_ID, Ability.VOLT_ABSORB)
        wild.currentHp = wild.maxHp / 2
        val events = engine.resolveTurn(battle(player, wild, 1), THUNDERBOLT)
        events.filterIsInstance<BattleEvent.DamageDealt>().shouldBeEmpty()
        wild.currentHp shouldBe wild.maxHp / 2 + wild.maxHp / 4
      }

      test("Intimidate drops the foe's Attack on entry") {
        val player = state(GYARADOS, 60, listOf(SPLASH), PLAYER_ID, Ability.INTIMIDATE)
        val wild = state(SNORLAX, 30, listOf(SPLASH), WILD_ID, Ability.THICK_FAT)
        val events = mutableListOf<BattleEvent>()
        engine.switchIn(battle(player, wild, 1), player, events)
        wild.stage(BattleStat.ATTACK) shouldBe -1
        events.filterIsInstance<BattleEvent.AbilityShown>().first().ability shouldBe Ability.INTIMIDATE
      }

      test("Clear Body refuses Intimidate") {
        val player = state(GYARADOS, 60, listOf(SPLASH), PLAYER_ID, Ability.INTIMIDATE)
        val wild = state(SNORLAX, 30, listOf(SPLASH), WILD_ID, Ability.CLEAR_BODY)
        engine.switchIn(battle(player, wild, 1), player, mutableListOf())
        wild.stage(BattleStat.ATTACK) shouldBe 0
      }

      test("Speed Boost rises every turn") {
        val player = state(YANMA, 60, listOf(SPLASH), PLAYER_ID, Ability.SPEED_BOOST)
        val wild = state(SNORLAX, 30, listOf(SPLASH), WILD_ID, Ability.THICK_FAT)
        val instance = battle(player, wild, 1)
        engine.resolveTurn(instance, SPLASH)
        engine.resolveTurn(instance, SPLASH)
        player.stage(BattleStat.SPEED) shouldBe 2
      }

      test("Sturdy shrugs off a one-hit KO move") {
        val player = state(SNORLAX, 60, listOf(FISSURE), PLAYER_ID, Ability.THICK_FAT)
        val wild = state(SKARMORY, 30, listOf(SPLASH), WILD_ID, Ability.STURDY)
        val events = engine.resolveTurn(battle(player, wild, 1), FISSURE)
        events.filterIsInstance<BattleEvent.Immune>().first().targetId shouldBe WILD_ID
        wild.currentHp shouldBe wild.maxHp
      }

      test("Limber cannot be paralyzed") {
        val player = state(PIDGEOT, 60, listOf(THUNDER_WAVE), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(SNORLAX, 30, listOf(SPLASH), WILD_ID, Ability.LIMBER)
        for (seed in 1L..10L) {
          val events = engine.resolveTurn(battle(player, wild, seed), THUNDER_WAVE)
          wild.status shouldBe StatusCondition.NONE
          if (events.any { it is BattleEvent.MoveFailed }) {
            events.filterIsInstance<BattleEvent.AbilityShown>().any { it.ability == Ability.LIMBER }.shouldBeTrue()
          }
        }
      }

      test("Static paralyzes a contact attacker some of the time") {
        var paralyzed = 0
        for (seed in 1L..40L) {
          val player = state(SNORLAX, 60, listOf(TACKLE), PLAYER_ID, Ability.THICK_FAT)
          val wild = state(ELECTRODE, 30, listOf(SPLASH), WILD_ID, Ability.STATIC)
          engine.resolveTurn(battle(player, wild, seed), TACKLE)
          if (StatusCondition.isParalyzed(player.status)) paralyzed++
        }
        paralyzed shouldBeGreaterThan 3
      }

      test("Arena Trap keeps a grounded monster from running, not a Flying one") {
        val diglett = state(50, 20, listOf(SPLASH), WILD_ID, Ability.ARENA_TRAP)
        val snorlax = state(SNORLAX, 30, listOf(SPLASH), PLAYER_ID, Ability.THICK_FAT)
        engine.canFlee(battle(snorlax, diglett, 1)) shouldBe false
        val pidgeot = state(PIDGEOT, 30, listOf(SPLASH), PLAYER_ID, Ability.KEEN_EYE)
        engine.canFlee(battle(pidgeot, diglett, 1)) shouldBe true
        val sandVeil = state(50, 20, listOf(SPLASH), WILD_ID, Ability.SAND_VEIL)
        engine.canFlee(battle(snorlax, sandVeil, 1)) shouldBe true
      }

      test("Huge Power doubles physical damage") {
        fun dealt(ability: Ability): Int {
          val player = state(SNORLAX, 60, listOf(TACKLE), PLAYER_ID, ability)
          val wild = state(SNORLAX, 60, listOf(SPLASH), WILD_ID, Ability.THICK_FAT)
          val events = engine.resolveTurn(battle(player, wild, 4), TACKLE)
          val hit = events.filterIsInstance<BattleEvent.DamageDealt>().firstOrNull { it.targetId == WILD_ID }
          return if (hit == null) 0 else wild.maxHp - hit.newHp
        }
        val plain = dealt(Ability.THICK_FAT)
        val huge = dealt(Ability.HUGE_POWER)
        huge shouldBeGreaterThan plain
        (huge >= plain * 3 / 2).shouldBeTrue()
      }
    })
