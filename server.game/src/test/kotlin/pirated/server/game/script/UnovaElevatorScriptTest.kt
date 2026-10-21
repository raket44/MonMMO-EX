package pirated.server.game.script

import pirated.server.game.script.interpreter.InterpretedScripts
import pirated.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Castelia elevator panels (bg scripts 10405-10418 = White script file 888, docs/gen5-elevators.md)
 * run their ROM script: 0x1CB names the floor table, the shared routine walks the player into the
 * car, 0x1CC + Cancel make the floor list, 0x1CD rides, and the arrival routine opens the door
 * and walks the player out two tiles south. Every command on that chain must be one the
 * interpreter runs, or the panel silently does nothing again.
 */
class UnovaElevatorScriptTest :
    FunSpec({
      val reg = InterpretedScripts.sources.first { it.corpus.source == "white" }
      val analyzer = ScriptSupportAnalyzer()

      fun chain(start: String): Pair<List<Pair<String, List<String>>>, Set<String>> {
        val seen = LinkedHashSet<String>()
        val out = ArrayList<Pair<String, List<String>>>()
        val reasons = sortedSetOf<String>()
        fun walk(label: String) {
          if (!seen.add(label)) return
          val s = reg.scriptsByLabel[label] ?: return
          analyzer.analyze(s).reason?.let { reasons += it }
          s.program.instructions.forEach { i ->
            out += i.command to i.args.map { it.token }
            i.args.forEach { a -> a.token.takeIf { t -> t.startsWith("U888") || t.startsWith("NDS_") }?.let(::walk) }
          }
        }
        walk(start)
        return out to reasons
      }

      test("all fourteen panel ids are bound") {
        (10405..10418).forEach { id -> reg.scriptsByLabel["NDS_CHUNK_$id"] shouldNotBe null }
        // Nothing else of the engine band came along: the Center back-counter attendants stay NdsStandardScripts'.
        reg.scriptsByLabel["NDS_CHUNK_10100"] shouldBe null
        reg.scriptsByLabel["NDS_CHUNK_10419"] shouldBe null
      }

      test("the 56:0 panel (10409) runs the whole ride and the interpreter accepts every step") {
        val (ins, reasons) = chain("NDS_CHUNK_10409")
        ins.forEach { (c, a) -> println("ELEVATOR $c ${a.joinToString(",")}") }
        reasons shouldBe emptySet<String>()
        val commands = ins.map { it.first }.toSet()
        commands shouldContainAll
            listOf("ds_elevatortable", "ds_elevatormenu", "ds_elevatorwarp", "ds_elevatorsettle", "applymovement", "ds_door")
        // 0x1CB of entry 4 points at the 1F/2F table at offset 448 of file 888.
        ins.first { it.first == "ds_elevatortable" }.second shouldBe listOf("888", "448")
        // The menu's own entry is Cancel: bank 261 entry 0, value 255.
        val menu = ins.first { it.first == "ds_elevatormenu" }.second
        menu.size shouldBe 3
        menu[2] shouldBe "255"
        (menu[1].toInt() and 0xFFFF) shouldBe 0
        ((menu[1].toInt() shr 16) and 0x7FF) shouldBe 261
      }

      test("every panel reaches the menu, the ride and the settle") {
        (10405..10418).forEach { id ->
          val (ins, reasons) = chain("NDS_CHUNK_$id")
          reasons shouldBe emptySet<String>()
          val commands = ins.map { it.first }
          commands.count { it == "ds_elevatortable" } shouldBe 1
          commands.count { it == "ds_elevatorsettle" } shouldBe 1
          ("ds_elevatormenu" in commands) shouldBe true
          ("ds_elevatorwarp" in commands) shouldBe true
        }
      }
    })
