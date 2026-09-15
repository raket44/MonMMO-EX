package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.script.GeneratedScriptCorpus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * DS stories start on step triggers the server never loaded (2026-09-14): the corpus now carries
 * the ROM's coord events with their vars named the way the scripts write them.
 */
class NdsCoordTriggerCorpusTest :
    FunSpec({
      fun rows(source: String) = GeneratedScriptCorpus.sources.first { it.source == source }.coordTriggers

      // pokeplatinum res/field/events/events_twinleaf_town_player_house_2f.json coord_events: scripts
      // 7-10 at (4,5) (3,6) (5,6) (4,7) waiting for VAR_PLAYER_HOUSE_RIVAL_STATE == 0 (ROM var 0x40A5).
      test("Twinleaf Town's upstairs rival triggers come through named as the scripts write the var") {
        rows("platinum").filter { it.startsWith("159;1;") } shouldBe
            listOf(
                "159;1;4;5;1;1;VAR_PLAYER_HOUSE_RIVAL_STATE;0;7",
                "159;1;3;6;1;1;VAR_PLAYER_HOUSE_RIVAL_STATE;0;8",
                "159;1;5;6;1;1;VAR_PLAYER_HOUSE_RIVAL_STATE;0;9",
                "159;1;4;7;1;1;VAR_PLAYER_HOUSE_RIVAL_STATE;0;10",
            )
      }

      // White's event file 391 (Nuvema, the player's room, header 391 = bank 135 map 1): one 22-byte
      // trigger, script 6 at (8,2) when var 0x4081 == 1; Unova scripts spell vars VAR_0x....
      test("Unova's start room trigger comes from White's event file with the var spelled as its scripts do") {
        rows("white").filter { it.startsWith("135;1;") } shouldBe listOf("135;1;8;2;1;1;VAR_0x4081;1;6")
      }

      test("HeartGold's New Bark triggers resolve their vars to names too") {
        val newBark = rows("heartgold").filter { it.startsWith("60;0;") }
        newBark.shouldNotBeEmpty()
        newBark.map { it.split(';')[6] }.filter { it.startsWith("VAR_0x") } shouldBe emptyList()
        // Every coord row of the ROM event tables (server.game/nds-npcs-3.txt, nds-npcs-4.txt).
        rows("platinum").size shouldBe 186
        rows("heartgold").size shouldBe 195
      }
    })
