package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.script.GeneratedScriptCorpus
import de.fiereu.openmmo.script.LabelArg
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class GeneratedScriptCorpusTest :
    FunSpec({
      val fireRed = GeneratedScriptCorpus.sources.single { it.source == "firered" }
      val emerald = GeneratedScriptCorpus.sources.single { it.source == "emerald" }
      val registry by lazy { ScriptRegistry.generated() }

      test("multiple FireRed programs are generated automatically") {
        fireRed.programs.size shouldBeGreaterThan 1_000
        fireRed.programs.keys shouldContain "PalletTown_EventScript_PlayersHouseSign"
      }

      test("Emerald programs are generated automatically") {
        emerald.programs.size shouldBeGreaterThan 1_000
        emerald.programs.keys shouldContain "LittlerootTown_EventScript_TownSign"
      }

      test("internal call target programs are available") {
        val root = fireRed.programs.getValue("MtMoon_B2F_OnTransition")
        val target =
            root.instructions
                .flatMap { it.args }
                .filterIsInstance<LabelArg>()
                .first { it.token == "MtMoon_B2F_EventScript_ShowFossils" }

        fireRed.programs.keys shouldContain target.token
      }

      test("supported generated program resolves interpreter-first") {
        val label = "PalletTown_EventScript_PlayersHouseSign"

        registry.forLabel(label, "firered") shouldBe registry.forId("gba:firered:BPRE:$label")
      }

      test("unsupported generated program falls back to Kotlin") {
        // The starter flow branches into givemon and pick-starter specials the interpreter
        // does not model, so the hand-written Kotlin cutscene keeps running it.
        val resolved =
            registry.forLabel("PalletTown_ProfessorOaksLab_EventScript_ProfOak", "firered")

        (resolved is InterpretedScript) shouldBe false
      }

      test("unsupported generated program without Kotlin fails clearly") {
        // The player's PC menu is a client special with no Kotlin port (the nickname specials
        // the test used before are accepted as no-ops now, for the gift monsters).
        val label = "EventScript_AccessPlayersPC"
        val error = shouldThrow<ScriptResolutionException> { registry.forLabel(label, "firered") }

        error.message shouldContain "gba:firered:BPRE:$label"
        error.message shouldContain "unsupported special"
        error.message shouldContain "Kotlin script is not registered"
      }

      test("generated corpus parses both script sources cleanly") {
        // FireRed's one historical failure (the nurse file's `case 1 Label` comma typo) is
        // repaired in the parser, so any new failure here is a regression worth seeing.
        fireRed.diagnostics.parseFailureCategories shouldBe emptyMap()
        emerald.diagnostics.parseFailureCategories shouldBe emptyMap()
        (emerald.diagnostics.unavailableDirectLabels.isNotEmpty()) shouldBe true
      }

      test("duplicate bare labels across game sources do not collide") {
        val label = "CableClub_EventScript_AbortLink"
        val fireRedId = "gba:firered:BPRE:$label"
        val emeraldId = "gba:emerald:BPEE:$label"

        fireRed.programs.keys shouldContain label
        emerald.programs.keys shouldContain label
        val fireRedScript = registry.forId(fireRedId) as InterpretedScript
        val emeraldScript = registry.forId(emeraldId) as InterpretedScript
        fireRedScript.program.id.source shouldBe "firered"
        emeraldScript.program.id.source shouldBe "emerald"
        fireRedScript shouldNotBeSameInstanceAs emeraldScript
      }

      test("source-qualified lookup does not select a program from the other game") {
        val label = "PalletTown_EventScript_PlayersHouseSign"

        (registry.forLabel(label, "emerald") is InterpretedScript) shouldBe false
      }

      test("namespaced IDs remain stable") {
        fireRed.programs.getValue("PalletTown_EventScript_PlayersHouseSign").id.stable shouldBe
            "gba:firered:BPRE:PalletTown_EventScript_PlayersHouseSign"
        emerald.programs.getValue("LittlerootTown_EventScript_TownSign").id.stable shouldBe
            "gba:emerald:BPEE:LittlerootTown_EventScript_TownSign"
      }
    })
