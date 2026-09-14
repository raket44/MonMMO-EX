package de.fiereu.openmmo.moves

import de.fiereu.openmmo.common.enums.DamageCategory
import de.fiereu.openmmo.common.enums.PokemonType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** The client's move table overrides the Expansion's numbers; our Fairy retypes do not give way. */
class ClientMoveStatsTest :
    FunSpec({
      val registry = MoveRegistry()

      test("the client's numbers win where the two tables disagree") {
        registry.get(282)!!.power shouldBe 20 // Knock Off
        registry.get(200)!!.power shouldBe 90 // Outrage
        registry.get(456)!!.pp shouldBe 5 // Heal Order
      }

      test("our Fairy retypes stay") {
        registry.get(204)!!.type shouldBe PokemonType.FAIRY // Charm
        registry.get(186)!!.type shouldBe PokemonType.FAIRY // Sweet Kiss
        registry.get(236)!!.type shouldBe PokemonType.FAIRY // Moonlight
      }

      test("the client's power markers never overwrite a status or variable-power move") {
        registry.get(382)!!.power shouldBe 0 // Me First stays a status move
        registry.get(267)!!.power shouldBe 1 // Nature Power stays a move that calls another
        registry.get(237)!!.category shouldBe DamageCategory.SPECIAL // Hidden Power
      }

      test("a move that never misses keeps accuracy 0") {
        registry.get(129)!!.accuracy shouldBe 0 // Swift
      }
    })
