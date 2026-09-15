package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/** The trainer card's "Time played" was always 0 hours: play time was never counted (2026-09-14). */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayTimeTest :
    FunSpec({
      test("time in the world is banked into play time, and leaving stamps the last-seen moment") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val id = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id

          store.startPlaySession(id)
          val later = System.nanoTime() + 125_000_000_000L
          // The periodic bank waits for a full minute; two minutes and five seconds have passed.
          store.bankPlayTime(id, minSeconds = 60, nowNanos = later)
          store.getCharacter(id)!!.info.playTimeSeconds shouldBe 125

          // Nothing new to bank from the same moment.
          store.bankPlayTime(id, minSeconds = 60, nowNanos = later)
          store.getCharacter(id)!!.info.playTimeSeconds shouldBe 125

          store.endPlaySession(id)
          store.getCharacter(id)!!.info.lastLogout.shouldNotBeNull()
        }
      }

      test("no session, no play time") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val id = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id

          store.bankPlayTime(id, nowNanos = System.nanoTime() + 3_600_000_000_000L)

          store.getCharacter(id)!!.info.playTimeSeconds shouldBe 0
        }
      }
    })
