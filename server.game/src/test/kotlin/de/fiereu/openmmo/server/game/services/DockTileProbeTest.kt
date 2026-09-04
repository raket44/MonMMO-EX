package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.maps.MapManager
import io.kotest.core.spec.style.FunSpec

class DockTileProbeTest :
    FunSpec({
      test("probe") {
        val maps = MapManager()
        val ext = maps.getMap(0, 1, 4)!!
        val verm = maps.getMap(0, 3, 5)!!
        for ((x, y) in listOf(31 to 5, 32 to 5, 33 to 5, 32 to 6, 32 to 4, 32 to 7)) {
          val t = ext.tileAt(x, y)
          println("DIAG ext(${x},${y}) ${t?.behavior} elev=${t?.elevation} collision=${t?.collision}")
        }
        for ((x, y) in listOf(23 to 33, 23 to 34, 23 to 35)) {
          val t = verm.tileAt(x, y)
          println("DIAG verm(${x},${y}) ${t?.behavior} elev=${t?.elevation} collision=${t?.collision}")
        }
        println("DIAG extWarps=" + ext.warps.joinToString { "(${it.x},${it.y})->${it.targetBankId}:${it.targetMapId}@(${it.targetX},${it.targetY}) z${it.targetElevation}" })
        println("DIAG vermWarps=" + verm.warps.mapIndexed { i, w -> "$i:(${w.x},${w.y})->${w.targetBankId}:${w.targetMapId}@(${w.targetX},${w.targetY})" }.joinToString())
      }
    })
