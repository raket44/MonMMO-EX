package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * The Castelia elevators as White's ROM has them (tools/nds/Elevators5.java, docs/gen5-elevators.md):
 * seven floor tables inside script file 888, one per office building, every landing inside the
 * lift car at (7,0) facing south, every label an entry of text bank 261.
 */
class NdsElevatorsTest :
    FunSpec({
      val elevators = NdsElevators()
      val region = 2
      val file = 888

      fun data(name: String) = listOf(File(name), File("server.game/$name")).first { it.isFile }

      test("seven buildings, 66 panel rows: 18 for the three-floor tower, 8 for each of the others") {
        val rows = data("nds-elevators-2.txt").readLines().filter { it.startsWith("elevator;") }.map { it.split(';') }
        rows.size shouldBe 66
        rows.groupingBy { it[21].toInt() }.eachCount() shouldBe
            mapOf(136 to 18, 220 to 8, 296 to 8, 372 to 8, 448 to 8, 524 to 8, 600 to 8)
      }

      test("each table's floors, in ROM order") {
        fun headers(table: Int) = elevators.floors(region, file, table).map { it.destHeader }
        headers(136) shouldBe listOf(44, 45, 46)
        headers(220) shouldBe listOf(47, 48)
        headers(296) shouldBe listOf(49, 50)
        headers(372) shouldBe listOf(54, 55)
        headers(448) shouldBe listOf(56, 57)
        headers(524) shouldBe listOf(58, 59)
        headers(600) shouldBe listOf(60, 61)
        elevators.floors(region, file, 136).map { it.label } shouldBe listOf("1F", "47F", "55F")
        elevators.floors(region, file, 136).map { it.index } shouldBe listOf(0, 1, 2)
      }

      test("every landing is the car tile (7,0), facing south, on a header White has") {
        val known =
            data("nds-headers-2.txt").readLines().filter { it.startsWith("hdr;2;") }.map { it.split(';')[4].toInt() }.toSet()
        val floors = listOf(136, 220, 296, 372, 448, 524, 600).flatMap { elevators.floors(region, file, it) }
        floors.size shouldBe 15
        floors.forEach { f ->
          (f.destHeader in known) shouldBe true
          (f.destBank or (f.destMap shl 8)) shouldBe f.destHeader
          (f.x to f.y) shouldBe (7 to 0)
          f.facing shouldBe Direction.DOWN
        }
      }

      test("every floor label is an entry of Unova text bank 261") {
        val floors = listOf(136, 220, 296, 372, 448, 524, 600).flatMap { elevators.floors(region, file, it) }
        floors.forEach { f ->
          (f.textId ushr 28) shouldBe 2
          ((f.textId shr 16) and 0x7FF) shouldBe 261
        }
        // Bank 261: 1 1F, 2 2F, 4 11F, 5 22F, 6 47F, 7 55F.
        floors.map { it.textId and 0xFFFF }.toSet() shouldBe setOf(1, 2, 4, 5, 6, 7)
      }

      test("the menu leaves out the floor the player is on, as opcode 0x1CC does") {
        elevators.menu(region, file, 136, 44).map { it.index } shouldBe listOf(1, 2)
        elevators.menu(region, file, 136, 45).map { it.index } shouldBe listOf(0, 2)
        elevators.menu(region, file, 448, 57).map { it.label } shouldBe listOf("1F")
      }

      test("both bg events of every elevator map point at their building's table") {
        val panels = elevators.panels(region)
        panels.size shouldBe 30
        panels.map { it.script }.toSet() shouldBe ((10405..10418).toSet())
        panels.filter { it.bank == 56 && it.map == 0 }.map { it.script to it.table } shouldContainAll listOf(10409 to 448, 10416 to 448)
        panels.forEach { p -> elevators.floors(region, p.file, p.table).any { it.destHeader == (p.bank or (p.map shl 8)) } shouldBe true }
      }
    })
