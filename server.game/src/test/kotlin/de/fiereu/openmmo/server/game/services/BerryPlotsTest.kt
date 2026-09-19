package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BerryPlotsTest :
    FunSpec({
      val plainSpicy = 1030
      val verySpicy = 1035
      val plainDry = 1031

      test("retail table loads and Cheri is three spicy degrees") {
        BerryPlots.berries.size shouldBe 64
        val cheri = BerryPlots.berries.first { it.gen5Index == 149 }
        cheri.degrees shouldBe listOf(3, 0, 0, 0, 0)
        cheri.growHours shouldBe 16.0
        cheri.wireItemId shouldBe 5149
      }

      test("seed combinations: three Plain Spicy or Plain + Very Spicy grow a Cheri, a wrong mix grows nothing") {
        BerryPlots.berryFor(listOf(plainSpicy, plainSpicy, plainSpicy))?.gen5Index shouldBe 149
        BerryPlots.berryFor(listOf(plainSpicy, verySpicy))?.gen5Index shouldBe 149
        BerryPlots.berryFor(listOf(plainSpicy, plainDry)) shouldBe null
        BerryPlots.berryFor(emptyList()) shouldBe null
      }

      test("seeds pack and unpack through the story var") {
        val vars = mapOf("p/seeds" to BerryPlots.packSeeds(listOf(plainSpicy, verySpicy)))
        BerryPlots.Plot(vars, "p").seeds shouldBe listOf(plainSpicy, verySpicy)
      }

      test("a 16 h Cheri: dry at 7 h, sprout at 4 h, ripe at 16 h, unpicked death at 24 h") {
        val t0 = 1_000_000
        val vars =
            mapOf(
                "p/seeds" to BerryPlots.packSeeds(listOf(plainSpicy, plainSpicy, plainSpicy)),
                "p/planted" to t0,
                "p/water" to BerryPlots.DROPLETS_AT_PLANTING,
                "p/waterAt" to t0)
        val plot = BerryPlots.Plot(vars, "p")
        fun at(hours: Double) = BerryPlots.view(plot, t0 + (hours * 60).toInt())
        at(0.0).stage shouldBe BerryPlots.STAGE_SEED
        at(0.0).droplets shouldBe 3
        at(2.9).droplets shouldBe 3
        at(3.0).droplets shouldBe 2
        at(5.0).droplets shouldBe 1
        at(6.9).droplets shouldBe 1
        at(7.0).droplets shouldBe 0
        at(4.0).stage shouldBe BerryPlots.STAGE_SPROUT
        at(8.0).stage shouldBe BerryPlots.STAGE_SAPLING
        at(12.0).stage shouldBe BerryPlots.STAGE_FLOWERING
        // Dry since 7 h: dead after DRY_DEATH_HOURS more.
        at(14.9).death shouldBe null
        at(15.0).death shouldBe BerryPlots.Death.DRY
      }

      test("a watered Cheri ripens and withers when left unpicked") {
        val t0 = 1_000_000
        val vars =
            mapOf(
                "p/seeds" to BerryPlots.packSeeds(listOf(plainSpicy, plainSpicy, plainSpicy)),
                "p/planted" to t0,
                "p/water" to 5,
                "p/waterAt" to t0 + 15 * 60)
        val plot = BerryPlots.Plot(vars, "p")
        fun at(hours: Double) = BerryPlots.view(plot, t0 + (hours * 60).toInt())
        at(16.0).stage shouldBe BerryPlots.STAGE_RIPE
        at(16.0).death shouldBe null
        at(23.9).death shouldBe null
        at(24.0).death shouldBe BerryPlots.Death.UNPICKED
        at(24.0).stage shouldBe BerryPlots.STAGE_DEAD
      }

      test("the packed state carries stage and droplets where the client reads them") {
        val view = BerryPlots.View(BerryPlots.STAGE_RIPE, 4, null, null, 0.0, 0.0)
        BerryPlots.packedState(view) shouldBe ((4 shl 12) or (5 shl 8))
      }

      test("empty soil renders as the empty stage") {
        BerryPlots.view(BerryPlots.Plot(emptyMap(), "p")).stage shouldBe BerryPlots.STAGE_EMPTY
      }
    })
