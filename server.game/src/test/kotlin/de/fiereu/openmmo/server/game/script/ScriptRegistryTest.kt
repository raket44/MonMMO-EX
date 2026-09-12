package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.script.MovementProgram
import de.fiereu.openmmo.script.PretMovementParser
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.server.game.config.DeveloperToolsConfig
import de.fiereu.openmmo.server.game.config.GameServerConfig
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val LABEL = "Example_EventScript"
private const val SCRIPT_ID = "gba:firered:BPRE:$LABEL"

class ScriptRegistryTest :
    FunSpec({
      val kotlin = Script {}

      test("fully supported interpreted script beats Kotlin") {
        val interpreted = interpreted("setvar VAR_TEST, 7", "end")

        registry(kotlin, interpreted).forLabel(LABEL) shouldBe interpreted
      }

      test("unsupported interpreted script falls back to Kotlin") {
        val interpreted = interpreted("special DoSomethingNative", "end")

        registry(kotlin, interpreted).forLabel(LABEL) shouldBe kotlin
      }

      test("transitive unsupported call causes Kotlin fallback") {
        val interpreted =
            interpreted(
                "call Example_Subroutine",
                "end",
                "Example_Subroutine::",
                "special DoSomethingNative",
                "return",
            )

        registry(kotlin, interpreted).forLabel(LABEL) shouldBe kotlin
      }

      test("unsupported movement causes Kotlin fallback") {
        val movement =
            PretMovementParser.parse(
                id = scriptId("Example_Movement"),
                sourceFile = "test.inc",
                lines = listOf("Example_Movement::", "fly_up", "step_end"),
            )
        val interpreted =
            interpreted(
                "applymovement LOCALID_PLAYER, Example_Movement",
                "waitmovement LOCALID_PLAYER",
                "end",
                movements = mapOf("Example_Movement" to movement),
            )

        registry(kotlin, interpreted).forLabel(LABEL) shouldBe kotlin
      }

      test("unresolved text resource causes Kotlin fallback") {
        val interpreted = interpreted("msgbox Example_Text, MSGBOX_SIGN", "end")

        registry(kotlin, interpreted).forLabel(LABEL) shouldBe kotlin
      }

      test("developer force-Kotlin wins over a complete interpreter") {
        val interpreted = interpreted("end")
        val tools = developerTools()
        tools.enableKotlinOverride(SCRIPT_ID)

        registry(kotlin, interpreted, tools).forLabel(LABEL) shouldBe kotlin
      }

      test("developer force-interpreter bypasses an incomplete support result") {
        val interpreted = interpreted("special DoSomethingNative", "end")
        val tools = developerTools()
        tools.enableOverride(SCRIPT_ID)

        registry(kotlin, interpreted, tools).forLabel(LABEL) shouldBe interpreted
      }

      test("neither available fails with a useful diagnostic") {
        val interpreted = interpreted("special DoSomethingNative", "end")

        val error =
            shouldThrow<ScriptResolutionException> { registry(null, interpreted).forLabel(LABEL) }

        error.message shouldContain SCRIPT_ID
        error.message shouldContain "unsupported special DoSomethingNative"
        error.message shouldContain "Kotlin script is not registered"
      }

      test("generated real scripts are routed by completeness instead of old manual registration") {
        val registry = ScriptRegistry.generated()
        val supported = "PalletTown_EventScript_PlayersHouseSign"
        val incomplete = "PowerPlant_EventScript_Zapdos"

        registry.forLabel(supported, "firered") shouldBe
            registry.forId("gba:firered:BPRE:$supported")
        (registry.forLabel(incomplete, "firered") is InterpretedScript) shouldBe false
      }
    })

private fun interpreted(
    vararg lines: String,
    movements: Map<String, MovementProgram> = emptyMap(),
): InterpretedScript {
  val program =
      PretScriptParser.parse(
          id = scriptId(LABEL),
          storyNamespace = "kanto",
          sourceFile = "test.inc",
          lines = listOf("$LABEL::") + lines,
      )
  return InterpretedScript(program, emptyMap(), movements)
}

private fun registry(
    kotlin: Script?,
    interpreted: InterpretedScript,
    tools: DeveloperTools? = null,
): ScriptRegistry =
    ScriptRegistry(
        byLabel = if (kotlin == null) emptyMap() else mapOf(LABEL to kotlin),
        interpretedById = mapOf(SCRIPT_ID to interpreted),
        interpretedByBareLabel = mapOf(LABEL to interpreted),
        developerTools = tools,
    )

private fun developerTools(): DeveloperTools =
    DeveloperTools(
        GameServerConfig(
            host = "127.0.0.1",
            port = 0,
            checksumSize = 2,
            rootKeyResource = "game.private.pem",
            sessionSecret = "test-secret".toByteArray(),
            developer = DeveloperToolsConfig(enabled = true),
        ))

private fun scriptId(label: String) =
    ScriptId(platform = "gba", source = "firered", gameCode = "BPRE", label = label)
