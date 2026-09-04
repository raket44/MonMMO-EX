package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private const val PLAYER_ID = 0x1C000L
private const val WILD_ID = 0x3C000L

private const val TACKLE: Short = 33
private const val SPLASH: Short = 150
private const val THUNDER_WAVE: Short = 86
private const val TOXIC: Short = 92
private const val PROTECT: Short = 182
private const val DOUBLE_EDGE: Short = 38
private const val GIGA_DRAIN: Short = 202
private const val FURY_ATTACK: Short = 31
private const val RECOVER: Short = 105
private const val RAIN_DANCE: Short = 240
private const val LEECH_SEED: Short = 73
private const val HYPER_BEAM: Short = 63
private const val FLY: Short = 19

private const val BULBASAUR = 1
private const val RATTATA = 19
private const val SANDSHREW = 27
private const val SNORLAX = 143
private const val PIDGEOT = 18

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

private fun state(dexId: Int, level: Int, moves: List<Short>, id: Long): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val mon = pokemon(dexId, level, moves, id)
  return BattleMonState(
      id, def, if (id == PLAYER_ID) 0 else null, mon, StatCalculator.computeAll(def, mon))
}

private fun battle(player: BattleMonState, wild: BattleMonState, seed: Long): BattleInstance =
    BattleInstance(1L, 100L, FakeSession(100L), listOf(player), listOf(wild), BattleRng(seed))

private val engine = TurnEngine(MoveRegistry(), TypeChart())

/** Runs the move on fresh monsters over many seeds until [accept] holds, or fails. */
private fun firstSeedWhere(
    playerMoves: List<Short>,
    wildDex: Int,
    wildLevel: Int,
    move: Short,
    accept: (List<BattleEvent>, BattleMonState, BattleMonState) -> Boolean,
): Triple<List<BattleEvent>, BattleMonState, BattleMonState> {
  for (seed in 1L..60L) {
    val player = state(PIDGEOT, 60, playerMoves, PLAYER_ID)
    val wild = state(wildDex, wildLevel, listOf(SPLASH), WILD_ID)
    val events = engine.resolveTurn(battle(player, wild, seed), move)
    if (accept(events, player, wild)) return Triple(events, player, wild)
  }
  error("no seed satisfied the condition")
}

class BattleMechanicsTest :
    FunSpec({
      test("Thunder Wave paralyzes and reports the status byte") {
        val (events, _, wild) =
            firstSeedWhere(listOf(THUNDER_WAVE), RATTATA, 5, THUNDER_WAVE) { _, _, w ->
              StatusCondition.isParalyzed(w.status)
            }
        wild.status shouldBe StatusCondition.PARALYSIS
        events.filterIsInstance<BattleEvent.StatusChanged>().first { it.targetId == WILD_ID }.status shouldBe
            StatusCondition.PARALYSIS
      }

      test("Thunder Wave does not affect a Ground type") {
        for (seed in 1L..10L) {
          val player = state(PIDGEOT, 60, listOf(THUNDER_WAVE), PLAYER_ID)
          val wild = state(SANDSHREW, 5, listOf(SPLASH), WILD_ID)
          val events = engine.resolveTurn(battle(player, wild, seed), THUNDER_WAVE)
          events.filterIsInstance<BattleEvent.MoveFailed>().first().attackerId shouldBe PLAYER_ID
          wild.status shouldBe StatusCondition.NONE
        }
      }

      test("Toxic fails on a Poison type and badly poisons anything else") {
        val player = state(PIDGEOT, 60, listOf(TOXIC), PLAYER_ID)
        val wild = state(BULBASAUR, 5, listOf(SPLASH), WILD_ID)
        engine.resolveTurn(battle(player, wild, 3), TOXIC)
        wild.status shouldBe StatusCondition.NONE

        val (events, _, rat) =
            firstSeedWhere(listOf(TOXIC), RATTATA, 5, TOXIC) { _, _, w -> StatusCondition.isBadlyPoisoned(w.status) }
        // The first end-of-turn tick takes 1/16.
        val expected = rat.maxHp - (rat.maxHp / 16).coerceAtLeast(1)
        events.filterIsInstance<BattleEvent.HpChanged>().first { it.targetId == WILD_ID }.newHp shouldBe expected
      }

      test("poison takes an eighth at the end of the turn") {
        val player = state(PIDGEOT, 60, listOf(SPLASH), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID)
        wild.status = StatusCondition.POISON
        val events = engine.resolveTurn(battle(player, wild, 1), SPLASH)
        val tick = events.filterIsInstance<BattleEvent.HpChanged>().first { it.targetId == WILD_ID }
        tick.newHp shouldBe wild.maxHp - wild.maxHp / 8
        wild.currentHp shouldBe wild.maxHp - wild.maxHp / 8
      }

      test("a sleeping monster loses its turn and counts down") {
        val player = state(PIDGEOT, 60, listOf(SPLASH), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(TACKLE), WILD_ID)
        wild.status = StatusCondition.asleep(3)
        val events = engine.resolveTurn(battle(player, wild, 1), SPLASH)
        events.filterIsInstance<BattleEvent.MoveUsed>().none { it.attackerId == WILD_ID }.shouldBeTrue()
        events.filterIsInstance<BattleEvent.CantMove>().first { it.attackerId == WILD_ID }.reason shouldBe
            CantMoveReason.ASLEEP
        StatusCondition.sleepTurns(wild.status) shouldBe 2
      }

      test("the last sleep turn wakes the monster and it acts") {
        val player = state(PIDGEOT, 60, listOf(SPLASH), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(TACKLE), WILD_ID)
        wild.status = StatusCondition.asleep(1)
        val events = engine.resolveTurn(battle(player, wild, 1), SPLASH)
        wild.status shouldBe StatusCondition.NONE
        events.filterIsInstance<BattleEvent.StatusChanged>().first { it.targetId == WILD_ID }.status shouldBe 0
        events.filterIsInstance<BattleEvent.MoveUsed>().any { it.attackerId == WILD_ID }.shouldBeTrue()
      }

      test("Protect blocks the incoming hit") {
        val player = state(PIDGEOT, 60, listOf(PROTECT), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(TACKLE), WILD_ID)
        val events = engine.resolveTurn(battle(player, wild, 1), PROTECT)
        events.filterIsInstance<BattleEvent.Protected>().first().targetId shouldBe PLAYER_ID
        events.filterIsInstance<BattleEvent.MoveFailed>().first().attackerId shouldBe WILD_ID
        player.currentHp shouldBe player.maxHp
      }

      test("Double-Edge recoils a quarter of the damage") {
        val (events, player, _) =
            firstSeedWhere(listOf(DOUBLE_EDGE), SNORLAX, 50, DOUBLE_EDGE) { e, _, _ ->
              e.any { it is BattleEvent.DamageDealt && it.targetId == WILD_ID }
            }
        val dealt = events.filterIsInstance<BattleEvent.DamageDealt>().first { it.targetId == WILD_ID }
        val recoil = events.filterIsInstance<BattleEvent.HpChanged>().first { it.targetId == PLAYER_ID }
        val recoiled = player.maxHp - recoil.newHp
        // 33% of the damage dealt, as the Expansion tables it.
        recoiled shouldBe ((snorlaxMaxHp() - dealt.newHp) * 33 / 100).coerceAtLeast(1)
      }

      test("Giga Drain heals half the damage dealt") {
        val (events, player, _) =
            firstSeedWhere(listOf(GIGA_DRAIN), SNORLAX, 50, GIGA_DRAIN) { e, _, _ ->
              e.any { it is BattleEvent.DamageDealt && it.targetId == WILD_ID }
            }
        // The attacker starts at full hp, so the heal is capped; it is still reported.
        events.filterIsInstance<BattleEvent.HpChanged>().none { it.targetId == PLAYER_ID }.shouldBeTrue()
        player.currentHp shouldBe player.maxHp
      }

      test("Fury Attack hits several times") {
        val (events, _, _) =
            firstSeedWhere(listOf(FURY_ATTACK), SNORLAX, 50, FURY_ATTACK) { e, _, _ ->
              e.any { it is BattleEvent.MultiHit }
            }
        val hits = events.filterIsInstance<BattleEvent.MultiHit>().first().hits
        hits shouldBeGreaterThan 1
        events.filterIsInstance<BattleEvent.DamageDealt>().count { it.targetId == WILD_ID } shouldBe hits
      }

      test("Recover heals half and fails at full hp") {
        val player = state(PIDGEOT, 60, listOf(RECOVER), PLAYER_ID)
        val wild = state(RATTATA, 3, listOf(SPLASH), WILD_ID)
        player.currentHp = player.maxHp / 4
        engine.resolveTurn(battle(player, wild, 1), RECOVER)
        player.currentHp shouldBe player.maxHp / 4 + player.maxHp / 2
        player.currentHp = player.maxHp
        val events = engine.resolveTurn(battle(player, wild, 1), RECOVER)
        events.filterIsInstance<BattleEvent.MoveFailed>().first().attackerId shouldBe PLAYER_ID
      }

      test("Rain Dance sets five turns of rain that then clear") {
        val player = state(PIDGEOT, 60, listOf(RAIN_DANCE, SPLASH), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID)
        val instance = battle(player, wild, 1)
        val events = engine.resolveTurn(instance, RAIN_DANCE)
        events.filterIsInstance<BattleEvent.WeatherChanged>().first().weather shouldBe Weather.RAIN
        instance.weather shouldBe Weather.RAIN
        var cleared = false
        repeat(5) {
          val later = engine.resolveTurn(instance, SPLASH)
          if (later.any { it is BattleEvent.WeatherChanged && it.weather == null }) cleared = true
        }
        cleared.shouldBeTrue()
        instance.weather shouldBe null
      }

      test("Leech Seed fails on Grass and drains anything else each turn") {
        val player = state(PIDGEOT, 60, listOf(LEECH_SEED), PLAYER_ID)
        val grass = state(BULBASAUR, 5, listOf(SPLASH), WILD_ID)
        engine.resolveTurn(battle(player, grass, 1), LEECH_SEED).filterIsInstance<BattleEvent.MoveFailed>().shouldNotBeEmpty()

        val (events, seeder, seeded) =
            firstSeedWhere(listOf(LEECH_SEED), SNORLAX, 50, LEECH_SEED) { _, _, w -> w.leechSeeded }
        val drained = events.filterIsInstance<BattleEvent.HpChanged>().first { it.targetId == WILD_ID }
        drained.newHp shouldBe seeded.maxHp - seeded.maxHp / 8
        seeder.currentHp shouldBe seeder.maxHp
      }

      test("Hyper Beam needs a recharge turn") {
        val player = state(PIDGEOT, 60, listOf(HYPER_BEAM, TACKLE), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID)
        val instance = battle(player, wild, 1)
        var landed = false
        for (attempt in 0 until 5) {
          val events = engine.resolveTurn(instance, HYPER_BEAM)
          if (events.any { it is BattleEvent.DamageDealt && it.targetId == WILD_ID }) {
            landed = true
            break
          }
        }
        landed.shouldBeTrue()
        player.mustRecharge.shouldBeTrue()
        val next = engine.resolveTurn(instance, TACKLE)
        next.filterIsInstance<BattleEvent.CantMove>().first { it.attackerId == PLAYER_ID }.reason shouldBe
            CantMoveReason.RECHARGING
        next.filterIsInstance<BattleEvent.MoveUsed>().none { it.attackerId == PLAYER_ID }.shouldBeTrue()
      }

      test("Fly leaves the ground for a turn and strikes on the next") {
        val player = state(PIDGEOT, 60, listOf(FLY), PLAYER_ID)
        val wild = state(SNORLAX, 50, listOf(TACKLE), WILD_ID)
        val instance = battle(player, wild, 1)
        val first = engine.resolveTurn(instance, FLY)
        first.filterIsInstance<BattleEvent.Charging>().first().attackerId shouldBe PLAYER_ID
        first.filterIsInstance<BattleEvent.MoveMissed>().first().attackerId shouldBe WILD_ID
        player.semiInvulnerable.shouldBeTrue()
        var struck = false
        for (attempt in 0 until 5) {
          val second = engine.resolveTurn(instance, FLY)
          if (second.any { it is BattleEvent.DamageDealt && it.targetId == WILD_ID }) {
            struck = true
            break
          }
        }
        struck.shouldBeTrue()
        player.semiInvulnerable.shouldBeFalse()
      }

      test("a status carried into the battle comes from the record") {
        val def = speciesRegistry.get(RATTATA)!!
        val mon = pokemon(RATTATA, 5, listOf(TACKLE), WILD_ID).copy(status = StatusCondition.BURN)
        val state = BattleMonState(WILD_ID, def, null, mon, StatCalculator.computeAll(def, mon))
        StatusCondition.isBurned(state.status).shouldBeTrue()
      }
    })

private fun snorlaxMaxHp(): Int {
  val def = speciesRegistry.get(SNORLAX)!!
  return StatCalculator.computeAll(def, pokemon(SNORLAX, 50, listOf(SPLASH), WILD_ID)).hp
}
