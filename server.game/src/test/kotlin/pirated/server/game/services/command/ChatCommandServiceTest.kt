package pirated.server.game.services.command

import pirated.common.CharacterPermissions
import pirated.common.enums.CharacterGender
import pirated.common.enums.Region
import pirated.net.game.packets.ChatMessagePacket
import pirated.server.game.storage.CharacterStore
import pirated.server.game.storage.EntityIdService
import pirated.server.game.testsupport.FakeCharacterRepository
import pirated.server.game.testsupport.FakeSession
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/** Dev tools off, so the permission checks under test actually gate. */
private fun disabledTools() =
    pirated.server.game.developer.DeveloperTools(
        pirated.server.game.config.GameServerConfig(
            host = "127.0.0.1",
            port = 0,
            checksumSize = 2,
            rootKeyResource = "game.private.pem",
            sessionSecret = "test-secret".toByteArray(),
            developer = pirated.server.game.config.DeveloperToolsConfig(enabled = false),
        ))

private class RecordingCommand(
    override val permission: Int = 0,
    private val fail: Boolean = false,
) : ChatCommand {
  override val name = "secret"
  override val usage = "/secret"
  override val description = "only for developers"
  var ran = false

  override suspend fun run(ctx: CommandContext) {
    ran = true
    if (fail) error("boom")
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatCommandServiceTest :
    FunSpec({
      fun FakeSession.replies() = sent.filterIsInstance<ChatMessagePacket>().map { it.message }

      test("ordinary chat is left alone") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "hello there") shouldBe false
          session.sent shouldBe emptyList()
        }
      }

      test("pos reports the tile the player stands on") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId, regionId = 0, bankId = 4, mapId = 3)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "/pos") shouldBe true

          session.replies().single() shouldContain "region 0 bank 4 map 3"
        }
      }

      test("a command name is matched without case and around stray spaces") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "  /PoS  ") shouldBe true

          session.replies().single() shouldContain "region"
        }
      }

      test("help lists only the commands the caller may run") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val gated = RecordingCommand(CharacterPermissions.DEVELOPER)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand(), gated), disabledTools())

          service.tryHandle(session, "/help") shouldBe true

          session.replies().any { it.startsWith("/secret") } shouldBe false
          session.replies().any { it.startsWith("/pos") } shouldBe true
        }
      }

      test("an unknown command is answered without echoing the rest of the line") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "/nonsense hunter2") shouldBe true

          val reply = session.replies().single()
          reply shouldContain "Unknown command: nonsense"
          reply.contains("hunter2") shouldBe false
        }
      }

      test("a gated command needs its permission bit") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val gated = RecordingCommand(CharacterPermissions.DEVELOPER)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand(), gated), disabledTools())

          service.tryHandle(session, "/secret") shouldBe true
          gated.ran shouldBe false
          session.replies().single() shouldContain "Unknown command"

          val info = store.getCharacter(charId)!!.info
          store.updateCharacter(
              info.copy(permissions = info.permissions or CharacterPermissions.DEVELOPER))
          session.sent.clear()

          service.tryHandle(session, "/secret") shouldBe true
          gated.ran shouldBe true
          session.sent shouldBe emptyList()
        }
      }

      test("a staff rank of DEV or higher runs developer commands without the bit; a lower rank does not") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val gated = RecordingCommand(CharacterPermissions.DEVELOPER)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand(), gated), disabledTools())
          val info = store.getCharacter(charId)!!.info

          store.updateCharacter(info.copy(permissions = pirated.common.withClientStaffLevel(info.permissions, 8)))
          service.tryHandle(session, "/secret") shouldBe true
          gated.ran shouldBe false

          store.updateCharacter(
              info.copy(
                  permissions =
                      pirated.common.withClientStaffLevel(info.permissions, CharacterPermissions.CLIENT_STAFF_LEVEL_FULL)))
          session.sent.clear()
          service.tryHandle(session, "/secret") shouldBe true
          gated.ran shouldBe true
        }
      }

      test("a command that throws answers instead of killing the connection") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val service =
              ChatCommandService(
                  store, setOf(RecordingCommand(permission = 0, fail = true)), disabledTools())

          service.tryHandle(session, "/secret") shouldBe true

          session.replies().single() shouldContain "/secret failed."
        }
      }

      test("a bare slash points at help") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "/   ") shouldBe true
          session.replies().single() shouldContain "/help"
        }
      }

      test("a command from a session with no character is refused") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val session = FakeSession()
          val service =
              ChatCommandService(store, setOf(HelpCommand(), PosCommand()), disabledTools())

          service.tryHandle(session, "/pos") shouldBe true
          session.replies().single() shouldContain "not in the world"
        }
      }
    })
