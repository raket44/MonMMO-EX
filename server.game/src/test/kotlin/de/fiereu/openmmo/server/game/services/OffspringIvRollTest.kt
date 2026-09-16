package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.server.game.battle.BattleRng
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private fun parent(ivs: List<Int>, shiny: Boolean = false, held: Int = 0) =
    Pokemon(
        id = 1,
        ownerId = 1,
        container = PokemonContainer.PC,
        containerSlot = 0,
        dexId = 133,
        seed = 0,
        ot = "RaKeT",
        nickname = "",
        level = 50,
        hp = 100,
        xp = 0,
        eVs = EVs(),
        // RC0.Df0 order: hp, atk, def, SPEED, spAtk, spDef.
        iVs =
            IVs().apply {
              hp = ivs[0]
              atk = ivs[1]
              def = ivs[2]
              spd = ivs[3]
              spAtk = ivs[4]
              spDef = ivs[5]
            },
        moves = List(4) { PokemonMove(0, 0) },
        isShiny = shiny,
        hasHiddenAbility = false,
        isAlpha = false,
        isSecret = false,
        isFatefulEncounter = false,
        isRaidEncounter = false,
        caughtAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        heldItem = held,
    )

private fun statsOf(ivs: IVs) = listOf(ivs.hp, ivs.atk, ivs.def, ivs.spd, ivs.spAtk, ivs.spDef)

/** The egg must roll the same model the forecast rows describe (client strings 2547 / 2548). */
class OffspringIvRollTest :
    FunSpec({
      val low = List(6) { 4 }
      val high = List(6) { 28 }

      test("every stat lands inside the parents' range, passed or rolled") {
        val rng = BattleRng()
        repeat(200) {
          val rolled = statsOf(rollOffspringIVs(parent(low), parent(high), rng))
          rolled.forEach { value ->
            value shouldBeGreaterThanOrEqual 4
            (value <= 28) shouldBe true
          }
        }
      }

      test("a shiny pair forces at least two stats to the better parent") {
        val rng = BattleRng()
        repeat(200) {
          val rolled =
              statsOf(rollOffspringIVs(parent(low, shiny = true), parent(high, shiny = true), rng))
          rolled.count { it == 28 } shouldBeGreaterThanOrEqual 2
        }
      }

      test("a Power brace pins its own stat to the holder's value") {
        // 5294 is a Power Weight: HP, the first stat in Df0 order.
        val braced = parent(low, held = 5294)
        val rng = BattleRng()
        repeat(200) { statsOf(rollOffspringIVs(braced, parent(high), rng))[0] shouldBe 4 }
      }

      test("identical parents breed an identical baby") {
        val rng = BattleRng()
        val same = List(6) { 17 }
        repeat(50) {
          statsOf(rollOffspringIVs(parent(same), parent(same), rng)) shouldBe same
        }
      }
    })
