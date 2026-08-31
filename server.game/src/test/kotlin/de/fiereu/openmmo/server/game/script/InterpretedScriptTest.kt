package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.script.TextArg
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import de.fiereu.openmmo.server.game.script.interpreter.UnsupportedScriptCommandException
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.NpcService
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

private const val PALLET_SIGN_ID = "gba:firered:BPRE:PalletTown_EventScript_OaksLabSign"
private const val PLAYERS_HOUSE_SIGN_LABEL = "PalletTown_EventScript_PlayersHouseSign"
private const val PLAYERS_HOUSE_SIGN_ID = "gba:firered:BPRE:$PLAYERS_HOUSE_SIGN_LABEL"
private const val MT_MOON_FOSSILS_ID = "gba:firered:BPRE:MtMoon_B2F_OnTransition"

@OptIn(ExperimentalCoroutinesApi::class)
class InterpretedScriptTest :
    FunSpec({
      fun program(label: String, lines: List<String>) =
          PretScriptParser.parse(
              id = ScriptId("gba", "firered", "BPRE", label),
              storyNamespace = "kanto",
              sourceFile = "test.inc",
              lines = lines,
          )

      suspend fun context(scope: CoroutineScope): ScriptContext {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val charId = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO).info.id
        val session = FakeSession(characterId = charId, regionId = 0, bankId = 3, mapId = 0)
        val mapManager = MapManager()
        return ScriptContext(
            session,
            session.attributes[PLAYER_STATE]!!,
            entityId = -1,
            dialog = DialogService(),
            story = StoryService(store),
            movement = ScriptMovementService(mapManager, NpcService(mapManager, store), store),
        )
      }

      suspend fun runProgram(scope: CoroutineScope, lines: List<String>): ScriptContext {
        val ctx = context(scope)
        InterpretedScript(program("Test", lines), emptyMap()).run(ctx)
        return ctx
      }

      test("parser keeps instructions, typed args, source lines, and labels") {
        val parsed =
            program(
                "PalletTown_EventScript_OaksLabSign",
                listOf(
                    "PalletTown_EventScript_OaksLabSign::",
                    "msgbox PalletTown_Text_OakPokemonResearchLab, MSGBOX_SIGN",
                    "end",
                ),
            )

        parsed.id.stable shouldBe PALLET_SIGN_ID
        parsed.labels["PalletTown_EventScript_OaksLabSign"] shouldBe 0
        parsed.instructions.map { it.command } shouldBe listOf("msgbox", "end")
        parsed.instructions[0].args[0] shouldBe TextArg("PalletTown_Text_OakPokemonResearchLab")
        parsed.instructions[0].args[1].token shouldBe "MSGBOX_SIGN"
        parsed.instructions[0].sourceLine shouldBe
            "msgbox PalletTown_Text_OakPokemonResearchLab, MSGBOX_SIGN"
      }

      test("parser normalizes switch and case to existing instructions") {
        val parsed =
            program(
                "Test",
                listOf(
                    "Test::",
                    "switch VAR_TEMP_1",
                    "case 7, Match",
                    "end",
                    "Match::",
                    "end",
                ),
            )

        parsed.instructions.map { it.command } shouldBe
            listOf("copyvar", "compare", "goto_if_eq", "end", "end")
        parsed.instructions[0].sourceLine shouldBe "switch VAR_TEMP_1"
        parsed.instructions[1].sourceLine shouldBe "case 7, Match"
        parsed.instructions[2].sourceLine shouldBe "case 7, Match"
        parsed.labels["Match"] shouldBe 4
      }

      listOf(
              Triple("first", 1, 10),
              Triple("middle", 2, 20),
              Triple("last", 3, 30),
          )
          .forEach { (position, selected, expected) ->
            test("switch selects the $position matching case") {
              runTest {
                val ctx =
                    runProgram(
                        backgroundScope,
                        listOf(
                            "Test::",
                            "setvar VAR_TEMP_1, $selected",
                            "switch VAR_TEMP_1",
                            "case 1, First",
                            "case 2, Middle",
                            "case 3, Last",
                            "setvar VAR_RESULT, 99",
                            "end",
                            "First::",
                            "setvar VAR_RESULT, 10",
                            "end",
                            "Middle::",
                            "setvar VAR_RESULT, 20",
                            "end",
                            "Last::",
                            "setvar VAR_RESULT, 30",
                            "end",
                        ),
                    )

                ctx.getVar("kanto/VAR_RESULT") shouldBe expected
              }
            }
          }

      test("switch with no matching case falls through") {
        runTest {
          val ctx =
              runProgram(
                  backgroundScope,
                  listOf(
                      "Test::",
                      "setvar VAR_TEMP_1, 4",
                      "switch VAR_TEMP_1",
                      "case 1, First",
                      "case 2, Second",
                      "case 3, Third",
                      "setvar VAR_RESULT, 99",
                      "end",
                      "First::",
                      "setvar VAR_RESULT, 1",
                      "end",
                      "Second::",
                      "setvar VAR_RESULT, 2",
                      "end",
                      "Third::",
                      "setvar VAR_RESULT, 3",
                      "end",
                  ),
              )

          ctx.getVar("kanto/VAR_RESULT") shouldBe 99
        }
      }

      test("sequential switches replace the shared switch value") {
        runTest {
          val ctx =
              runProgram(
                  backgroundScope,
                  listOf(
                      "Test::",
                      "setvar VAR_TEMP_1, 2",
                      "setvar VAR_TEMP_2, 3",
                      "switch VAR_TEMP_1",
                      "case 2, FirstMatch",
                      "setvar VAR_RESULT, 99",
                      "end",
                      "FirstMatch::",
                      "switch VAR_TEMP_2",
                      "case 3, SecondMatch",
                      "setvar VAR_RESULT, 98",
                      "end",
                      "SecondMatch::",
                      "setvar VAR_RESULT, 7",
                      "end",
                  ),
              )

          ctx.getVar("kanto/VAR_RESULT") shouldBe 7
        }
      }

      test("switch matches unsigned 16-bit values zero and 65535") {
        runTest {
          val ctx =
              runProgram(
                  backgroundScope,
                  listOf(
                      "Test::",
                      "setvar VAR_TEMP_1, 0",
                      "setvar VAR_TEMP_2, 65535",
                      "switch VAR_TEMP_1",
                      "case 0, Zero",
                      "setvar VAR_RESULT, 99",
                      "end",
                      "Zero::",
                      "setvar VAR_RESULT, 1",
                      "switch VAR_TEMP_2",
                      "case 65535, Max",
                      "setvar VAR_RESULT, 99",
                      "end",
                      "Max::",
                      "addvar VAR_RESULT, 1",
                      "end",
                  ),
              )

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("case goto preserves an existing call return address") {
        runTest {
          val ctx =
              runProgram(
                  backgroundScope,
                  listOf(
                      "Test::",
                      "setvar VAR_TEMP_1, 2",
                      "call Dispatch",
                      "addvar VAR_RESULT, 2",
                      "end",
                      "Dispatch::",
                      "switch VAR_TEMP_1",
                      "case 2, Match",
                      "return",
                      "Match::",
                      "addvar VAR_RESULT, 1",
                      "return",
                  ),
              )

          ctx.getVar("kanto/VAR_RESULT") shouldBe 3
        }
      }

      test("goto resolves labels") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "goto Done",
                          "setvar VAR_RESULT, 0",
                          "Done::",
                          "setvar VAR_RESULT, 7",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 7
        }
      }

      test("setflag and clearflag update story state") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf("Test::", "setflag FLAG_TEST", "clearflag FLAG_TEST", "end"),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.isFlagSet("kanto/FLAG_TEST") shouldBe false
        }
      }

      test("setvar and goto_if_eq update story state") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 3",
                          "goto_if_eq VAR_TEMP_1, 3, Match",
                          "setvar VAR_RESULT, 0",
                          "end",
                          "Match::",
                          "setvar VAR_RESULT, 1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_TEMP_1") shouldBe 3
          ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }

      test("compare equal feeds the one-argument conditional form") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 7",
                          "compare VAR_TEMP_1, 7",
                          "goto_if_eq Equal",
                          "setvar VAR_RESULT, 0",
                          "end",
                          "Equal::",
                          "setvar VAR_RESULT, 1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }

      test("compare not equal feeds goto_if_ne") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 7",
                          "compare VAR_TEMP_1, 8",
                          "goto_if_ne Different",
                          "setvar VAR_RESULT, 0",
                          "end",
                          "Different::",
                          "setvar VAR_RESULT, 1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }

      test("less and greater goto conditions follow the GBA comparison table") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 2",
                          "goto_if_lt VAR_TEMP_1, 3, Less",
                          "end",
                          "Less::",
                          "setvar VAR_TEMP_2, 5",
                          "goto_if_gt VAR_TEMP_2, 3, Greater",
                          "end",
                          "Greater::",
                          "goto_if_le VAR_TEMP_1, 2, LessOrEqual",
                          "end",
                          "LessOrEqual::",
                          "goto_if_ge VAR_TEMP_2, 5, GreaterOrEqual",
                          "end",
                          "GreaterOrEqual::",
                          "setvar VAR_RESULT, 1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }

      test("copyvar copies through ScriptContext story variables") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 42",
                          "copyvar VAR_RESULT, VAR_TEMP_1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 42
        }
      }

      test("setorcopyvar stores an immediate value") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf("Test::", "setorcopyvar VAR_RESULT, 0x1234", "end"),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 0x1234
        }
      }

      test("setorcopyvar copies from an existing variable") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 77",
                          "setorcopyvar VAR_RESULT, VAR_TEMP_1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 77
        }
      }

      test("addvar wraps unsigned 16-bit overflow") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_RESULT, 65535",
                          "addvar VAR_RESULT, 1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 0
        }
      }

      test("subvar reads variables and wraps unsigned 16-bit underflow") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_RESULT, 1",
                          "setvar VAR_TEMP_1, 2",
                          "subvar VAR_RESULT, VAR_TEMP_1",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 65535
        }
      }

      test("call_if_set taken returns to the following instruction") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setflag FLAG_READY",
                          "call_if_set FLAG_READY, Helper",
                          "addvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "addvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 3
        }
      }

      test("call_if_set not taken continues without pushing a return address") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "call_if_set FLAG_READY, Helper",
                          "setvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("call_if_unset taken returns to the following instruction") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "call_if_unset FLAG_BLOCKED, Helper",
                          "addvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "addvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 3
        }
      }

      test("call_if_unset not taken continues without pushing a return address") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setflag FLAG_BLOCKED",
                          "call_if_unset FLAG_BLOCKED, Helper",
                          "setvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("nested conditional calls share the real call stack") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setflag FLAG_OUTER",
                          "call_if_set FLAG_OUTER, Outer",
                          "end",
                          "Outer::",
                          "addvar VAR_RESULT, 1",
                          "call_if_unset FLAG_BLOCK_INNER, Inner",
                          "addvar VAR_RESULT, 4",
                          "return",
                          "Inner::",
                          "addvar VAR_RESULT, 2",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 7
        }
      }

      test("call and return resume after the call") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "call Helper",
                          "setvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("nested calls unwind in last-in first-out order") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "call Outer",
                          "end",
                          "Outer::",
                          "call Inner",
                          "setvar VAR_RESULT, 2",
                          "return",
                          "Inner::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("call and return cross generated program boundaries") {
        runTest {
          val ctx = context(backgroundScope)
          val root =
              program(
                  "Root",
                  listOf(
                      "Root::",
                      "call SharedHelper",
                      "addvar VAR_RESULT, 2",
                      "end",
                  ),
              )
          val helper =
              program(
                  "SharedHelper",
                  listOf(
                      "SharedHelper::",
                      "setvar VAR_RESULT, 1",
                      "return",
                  ),
              )
          val script =
              InterpretedScript(
                  root,
                  emptyMap(),
                  programLibrary = mapOf("SharedHelper" to helper),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 3
        }
      }

      test("conditional call taken returns to the following instruction") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 5",
                          "call_if_eq VAR_TEMP_1, 5, Helper",
                          "setvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("conditional call not taken does not alter the call stack") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "setvar VAR_TEMP_1, 4",
                          "call_if_eq VAR_TEMP_1, 5, Helper",
                          "setvar VAR_RESULT, 2",
                          "end",
                          "Helper::",
                          "setvar VAR_RESULT, 1",
                          "return",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("return without a call fails with a call stack underflow") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program("Test", listOf("Test::", "return")),
                  emptyMap(),
              )

          val error = shouldThrow<IllegalStateException> { script.run(ctx) }
          error.message shouldBe "Script gba:firered:BPRE:Test call stack underflow at `return`"
        }
      }

      test("unresolved call label fails with source context") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program("Test", listOf("Test::", "call Missing")),
                  emptyMap(),
              )

          val error = shouldThrow<IllegalStateException> { script.run(ctx) }
          error.message shouldBe
              "Script gba:firered:BPRE:Test has no label Missing from `call Missing`"
        }
      }

      test("goto_if_set and goto_if_unset branch on flags") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program(
                      "Test",
                      listOf(
                          "Test::",
                          "goto_if_unset FLAG_READY, Missing",
                          "setvar VAR_RESULT, 0",
                          "end",
                          "Missing::",
                          "setflag FLAG_READY",
                          "goto_if_set FLAG_READY, Ready",
                          "setvar VAR_RESULT, 1",
                          "end",
                          "Ready::",
                          "setvar VAR_RESULT, 2",
                          "end",
                      ),
                  ),
                  emptyMap(),
              )

          script.run(ctx)

          ctx.isFlagSet("kanto/FLAG_READY") shouldBe true
          ctx.getVar("kanto/VAR_RESULT") shouldBe 2
        }
      }

      test("unsupported commands fail with script id command and source line") {
        runTest {
          val ctx = context(backgroundScope)
          val script =
              InterpretedScript(
                  program("Test", listOf("Test::", "special GetPokedexCount", "end")),
                  emptyMap(),
              )

          val error = shouldThrow<UnsupportedScriptCommandException> { script.run(ctx) }
          error.message shouldBe
              "Unsupported script command in gba:firered:BPRE:Test: " +
                  "special GetPokedexCount from `special GetPokedexCount`"
        }
      }

      test("generated registry exposes the interpreted Pallet Town proof script by stable id") {
        val script = ScriptRegistry.generated().forId(PALLET_SIGN_ID)

        (script is InterpretedScript) shouldBe true
      }

      test("generated registry exposes the live Pallet player house sign by stable id") {
        val script = ScriptRegistry.generated().forId(PLAYERS_HOUSE_SIGN_ID)

        (script is InterpretedScript) shouldBe true
      }

      test("generated registry executes the interpreted Mt Moon fossil transition by stable id") {
        runTest {
          val ctx = context(backgroundScope)
          ctx.setFlag("kanto/FLAG_HIDE_DOME_FOSSIL")
          ctx.setFlag("kanto/FLAG_HIDE_HELIX_FOSSIL")
          val script = ScriptRegistry.generated().forId(MT_MOON_FOSSILS_ID)

          (script is InterpretedScript) shouldBe true
          script!!.run(ctx)

          ctx.isFlagSet("kanto/FLAG_HIDE_DOME_FOSSIL") shouldBe false
          ctx.isFlagSet("kanto/FLAG_HIDE_HELIX_FOSSIL") shouldBe false
        }
      }

      test("registered supported interpreters have priority over generated Kotlin scripts") {
        val registry = ScriptRegistry.generated()

        (registry.forLabel(PLAYERS_HOUSE_SIGN_LABEL) is InterpretedScript) shouldBe true
      }

      test("bare label lookup prefers an explicitly registered supported interpreter") {
        runTest {
          val label = "Test"
          val ctx = context(backgroundScope)
          val generated = Script { it.setVar("kanto/VAR_RESULT", 9) }
          val interpreted =
              InterpretedScript(
                  program(label, listOf("Test::", "setvar VAR_RESULT, 1", "end")),
                  emptyMap(),
              )
          val registry =
              ScriptRegistry(
                  byLabel = mapOf(label to generated),
                  interpretedByBareLabel = mapOf(label to interpreted),
              )

          registry.forLabel(label)!!.run(ctx)

          ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }
    })
