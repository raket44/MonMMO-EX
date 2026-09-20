package de.fiereu.openmmo.server.game.script.interpreter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * White's scripts are disassembled with a hand-built opcode table (tools/nds/bw1-script-commands.txt),
 * and a wrong argument count there is silent: the command swallows the next command's opcode (or
 * leaves its own last argument behind) and the scene simply loses a step. Found one playtest at a
 * time on 2026-09-19/20 - ResetCamera, opcode 0xB4, CMD_24F/252, and Message2, which swallowed the
 * command after the text in all 1,399 npc conversations (83 jumps, 45 yes/no prompts). These are
 * the two whole-corpus signals that expose that class of error, pinned so it cannot creep back.
 */
class UnovaScriptDecodeAuditTest :
    FunSpec({
      val corpus =
          listOf(File("nds-scripts-2.txt"), File("server.game/nds-scripts-2.txt")).first { it.isFile }
      val commands =
          corpus.readLines().filterNot { it.startsWith("mv;") }.mapNotNull { it.split(';').getOrNull(3)?.trim()?.split(' ') }

      test("no command is left one argument short (a story var decoded as an opcode)") {
        // A var (0x4000+/0x8000+) can never be an opcode; one in opcode position means the command
        // before it is missing its last argument. 97 before the 2026-09-20 fixes; the rest are
        // single sites not yet understood.
        commands.count { it[0].matches(Regex("CMD_[48][0-9A-F]{3}")) } shouldBeLessThanOrEqual 13
      }

      test("entries the decoder cannot finish are marked, and do not grow") {
        // Dis5 writes DecodeStopped where it met an opcode the table lacks; the generator ends the
        // script there. Without the marker a truncated entry fell through into the NEXT script of
        // its file - Nuvema's exit ran on into the post-game scenes (2026-09-20). 87 at first.
        commands.count { it[0] == "DecodeStopped" } shouldBeLessThanOrEqual 81
      }

      test("no command's last argument is really the next command's opcode") {
        // WaitButton, CloseMessageKP(2), Jump, YesNoBox, ApplyMovement, WaitMovement, RemoveNPC:
        // what follows a text or precedes a walk. A command whose LAST argument is one of these in
        // most of its uses is one argument too long. (BubbleMessage/EventGreyMessage/FadeScreen end
        // in a genuine type byte of 2 - End's opcode - and are excluded by not listing 2 here.)
        val swallowed = setOf("30", "50", "62", "63", "71", "100", "101", "108")
        val suspects =
            commands
                .filter { it.size >= 2 }
                .groupBy { it[0] }
                .filterValues { uses -> uses.size >= 5 && uses.count { it.last() in swallowed } >= uses.size * 0.6 }
                .keys
        suspects shouldBe emptySet()
      }
    })
