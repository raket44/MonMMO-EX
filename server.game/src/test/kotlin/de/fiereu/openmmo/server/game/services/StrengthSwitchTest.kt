package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.maps.MapManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Victory Road's Strength puzzle: every floor switch is an MB_STRENGTH_BUTTON tile with a coord
 * event armed by the 99 sentinel, and only a pushed boulder runs it (2026-09-11).
 */
class StrengthSwitchTest :
    FunSpec({
      test("victory road floor switches are strength buttons with a coord script on them") {
        val maps = MapManager()
        val expected =
            mapOf(
                "VictoryRoad_1F" to listOf(Triple(20, 16, "VictoryRoad_1F_EventScript_FloorSwitch")),
                "VictoryRoad_2F" to
                    listOf(
                        Triple(2, 19, "VictoryRoad_2F_EventScript_FloorSwitch1"),
                        Triple(14, 19, "VictoryRoad_2F_EventScript_FloorSwitch2")),
                "VictoryRoad_3F" to listOf(Triple(7, 7, "VictoryRoad_3F_EventScript_FloorSwitch")),
            )
        for ((name, switches) in expected) {
          val map = maps.getMapsByName(name).first { it.regionId.toInt() == 0 }
          for ((x, y, script) in switches) {
            map.tileAt(x, y)?.behavior shouldBe TileBehavior.STRENGTH_BUTTON
            map.coordScripts.filter { it.x == x && it.y == y }.map { it.script } shouldContainExactly listOf(script)
          }
        }
      }
    })
