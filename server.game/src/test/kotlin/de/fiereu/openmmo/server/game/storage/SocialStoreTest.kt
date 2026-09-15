package de.fiereu.openmmo.server.game.storage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SocialStoreTest :
    FunSpec({
      test("friends start empty per user and survive add/remove") {
        val store = SocialStore()
        store.getFriends(1) shouldBe emptySet()

        store.addFriend(1, "Yellow")
        store.addFriend(1, "Red")
        store.getFriends(1) shouldBe linkedSetOf("Yellow", "Red")
        store.getFriends(2) shouldBe emptySet()

        store.removeFriend(1, "Red") shouldBe true
        store.getFriends(1).contains("Red") shouldBe false
        store.removeFriend(1, "Red") shouldBe false
      }

      test("a friend keeps the moment it was added, and loses it when removed") {
        val store = SocialStore()
        val added = java.time.LocalDateTime.of(2026, 9, 14, 20, 0)

        store.addFriend(1, "Argeno", added)
        // Adding the same friend again does not move the date.
        store.addFriend(1, "Argeno", added.plusDays(1))

        store.friendsSince(1) shouldBe mapOf("Argeno" to added)
        store.removeFriend(1, "Argeno") shouldBe true
        store.friendsSince(1) shouldBe emptyMap()
      }

      test("block list is independent per user") {
        val store = SocialStore()
        store.getBlocked(1) shouldBe emptySet()

        store.block(1, "Troll")
        store.getBlocked(1) shouldBe setOf("Troll")
        store.getBlocked(2) shouldBe emptySet()

        store.unblock(1, "Troll") shouldBe true
        store.getBlocked(1) shouldBe emptySet()
      }
    })
