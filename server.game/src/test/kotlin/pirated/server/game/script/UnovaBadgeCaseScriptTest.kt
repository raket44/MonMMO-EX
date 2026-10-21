package pirated.server.game.script

import pirated.server.game.script.interpreter.InterpretedScripts
import pirated.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Every Unova gym win plays the badge case after the fanfare: White's CMD_19F with the gym's
 * a/1/1/7 entry, picked by the hero's gender (male 2,4..10 = gymbadge1..8, female 42..49 =
 * gymbadge1f..8f in a/1/1/5); 41/50/51 are an item and the two Pokedex gets. It was dropped as an
 * unknown opcode until 2026-10-21; it now lowers to ds_cutin, which the client plays itself
 * (0xB6 action 10), and every block carrying one must still run.
 */
class UnovaBadgeCaseScriptTest :
    FunSpec({
      test("every CMD_19F cut-in reaches the client, gym badge cases included") {
        val analyzer = ScriptSupportAnalyzer()
        val reg = InterpretedScripts.sources.first { it.corpus.source == "white" }
        val blocks = reg.scriptsByLabel.filterValues { s -> s.program.instructions.any { it.command == "ds_cutin" } }
        val ids = blocks.values.flatMap { s -> s.program.instructions.filter { it.command == "ds_cutin" }.map { it.args.first().token.toInt() } }.toSet()
        ids shouldBe (listOf(2) + (4..10) + (41..51)).toSet()
        blocks.mapValues { analyzer.analyze(it.value).reason }.filterValues { it != null } shouldBe emptyMap()
      }
    })
