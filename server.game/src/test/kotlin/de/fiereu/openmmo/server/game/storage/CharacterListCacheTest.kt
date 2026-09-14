package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/**
 * The character list must always show every character the account owns (2026-09-14: after a
 * deploy's mass reconnect one account's list showed only NEW CHARACTER, and a character created
 * then appeared alone, while all four old ones sat untouched in the database).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterListCacheTest :
    FunSpec({
      test("a character created before the account's list was loaded does not hide the older ones") {
        runTest {
          val repo = FakeCharacterRepository()
          // An older character, already in the database but not in this (restarted) server's cache.
          CharacterStore(repo, EntityIdService(), backgroundScope)
              .createCharacter(6, "Argeno", CharacterGender.MALE, Region.KANTO)
          val store = CharacterStore(repo, EntityIdService(), backgroundScope)

          store.createCharacter(6, "TeamRocketGirl", CharacterGender.FEMALE, Region.KANTO)

          store.getCharactersByUser(6).map { it.info.name } shouldContainExactlyInAnyOrder
              listOf("Argeno", "TeamRocketGirl")
        }
      }

      test("a list whose characters were evicted underneath it reloads them from the database") {
        runTest {
          val repo = FakeCharacterRepository()
          val store = CharacterStore(repo, EntityIdService(), backgroundScope)
          store.createCharacter(6, "Argeno", CharacterGender.MALE, Region.KANTO)
          store.createCharacter(6, "Red", CharacterGender.MALE, Region.KANTO)
          store.getCharactersByUser(6)

          // What the eviction race leaves behind: the account's index still names the characters,
          // the cache no longer holds them.
          @Suppress("UNCHECKED_CAST")
          val cached =
              CharacterStore::class.java.getDeclaredField("characters").apply { isAccessible = true }.get(store)
                  as ConcurrentHashMap<Long, StoredCharacter>
          cached.clear()

          store.getCharactersByUser(6).map { it.info.name } shouldContainExactlyInAnyOrder
              listOf("Argeno", "Red")
        }
      }
    })
