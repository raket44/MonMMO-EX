package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * N's scene outside Skyla's gym is Mistralton's frame entry (VAR_0x4099 == 2 -> file 214 script 13,
 * block U214_58). It went missing twice (owner, 2026-09-28): CMD_10D was sized one halfword short,
 * so the scene decoded as a 20-byte stub; then StorePartySpecies had no lowering, and one unknown
 * command fails the WHOLE frame program - the table stopped running on arrival at all. The frame
 * table and the scene must both pass the support check, and the scene must end the way the ROM's
 * does: N removed, VAR_0x4099 = 3.
 */
class UnovaMistraltonNScriptTest :
    FunSpec({
      test("Mistralton's frame table runs N's whole scene") {
        val analyzer = ScriptSupportAnalyzer()
        val reg = InterpretedScripts.sources.first { it.corpus.source == "white" }
        val frame = reg.scriptsByLabel["NDS_INIT_107_FRAME"]
        val scene = reg.scriptsByLabel["U214_58"]
        frame shouldNotBe null
        scene shouldNotBe null
        analyzer.analyze(frame!!).reason shouldBe null
        analyzer.analyze(scene!!).reason shouldBe null
        val commands = scene.program.instructions.map { it.command to it.args.map { a -> a.token } }
        commands.any { (c, a) -> c == "removeobject" && a.firstOrNull() == "OBJ_13" } shouldBe true
        commands.any { (c, a) -> c == "setvar" && a.firstOrNull()?.contains("4099") == true && a.getOrNull(1) == "3" } shouldBe true
      }
    })
