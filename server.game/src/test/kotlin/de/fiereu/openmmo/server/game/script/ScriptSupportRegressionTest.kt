package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scripts players meet constantly must stay interpretable. The analyzer's shape table is a
 * `when` with multi-label cases: an edit that slips a new case between labels of an existing one
 * silently re-types those labels (2026-09-11: every `special` and `delay` fell into the
 * fadescreen shape and every Pokemon Center nurse went mute). This pins the verdicts.
 */
class ScriptSupportRegressionTest :
    FunSpec({
      test("the everyday FireRed scripts are interpretable") {
        val labels =
            listOf(
                "SaffronCity_PokemonCenter_1F_EventScript_Nurse",
                "ViridianCity_PokemonCenter_1F_EventScript_Nurse",
                "ViridianCity_Mart_EventScript_Clerk",
                "PewterCity_Gym_EventScript_Brock",
                "SilphCo_11F_EventScript_BattleGiovanni",
                "CinnabarIsland_PokemonLab_Lounge_EventScript_Clifton",
                "CinnabarIsland_EventScript_GymDoorLocked",
                "CinnabarIsland_EventScript_BillScene",
                "CinnabarIsland_EventScript_SailToOneIsland",
                "TwoIsland_JoyfulGameCorner_EventScript_LostellesDaddy",
                "OneIsland_PokemonCenter_1F_EventScript_Celio",
                "OneIsland_PokemonCenter_1F_EventScript_MeetCelioScene",
            )
        val analyzer = ScriptSupportAnalyzer()
        val firered = InterpretedScripts.sources.first { it.corpus.source == "firered" }
        for (label in labels) {
          val script = checkNotNull(firered.scriptsByLabel[label]) { "missing $label" }
          val support = analyzer.analyze(script)
          withClue("$label: ${support.reason}") { support.complete shouldBe true }
        }
      }
    })
