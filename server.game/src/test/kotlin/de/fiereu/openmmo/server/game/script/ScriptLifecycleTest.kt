package de.fiereu.openmmo.server.game.script

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.DialogStatePacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionResponsePacket
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.NpcService
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.session.ScriptLockScope
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.server.game.testsupport.scriptRunner
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val TEST_ENTITY_ID = 42L
private const val TEST_FLAG = "kanto/FLAG_LIFECYCLE_TEST"

private object TestDialogLine : DialogLine {
  override val textId = 0x123456
}

private data class LifecycleFixture(
    val ctx: ScriptContext,
    val session: FakeSession,
    val dialog: DialogService,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptLifecycleTest :
    FunSpec({
      fun program(lines: List<String>) =
          PretScriptParser.parse(
              id = ScriptId("gba", "firered", "BPRE", "LifecycleTest"),
              storyNamespace = "kanto",
              sourceFile = "lifecycle-test.inc",
              lines = listOf("LifecycleTest::") + lines,
          )

      suspend fun fixture(scope: CoroutineScope): LifecycleFixture {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
        val session = FakeSession(characterId = charId)
        val maps = MapManager()
        val dialog = DialogService()
        val ctx =
            ScriptContext(
                session,
                session.attributes[PLAYER_STATE]!!,
                entityId = TEST_ENTITY_ID,
                dialog = dialog,
                story = StoryService(store),
                movement = ScriptMovementService(maps, NpcService(maps, store), store),
            )
        return LifecycleFixture(ctx, session, dialog)
      }

      fun interpreted(lines: List<String>) =
          InterpretedScript(program(lines), mapOf("Test_Text" to TestDialogLine))

      fun acknowledgeMessage(fixture: LifecycleFixture) {
        fixture.dialog.onInteractive(
            PacketEvent(DialogActionResponsePacket(id = 0, unk = 0), fixture.session))
      }

      test("lock message and closemessage leave the local script lock owned") {
        runTest {
          val fixture = fixture(backgroundScope)
          val job = launch {
            interpreted(listOf("lock", "msgbox Test_Text", "closemessage", "end")).run(fixture.ctx)
          }
          runCurrent()

          fixture.ctx.state.dialogVisible shouldBe true
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
          acknowledgeMessage(fixture)
          job.join()

          fixture.ctx.state.dialogVisible shouldBe false
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
          fixture.ctx.state.scriptLockedEntityId shouldBe TEST_ENTITY_ID
          // lock turns the scripted state ON; closemessage turns it OFF here because this
          // fixture runs the script directly (no runner, so scriptRunning stays false).
          fixture.session.sent.filterIsInstance<DialogStatePacket>().map { it.active } shouldBe
              listOf(true, false)
        }
      }

      test("lock closemessage more instructions and release preserve script flow") {
        runTest {
          val fixture = fixture(backgroundScope)
          fixture.ctx.state.dialogVisible = true

          interpreted(
                  listOf(
                      "lock",
                      "closemessage",
                      "setvar VAR_RESULT, 7",
                      "release",
                      "end",
                  ))
              .run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 7
          fixture.ctx.state.dialogVisible shouldBe false
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("lockall and releaseall update the represented global lock") {
        runTest {
          val fixture = fixture(backgroundScope)
          interpreted(listOf("lockall", "setvar VAR_RESULT, 1", "releaseall", "end"))
              .run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 1
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
          fixture.ctx.state.scriptLockedEntityId shouldBe null
        }
      }

      listOf("release", "releaseall").forEach { command ->
        test("$command followed by end completes normally") {
          runTest {
            val fixture = fixture(backgroundScope)
            fixture.ctx.state.lockAll()

            interpreted(listOf(command, "end")).run(fixture.ctx)

            fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
          }
        }
      }

      test("waitmessage after a message operation resumes the script") {
        runTest {
          val fixture = fixture(backgroundScope)
          val job = launch {
            interpreted(
                    listOf(
                        "lock",
                        "msgbox Test_Text",
                        "waitmessage",
                        "setvar VAR_RESULT, 3",
                        "release",
                        "end",
                    ))
                .run(fixture.ctx)
          }
          runCurrent()
          acknowledgeMessage(fixture)
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("closemessage does not terminate execution") {
        runTest {
          val fixture = fixture(backgroundScope)
          fixture.ctx.state.dialogVisible = true

          interpreted(listOf("lock", "closemessage", "setvar VAR_RESULT, 9", "end"))
              .run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 9
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
        }
      }

      test("nested calls do not lose lifecycle lock ownership") {
        runTest {
          val fixture = fixture(backgroundScope)
          fixture.ctx.state.dialogVisible = true

          interpreted(
                  listOf(
                      "lock",
                      "call Helper",
                      "addvar VAR_RESULT, 2",
                      "end",
                      "Helper::",
                      "closemessage",
                      "addvar VAR_RESULT, 1",
                      "return",
                  ))
              .run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
          fixture.ctx.state.dialogVisible shouldBe false
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
        }
      }

      test("script completion cleans an unreleased lock") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          session.attributes[SCRIPT_SCOPE] = backgroundScope
          val runner = scriptRunner(store)

          runner.run(session, session.state(), Script { it.lockAll() }, TEST_ENTITY_ID)
          runCurrent()

          session.state().scriptRunning shouldBe false
          session.state().dialogVisible shouldBe false
          session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("release unlocks input without ending active script ownership") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          session.attributes[SCRIPT_SCOPE] = backgroundScope
          val parked = CompletableDeferred<Unit>()
          val runner = scriptRunner(store)
          val releasing = Script {
            it.release()
            parked.await()
          }

          runner.run(session, session.state(), releasing, TEST_ENTITY_ID)
          runCurrent()

          session.state().scriptRunning shouldBe true
          session.state().scriptLockScope shouldBe ScriptLockScope.NONE
          session.state().blocksPlayerInput shouldBe false
          session.state().blocksNewScript shouldBe true

          parked.complete(Unit)
          runCurrent()
          session.state().scriptRunning shouldBe false
        }
      }

      test("script error rolls back and releases owned state") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
          val session = FakeSession(characterId = charId)
          session.attributes[SCRIPT_SCOPE] = backgroundScope
          val runner = scriptRunner(store)
          val failing = Script {
            it.setFlag(TEST_FLAG)
            it.lockAll()
            error("lifecycle test failure")
          }

          runner.run(session, session.state(), failing, TEST_ENTITY_ID)
          runCurrent()

          store.getCharacter(charId)!!.storyFlags.contains(TEST_FLAG) shouldBe false
          session.state().scriptRunning shouldBe false
          session.state().dialogVisible shouldBe false
          session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }
    })
