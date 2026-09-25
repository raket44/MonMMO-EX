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

      test("the generator never invents a zero write to a story var") {
        // Chargestone's entrance load scripts pass the Clay progress var (VAR_0x409A) to opcode
        // 0x1EB, an engine command the table only numbers. The corpus fallback for such commands
        // zeroed "the var they answer into" - the last var-looking argument - so every arrival
        // wiped the var Clay's badge script had set and the web never yielded his scene (owner,
        // 2026-09-25). A story var (0x4000-0x7FFF) among an unnamed command's arguments is an
        // input; only temp vars (0x8000+) may be answered. Every `setvar VAR_0x4xxx, 0` the
        // corpus holds must be backed by a literal zero write in the ROM dump, bar the ones the
        // generator writes on purpose (Nimbasa's switch mask 0x400A).
        val romZeroed =
            commands.filter { it[0] == "StoreValueInVar" && it.size >= 3 && it[2] == "0" }
                .mapNotNull { it[1].toIntOrNull() }
                .map { "VAR_0x" + it.toString(16).uppercase() }
                .toSet()
        val allowed = romZeroed + setOf("VAR_0x400A", "VAR_0x4000", "VAR_0x4001", "VAR_0x4002", "VAR_0x4003")
        val invented =
            de.fiereu.openmmo.script.GeneratedScriptCorpus.sources
                .filter { it.source == "white" }
                .flatMap { it.programs.values }
                .flatMap { program ->
                  program.instructions
                      .filter { it.command == "setvar" && it.args.size == 2 && it.args[0].token.startsWith("VAR_0x4") && it.args[1].token == "0" }
                      .filter { it.args[0].token !in allowed }
                      .map { "${program.id.stable}: ${it.sourceLine}" }
                }
        invented shouldBe emptyList()
      }

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
        // 100 since 2026-09-24: an unknown opcode used to end every branch still queued for that
        // entry after one command (Dis5 `bad` poisoned the queue), which hid the unknowns those
        // branches reach; the fix recovered ~5,000 lines and reaches 25 more of them.
        commands.count { it[0] == "DecodeStopped" } shouldBeLessThanOrEqual 100
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

      test("no command is one argument SHORT (its argument read as the next opcode)") {
        // The mirror of the test above, and the one that cost Accumula its Team Plasma speech:
        // CloseShowMessageAt was sized with no arguments though it takes the window id, so the id
        // was decoded as an opcode - 1 and 2 became Nop2 and End, and the phantom End cut the
        // scene off after the crowd's two bubbles, losing 939 script steps across the game
        // (2026-09-21). A leftover argument is a SMALL number, so it decodes as one of the
        // low no-argument opcodes; those never appear mid-scene for real.
        val neverMidScene = setOf("Nop2", "GetDerefVar06", "GetDerefVar07")
        val suspects = mutableMapOf<String, Pair<Int, Int>>()
        val byEntry = corpus.readLines().filterNot { it.startsWith("mv;") }.map { it.split(';') }
        var previous: Pair<String, String>? = null
        val total = mutableMapOf<String, Int>()
        val followed = mutableMapOf<String, Int>()
        for (row in byEntry) {
          val key = "${row.getOrNull(0)};${row.getOrNull(1)}"
          val name = row.getOrNull(3)?.trim()?.substringBefore(' ') ?: continue
          previous?.let { (prevKey, prevName) ->
            if (prevKey == key) {
              total[prevName] = (total[prevName] ?: 0) + 1
              if (name in neverMidScene || name.startsWith("Unknown_"))
                  followed[prevName] = (followed[prevName] ?: 0) + 1
            }
          }
          previous = key to name
        }
        total.forEach { (name, uses) ->
          val hits = followed[name] ?: 0
          if (uses >= 5 && hits * 2 >= uses) suspects[name] = hits to uses
        }
        println("ONE-SHORT suspects: $suspects")
        // The six that remain are unnamed engine calls, each one a scene still losing its tail.
        // Shrink this set; never grow it.
        // CMD_154 left the set 2026-09-24: it is the cutscene app call and takes the scene id (H).
        suspects.keys shouldBe setOf("CMD_0DA", "CMD_107", "CMD_12B", "CMD_146", "CMD_20F")
      }

      test("every entry ends on a terminator - one that does not runs into its neighbour") {
        // The strongest signal of the lot, and the one that would have caught all three of
        // 2026-09-21's mis-sized commands at once. A command sized one argument too long eats its
        // entry's `End`, and the script then runs straight on into whatever byte follows - for the
        // Striaton gym that was the map-enter script falling into the gym guide's own script, so
        // his line fired the moment the owner walked in, before the npcs had even loaded.
        val lastOf = LinkedHashMap<String, String>()
        val lastAt = HashMap<String, Int>()
        corpus.readLines().filterNot { it.startsWith("mv;") }.forEach { line ->
          val p = line.split(';')
          val file = p.getOrNull(0)?.toIntOrNull() ?: return@forEach
          val entry = p.getOrNull(1)?.toIntOrNull() ?: return@forEach
          val at = p.getOrNull(2)?.toIntOrNull() ?: return@forEach
          val key = "$file;$entry"
          if (at >= (lastAt[key] ?: -1)) {
            lastAt[key] = at
            lastOf[key] = p.getOrNull(3)?.trim().orEmpty()
          }
        }
        val terminators = setOf("End", "EndRoutine", "Jump", "Return", "ReturnStd", "DecodeStopped")
        val open = lastOf.filterValues { it.substringBefore(' ') !in terminators }
        open.forEach { (key, cmd) -> println("OPEN-ENDED $key ends on $cmd") }
        // Was 6 before CMD_186 was sized right. Each one left is a scene that runs into the next.
        open.size shouldBeLessThanOrEqual 5
      }
    })
