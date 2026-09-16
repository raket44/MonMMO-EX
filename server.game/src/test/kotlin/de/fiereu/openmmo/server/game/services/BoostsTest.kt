package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

private fun withVar(key: String, value: Int): Map<String, Int> = mapOf(key to value)

/**
 * The charms are timed Boost Items: consumed on use, one hour, one of each type (client strings
 * 101120-101129 and 5991). The Shiny Charm is item 1409 (owner-supplied).
 */
class BoostsTest :
    FunSpec({
      test("the shiny charm is the bag item the owner named, and runs for an hour") {
        Boosts.Kind.SHINY.itemId shouldBe 1409
        Boosts.Kind.byItem(1409) shouldBe Boosts.Kind.SHINY
        Boosts.DURATION_SECONDS shouldBe 3600
      }

      test("an unknown item is not a charm, and neither is a charm we have no id for") {
        Boosts.Kind.byItem(0) shouldBe null
        Boosts.Kind.byItem(9999) shouldBe null
        // The charms whose ids are still unknown must never match item 0.
        Boosts.Kind.entries.filter { it.itemId == 0 }.forEach { Boosts.Kind.byItem(it.itemId) shouldBe null }
      }

      test("a charm is measured in PLAY TIME, so it pauses while logged out") {
        val played = 1000
        Boosts.isActive(played, withVar(Boosts.Kind.SHINY.key, played + 60), Boosts.Kind.SHINY) shouldBe true
        Boosts.isActive(played, withVar(Boosts.Kind.SHINY.key, played - 60), Boosts.Kind.SHINY) shouldBe false
        // Never used at all.
        Boosts.isActive(played, withVar("other", 1), Boosts.Kind.SHINY) shouldBe false
        // Hours of wall clock pass while offline; play time does not move, so the charm survives.
        val expiry = Boosts.expiryFrom(played)
        Boosts.isActive(played, mapOf(Boosts.Kind.SHINY.key to expiry), Boosts.Kind.SHINY) shouldBe true
        Boosts.secondsLeft(played, mapOf(Boosts.Kind.SHINY.key to expiry), Boosts.Kind.SHINY) shouldBe
            Boosts.DURATION_SECONDS
      }

      test("using one costs an hour of play time") {
        Boosts.expiryFrom(0) shouldBe Boosts.DURATION_SECONDS
        Boosts.expiryFrom(500) shouldBeGreaterThan 500
      }
    })
