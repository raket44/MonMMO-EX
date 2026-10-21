package pirated.server.game.script

import pirated.server.game.script.interpreter.InterpretedScripts
import pirated.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Unova's Pokemon Center clerks (owner, 2026-10-21): the two on the mart counter are map scripts
 * that set VAR_0x8000 and CallStd 10110, which the transpiler dropped, so they did nothing while
 * the back counter opened marts. Accumula's Center (header 398): npc 5 at (11,16) runs script 4
 * with shelf 255 (the common one), npc 0 at (11,18) runs script 5 with shelf 0 (the town's own).
 */
class UnovaCenterMartScriptTest :
    FunSpec({
      val reg = InterpretedScripts.sources.first { it.corpus.source == "white" }

      fun martShelf(label: String): String? {
        // The entry label jumps into the file's blocks (U796_...); follow them in order.
        val ins = mutableListOf<Pair<String, List<String>>>()
        val seen = mutableSetOf<String>()
        fun walk(l: String) {
          if (!seen.add(l)) return
          val s = reg.scriptsByLabel[l]
          s shouldNotBe null
          ScriptSupportAnalyzer().analyze(s!!).reason shouldBe null
          s.program.instructions.forEach { i ->
            ins += i.command to i.args.map { it.token }
            i.args.forEach { a -> a.token.takeIf { t -> t.startsWith("U796") }?.let(::walk) }
          }
        }
        walk(label)
        val mart = ins.indexOfFirst { it.first == "ds_unovamart" }
        if (mart < 0) return null
        ins[mart].second shouldBe listOf("VAR_0x8000")
        return ins.take(mart).lastOrNull { (c, a) -> c == "setvar" && a.firstOrNull() == "VAR_0x8000" }?.second?.getOrNull(1)
      }

      test("both Accumula clerks open their ROM shelf") {
        martShelf("NDS_398_4") shouldBe "255"
        martShelf("NDS_398_5") shouldBe "0"
      }

      test("the shelves are the ROM's (tools/nds/Marts5)") {
        UnovaMarts.shelf(255, 0) shouldBe listOf(4, 17)
        UnovaMarts.shelf(255, 8) shouldBe listOf(4, 3, 2, 17, 26, 25, 24, 23, 28, 18, 22, 21, 19, 20, 27, 78, 79, 76, 77)
        UnovaMarts.shelf(0, 0) shouldBe listOf(137, 138, 139, 140, 141, 142, 143)
        UnovaMarts.shelf(13, 0) shouldBe listOf(59, 57, 58, 55, 56, 60, 61, 62)
        UnovaMarts.shelf(19, 0) shouldBe listOf(46, 47, 49, 52, 48, 45)
        UnovaMarts.shelf(247, 0) shouldBe null
      }
    })
