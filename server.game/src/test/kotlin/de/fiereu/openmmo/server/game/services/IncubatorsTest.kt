package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The client (f/be0.U00) sums store-10 flags 771..774 with weights 5, 1, 1, 1 and locks
 * `8 - total` of the eight permanent incubator slots. Its gate f/o80.Yt0 accepts store 10 only for
 * flag 770 and the four ids in f/qy4.gY1, so these numbers cannot drift without the page silently
 * showing nothing. Owner's mapping (2026-09-16): first championship 771, first daycare man 772,
 * the other two held back.
 */
class IncubatorsTest :
    FunSpec({
      test("the unlock flags match the ids and store the client gate accepts") {
        Incubators.STORE shouldBe 10.toByte()
        Incubators.FIRST_CHAMPION_FLAG shouldBe 771
        Incubators.DAYCARE_MAN_FLAG shouldBe 772
        Incubators.RESERVED_FLAGS shouldContainExactly listOf(773, 774)
      }

      test("both unlock packets address the incubator store with a set value") {
        val champion = Incubators.firstChampionPacket()
        champion.regionId shouldBe 10.toByte()
        champion.flagId shouldBe 771
        champion.enabled shouldBe true

        val daycare = Incubators.daycareManPacket()
        daycare.regionId shouldBe 10.toByte()
        daycare.flagId shouldBe 772
        daycare.enabled shouldBe true
      }

      test("the championship and daycare unlocks add up to six of the eight slots") {
        val weights = mapOf(771 to 5, 772 to 1, 773 to 1, 774 to 1)
        weights.getValue(Incubators.FIRST_CHAMPION_FLAG) +
            weights.getValue(Incubators.DAYCARE_MAN_FLAG) shouldBe 6
        weights.values.sum() shouldBe 8
      }
    })
