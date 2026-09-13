package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.StatusCondition
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.items.generated.Items
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.CatchModifiers.BallContext
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private fun rate(ball: de.fiereu.openmmo.items.ItemDef, context: BallContext = BallContext()) =
    CatchModifiers.ballRate(ball, context)

private fun wildState(dexId: Int): BattleMonState {
  val registry = SpeciesRegistry()
  val def = registry.get(dexId)!!
  val mon =
      Pokemon(
          id = 1,
          ownerId = 0,
          container = PokemonContainer.PARTY,
          containerSlot = 0,
          dexId = dexId,
          seed = 0,
          ot = "",
          nickname = "",
          level = 10,
          hp = Short.MAX_VALUE,
          xp = 0,
          eVs = EVs(),
          iVs = IVs(),
          moves = List(4) { PokemonMove(0, 0) },
          isShiny = false,
          hasHiddenAbility = false,
          isAlpha = false,
          isSecret = false,
          isFatefulEncounter = false,
          isRaidEncounter = false,
          caughtAt = LocalDateTime.now(),
      )
  return BattleMonState(1, def, null, mon, StatCalculator.computeAll(def, mon))
}

class CatchModifiersTest :
    FunSpec({
      test("plain balls carry PokeMMO's flat bonuses") {
        rate(Items.POKE_BALL) shouldBe 100
        rate(Items.GREAT_BALL) shouldBe 150
        rate(Items.ULTRA_BALL) shouldBe 200
        rate(Items.SAFARI_BALL) shouldBe 150
        rate(Items.PREMIER_BALL) shouldBe 150
        rate(Items.CHERISH_BALL) shouldBe 200
        rate(Items.HEAL_BALL) shouldBe 125
        rate(Items.SPORT_BALL) shouldBe 100
        rate(Items.PARK_BALL) shouldBe 100
      }

      test("Quick Ball is 5x on the first turn only") {
        rate(Items.QUICK_BALL, BallContext(turn = 1)) shouldBe 500
        rate(Items.QUICK_BALL, BallContext(turn = 2)) shouldBe 100
      }

      test("Timer Ball starts at 1x, adds 0.3x a turn and caps at 4x from turn 11") {
        rate(Items.TIMER_BALL, BallContext(turn = 1)) shouldBe 100
        rate(Items.TIMER_BALL, BallContext(turn = 2)) shouldBe 130
        rate(Items.TIMER_BALL, BallContext(turn = 10)) shouldBe 370
        rate(Items.TIMER_BALL, BallContext(turn = 11)) shouldBe 400
        rate(Items.TIMER_BALL, BallContext(turn = 40)) shouldBe 400
      }

      test("Nest Ball follows the wiki table: 4x to level 16, 1x from level 31") {
        rate(Items.NEST_BALL, BallContext(targetLevel = 1)) shouldBe 400
        rate(Items.NEST_BALL, BallContext(targetLevel = 16)) shouldBe 400
        rate(Items.NEST_BALL, BallContext(targetLevel = 17)) shouldBe 380
        rate(Items.NEST_BALL, BallContext(targetLevel = 25)) shouldBe 220
        rate(Items.NEST_BALL, BallContext(targetLevel = 30)) shouldBe 120
        rate(Items.NEST_BALL, BallContext(targetLevel = 31)) shouldBe 100
        rate(Items.NEST_BALL, BallContext(targetLevel = 100)) shouldBe 100
      }

      test("Net Ball is 3.5x on Water or Bug") {
        rate(Items.NET_BALL, BallContext(targetTypes = setOf(PokemonType.WATER))) shouldBe 350
        rate(Items.NET_BALL, BallContext(targetTypes = setOf(PokemonType.GRASS, PokemonType.BUG))) shouldBe 350
        rate(Items.NET_BALL, BallContext(targetTypes = setOf(PokemonType.FIRE))) shouldBe 100
      }

      test("Dive, Dusk and Lure Balls read where and when the encounter happened") {
        rate(Items.DIVE_BALL, BallContext(encounter = EncounterContext(surfing = true))) shouldBe 350
        rate(Items.DIVE_BALL) shouldBe 100
        rate(Items.DUSK_BALL, BallContext(night = true)) shouldBe 250
        rate(Items.DUSK_BALL, BallContext(encounter = EncounterContext(cave = true))) shouldBe 250
        rate(Items.DUSK_BALL) shouldBe 100
        rate(Items.LURE_BALL, BallContext(encounter = EncounterContext(fishing = true))) shouldBe 400
        rate(Items.LURE_BALL) shouldBe 100
      }

      test("Repeat Ball adds 0.1x per chain link and caps at 2.5x") {
        rate(Items.REPEAT_BALL, BallContext(repeatChain = 0)) shouldBe 100
        rate(Items.REPEAT_BALL, BallContext(repeatChain = 1)) shouldBe 110
        rate(Items.REPEAT_BALL, BallContext(repeatChain = 14)) shouldBe 240
        rate(Items.REPEAT_BALL, BallContext(repeatChain = 15)) shouldBe 250
        rate(Items.REPEAT_BALL, BallContext(repeatChain = 40)) shouldBe 250
      }

      test("Dream Ball grows with consecutive sleep turns") {
        rate(Items.DREAM_BALL, BallContext(targetSleepTurns = 0)) shouldBe 100
        rate(Items.DREAM_BALL, BallContext(targetSleepTurns = 1)) shouldBe 150
        rate(Items.DREAM_BALL, BallContext(targetSleepTurns = 2)) shouldBe 250
        rate(Items.DREAM_BALL, BallContext(targetSleepTurns = 3)) shouldBe 400
        rate(Items.DREAM_BALL, BallContext(targetSleepTurns = 9)) shouldBe 400
      }

      test("Heavy, Fast and Level Balls read the target") {
        rate(Items.HEAVY_BALL, BallContext(targetWeight = 999)) shouldBe 100
        rate(Items.HEAVY_BALL, BallContext(targetWeight = 4600)) shouldBe 400
        rate(Items.FAST_BALL, BallContext(targetBaseSpeed = 99)) shouldBe 100
        rate(Items.FAST_BALL, BallContext(targetBaseSpeed = 100)) shouldBe 400
        rate(Items.LEVEL_BALL, BallContext(targetLevel = 30, userLevel = 30)) shouldBe 400
        rate(Items.LEVEL_BALL, BallContext(targetLevel = 12, userLevel = 30)) shouldBe 400
        rate(Items.LEVEL_BALL, BallContext(targetLevel = 31, userLevel = 30)) shouldBe 100
      }

      test("Love Ball needs the same family and opposite genders") {
        val family = BallContext(sameEvolutionFamily = true)
        rate(Items.LOVE_BALL, family.copy(targetGender = 0, userGender = 1)) shouldBe 800
        rate(Items.LOVE_BALL, family.copy(targetGender = 1, userGender = 0)) shouldBe 800
        rate(Items.LOVE_BALL, family.copy(targetGender = 1, userGender = 1)) shouldBe 100
        rate(Items.LOVE_BALL, family.copy(targetGender = -1, userGender = 0)) shouldBe 100
        rate(Items.LOVE_BALL, BallContext(targetGender = 0, userGender = 1)) shouldBe 100
      }

      test("Moon, Friend and Luxury Balls read the family's evolutions") {
        rate(Items.MOON_BALL, BallContext(familyEvolvesByMoonStone = true)) shouldBe 400
        rate(Items.MOON_BALL) shouldBe 100
        rate(Items.FRIEND_BALL, BallContext(familyEvolvesByFriendship = true)) shouldBe 250
        rate(Items.FRIEND_BALL) shouldBe 100
        rate(Items.LUXURY_BALL, BallContext(familyEvolvesByFriendship = true)) shouldBe 200
        rate(Items.LUXURY_BALL) shouldBe 100
      }

      test("sleep and freeze give 2.5x, any other status 1.5x") {
        CatchModifiers.statusRate(StatusCondition.NONE) shouldBe 100
        CatchModifiers.statusRate(1) shouldBe 250
        CatchModifiers.statusRate(StatusCondition.SLEEP_MASK) shouldBe 250
        CatchModifiers.statusRate(StatusCondition.FREEZE) shouldBe 250
        CatchModifiers.statusRate(StatusCondition.PARALYSIS) shouldBe 150
        CatchModifiers.statusRate(StatusCondition.BURN) shouldBe 150
        CatchModifiers.statusRate(StatusCondition.POISON) shouldBe 150
        CatchModifiers.statusRate(StatusCondition.TOXIC) shouldBe 150
      }

      test("the odds scale by ball, hp and status and never drop below 1") {
        // Full hp leaves a third: 45 * 1x = 45 -> 15.
        CatchModifiers.odds(45, 100, 90, 90, 0) shouldBe 15
        CatchModifiers.odds(45, 150, 90, 90, 0) shouldBe 22
        CatchModifiers.odds(45, 100, 90, 1, 0) shouldBe 44
        CatchModifiers.odds(45, 100, 90, 90, StatusCondition.FREEZE) shouldBe 37
        CatchModifiers.odds(3, 100, 90, 90, 0) shouldBe 1
      }

      test("a monster counts the turns it ends asleep, and the count resets when it wakes") {
        val mon = wildState(25)
        mon.status = 3
        mon.endTurn()
        mon.sleepTurns shouldBe 1
        mon.endTurn()
        mon.sleepTurns shouldBe 2
        mon.status = 0
        mon.endTurn()
        mon.sleepTurns shouldBe 0
      }
    })
