package de.fiereu.openmmo.server.game.developer

import de.fiereu.openmmo.server.game.testsupport.testOcarinas
import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.ChatMessagePacket
import de.fiereu.openmmo.net.game.packets.MapTransitionPacket
import de.fiereu.openmmo.server.game.config.DeveloperToolsConfig
import de.fiereu.openmmo.server.game.config.GameServerConfig
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import de.fiereu.openmmo.server.game.services.DexProgressService
import de.fiereu.openmmo.server.game.services.MapLoadService
import de.fiereu.openmmo.server.game.services.PresenceService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.services.WarpRules
import de.fiereu.openmmo.server.game.services.WarpService
import de.fiereu.openmmo.server.game.services.WorldStateService
import de.fiereu.openmmo.server.game.services.command.CommandContext
import de.fiereu.openmmo.server.game.services.command.DeveloperStoryCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperTeleportCommand
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.server.game.testsupport.battleService
import de.fiereu.openmmo.server.game.world.interest.InterestManager
import de.fiereu.openmmo.server.game.world.interest.PassThroughInterestPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

private const val TEST_LABEL = "Test_EventScript_A"
private const val OTHER_LABEL = "Test_EventScript_B"
private const val TEST_ID = "gba:firered:BPRE:$TEST_LABEL"

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperToolsTest :
    FunSpec({
      fun config(enabled: Boolean) =
          GameServerConfig(
              host = "127.0.0.1",
              port = 0,
              checksumSize = 2,
              rootKeyResource = "game.private.pem",
              sessionSecret = "test-secret".toByteArray(),
              developer = DeveloperToolsConfig(enabled = enabled),
          )

      val kotlinA = Script {}
      val kotlinB = Script {}
      val interpretedA = Script {}

      fun registry(tools: DeveloperTools) =
          ScriptRegistry(
              byLabel = mapOf(TEST_LABEL to kotlinA, OTHER_LABEL to kotlinB),
              interpretedById = mapOf(TEST_ID to interpretedA),
              interpretedByBareLabel = mapOf(TEST_LABEL to interpretedA),
              developerTools = tools,
          )

      suspend fun commandContext(
          scope: CoroutineScope,
          args: List<String>,
      ): Triple<CommandContext, CharacterStore, FakeSession> {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val created = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO)
        store.updateCharacter(
            created.info.copy(
                permissions = created.info.permissions or CharacterPermissions.DEVELOPER))
        val character = store.getCharacter(created.info.id)!!
        val session =
            FakeSession(
                characterId = character.info.id,
                regionId = character.info.positionRegionId.toInt(),
                bankId = character.info.positionBankId.toInt(),
                mapId = character.info.positionMapId.toInt(),
            )
        return Triple(
            CommandContext(
                session,
                session.attributes[PLAYER_STATE]!!,
                character,
                args,
                emptyList(),
            ),
            store,
            session,
        )
      }

      test("override off keeps Kotlin priority") {
        val tools = DeveloperTools(config(enabled = true))

        registry(tools).forLabel(TEST_LABEL).shouldBeSameInstanceAs(kotlinA)
      }

      test("override on by stable id selects the interpreted script") {
        val tools = DeveloperTools(config(enabled = true))
        tools.enableOverride(TEST_ID)

        registry(tools).forLabel(TEST_LABEL).shouldBeSameInstanceAs(interpretedA)
      }

      test("override only affects its named script") {
        val tools = DeveloperTools(config(enabled = true))
        tools.enableOverride(TEST_LABEL)
        val registry = registry(tools)

        registry.forLabel(TEST_LABEL).shouldBeSameInstanceAs(interpretedA)
        registry.forLabel(OTHER_LABEL).shouldBeSameInstanceAs(kotlinB)
      }

      test("teleport command uses the existing warp flow") {
        runTest {
          val (ctx, store, session) =
              commandContext(backgroundScope, listOf("ViridianCity", "22", "11"))
          val maps = MapManager()
          val mapLoad = MapLoadService(maps)
          val interest = InterestManager()
          val presence = PresenceService(interest, PassThroughInterestPolicy(), mapLoad, store)
          val command =
              DeveloperTeleportCommand(
                  DeveloperTools(config(enabled = true)),
                  maps,
                  WarpService(mapLoad, maps, store, presence, WarpRules()),
                  battleService(store, interest),
              )

          command.run(ctx)

          val position = store.getCharacter(ctx.characterId)!!.info
          position.positionRegionId shouldBe 0.toByte()
          position.positionBankId shouldBe 3.toByte()
          position.positionMapId shouldBe 1.toByte()
          position.positionX shouldBe 22.toShort()
          position.positionY shouldBe 11.toShort()
          session.sent.any { it is MapTransitionPacket } shouldBe true
        }
      }

      test("story command reads and writes through StoryService") {
        runTest {
          val (ctx, store, _) = commandContext(backgroundScope, emptyList())
          val tools = DeveloperTools(config(enabled = true))
          val story = StoryService(store)
          val command =
              DeveloperStoryCommand(
                  tools, story, store, WorldStateService(DexProgressService(store), testOcarinas(store)))

          command.run(ctx.copyArgs("flag", "set", "FLAG_DEV_TEST"))
          command.run(ctx.copyArgs("var", "set", "VAR_DEV_TEST", "37"))

          story.isFlagSet(ctx.characterId, "kanto/FLAG_DEV_TEST") shouldBe true
          story.getVar(ctx.characterId, "kanto/VAR_DEV_TEST") shouldBe 37

          command.run(ctx.copyArgs("flag", "clear", "FLAG_DEV_TEST"))
          story.isFlagSet(ctx.characterId, "kanto/FLAG_DEV_TEST") shouldBe false
        }
      }

      test("developer commands are inert when server configuration disables them") {
        runTest {
          val (ctx, store, session) = commandContext(backgroundScope, emptyList())
          val tools = DeveloperTools(config(enabled = false))
          val story = StoryService(store)
          val command =
              DeveloperStoryCommand(
                  tools, story, store, WorldStateService(DexProgressService(store), testOcarinas(store)))

          tools.enableOverride(TEST_LABEL) shouldBe false
          registry(tools).forLabel(TEST_LABEL).shouldBeSameInstanceAs(kotlinA)
          command.run(ctx.copyArgs("flag", "set", "FLAG_MUST_NOT_CHANGE"))

          story.isFlagSet(ctx.characterId, "kanto/FLAG_MUST_NOT_CHANGE") shouldBe false
          session.sent.filterIsInstance<ChatMessagePacket>().last().message shouldBe
              "Developer tools are disabled in the server configuration."
        }
      }
    })

private fun CommandContext.copyArgs(vararg newArgs: String): CommandContext =
    CommandContext(session, state, character, newArgs.toList(), commands)
