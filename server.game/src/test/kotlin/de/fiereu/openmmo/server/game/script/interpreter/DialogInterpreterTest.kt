package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.DialogStatePacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionPacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionResponsePacket
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.script.TextArg
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.NpcService
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.DialogMessageMode
import de.fiereu.openmmo.server.game.session.DialogTextColor
import de.fiereu.openmmo.server.game.session.PENDING_DIALOG
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.ScriptLockScope
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private object FirstText : DialogLine {
  override val textId = 0x123456
}

private object SecondText : DialogLine {
  override val textId = 0x654321
}

private data class DialogFixture(
    val ctx: ScriptContext,
    val session: FakeSession,
    val dialog: DialogService,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DialogInterpreterTest :
    FunSpec({
      fun program(lines: List<String>, source: String = "firered") =
          PretScriptParser.parse(
              id = ScriptId("gba", source, if (source == "firered") "BPRE" else "BPEE", "Test"),
              storyNamespace = if (source == "firered") "kanto" else "hoenn",
              sourceFile = "dialog-test.inc",
              lines = listOf("Test::") + lines,
          )

      suspend fun fixture(scope: CoroutineScope): DialogFixture {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
        val session = FakeSession(characterId = charId)
        val maps = MapManager()
        val dialog = DialogService()
        val ctx =
            ScriptContext(
                session,
                session.attributes[PLAYER_STATE]!!,
                entityId = 42,
                dialog = dialog,
                story = StoryService(store),
                movement = ScriptMovementService(maps, NpcService(maps, store), store),
            )
        return DialogFixture(ctx, session, dialog)
      }

      fun script(lines: List<String>, source: String = "firered") =
          InterpretedScript(
              program(lines, source),
              mapOf("First_Text" to FirstText, "Second_Text" to SecondText),
          )

      fun respond(fixture: DialogFixture, result: Int = 0) {
        fixture.dialog.onInteractive(
            PacketEvent(DialogActionResponsePacket(id = 0, unk = result), fixture.session))
      }

      test("parser types message text references for both GBA sources") {
        listOf("firered", "emerald").forEach { source ->
          val parsed = program(listOf("message First_Text", "end"), source)
          parsed.instructions.first().args.single() shouldBe TextArg("First_Text")
        }
      }

      test("message followed by waitbuttonpress suspends until dialog acknowledgement") {
        runTest {
          val fixture = fixture(backgroundScope)
          val job = launch {
            script(
                    listOf(
                        "message First_Text",
                        "waitbuttonpress",
                        "setvar VAR_RESULT, 7",
                        "end",
                    ))
                .run(fixture.ctx)
          }
          runCurrent()

          job.isCompleted shouldBe false
          fixture.session.attributes.contains(PENDING_DIALOG) shouldBe true
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 0

          respond(fixture)
          job.join()
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 7
        }
      }

      test("message followed by closemessage closes without waiting or ending the script") {
        runTest {
          val fixture = fixture(backgroundScope)

          script(
                  listOf(
                      "message First_Text",
                      "closemessage",
                      "setvar VAR_RESULT, 3",
                      "end",
                  ))
              .run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
          fixture.ctx.state.dialogVisible shouldBe false
          fixture.session.sent.filterIsInstance<DialogStatePacket>().size shouldBe 1
        }
      }

      listOf(1 to 1, 0 to 0).forEach { (clientResult, expected) ->
        val answer = if (expected == 1) "yes" else "no"
        test("yesnobox stores the authentic $answer result in VAR_RESULT") {
          runTest {
            val fixture = fixture(backgroundScope)
            val job = launch {
              script(
                      listOf(
                          "message First_Text",
                          "waitmessage",
                          "yesnobox 20, 8",
                          "end",
                      ))
                  .run(fixture.ctx)
            }
            runCurrent()
            respond(fixture)
            runCurrent()
            respond(fixture, clientResult)
            job.join()

            fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe expected
          }
        }
      }

      test("textcolor retains authentic FireRed presentation state") {
        runTest {
          val fixture = fixture(backgroundScope)

          script(listOf("textcolor NPC_TEXT_COLOR_FEMALE", "end")).run(fixture.ctx)

          fixture.ctx.state.dialogTextColor shouldBe DialogTextColor.FEMALE
        }
      }

      test("signmsg and normalmsg change subsequent FireRed message frames") {
        runTest {
          val fixture = fixture(backgroundScope)

          script(
                  listOf(
                      "signmsg",
                      "message First_Text",
                      "closemessage",
                      "normalmsg",
                      "message Second_Text",
                      "closemessage",
                      "end",
                  ))
              .run(fixture.ctx)

          fixture.session.sent
              .filterIsInstance<DialogActionPacket>()
              .map { it.actionType.toInt() }
              .shouldBe(listOf(3, 4))
          fixture.ctx.state.dialogMessageMode shouldBe DialogMessageMode.NORMAL
        }
      }

      test("lifecycle lock survives dialog waiting") {
        runTest {
          val fixture = fixture(backgroundScope)
          val job = launch {
            script(
                    listOf(
                        "lock",
                        "message First_Text",
                        "waitbuttonpress",
                        "release",
                        "end",
                    ))
                .run(fixture.ctx)
          }
          runCurrent()

          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
          respond(fixture)
          job.join()
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("call stack survives dialog waiting") {
        runTest {
          val fixture = fixture(backgroundScope)
          val job = launch {
            script(
                    listOf(
                        "call Helper",
                        "addvar VAR_RESULT, 2",
                        "end",
                        "Helper::",
                        "message First_Text",
                        "waitbuttonpress",
                        "addvar VAR_RESULT, 1",
                        "return",
                    ))
                .run(fixture.ctx)
          }
          runCurrent()
          respond(fixture)
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
        }
      }

      test("unsupported FireRed textcolor values fail with source context") {
        runTest {
          val fixture = fixture(backgroundScope)

          val error =
              shouldThrow<IllegalStateException> {
                script(listOf("textcolor NPC_TEXT_COLOR_UNKNOWN", "end")).run(fixture.ctx)
              }

          error.message shouldBe
              "Script gba:firered:BPRE:Test cannot apply textcolor value " +
                  "NPC_TEXT_COLOR_UNKNOWN from `textcolor NPC_TEXT_COLOR_UNKNOWN`"
        }
      }

      test("Emerald accepts presentation syntax as authentic no-op commands") {
        runTest {
          val fixture = fixture(backgroundScope)

          script(
                  listOf(
                      "textcolor NPC_TEXT_COLOR_UNKNOWN",
                      "signmsg",
                      "normalmsg",
                      "end",
                  ),
                  source = "emerald",
              )
              .run(fixture.ctx)

          fixture.ctx.state.dialogTextColor shouldBe DialogTextColor.DEFAULT
          fixture.ctx.state.dialogMessageMode shouldBe DialogMessageMode.NORMAL
        }
      }
    })
