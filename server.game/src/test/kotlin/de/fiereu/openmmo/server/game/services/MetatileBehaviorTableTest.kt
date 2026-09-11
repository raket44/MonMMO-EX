package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.maps.MapManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A setmetatile must get the tileset's behavior for a metatile the map never uses itself: the
 * Pokemon League rooms open their exit with METATILE_PokemonLeague_Door_Mid_Open (0x296, a cave
 * door in the tileset), and the closed door the map starts with is no warp (2026-09-11).
 */
class MetatileBehaviorTableTest :
    FunSpec({
      test("the opened league door is a warp door in the tileset table") {
        val room = MapManager().getMapsByName("PokemonLeague_LoreleisRoom").first { it.regionId.toInt() == 0 }
        room.metatileBehavior(0x296) shouldBe TileBehavior.NON_ANIMATED_DOOR
        room.tileAt(6, 2)?.behavior shouldBe TileBehavior.NORMAL
        room.warps.any { it.x == 6 && it.y == 2 } shouldBe true
      }
    })
