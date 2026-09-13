package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.MoveEffect
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private const val PLAYER_ID = 0x1C000L
private const val WILD_ID = 0x3C000L

private const val LOW_KICK: Short = 67
private const val GRASS_KNOT: Short = 447
private const val HEAVY_SLAM: Short = 484
private const val HEAT_CRASH: Short = 535
private const val SPLASH: Short = 150

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()

private fun state(dexId: Int, level: Int, moves: List<Short>, id: Long): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val padded = List(4) { i -> moves.getOrNull(i) ?: 0 }
  val mon =
      Pokemon(
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
  return BattleMonState(
      id, def, if (id == PLAYER_ID) 0 else null, mon, StatCalculator.computeAll(def, mon))
}

private val engine = TurnEngine(MoveRegistry(), TypeChart())

/**
 * Damage the player's [move] deals to a Splash-only Snorlax on [seed], with the two weights
 * overridden so everything but the weight-driven power is identical between runs.
 */
private fun damage(move: Short, userWeight: Int, targetWeight: Int, seed: Long): Int {
  val player = state(66, 60, listOf(move), PLAYER_ID)
  val wild = state(143, 100, listOf(SPLASH), WILD_ID)
  player.species = player.species.copy(weight = userWeight)
  wild.species = wild.species.copy(weight = targetWeight)
  val before = wild.currentHp
  val battle =
      BattleInstance(1L, 100L, FakeSession(100L), listOf(player), listOf(wild), BattleRng(seed))
  val hit =
      engine.resolveTurn(battle, move).filterIsInstance<BattleEvent.DamageDealt>().first {
        it.targetId == WILD_ID
      }
  return before - hit.newHp
}

class WeightMechanicsTest :
    FunSpec({
      test("Low Kick and Grass Knot power steps at 10, 25, 50, 100 and 200 kg") {
        WeightMechanics.lowKickPower(0) shouldBe 20
        WeightMechanics.lowKickPower(1) shouldBe 20
        WeightMechanics.lowKickPower(99) shouldBe 20
        WeightMechanics.lowKickPower(100) shouldBe 40
        WeightMechanics.lowKickPower(249) shouldBe 40
        WeightMechanics.lowKickPower(250) shouldBe 60
        WeightMechanics.lowKickPower(499) shouldBe 60
        WeightMechanics.lowKickPower(500) shouldBe 80
        WeightMechanics.lowKickPower(999) shouldBe 80
        WeightMechanics.lowKickPower(1000) shouldBe 100
        WeightMechanics.lowKickPower(1999) shouldBe 100
        WeightMechanics.lowKickPower(2000) shouldBe 120
        WeightMechanics.lowKickPower(9999) shouldBe 120
      }

      test("Heavy Slam and Heat Crash power steps at 2, 3, 4 and 5 times the target's weight") {
        WeightMechanics.heavySlamPower(100, 100) shouldBe 40
        WeightMechanics.heavySlamPower(199, 100) shouldBe 40
        WeightMechanics.heavySlamPower(200, 100) shouldBe 60
        WeightMechanics.heavySlamPower(299, 100) shouldBe 60
        WeightMechanics.heavySlamPower(300, 100) shouldBe 80
        WeightMechanics.heavySlamPower(399, 100) shouldBe 80
        WeightMechanics.heavySlamPower(400, 100) shouldBe 100
        WeightMechanics.heavySlamPower(499, 100) shouldBe 100
        WeightMechanics.heavySlamPower(500, 100) shouldBe 120
        WeightMechanics.heavySlamPower(50, 100) shouldBe 40
        // An unweighed target counts as 0.1 kg rather than dividing by zero.
        WeightMechanics.heavySlamPower(5, 0) shouldBe 120
      }

      test("Heavy Ball multiplies by 2, 3 and 4 at 100, 200 and 300 kg") {
        WeightMechanics.heavyBallRate(0) shouldBe 100
        WeightMechanics.heavyBallRate(999) shouldBe 100
        WeightMechanics.heavyBallRate(1000) shouldBe 200
        WeightMechanics.heavyBallRate(1999) shouldBe 200
        WeightMechanics.heavyBallRate(2000) shouldBe 300
        WeightMechanics.heavyBallRate(2999) shouldBe 300
        WeightMechanics.heavyBallRate(3000) shouldBe 400
        WeightMechanics.heavyBallRate(9999) shouldBe 400
      }

      test("the four moves carry the effects the engine keys the weight formulas on") {
        moveRegistry.get(LOW_KICK.toInt())!!.effect shouldBe MoveEffect.LOW_KICK
        moveRegistry.get(GRASS_KNOT.toInt())!!.effect shouldBe MoveEffect.LOW_KICK
        moveRegistry.get(HEAVY_SLAM.toInt())!!.effect shouldBe MoveEffect.HEAT_CRASH
        moveRegistry.get(HEAT_CRASH.toInt())!!.effect shouldBe MoveEffect.HEAT_CRASH
      }

      test("Low Kick hits a heavy target far harder than a light one") {
        // 200 kg is power 120, 9.9 kg power 20: the same roll, six times the power.
        for (seed in 0L..4L) {
          val light = damage(LOW_KICK, userWeight = 195, targetWeight = 99, seed = seed)
          val heavy = damage(LOW_KICK, userWeight = 195, targetWeight = 2000, seed = seed)
          light shouldBeGreaterThan 0
          heavy shouldBeGreaterThan light * 4
        }
      }

      test("Heavy Slam hits harder the more the user outweighs the target") {
        // 5x is power 120, 1.5x power 40.
        for (seed in 0L..4L) {
          val light = damage(HEAVY_SLAM, userWeight = 300, targetWeight = 200, seed = seed)
          val heavy = damage(HEAVY_SLAM, userWeight = 1000, targetWeight = 200, seed = seed)
          light shouldBeGreaterThan 0
          heavy shouldBeGreaterThan light * 2
        }
      }
    })
