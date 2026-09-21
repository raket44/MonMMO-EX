package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.script.interpreter.ScriptSupportAnalyzer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Pokemon Center nurse runs her real ROM script (NDS_CHUNK_2100 -> file 855 entry 0), because
 * that script is the only thing that sets VAR 16507 to 2 - the Accumula tour hand-off - and the
 * hardcoded stand-in soft-locked the Unova story at the first heal (owner, 2026-09-21).
 *
 * Healing is the one thing a player cannot work around, so this walks her whole reachable chain and
 * reports every command it still needs, rather than leaving them to surface one per playtest.
 */
class UnovaNurseScriptTest :
    FunSpec({
      test("the Unova nurse chain, and everything it still needs") {
        val analyzer = ScriptSupportAnalyzer()
        val reg = InterpretedScripts.sources.first { it.corpus.source == "white" }
        val start = reg.scriptsByLabel["NDS_CHUNK_2100"]
        println("NURSE bound: ${start != null}")
        if (start == null) return@test

        val seen = LinkedHashSet<String>()
        val commands = sortedSetOf<String>()
        val reasons = sortedSetOf<String>()
        val specials = sortedSetOf<String>()
        val texts = sortedSetOf<String>()
        var setsTourVar = false
        fun walk(label: String) {
          if (!seen.add(label)) return
          val s = reg.scriptsByLabel[label] ?: return
          analyzer.analyze(s).reason?.let { reasons += it }
          s.program.instructions.forEach { i ->
            commands += i.command
            if (i.command == "special") i.args.firstOrNull()?.let { specials += it.token }
            if (i.command == "message" || i.command == "msgbox")
                i.args.firstOrNull()?.let { texts += it.token }
            if (i.command == "setvar" && i.args.size >= 2 &&
                i.args[0].token.contains("407B") && i.args[1].token == "2") setsTourVar = true
            if (i.command in setOf("goto", "call", "goto_if_eq", "goto_if_set", "goto_if_unset", "call_if_set", "call_if_unset")) {
              i.args.forEach { a -> a.token.takeIf { t -> t.startsWith("U855") || t.startsWith("NDS_") }?.let(::walk) }
            }
          }
        }
        walk("NDS_CHUNK_2100")
        println("NURSE blocks reached: ${seen.size}")
        println("NURSE commands: ${commands.joinToString(" ")}")
        reasons.forEach { println("NURSE reason: $it") }
        println("NURSE texts: ${texts.joinToString(" ")}")

        // She must be bound, complete, and actually heal - a player cannot work around a nurse
        // that does not, and the hardcoded stand-in she replaced always did.
        start shouldNotBe null
        reasons shouldBe emptySet<String>()
        specials.any { it.contains("Heal", ignoreCase = true) } shouldBe true
        // The tour hand-off: setting VAR 16507 to 2 is the whole reason she is bound at all.
        setsTourVar shouldBe true
        // Her lines come from text bank 346. Left at the default bank 0 she spoke as the GOURMET
        // MAID, sniffing and asking for an ingredient - the same failure the item gifts had.
        texts.isNotEmpty() shouldBe true
        texts.all { it.startsWith("T0346_") } shouldBe true
      }
    })
