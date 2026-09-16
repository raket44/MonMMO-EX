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

      test("a charm is active until its stored expiry passes") {
        val now = (System.currentTimeMillis() / 1000).toInt()
        Boosts.isActive(withVar(Boosts.Kind.SHINY.key, now + 60), Boosts.Kind.SHINY) shouldBe true
        Boosts.isActive(withVar(Boosts.Kind.SHINY.key, now - 60), Boosts.Kind.SHINY) shouldBe false
        // Never used at all.
        Boosts.isActive(withVar("other", 1), Boosts.Kind.SHINY) shouldBe false
      }

      test("using one now expires about an hour out") {
        val now = (System.currentTimeMillis() / 1000).toInt()
        Boosts.expiryFromNow() shouldBeGreaterThan now + Boosts.DURATION_SECONDS - 5
      }
    })
