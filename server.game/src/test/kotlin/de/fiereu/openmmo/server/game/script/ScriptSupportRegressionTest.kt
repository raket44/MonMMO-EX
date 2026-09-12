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
                "ThreeIsland_BerryForest_EventScript_Lostelle",
                "Route23_EventScript_CascadeBadgeGuardTrigger",
                "Route22_NorthEntrance_EventScript_BoulderBadgeGuardTrigger",
                "ThreeIsland_BerryForest_OnTransition",
                "ThreeIsland_DunsparceTunnel_OnTransition",
                "SeafoamIslands_B3F_OnTransition",
                "SeafoamIslands_B4F_OnTransition",
                "VictoryRoad_1F_EventScript_FloorSwitch",
                "VictoryRoad_2F_EventScript_FloorSwitch1",
                "VictoryRoad_2F_EventScript_FloorSwitch2",
                "VictoryRoad_3F_EventScript_FloorSwitch",
                "MysteryEventClub_EventScript_Woman",
                "ViridianCity_EventScript_GymDoorLocked",
                "PewterCity_PokemonCenter_1F_EventScript_Jigglypuff",
                "ViridianCity_School_EventScript_Blackboard",
                "PokemonLeague_ChampionsRoom_EventScript_EnterRoom",
                "PokemonLeague_HallOfFame_EventScript_EnterRoom",
                "EventScript_ResetEliteFourEnd",
                "CeruleanCity_House1_EventScript_BadgeGuy",
                "PokemonLeague_LoreleisRoom_EventScript_Lorelei",
                "PokemonLeague_BrunosRoom_EventScript_Bruno",
                "PokemonLeague_AgathasRoom_EventScript_Agatha",
                "PokemonLeague_LancesRoom_EventScript_Lance",
                "PalletTown_ProfessorOaksLab_EventScript_ProfOak",
                "PokedexRating_EventScript_RateInPerson",
                "FourIsland_IcefallCave_1F_OnResume",
                "FourIsland_IcefallCave_1F_OnLoad",
                "FourIsland_IcefallCave_1F_EventScript_FallDownHole",
                "SixIsland_RuinValley_EventScript_DottedHoleDoor",
                "SixIsland_RuinValley_OnLoad",
                "SixIsland_DottedHole_B1F_EventScript_BrailleUp",
                "SixIsland_DottedHole_B4F_EventScript_BrailleDown",
                "SixIsland_DottedHole_B3F_EventScript_BrailleRight",
                "SixIsland_DottedHole_B2F_EventScript_BrailleLeft",
            )
        val analyzer = ScriptSupportAnalyzer()
        val firered = InterpretedScripts.sources.first { it.corpus.source == "firered" }
        for (label in labels) {
          val script = checkNotNull(firered.scriptsByLabel[label]) { "missing $label" }
          val support = analyzer.analyze(script)
          withClue("$label: ${support.reason}") { support.complete shouldBe true }
        }
      }

      test("the emerald braille signs interpret through the braille dialog") {
        val labels =
            listOf(
                "SealedChamber_OuterRoom_EventScript_BrailleABC",
                "SealedChamber_OuterRoom_EventScript_BrailleDigHere",
                "SealedChamber_InnerRoom_EventScript_BrailleStoryPart1",
                "SealedChamber_InnerRoom_EventScript_BrailleBackWall",
                "Underwater_SealedChamber_EventScript_Braille",
                "DesertRuins_EventScript_CaveEntranceSide",
                "AncientTomb_EventScript_CaveEntranceSide",
            )
        val analyzer = ScriptSupportAnalyzer()
        val emerald = InterpretedScripts.sources.first { it.corpus.source == "emerald" }
        for (label in labels) {
          val script = checkNotNull(emerald.scriptsByLabel[label]) { "missing $label" }
          val support = analyzer.analyze(script)
          withClue("$label: ${support.reason}") { support.complete shouldBe true }
        }
      }
    })
