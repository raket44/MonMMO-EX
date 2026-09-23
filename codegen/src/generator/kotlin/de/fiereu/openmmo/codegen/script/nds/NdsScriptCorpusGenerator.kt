package de.fiereu.openmmo.codegen.script.nds

import de.fiereu.openmmo.codegen.script.BuiltScriptCorpus
import de.fiereu.openmmo.codegen.script.ConstantsIndex
import de.fiereu.openmmo.codegen.script.ScriptCorpusMovementRecord
import de.fiereu.openmmo.codegen.script.ScriptCorpusProgramRecord
import de.fiereu.openmmo.codegen.script.ScriptCorpusSpec
import de.fiereu.openmmo.script.PretMovementParser
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.script.TextArg
import java.io.File
import java.util.TreeMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The DS story scripts as programs the Kanto interpreter already runs.
 *
 * The decomps ship the ROM's field scripts disassembled into two macro dialects - pokeplatinum's
 * `res/field/scripts` and pokeheartgold's `files/fielddata/script/scr_seq`. The
 * client runs no script VM of its own, so those scripts run on the server exactly like the GBA
 * ones: this generator rewrites each Gen 4 command into the GBA dialect the interpreter speaks,
 * keeps unknown commands visible under a `ds_` prefix so coverage reports name them, and binds
 * every script to the map header that owns it.
 *
 * Three things bind a DS script to the running world without any decomp map data:
 * - The client's DS "bank;map" pair IS the ROM map header id (`map * 256 + bank`). Each header
 *   names its script file, its event file and its text bank, so an npc's script index from the
 *   ROM's own event table resolves through the program label `NDS_<header>_<index>`.
 * - Dialog text is addressed the way the client addresses its own ROM text
 *   (`f/mk1.zD1` -> `nV0.CoM5`): `region << 28 | msgFile << 16 | entry`. The server sends the
 *   id; the client decrypts and formats the entry from its ROM.
 * - Flags and vars keep the ROM's numbering (Gen 4 vars start at 0x4000 like the GBA), so the
 *   story store, the client-aware whitelist and the interpreter's compare/goto logic carry over.
 */
class NdsScriptCorpusGenerator {

  private sealed interface Dialect {
    val region: Int
    val scriptFiles: List<File>

    fun mapHeaders(): List<MapHeader>

    fun objectIdsFor(header: MapHeader): Map<String, Int>

    fun textId(token: String): Int?

    fun constants(): Map<String, Int>

    /** Script id chunks that live outside map headers (Platinum's common scripts and friends). */
    fun chunkFiles(): Map<Int, String> = emptyMap()

    /**
     * One script id bound to ONE entry of a shared file, for engine ids that do not sit in a
     * base+offset run. Unova's counter scripts are like this: the nurse is file 855 entry 0 and the
     * mart is a different file entirely (856, the one with MoneyBox), so a base of 2100 over 855
     * would hand id 2101 the nurse file's second entry instead of the mart.
     */
    fun soloChunks(): Map<Int, Pair<String, Int>> = emptyMap()

    /**
     * Engine standard-script ids with no script of their own: a CallStd to one is dropped instead
     * of being emitted as an unresolvable call (which aborts the script at that line).
     */
    fun unboundStdCalls(): IntRange? = null

    /** Convenience macros the scripts use as commands, expanded to their plain-command bodies. */
    fun macros(): Map<String, Macro> = emptyMap()

    /** For a chunk whose ids are base + trainer id: the number of trainer ids, else null. */
    fun trainerChunkSize(base: Int): Int? = null

    /** Specialty mart shelves by the game's mart index: ITEM_ tokens. */
    fun martTables(): Map<Int, List<String>> = emptyMap()

    /** Dialects without source files (Unova, from the ROM disassembly): name -> parsed file. */
    fun preparsed(): Map<String, ParsedFile>? = null

    /** Elevator floor by map header NAME: the game's GetFloorsAbove (Platinum) / MapNumToFloorNo (HeartGold). */
    fun elevatorFloors(): Map<String, Int> = emptyMap()

    /** The header's warp tiles whose destination is the dynamic (elevator) header, as (x, y). */
    fun dynamicExits(header: MapHeader): List<Pair<Int, Int>> = emptyList()

    /** Item tokens as the catalogue names them: Gen 4 TMs by move (ITEM_TM70 -> ITEM_TM_FLASH). */
    fun itemAliases(): Map<String, String> = emptyMap()
  }

  class Macro(val params: List<String>, val body: List<String>)

  /** One ROM map header: its id as the client numbers it and the files it references. */
  data class MapHeader(val id: Int, val name: String, val scriptFile: String?, val eventsFile: String?, val msgBank: Int? = null, val initFile: File? = null)

  fun build(spec: ScriptCorpusSpec): BuiltScriptCorpus {
    val dialect: Dialect =
        when (spec.source) {
          "platinum" -> Platinum(spec.decompDir)
          "heartgold" -> HeartGold(spec.decompDir)
          "white" -> Unova(spec.decompDir)
          else -> error("Unknown DS script source ${spec.source}")
        }
    val constants = HashMap(dialect.constants())
    constants["OBJ_EVENT_ID_PLAYER"] = 0xFF
    constants["TRUE"] = 1
    constants["FALSE"] = 0
    constants["VAR_RESULT"] = 0x800C
    // Renamed item tokens keep their number for value positions (setvar VAR_0x8004, ITEM_TM27).
    for ((from, to) in dialect.itemAliases()) constants[from]?.let { constants.putIfAbsent(to, it) }
    constants.entries.filter { it.key.startsWith("ITEM_") && "__" in it.key }.toList()
        .forEach { (key, value) -> constants.putIfAbsent(key.replace("__", "_"), value) }

    val failures = linkedMapOf<String, Int>()
    val samples = linkedMapOf<String, MutableList<String>>()
    fun failure(category: String, detail: String) {
      failures[category] = failures.getOrDefault(category, 0) + 1
      samples.getOrPut(category) { mutableListOf() }.let { if (it.size < 5) it += detail }
    }

    val headers = dialect.mapHeaders()
    val headersByScript = headers.filter { it.scriptFile != null }.groupBy { it.scriptFile!! }
    // Elevators: the doors that lead "back where you came from" and the floor each map is.
    val dynamicExits = headers.flatMap { h -> dialect.dynamicExits(h).map { (x, y) -> "${h.id and 0xFF};${h.id shr 8};$x;$y" } }
    val headerFloors =
        dialect.elevatorFloors().mapNotNull { (name, floor) ->
          (constants[name] ?: headers.firstOrNull { it.name == name }?.id)?.let { it.toString() to floor }
        }.toMap()

    val programs = mutableListOf<ScriptCorpusProgramRecord>()
    val movements = mutableListOf<ScriptCorpusMovementRecord>()
    val referencedText = linkedSetOf<String>()
    val interactable = linkedSetOf<String>()
    val referencedTokens = HashSet<String>()
    val tokenPattern = Regex("[A-Za-z_]\\w*")
    var indexedLabels = 0

    val parsedByFile = HashMap<String, ParsedFile>()
    val sources: List<Triple<String, ParsedFile, String>> =
        dialect.preparsed()?.map { (name, parsed) -> Triple(name, parsed, "rom/${spec.source}/$name") }
            ?: dialect.scriptFiles.map { file ->
              Triple(file.name, parseScriptFile(file, dialect), "decomp/${spec.decompDir.name}/${file.relativeTo(spec.decompDir).invariantSeparatorsPath}")
            }
    for ((fileName, parsed, sourceFile) in sources) {
      parsedByFile[fileName] = parsed
      val owners = headersByScript[fileName].orEmpty()
      val objectIds = owners.firstOrNull()?.let(dialect::objectIdsFor).orEmpty()
      indexedLabels += parsed.blocks.size
      // MessageVar prints an entry of the script's own text bank (the loader the file was built
      // with): the bank of the first text token the file names.
      val fileTextBase =
          parsed.blocks.asSequence().flatMap { it.lines.asSequence() }.flatMap { it.drop(1).asSequence() }
              .firstNotNullOfOrNull { dialect.textId(it) }?.let { it and 0xFFFF.inv() }
      // Blocks that open a naming screen or Mom's bank, and blocks that jump straight into one: the
      // owner drops the questions leading there (nicknames, the rival's name, Mom's savings).
      val namingLabels = parsed.blocks.filter { b -> b.lines.any { isDroppedSystemLine(it) } }.mapTo(HashSet()) { it.label }
      // The nickname offers among them: the only questions whose own text goes with the screen.
      val nicknameLabels = parsed.blocks.filter { b -> b.lines.any { it[0] in NICKNAME_COMMANDS } }.mapTo(HashSet()) { it.label }
      repeat(2) {
        for (set in listOf(namingLabels, nicknameLabels)) {
          parsed.blocks
              // The block's first way out is an unconditional jump there (HeartGold _0A59: msg, GoTo, End).
              .filter { b -> b.lines.firstOrNull { it[0] in TERMINAL }?.let { it[0] == "GoTo" && it.getOrNull(1) in set } == true }
              .forEach { set += it.label }
        }
      }

      // Nuvema's "first steps together" runs straight into Juniper's catching lesson. The scene
      // (U778 entry 13, bound as NDS_389_14, reached from the seam trigger at y 739 once
      // VAR 16512 == 2) walks the player up Route 1 with Cheren and Bianca and then just ends,
      // leaving the lesson to be started by talking to Juniper - and she is one npc on a wide
      // route, so the owner walked past and skipped it. That dead-ends Route 1: the end-of-route
      // comparison scene is gated on VAR 16508 == 1 and the lesson is the only thing that sets it.
      //
      // Verified against White (IRAO) 2026-09-20 rather than assumed - the ROM has nothing that
      // chains it: Route 1's event file holds exactly one trigger record (the comparison one), its
      // level script file 635 is four zero bytes, and CMD_21 at the scene's tail is a screen
      // transition selector, not a hand-off. Entering the lesson from wherever the walk-up ended is
      // safe because the lesson opens with MoveCamera onto Juniper and ApplyMovement for her,
      // Cheren, Bianca AND the player: it walks everyone into position itself.
      //
      // Written as an ordinary GoTo on the scene's own terminator, the way any other script hands
      // over. A rule in the interpreter's `end` handler was tried first and trapped the owner in a
      // repeating tutorial, because `end` is shared by every script in every region; this is inert
      // data in one block and cannot loop - the lesson's own End is still an End.
      // MUST run before the loop below turns the blocks into programs: the first attempt sat after
      // it, rewrote a block nobody read again, and the hand-off silently did nothing while the
      // generator reported success. It now fails loudly instead of quietly.
      if (fileName == SCENE_CHAIN_FILE) {
        // The entry block is only the scene's FIRST chunk - it is split at every label and leaves off
        // with a GoTo into the next one - so walk that chain to the block that actually ends it.
        var scene = parsed.blocks.firstOrNull { it.label == parsed.entries.getOrNull(SCENE_CHAIN_ENTRY) }
        var hops = 0
        while (scene != null && scene.lines.lastOrNull()?.firstOrNull() == "GoTo" && hops++ < 32) {
          val next = scene.lines.last().getOrNull(1)
          scene = parsed.blocks.firstOrNull { it.label == next }
        }
        checkNotNull(scene) { "$SCENE_CHAIN_FILE entry $SCENE_CHAIN_ENTRY leads nowhere; cannot hand over to $SCENE_CHAIN_TARGET" }
        check(scene.lines.lastOrNull()?.firstOrNull() == "End") {
          "$SCENE_CHAIN_FILE entry $SCENE_CHAIN_ENTRY ends with ${scene.lines.lastOrNull()}, not End; the hand-off to $SCENE_CHAIN_TARGET would be lost"
        }
        scene.lines[scene.lines.size - 1] = listOf("GoTo", SCENE_CHAIN_TARGET)
        println("[script-corpus] scene hand-off: $SCENE_CHAIN_FILE entry $SCENE_CHAIN_ENTRY ends at block ${scene.label} -> $SCENE_CHAIN_TARGET")
      }

      for (block in parsed.blocks) {
        if (block.movement) {
          val actions = transpileMovement(block.lines)
          try {
            PretMovementParser.parse(
                ScriptId("gba", spec.source, spec.gameCode, block.label), sourceFile, listOf("${block.label}:") + actions)
            movements += ScriptCorpusMovementRecord(block.label, sourceFile, actions)
          } catch (cause: IllegalArgumentException) {
            failure("movement structure", "${block.label}: ${cause.message}")
          }
          continue
        }
        val extra = mutableListOf<Pair<String, List<String>>>()
        // The server saves as things happen: the games' "Would you like to save?" routines are a
        // silent success (owner, 2026-09-15) - the scene carries on with nothing shown.
        val commands =
            if (block.label in SILENT_SAVE_ROUTINES) listOf("setvar VAR_RESULT, 1", "return")
            else transpile(block.lines, dialect, block.label, extra, owners.firstOrNull()?.msgBank, constants, fileTextBase, namingLabels, nicknameLabels)
        for ((label, body) in listOf(block.label to commands) + extra) {
          try {
            val program =
                PretScriptParser.parse(
                    id = ScriptId("gba", spec.source, spec.gameCode, label),
                    storyNamespace = spec.storyNamespace,
                    sourceFile = sourceFile,
                    lines = listOf("$label::") + body,
                    objectIds = objectIds,
                    constants = constants,
                )
            program.instructions.flatMap { it.args }.filterIsInstance<TextArg>().mapTo(referencedText) { it.token }
            programs += ScriptCorpusProgramRecord(label, sourceFile, body, objectIds)
            body.forEach { c -> tokenPattern.findAll(c).forEach { referencedTokens += it.value } }
          } catch (cause: IllegalArgumentException) {
            failure("script structure", "$label: ${cause.message}")
          }
        }
      }


      // Header bindings: script index k (1-based, the ROM's own event numbering) -> entry label.
      for (header in owners) {
        parsed.entries.forEachIndexed { i, target ->
          val label = "NDS_${header.id}_${i + 1}"
          programs += ScriptCorpusProgramRecord(label, sourceFile, listOf("goto $target", "end"), objectIds)
          interactable += label
        }
      }
      // Single-id bindings (see Dialect.soloChunks): one engine script id -> one entry of a shared
      // file. Checked rather than silently skipped, because a miss here is a counter npc that
      // answers nothing.
      dialect.soloChunks().filterValues { it.first == fileName }.forEach { (id, where) ->
        val target = parsed.entries.getOrNull(where.second)
        checkNotNull(target) { "solo chunk $id: ${where.first} has no entry ${where.second}" }
        val label = "NDS_CHUNK_$id"
        programs += ScriptCorpusProgramRecord(label, sourceFile, listOf("goto $target", "end"), emptyMap())
        interactable += label
        println("[script-corpus] solo chunk $id -> ${where.first} entry ${where.second} ($target)")
      }
      // Chunk bindings (Platinum common scripts etc.): global ids -> entry label.
      dialect.chunkFiles().filterValues { it == fileName }.keys.forEach { base ->
        val trainerChunk = dialect.trainerChunkSize(base)
        val trainerEntry = parsed.entries.firstOrNull() ?: parsed.blocks.firstOrNull { !it.movement }?.label
        if (trainerChunk != null && trainerEntry != null) {
          // Platinum: script id = base + trainer id, all through one shared script that reads the
          // trainer from VAR_0x8004 (the engine sets it from the script id; the binding does here).
          for (k in 1 until trainerChunk) {
            val label = "NDS_CHUNK_${base + k}"
            programs += ScriptCorpusProgramRecord(label, sourceFile, listOf("setvar VAR_0x8004, $k", "goto $trainerEntry", "end"), emptyMap())
            interactable += label
          }
          return@forEach
        }
        parsed.entries.forEachIndexed { i, target ->
          val label = "NDS_CHUNK_${base + i}"
          programs += ScriptCorpusProgramRecord(label, sourceFile, listOf("goto $target", "end"), emptyMap())
          interactable += label
        }
      }
    }

    // Map init scripts: the header's OnTransition entry and its frame table (var == value ->
    // entry), bound as NDS_INIT_<header>_TRANSITION / _FRAME for the arrival hook.
    for (header in headers) {
      val init = header.initFile?.takeIf { it.isFile } ?: continue
      val entries = header.scriptFile?.let { parsedByFile[it] }?.entries ?: continue
      fun entryLabel(token: String): String? {
        val t = token.trim()
        val n = Regex("_(\\d+)\\s*\\+\\s*1$").find(t)?.groupValues?.get(1)?.toInt()?.plus(1) ?: t.toIntOrNull() ?: return null
        return entries.getOrNull(n - 1)
      }
      val frame = mutableListOf<String>()
      var transition: String? = null
      for (raw in init.readLines()) {
        val line = raw.substringBefore("//").substringBefore(";").trim()
        val name = line.substringBefore(" ").substringBefore("	")
        val args = line.removePrefix(name).trim().split(",").map { it.trim() }
        when (name) {
          "InitScriptEntry_OnTransition" -> transition = entryLabel(args[0])
          "InitScriptGoToIfEqual" -> if (args.size >= 3) entryLabel(args[2])?.let { target ->
            frame += "compare ${args[0]}, ${args[1]}"
            frame += "goto_if_eq $target"
          }
        }
      }
      val sourceFile = "decomp/${spec.decompDir.name}/${init.relativeTo(spec.decompDir).invariantSeparatorsPath}"
      transition?.let { programs += ScriptCorpusProgramRecord("NDS_INIT_${header.id}_TRANSITION", sourceFile, listOf("goto $it", "end"), emptyMap()) }
      if (frame.isNotEmpty()) programs += ScriptCorpusProgramRecord("NDS_INIT_${header.id}_FRAME", sourceFile, frame + "end", emptyMap())
    }

    // Facing an npc a fixed way is a one-step movement; four shared programs cover it.
    for ((name, step) in listOf("NDS_FACE_UP" to "face_up", "NDS_FACE_DOWN" to "face_down", "NDS_FACE_LEFT" to "face_left", "NDS_FACE_RIGHT" to "face_right")) {
      movements += ScriptCorpusMovementRecord(name, "generated", listOf(step, "step_end"))
    }

    val textIds = referencedText.mapNotNull { token -> dialect.textId(token)?.let { token to it } }.toMap()
    val missingText = referencedText.count { dialect.textId(it) == null }
    if (missingText > 0) failure("text id unresolved", "$missingText text tokens have no bank/entry")
    val coordTriggers = coordTriggerRows(spec, dialect, constants, referencedTokens)

    return BuiltScriptCorpus(
        spec = spec,
        indexedLabels = indexedLabels,
        programs = programs,
        movements = movements,
        textIds = textIds,
        constants = constants.filterKeys { it in referencedTokens },
        interactableLabels = interactable,
        mapEntryLabels = emptySet(),
        unavailableDirectLabels = emptySet(),
        parseFailureCategories = failures,
        parseFailureSamples = samples.mapValues { it.value.toList() },
        dynamicExits = dynamicExits,
        headerFloors = headerFloors,
        coordTriggers = coordTriggers,
    )
  }

  /**
   * The DS maps' step triggers (the ROM's coord events: the rival waiting upstairs in Twinleaf, the
   * Route 201 grass...) as `bank;map;x;y;width;height;VAR;value;script` rows. They come from the ROM
   * event tables beside the server (tools/nds, `coord;region;bank;map;idx;script;x;y;w;h;height;val;var`),
   * which carry the var's NUMBER; the story store keys vars by the token the scripts use, so the
   * number is named here from the same constants the scripts were built with (a name the scripts
   * reference wins, `VAR_0x....` when the game has none - Unova, whose scripts name every var that
   * way). Unova's rows come from tools/nds/Triggers5 (White's 22-byte trigger records).
   */
  private fun coordTriggerRows(
      spec: ScriptCorpusSpec,
      dialect: Dialect,
      constants: Map<String, Int>,
      referenced: Set<String>,
  ): List<String> {
    if (dialect.region !in 2..4) return emptyList()
    // Unova's "decomp" directory IS server.game (the ROM extracts live there); the decomps sit two
    // levels under the repository, beside server.game.
    val file =
        if (dialect.region == 2) File(spec.decompDir, "nds-npcs-2.txt")
        else File(spec.decompDir.absoluteFile.parentFile?.parentFile, "server.game/nds-npcs-${dialect.region}.txt")
    if (!file.isFile) return emptyList()
    val names = constants.entries.filter { it.key.startsWith("VAR_") }.groupBy({ it.value }, { it.key })
    fun varName(number: Int): String {
      val candidates = names[number].orEmpty()
      return candidates.firstOrNull { it in referenced } ?: candidates.minOrNull() ?: "VAR_0x" + number.toString(16).uppercase()
    }
    return file.readLines().filter { it.startsWith("coord;") }.mapNotNull { line ->
      val p = line.split(';')
      if (p.size < 13) return@mapNotNull null
      // p: kind, region, bank, map, idx, script, x, y, w, h, height, value, var
      val n = p.subList(5, 13).map { it.toIntOrNull() ?: return@mapNotNull null }
      "${p[2]};${p[3]};${n[1]};${n[2]};${n[3]};${n[4]};${varName(n[7])};${n[6]};${n[0]}"
    }
  }

  // ---------------------------------------------------------------- source parsing

  class Block(val label: String, var movement: Boolean, val lines: MutableList<List<String>>)

  class ParsedFile(val entries: List<String>, val blocks: List<Block>)

  /**
   * Splits a script file into labelled blocks of `Command arg, arg` lines. Local labels (`_0033`)
   * are namespaced by file so the corpus stays label-unique; a block that falls through into the
   * next label gets an explicit `goto` so the interpreter never runs off the end.
   */
  private fun parseScriptFile(file: File, dialect: Dialect): ParsedFile {
    val stem = file.nameWithoutExtension
    // Local labels get the file as namespace; the Gen 4 result var takes the GBA name the
    // interpreter's own commands write to (vars are keyed by token, not number).
    val itemAliases = dialect.itemAliases()
    // File-local names: Platinum `#define LOCAL_VAR_PARTY_SLOT VAR_0x8002` / `#define KINSEY 22`,
    // HeartGold `.set GATE_OPEN, 1` - the assembler substitutes them, so this does too.
    val defines = HashMap<String, String>()
    fun localize(token: String): String {
      var t = token
      repeat(4) { t = defines[t] ?: return@repeat }
      return when {
        LOCAL_LABEL.matches(t) -> "${stem}_$t"
        t == "VAR_SPECIAL_RESULT" || t == "VAR_0x800C" -> "VAR_RESULT"
        // HeartGold spells "S.S. Ticket" ITEM_S_S__TICKET; the catalogue mangles it to S_S_TICKET.
        t.startsWith("ITEM_") -> itemAliases[t] ?: t.replace("__", "_")
        else -> t
      }
    }
    val entries = mutableListOf<String>()
    val blocks = mutableListOf<Block>()
    var current: Block? = null
    val macros = dialect.macros()
    val source = ArrayDeque(file.readLines())
    while (source.isNotEmpty()) {
      val raw = source.removeFirst()
      val line = raw.replace(BLOCK_COMMENT, "").substringBefore("//").substringBefore(';').trim()
      Regex("^#define\\s+(\\w+)\\s+(\\S+)").find(line)?.let { defines[it.groupValues[1]] = it.groupValues[2] }
      Regex("^\\.(?:set|equ)\\s+(\\w+)\\s*,\\s*(\\S+)").find(line)?.let { defines[it.groupValues[1]] = it.groupValues[2] }
      // HeartGold keeps a map's local names in a sibling header (`#include ".../event_D37R0104.h"`).
      Regex("^#include\\s+\"([^\"]+)\"").find(line)?.let { inc ->
        val sibling = File(file.parentFile, inc.groupValues[1].substringAfterLast('/'))
        if (sibling.isFile && sibling != file) {
          sibling.readLines().forEach { h ->
            Regex("^#define\\s+(\\w+)\\s+(\\S+)").find(h.replace(BLOCK_COMMENT, "").substringBefore("//").trim())?.let { defines.putIfAbsent(it.groupValues[1], it.groupValues[2]) }
          }
        }
      }
      if (line.isEmpty() || line.startsWith("#") || line.startsWith(".")) continue
      val macroName = line.substringBefore(' ').substringBefore('\t')
      val macro = macros[macroName]
      if (macro != null) {
        val actual = line.removePrefix(macroName).trim().let { if (it.isEmpty()) emptyList() else it.split(',').map(String::trim) }
        val expanded = macro.body.map { body ->
          var text = body
          macro.params.forEachIndexed { i, p -> text = text.replace("\\$p", actual.getOrElse(i) { "0" }) }
          text
        }
        source.addAll(0, expanded)
        continue
      }
      val labelMatch = LABEL_LINE.matchEntire(line)
      if (labelMatch != null) {
        current = Block(localize(labelMatch.groupValues[1]), false, mutableListOf()).also { blocks += it }
        continue
      }
      val name = line.substringBefore(' ').substringBefore('\t')
      val rest = line.removePrefix(name).trim()
      val args = if (rest.isEmpty()) emptyList() else rest.split(',').map { localize(it.trim()) }
      when (name) {
        "ScriptEntry", "ScrDef" -> entries += localize(args.first().substringBefore('@').trim())
        "ScriptEntryEnd", "ScrDefEnd", "InitScriptEntryEnd", "InitScriptEnd" -> {}
        else -> {
          val block = current ?: continue
          if (name in MOVEMENT_STEPS || name == "EndMovement") block.movement = true
          block.lines += listOf(name) + args
        }
      }
    }
    // Fall-through: a script block whose last command is not terminal continues into the next.
    for ((i, block) in blocks.withIndex()) {
      if (block.movement || block.lines.isEmpty()) continue
      val last = block.lines.last()[0]
      if (last !in TERMINAL && i + 1 < blocks.size && !blocks[i + 1].movement) {
        block.lines += listOf("GoTo", blocks[i + 1].label)
      }
    }
    return ParsedFile(entries, blocks)
  }

  // ---------------------------------------------------------------- transpile

  private fun transpile(
      lines: List<List<String>>,
      dialect: Dialect,
      label: String,
      extra: MutableList<Pair<String, List<String>>>,
      msgBank: Int?,
      constants: Map<String, Int>,
      fileTextBase: Int? = null,
      namingLabels: Set<String> = emptySet(),
      nicknameLabels: Set<String> = emptySet(),
  ): List<String> {
    val out = mutableListOf<String>()
    // HeartGold standard messages: GetStdMsgNaix puts a message file id in a var, MsgBoxExtern
    // shows an entry of it; folded here into the same ROM text ids as every other message.
    val stdMsg = HashMap<String, Int>()
    var lastVar8004: Int? = null
    // A scripted menu builds up item by item: Platinum Init*TextMenu / AddMenuEntry* / ShowMenu,
    // HeartGold MenuInit(StdGmm) / MenuItemAdd / MenuExec. Emitted as one ds_menu.
    var menuVar: String? = null
    var menuCursor = "0"
    var menuStdBank = false
    val menuItems = mutableListOf<Pair<String, String>>()
    var genderBlocks = 0
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      val name = line[0]
      // A bare number in a message command indexes the owning map header's own text bank.
      val a = line.drop(1).map { t -> if (msgBank != null && name in MESSAGE_COMMANDS && t.toIntOrNull() != null) "msg_%04d_MAP_%05d".format(msgBank, t.toInt()) else t }
      val next = lines.getOrNull(i + 1)
      when {
        // Text placeholders: the client fills `{0N}` from message args the dialog carries.
        name.startsWith("Buffer") ->
            when (name) {
              "BufferPlayerName", "BufferPlayersName" -> out += "ds_buffer ${a[0]}, player"
              "BufferRivalName", "BufferRivalsName" -> out += "ds_buffer ${a[0]}, rival"
              // Platinum StringTemplate_SetCounterpartName: the professor's other assistant (Lucas / Dawn).
              "BufferCounterpartName" -> out += "ds_buffer ${a[0]}, counterpart"
              "BufferItemName", "BufferItemNameWithArticle", "BufferItemNamePlural", "BufferItemNameIndef" ->
                  if (a.size >= 2) out += "ds_buffer ${a[0]}, item, ${a[1]}"
              "BufferNumber", "BufferInt", "BufferFloorNumber", "BufferDeptStoreFloorNo" ->
                  if (a.size >= 2) out += "ds_buffer ${a[0]}, number, ${a[1]}"
              // White SetVarPoke: the species by national number, a literal or a var.
              "BufferSpeciesName" -> if (a.size >= 2) out += "ds_buffer ${a[0]}, species, ${a[1]}"
              // White's obtain-item routine: a pocket index's name, and a TM item's move name.
              "BufferUnovaPocket" -> if (a.size >= 2) out += "ds_buffer ${a[0]}, pocket, ${a[1]}"
              // slot, kind (partynick | partyspecies | move | type), value
              "BufferUnovaText" -> if (a.size >= 3) out += "ds_buffer ${a[0]}, ${a[1]}, ${a[2]}"
              "BufferUnovaTmMove" -> if (a.size >= 2) out += "ds_buffer ${a[0]}, tmmove, ${a[1]}"
              else -> {}
            }
        else -> when (name) {
        // -- flow
        "End" -> out += "end"
        "Return" -> out += "return"
        "GoTo" -> out += "goto ${a[0]}"
        "Call" -> out += "call ${a[0]}"
        "GoToIfSet" -> out += "goto_if_set ${a[0]}, ${a[1]}"
        "GoToIfUnset" -> out += "goto_if_unset ${a[0]}, ${a[1]}"
        "CallIfSet" -> out += "call_if_set ${a[0]}, ${a[1]}"
        "CallIfUnset" -> out += "call_if_unset ${a[0]}, ${a[1]}"
        "CheckFlag" -> {
          // CheckFlag + GoToIf/CallIf pair (HeartGold spells the macros out).
          if (next != null && (next[0] == "GoToIf" || next[0] == "CallIf")) {
            val set = next[1] == "1" || next[1] == "TRUE"
            val verb = if (next[0] == "GoToIf") "goto" else "call"
            out += "${verb}_if_${if (set) "set" else "unset"} ${a[0]}, ${next[2]}"
            i++
          } else out += "ds_checkflag ${a.joinToString(", ")}"
        }
        "GoToIfEq", "GoToIfNe", "GoToIfLt", "GoToIfLe", "GoToIfGt", "GoToIfGe",
        "CallIfEq", "CallIfNe", "CallIfLt", "CallIfLe", "CallIfGt", "CallIfGe" -> {
          val verb = if (name.startsWith("GoTo")) "goto" else "call"
          val cond = name.takeLast(2).lowercase()
          if (a.size >= 3) {
            out += "compare ${a[0]}, ${a[1]}"
            out += "${verb}_if_$cond ${a[2]}"
          } else out += "${verb}_if_$cond ${a[0]}"
        }
        "Compare", "CompareVarToValue", "CompareVarToVar", "CompareVar" -> out += "compare ${a[0]}, ${a[1]}"
        "GoToIf", "CallIf" -> {
          val verb = if (name == "GoToIf") "goto" else "call"
          val cond = COND_CODES[a[0]] ?: "eq"
          out += "${verb}_if_$cond ${a[1]}"
        }
        // -- flags and vars
        "SetFlag" -> out += "setflag ${a[0]}"
        "ClearFlag" -> out += "clearflag ${a[0]}"
        "SetVar", "SetVarFromValue" -> {
          if (a[0] == "VAR_SPECIAL_x8004") lastVar8004 = a[1].toIntOrNull()
          // A text entry as a value (the nurse's greeting for MessageVar): its index in the bank.
          val entry = dialect.textId(a[1])?.let { it and 0xFFFF }
          out += "setvar ${a[0]}, ${entry ?: a[1]}"
        }
        "AddVar" -> out += "addvar ${a[0]}, ${a[1]}"
        "SubVar" -> out += "subvar ${a[0]}, ${a[1]}"
        "CopyVar", "SetVarFromVar" -> out += "copyvar ${a[0]}, ${a[1]}"
        "SetOrCopyVar" -> out += "setorcopyvar ${a[0]}, ${a[1]}"
        // -- locking and facing
        "LockAll" -> out += "lockall"
        "ReleaseAll" -> out += "releaseall"
        "Lock", "LockObject" -> out += "lock"
        "Release", "ReleaseObject" -> out += "release"
        "FacePlayer" -> out += "faceplayer"
        // -- text
        "Message", "MessageInstant", "MessageNoSkip", "MessageSynchronized", "NPCMessage", "EventMessage",
        "NPCMsg", "NonNPCMsg", "MessageVar", "NPCMsgVar", "NonNPCMsgVar" -> {
          val bankBase = msgBank?.let { dialect.textId("msg_%04d_MAP_%05d".format(it, 0)) } ?: fileTextBase
          if (name.endsWith("Var") && a.size == 1 && a[0].startsWith("VAR_") && bankBase != null) {
            // MessageVar: the var holds an entry of the script's own bank.
            out += "ds_messagevar ${a[0]}, $bankBase"
            out += "waitmessage"
          } else if (dialect.textId(a[0]) == null) out += "ds_${name.lowercase()} ${a.joinToString(", ")}"
          else {
            // A yes/no prompt right after the text (Platinum ShowYesNoMenu, HeartGold's {YESNO}
            // text + GetMenuChoice) is one GBA MSGBOX_YESNO; Gen 4 answers 0 = yes, 1 = no. The
            // question is the LAST text before it: another message in between owns the prompt.
            var prompt: Int? = null
            for (k in 1..3) {
              val l = lines.getOrNull(i + k) ?: break
              if (l[0] in YESNO_COMMANDS) {
                prompt = k
                break
              }
              if (l[0] in PROMPT_MESSAGES) break
            }
            val skipped = prompt?.let { skippedQuestion(lines, i + prompt + 1, lines[i + prompt].getOrNull(1), namingLabels, constants, nicknameLabels) }
            if (skipped != null) {
              // "Give it a nickname?" / "So his name was X?" / Mom's savings: nicknames and names
              // are not given in the story here and Mom keeps no bank, so the question is skipped
              // and the script takes the other answer. The line itself still shows unless it is
              // the nickname offer.
              if (skipped.keepMessage) {
                out += "message ${a[0]}"
                out += "waitmessage"
              }
              skipped.goto?.let { out += "goto $it" }
              skipped.call?.let { out += "call $it" }
              i = skipped.last
            } else if (prompt != null) {
              out += "msgbox ${a[0]}, MSGBOX_YESNO"
              out += "ds_yesno ${lines[i + prompt][1]}"
              i += prompt
            } else {
              out += "message ${a[0]}"
              out += "waitmessage"
            }
          }
        }
        // Signposts: the client draws them as plain dialog over the same ROM text.
        "ShowLandmarkSign", "ShowArrowSign", "ShowMapSign", "ShowScrollingSign", "TrainerTips", "TrainerTipsEx",
        "DirectionSignpost", "DirectionSignpostEx", "PokemonCryAndMessage", "DrawSignpostInstantMessage" -> {
          val text = a.lastOrNull { dialect.textId(it) != null }
          if (text == null) out += "ds_${name.lowercase()} ${a.joinToString(", ")}"
          else {
            out += "msgbox $text, MSGBOX_DEFAULT"
          }
        }
        "GetMenuChoice" -> {
          val skipped = skippedQuestion(lines, i + 1, a[0], namingLabels, constants, nicknameLabels)
          if (skipped != null) {
            // The question was shown by the message before it; a dropped nickname offer takes its
            // shown text back out.
            if (!skipped.keepMessage && out.size >= 2 && out.last() == "waitmessage" && out[out.size - 2].startsWith("message ")) repeat(2) { out.removeAt(out.size - 1) }
            skipped.goto?.let { out += "goto $it" }
            skipped.call?.let { out += "call $it" }
            i = skipped.last
          } else {
            out += "yesnobox 0, 0"
            out += "ds_yesno ${a[0]}"
          }
        }
        "GetPlayerMapPos", "GetPlayerCoords" -> out += "getplayerxy ${a[0]}, ${a[1]}"
        "SetObjectEventPos" -> out += "setobjectxy ${a[0]}, ${a[1]}, ${a[2]}" + (a.getOrNull(3)?.let { ", $it" } ?: "")
        "GoToIfCannotFitItem" -> {
          out += "checkitemspace ${a[0]}, ${a[1]}"
          out += "compare VAR_RESULT, 0"
          out += "goto_if_eq ${a.last()}"
        }
        "GoToIfNoItemSpace" -> {
          out += "checkitemspace ${a[0]}, ${a[1]}"
          out += "compare VAR_RESULT, 0"
          out += "goto_if_eq ${a[2]}"
        }
        // Badges are a save bitfield on the DS, not event flags: they live as synthetic story
        // flags the gym scripts set the same way.
        "GoToIfBadgeAcquired" -> out += "goto_if_set FLAG_DS_BADGE_${badge(a[0], constants)}, ${a[1]}"
        "CheckBadge", "CheckBadgeAcquired" -> out += "ds_flagtovar FLAG_DS_BADGE_${badge(a[0], constants)}, ${a[1]}"
        "CheckFlagVar" -> out += "ds_flagtovar ${a[0]}, ${a[1]}"
        "GiveBadge" -> out += "setflag FLAG_DS_BADGE_${badge(a[0], constants)}"
        "CountBadgesAcquired" -> out += "ds_countbadges ${a[0]}"
        "HealParty" -> out += "special HealPlayerParty"
        // Platinum's briefcase (choose_starter_app): STARTER_OPTION_0..2 with bank 360's "Now choose!"
        // (entry 7) and one question per ball (entries 1-3); the pick lands in VAR_PLAYER_STARTER
        // (SystemVars_SetPlayerStarter), which SaveChosenStarter would store.
        "StartChooseStarterScene" -> {
          val species = listOf("SPECIES_TURTWIG", "SPECIES_CHIMCHAR", "SPECIES_PIPLUP").map { constants[it] }
          val texts = listOf(7, 1, 2, 3).map { dialect.textId("pl_msg_00000360_%05d".format(it)) }
          if (species.any { it == null } || texts.any { it == null }) out += "ds_startchoosestarterscene"
          else out += "ds_startchoosestarterscene VAR_PLAYER_STARTER, ${species.joinToString(", ")}, ${texts.joinToString(", ")}"
        }
        // White's gift box (see the first pass): Snivy, Tepig, Oshawott - entry 8's own table -
        // over bank 430's "Choose a Pokémon." (19) and the type lines (18 grass, 17 fire, 16
        // water; each names the species through text slot 1). The script wants the INDEX back.
        // White's FadeScreen (see the first pass): 1 darkens, 0 restores - the GBA fadescreen modes.
        "ScreenFade" -> out += "fadescreen ${a[0]}"
        // White OpenInterpoke: the client's own Xtransceiver call window, by call id.
        "DsXtransceiver" -> out += "ds_xtransceiver ${a[0]}"
        // White MakeNPC: id, sprite, x, y, DS facing - an actor the script creates.
        "DsMakeNpc" -> out += "ds_makenpc ${a[0]}, ${a[1]}, ${a[2]}, ${a[3]}, ${a[4]}"
        // White Message's speaker object for the next line ("none" = the script's own entity).
        "DsSpeaker" -> out += "ds_speaker ${a[0]}"
        // White CMD_129 on a door: 0 opens, 1 closes the door at x, y.
        "DsDoor" -> out += "ds_door ${a[0]}, ${a[1]}, ${a[2]}"
        // White DoubleTrainerBattle partner, enemy1, enemy2 (every Ds* name needs its own case).
        "DsDoubleTrainerBattle" -> out += "ds_doubletrainerbattle ${a[0]}, ${a[1]}, ${a[2]}"
        "DsStorePartyCount" -> out += "ds_storepartycount ${a[0]}, ${a[1]}"
        "DsGameVersion" -> out += "ds_gameversion ${a[0]}"
        // Every Ds* intermediate name needs its own case here: the fallback writes
        // ds_<name.lowercase()>, which for DsMapGimmick was `ds_dsmapgimmick` - an unknown
        // command, so every Striaton button script failed to resolve and the buttons did
        // nothing at all (owner, 2026-09-21).
        "DsMapGimmick" -> out += "ds_mapgimmick ${a.joinToString(", ")}"
        // The ROM asks for its own sounds and the client can play them (s2c 0x00 -> the region's
        // SDAT); dropping them left the DS regions silent where the cartridge is not. Only a
        // numeric id goes out - a decomp symbol we cannot resolve stays dropped rather than
        // risking the client's sound engine, which mutes the whole session on one bad id.
        "PlaySound" -> a.getOrNull(0)?.toIntOrNull()?.let { out += "ds_playsound $it" }
        "ChangeMusic" -> a.getOrNull(0)?.toIntOrNull()?.let { out += "ds_changemusic $it" }
        "WaitSound", "WaitSoundA7" -> out += "ds_waitsound"
        // The PC's boot sound is an engine call with no id; the ROM's own SDAT names it
        // SEQ_SE_PC_ON (1371).
        "BootPCSound" -> out += "ds_playsound 1371"
        // White CMD_BB: the item's pocket (0 Items .. 4 Key Items) into a var.
        "UnovaItemPocket" -> out += "ds_itempocket ${a[0]}, ${a[1]}"
        "ChooseUnovaStarter" -> {
          val texts = listOf(19, 18, 17, 16).map { dialect.textId("T0430_%05d".format(it)) }
          if (texts.any { it == null }) out += "ds_startchoosestarterscene"
          else out += "ds_startchoosestarterscene ${a[0]}, 495, 498, 501, ${texts.joinToString(", ")}, index"
        }
        "SaveChosenStarter" -> {}
        "GetPlayerStarterSpecies" -> out += "copyvar ${a[0]}, VAR_PLAYER_STARTER"
        // The macro compares each value in turn (asm/macros/scrcmd.inc): lower..upper inclusive.
        "GoToIfInRange" -> {
          val lower = constants[a[1]] ?: a[1].toIntOrNull()
          val upper = constants[a[2]] ?: a[2].toIntOrNull()
          if (lower == null || upper == null || upper - lower > 64) out += "ds_gotoifinrange ${a.joinToString(", ")}"
          else for (v in lower..upper) {
            out += "compare ${a[0]}, $v"
            out += "goto_if_eq ${a[3]}"
          }
        }
        // One dex for the whole game (the owner's rule for an MMO, the same one Kanto's
        // GetPokedexCount follows): every rating, count and completion check is the NATIONAL one -
        // Pokedex_GetRatingMessageID_National's bands over the caught count, Oak's lines.
        "LoadLocalDexRating", "LoadNationalDexRating" -> {
          val entries = NATIONAL_DEX_RATING_TEXTS.map { dialect.textId(it)?.and(0xFFFF) }
          if (entries.any { it == null }) out += "ds_${name.lowercase()} ${a.joinToString(", ")}"
          else out += "ds_dexrating ${a[0]}, ${entries.joinToString(", ")}"
        }
        "GetLocalDexSeenCount", "GetNationalDexSeenCount" -> out += "ds_dexcount seen, ${a[0]}"
        "GetLocalDexCaughtCount_Unused", "GetNationalDexCaughtCount" -> out += "ds_dexcount caught, ${a[0]}"
        "CheckLocalDexCompleted", "CheckNationalDexCompleted" -> out += "ds_dexcompleted ${a[0]}"
        // The National Dex is a story flag, the same one FireRed's EnableNationalPokedex sets.
        "GetNationalDexEnabled" -> out += "ds_flagtovar FLAG_SYS_NATIONAL_DEX, ${a[0]}"
        "SetNationalDexEnabled" -> out += "setflag FLAG_SYS_NATIONAL_DEX"
        "GiveRunningShoes" -> out += "setflag FLAG_DS_RUNNING_SHOES"
        // -- in-game trades, the same npc-trade service Kanto's Cerulean/Vermilion traders use.
        // Platinum: SelectPokemonToTrade -> InitNPCTrade id -> GetNPCTradeRequestedSpecies VAR ->
        // StartNPCTrade slot -> FinishNPCTrade. HeartGold: PartySelectUI/GetPartySelection ->
        // LoadNPCTrade n -> NPCTradeGetReqSpecies VAR -> NPCTradeExec slot -> NPCTradeEnd.
        "SelectPokemonToTrade" -> out += "ds_choosepartymon VAR_RESULT, trade"
        "InitNPCTrade", "LoadNPCTrade" -> out += "ds_npctrade_init ${constants[a[0]] ?: a[0]}"
        "GetNPCTradeRequestedSpecies", "NPCTradeGetReqSpecies" -> out += "ds_npctrade_species ${a[0]}"
        "StartNPCTrade", "NPCTradeExec" -> out += "ds_npctrade_exec ${a[0]}"
        "FinishNPCTrade", "NPCTradeEnd" -> {}
        "SetMonMove" -> out += "ds_setmonmove ${a[0]}, ${a[1]}, ${constants[a[2]] ?: a[2]}"
        // -- scripted wild battles ride the GBA wild-battle path; the outcome reads like the games'.
        // Platinum's CheckWonBattle (CheckPlayerWonBattle: FALSE only for LOSE/DRAW) copies VAR_RESULT.
        // PokeMMO's story legendaries do not battle at all (the scene carries on as if won) except
        // the bosses - Ho-Oh, Giratina - which are boss fights still to be built, and Zekrom's
        // required catch; the owner's rule, verified on the PokeMMO wiki (memory pokemmo-story-legendaries).
        "StartLegendaryBattle" -> {
          when (legendaryKind(constants[a[0]] ?: a[0].toIntOrNull())) {
            "boss" -> out += "ds_bossbattle ${a[0]}, ${a[1]}"
            "none" -> out += "ds_wildoutcome VAR_RESULT, won"
            else -> {
              out += "setwildbattle ${a[0]}, ${a[1]}, ITEM_NONE"
              out += "dowildbattle"
              out += "ds_wildoutcome VAR_RESULT, won"
            }
          }
        }
        // HeartGold: the BATTLE_OUTCOME_* code (1 win, 2 lose, 4 caught, 5 fled);
        // StaticWildWonOrCaughtCheck answers TRUE when the monster is still out there (not won, not caught).
        "GetStaticEncounterOutcome" -> out += "ds_wildoutcome ${a[0]}, outcome"
        "StaticWildWonOrCaughtCheck" -> out += "ds_wildoutcome ${a[0]}, escaped"
        // -- party, dex, money, PC, world: the Kanto mechanics.
        "HideObject" -> out += "removeobject ${a[0]}"
        "ShowObject" -> out += "addobject ${a[0]}"
        "GetFirstNonEggInParty" -> out += "ds_firstnonegg ${a[0]}"
        "FindPartySlotWithMove", "GetPartySlotWithMove" -> out += "ds_partyslotwithmove ${a[0]}, ${constants[a[1]] ?: a[1]}"
        "GetDayOfWeek" -> out += "ds_getweekday ${a[0]}"
        "CheckGameCompleted" -> out += "ds_gamecompleted ${a[0]}"
        "SetPlayerBike" -> out += "ds_setbike ${constants[a[0]] ?: a[0]}"
        // Saving is the server's: every save prompt finds a quick save that succeeds.
        "CheckSaveType" -> out += "setvar ${a[0]}, ${constants["SAVE_TYPE_QUICK_SAVE"] ?: 3}"
        "TrySaveGame" -> out += "setvar ${a[0]}, 1"
        "PlayerHasSpecies" -> out += "ds_partyhasspecies ${a[0]}, ${constants[a[1]] ?: a[1]}"
        "CheckPartyHasFatefulEncounterRegigigas" -> out += "ds_partyhasspecies ${a[0]}, ${constants["SPECIES_REGIGIGAS"] ?: 486}, fateful"
        "Random" -> {
          out += "random ${a[1]}"
          resultCopy(out, a[0])
        }
        "SubMoneyVar" -> out += "removemoney ${a[0]}, 0"
        "MovePerson" -> out += "setobjectxy ${OBJECT_ALIASES[a[0]] ?: a[0]}, ${a[1]}, ${a[2]}"
        // NatDexFlagAction 1 = enable, 2 = query; one dex for the whole game, so it is the Kanto flag.
        "NatDexFlagAction" -> if (a[0] == "1") { out += "setflag FLAG_SYS_NATIONAL_DEX"; out += "setvar ${a[1]}, 0" } else out += "ds_flagtovar FLAG_SYS_NATIONAL_DEX, ${a[1]}"
        "CountPCEmptySpace" -> out += "ds_pcemptyspace ${a[0]}"
        "CountAliveMonsAndPC" -> out += "ds_countalive ${a[0]}"
        // MonHasMove VAR, move, slot; GetPartyMonFriendship VAR, slot (the HeartGold shape).
        "MonHasMove" -> out += "ds_monhasmove ${a[0]}, ${constants[a[1]] ?: a[1]}, ${a[2]}"
        "GetPartyMonFriendship" -> out += "ds_mongetfriendship ${a[0]}, ${a[1]}"
        // Rotom's forms: count of transformed Rotom and the first slot (Platinum count, slot; HeartGold count, slot).
        "GetPartyRotomCountAndFirst", "CountTranformedRotomsInParty" -> out += "ds_rotomcount ${a[0]}, ${a[1]}"
        "CheckPartyHasSpecies2" -> out += "ds_partyhasspecies ${a[1]}, ${constants[a[0]] ?: a[0]}"
        "CheckDidNotCapture" -> out += "ds_wildoutcome ${a[0]}, notcaught"
        "GetCurrentMapID" -> out += "ds_currentmapid ${a[0]}"
        "CheckSeenAllLetterUnown" -> out += "ds_unownforms ${a[0]}, all"
        // GoToIfNotEnoughMoney value, offset: CheckMoney into VAR_RESULT, branch on FALSE.
        "GoToIfNotEnoughMoney" -> {
          out += "checkmoney ${a[0]}, 0"
          out += "compare VAR_RESULT, 0"
          out += "goto_if_eq ${a[1]}"
        }
        "SaveGameNormal" -> out += "setvar ${a[0]}, 1"
        "GetUnownFormsSeenCount" -> out += "ds_unownforms ${a[0]}"
        // The Safari Game is the same one Kanto plays (SafariService): balls, steps, the exit warp.
        "SafariZoneAction" -> out += if (a[0] == "0") "special EnterSafariMode" else "special ExitSafariMode"
        "StartEndSafariGame" -> out += if ((constants[a[0]] ?: a[0].toIntOrNull()) == 0 && !a[0].contains("INACTIVE")) "special EnterSafariMode" else "special ExitSafariMode"
        "EndSafariGame" -> out += "special ExitSafariMode"
        "MessageFromBank" -> {
          out += "ds_messagefrombank ${a[0]}, ${a[1]}"
          out += "waitmessage"
        }
        // No Griseous Orb to hand back, no forms to reset on the way into the daycare.
        "DaycareSanitizeMon" -> out += "setvar ${a[1]}, 0"
        // HeartGold's Elm's lab (src/choose_starter.c): Chikorita, Cyndaquil, Totodile at level 5,
        // msg_0190 entry 7 "Once you've decided, touch a Poke Ball!", entries 1-3 per ball. The app
        // puts the pick in the party itself; the script reads it back with GetPartyMonSpecies.
        "ChooseStarter" -> {
          val species = listOf("SPECIES_CHIKORITA", "SPECIES_CYNDAQUIL", "SPECIES_TOTODILE").map { constants[it] }
          val texts = listOf(7, 1, 2, 3).map { dialect.textId("msg_0190_%05d".format(it)) }
          if (species.any { it == null } || texts.any { it == null }) out += "ds_choosestarter"
          else {
            out += "ds_startchoosestarterscene VAR_DS_CHOSEN_STARTER, ${species.joinToString(", ")}, ${texts.joinToString(", ")}"
            out += "givemon VAR_DS_CHOSEN_STARTER, 5, ITEM_NONE"
          }
        }
        // Save_VarsFlags_SetStarter / GetStarter: VAR_PLAYER_STARTER (src/sys_vars.c).
        "SetStarterChoice" -> out += "copyvar VAR_PLAYER_STARTER, ${a[0]}"
        "GetStarterChoice" -> out += "copyvar ${a[0]}, VAR_PLAYER_STARTER"
        "GetFriendSprite" -> out += "ds_getfriendsprite ${a[0]}"
        "GetPersonCoords" -> out += "ds_getpersoncoords ${a[0]}, ${a[1]}, ${a[2]}"
        "MonGetFriendship" -> out += "ds_mongetfriendship ${a[0]}, ${a[1]}"
        "GetPartyMonForm2" -> out += "ds_getpartymonform ${a[0]}, ${a[1]}"
        "HasEnoughMoneyVar" -> out += "ds_hasenoughmoney ${a[0]}, ${a[1]}"
        "PartySelectUI" -> {}
        "GetPartySelection" -> out += "ds_choosepartymon ${a[0]}"
        // Platinum's party picker (stats judge, tutors): the pick lands in the GetSelectedPartySlot that follows.
        "SelectMoveTutorPokemon" -> {}
        "GetSelectedPartySlot" -> out += "ds_choosepartymon ${a[0]}"
        // HeartGold tutors teach one named move to the chosen slot through the client's own learn/forget
        // dialog (the Kanto tutors' path); the result var reads 255 when the player backed out.
        "MoveTutorInit" -> out += "ds_teachmove ${a[0]}, ${constants[a[1]] ?: a[1]}"
        "MoveRelearnerGetResult" -> out += "copyvar ${a[0]}, VAR_DS_TUTOR_RESULT"
        "GetPartyLeadAlive", "GetFollowPokePartyIndex" -> out += "ds_getpartyleadalive ${a[0]}"
        "PartyCountNotEgg" -> out += "ds_countpartynoneggs ${a[0]}"
        "TakeItemNoCheck" -> out += "removeitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
        // The rival keeps his canonical name: no naming screen, and a result that never re-asks.
        "NameRival" -> out += "setvar ${a[0]}, 0"
        // No mail in the catalogue, no ribbons on this server's monsters: the checks answer no
        // into their FIRST argument (KenyaCheck VAR, slot, kind / MonHasRibbon VAR, slot, ribbon).
        "KenyaCheck", "MonHasRibbon" -> out += "setvar ${a[0]}, 0"
        // Marts: the badge-tiered common shelf, or a specialty shelf by the game's mart index.
        "PokeMartCommon", "MartBuy" -> out += "ds_martcommon"
        "MartSell", "PokeMartDecor", "PokeMartSeal", "ShowAccessoryShop" -> {}
        "PokeMartSpecialties" -> {
          val items = dialect.martTables()[constants[a[0]] ?: a[0].toIntOrNull() ?: -1]
          if (items == null) out += "ds_pokemartspecialties ${a[0]}" else out += "ds_pokemart ${items.joinToString(", ")}"
        }
        "GetStdMsgNaix" -> STD_MSG_BANKS[a[0].toIntOrNull() ?: -1]?.let { stdMsg[a[1]] = it }
        "MsgBoxExtern", "NonNPCMsgExtern" -> {
          val bank = stdMsg[a[0]]
          val idx = a.getOrNull(1)?.toIntOrNull()
          if (bank != null && idx != null) {
            out += "message msg_%04d_STD_%05d".format(bank, idx)
            out += "waitmessage"
          } else out += "ds_${name.lowercase()} ${a.joinToString(", ")}"
        }
        "ReturnCommonScript" -> out += "return"
        // Trainers by ROM id: HeartGold names the constant, Platinum's shared battle script reads
        // VAR_0x8004 (set by the chunk binding from the script id).
        "TrainerBattle", "StartTrainerBattle" -> out += "ds_trainerbattle ${a[0]}"
        // Trainer speech by ROM table: (trainer, message kind). The kind vars come from the
        // types query the shared battle script runs first (single 0/2, doubles 3-10, rematch 17-19).
        "PrintTrainerDialogue", "TrainerMessage" -> out += "ds_trainermsg ${a[0]}, ${a[1]}"
        "GetTrainerMessageTypes", "GetTrainerMsgParams" -> out += "ds_trainermsgtypes ${a[0]}, ${a[1]}, ${a[2]}"
        "GetTrainerRematchMessageTypes", "GetRematchMsgParams" -> out += "ds_trainermsgtypes_rematch ${a[0]}, ${a[1]}, ${a[2]}"
        "CheckTrainerFlag" -> {
          // HeartGold checks into the compare result; Platinum names a var.
          out += "ds_checktrainerflag ${a[0]}, ${a.getOrElse(1) { "VAR_RESULT" }}"
          if (a.size < 2) out += "compare VAR_RESULT, 1"
        }
        "SetTrainerFlag" -> out += "ds_settrainerflag ${a[0]}"
        // The approaching trainer's id: the chunk binding already put it in VAR_0x8004.
        "GetTrainerID" -> if (a[0] != "VAR_0x8004") out += "copyvar ${a[0]}, VAR_0x8004"
        "CheckWonBattle", "CheckBattleWon" -> resultCopy(out, a.getOrNull(0))
        "GoToIfDefeated" -> {
          out += "ds_checktrainerflag ${a[0]}, VAR_RESULT"
          out += "compare VAR_RESULT, 1"
          out += "goto_if_eq ${a[1]}"
        }
        "WildBattle", "RocketTrapBattle" -> {
          // HeartGold's static legendaries (Ho-Oh, Lugia, Suicune, Kyogre/Groudon...) follow the same
          // PokeMMO rule as Platinum's StartLegendaryBattle: no battle, or a boss fight to come.
          when (legendaryKind(constants[a[0]] ?: a[0].toIntOrNull())) {
            "boss" -> out += "ds_bossbattle ${a[0]}, ${a.getOrElse(1) { "5" }}"
            "none" -> {}
            else -> {
              out += "setwildbattle ${a[0]}, ${a.getOrElse(1) { "5" }}, ITEM_NONE"
              out += "dowildbattle"
            }
          }
        }
        "ClearTrainerFlag" -> out += "ds_cleartrainerflag ${a[0]}"
        "Switch" -> out += "switch ${a[0]}"
        "Case" -> out += "case ${a[0]}, ${a[1]}"
        "SetPosition" -> {
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          out += "setobjectxy $target, ${a[1]}, ${a[2]}"
          FACE_MOVEMENTS[a[4]]?.let { out += "applymovement $target, $it" }
        }
        "GetRandom" -> {
          out += "random ${a[1]}"
          resultCopy(out, a[0])
        }
        "GetItemQuantity" -> {
          out += "checkitem ${a[0]}, 1"
          resultCopy(out, a[1])
        }
        "MakeObjectVisible" -> {
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          if (target != "LOCALID_PLAYER") out += "addobject $target"
        }
        "SetObjectEventDir", "SetObjectFacing" -> {
          val face = FACE_MOVEMENTS[a[1]]
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          if (face != null) out += "applymovement $target, $face"
        }
        "MovePersonFacing" -> {
          // person, x, z, y(height), facing: z is the map's vertical axis.
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          out += "setobjectxy $target, ${a[1]}, ${a[2]}"
          FACE_MOVEMENTS[a[4]]?.let { out += "applymovement $target, $it" }
        }
        // Two texts, one per player gender.
        "GenderMsgBox" -> genderedMessage(out, extra, "${label}_g${genderBlocks++}", a[0], a[1])
        // A Pokegear call from a map script (PhoneCall contact, 2, n): the ROM's call app prints the
        // contact's n-th scripted line straight away (these contacts have no greeting row, phone
        // book unkC = 255), waits for A, and hangs up. The line is ROM text in the contact's own
        // bank, so it is a dialog box here.
        "PhoneCall" -> {
          val call = if (a.getOrNull(1) == "2") HEARTGOLD_SCRIPTED_CALLS[a[0]]?.getOrNull(a.getOrNull(2)?.toIntOrNull() ?: -1) else null
          if (call == null) out += "ds_phonecall ${a.joinToString(", ")}"
          else {
            out += "ds_buffer 0, player"
            genderedMessage(out, extra, "${label}_call${genderBlocks++}", call.male, call.female)
            out += "waitbuttonpress"
            out += "closemessage"
            call.followUp?.let {
              out += "message $it"
              out += "waitmessage"
              out += "waitbuttonpress"
              out += "closemessage"
            }
          }
        }
        in STUB_QUERIES.keys -> {
          val target = a.lastOrNull { it.startsWith("VAR_") }
          if (target != null) out += "setvar $target, ${STUB_QUERIES.getValue(name)}"
        }
        "GetPlayerDir", "GetPlayerFacing" -> out += "ds_getplayerdir ${a[0]}"
        "GetWeekday" -> out += "ds_getweekday ${a[0]}"
        "CallCommonScript" -> out += "call NDS_CHUNK_${a[0].removePrefix("0x").toIntOrNull(if (a[0].startsWith("0x")) 16 else 10) ?: a[0]}"
        "CallStd" -> {
          val id = constants[a[0]] ?: a[0].toIntOrNull() ?: a[0].removePrefix("0x").toIntOrNull(16).takeIf { a[0].startsWith("0x") }
          when (id) {
            2011 -> out += "ds_martcommon"
            2052 -> {
              // std_special_mart reads the shelf index the caller put in VAR_SPECIAL_x8004.
              val items = lastVar8004?.let { dialect.martTables()[it] }
              if (items == null) out += "ds_pokemartspecialties ${lastVar8004 ?: "?"}" else out += "ds_pokemart ${items.joinToString(", ")}"
            }
            else ->
                // Gen 5 engine standard scripts (10000+) have no script file of their own: file
                // 865 is the HIDDEN ITEM file and binding the band to it made `CallStd 10110`,
                // 55 call sites, hand out item 88, while the Center counter npcs (10100, 10105)
                // each gave a one-off Pearl - the owner found them on the mart clerks 2026-09-21.
                // Unbound ones are dropped: an unresolvable call aborts the script where it sits.
                if (dialect.unboundStdCalls()?.contains(id) == true) Unit
                else out += "call NDS_CHUNK_${id ?: a[0]}"
          }
        }
        "GiveItemNoCheck" -> {
          out += "giveitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "RestartCurrentScript" -> out += "end"
        // -- scripted menus -> the client's text-button list over the current message; the
        // chosen entry's value lands in the menu's var (the games' entryIndex / MenuItemAdd value).
        "InitGlobalTextMenu", "InitLocalTextMenu", "InitGlobalTextListMenu", "InitLocalTextListMenu" -> {
          menuVar = a.getOrNull(3); menuCursor = a.getOrElse(2) { "0" }; menuStdBank = false; menuItems.clear()
        }
        "MenuInitStdGmm", "MenuInit" -> {
          menuVar = a.getOrNull(4); menuCursor = a.getOrElse(2) { "0" }; menuStdBank = name == "MenuInitStdGmm"; menuItems.clear()
        }
        "AddMenuEntryImm", "AddMenuEntry", "AddListMenuEntry" -> {
          val id = a.getOrNull(0)?.let(dialect::textId)
          if (id != null && a.size >= 2) menuItems += id.toString() to a[1]
        }
        "MenuItemAdd" -> {
          // entry of the standard menu bank (HeartGold msg_0191: 1F..6F, B1F, ROOF, EXIT) or of the map's own bank.
          val bank = if (menuStdBank) HEARTGOLD_STD_MENU_BANK else msgBank
          val entry = a.getOrNull(0)?.toIntOrNull()
          val id = if (bank != null && entry != null) dialect.textId("msg_%04d_STD_%05d".format(bank, entry)) else null
          if (id != null && a.size >= 3) menuItems += id.toString() to a[2]
        }
        "ShowMenu", "ShowListMenu", "ShowMenuMultiColumn", "MenuExec" -> {
          val target = menuVar
          val exit = target?.let { droppedMenuExit(lines, i + 1, it, namingLabels) }
          if (exit != null) {
            // A menu whose choices lead into Mom's bank: her line stays, the menu goes, and the
            // script takes the Switch's own way out.
            out += "goto ${exit.first}"
            i = exit.second
          } else if (target != null && menuItems.isNotEmpty()) out += "ds_menu $target, $menuCursor, " + menuItems.joinToString(", ") { "${it.first}, ${it.second}" }
          else if (target != null) out += "setvar $target, 127"
          menuItems.clear()
        }
        // -- elevators: the dynamic warp is the door the player came in by (SetDynamicWarp /
        // SetSpecialLocation: header, warpId, x, y|z, dir); the floor comes from the game's table.
        "GetFloorsAbove", "GetDynamicWarpFloorNo" -> out += "ds_dynamicwarpfloor ${a[0]}"
        "SetSpecialLocation", "SetDynamicWarp" -> if (a.size >= 4) out += "ds_setdynamicwarp ${a[0]}, ${a[2]}, ${a[3]}"
        // The "Now on nF" window and the shaking-cab animation: presentation only.
        "ShowCurrentFloor", "ElevatorCurFloorBox", "PlayElevatorAnimation", "ElevatorAnim" -> {}
        "Warp" -> {
          // Platinum: header, x, z, dir. HeartGold: header, 0, x, y, dir. The header id is the
          // client's bank/map pair; the interpreter raw-warps to it.
          if (a.size >= 5) out += "ds_warp ${a[0]}, ${a[2]}, ${a[3]}" else out += "ds_warp ${a[0]}, ${a[1]}, ${a[2]}"
        }
        "SimpleNPCMsg" -> {
          out += "lockall"
          out += "faceplayer"
          out += "message ${a[0]}"
          out += "waitmessage"
          out += "waitbuttonpress"
          out += "closemessage"
          out += "releaseall"
        }
        "WaitButton", "WaitABPress", "WaitABPadPress", "WaitButtonOrDpad" -> out += "waitbuttonpress"
        "CloseMessage", "CloseMsg", "CloseMessageWithoutErasing", "HoldMsg", "OpenMessage", "OpenMsg" ->
            if (name.startsWith("Open")) {} else out += "closemessage"
        "ShowYesNoMenu", "YesNo" -> {
          val skipped = skippedQuestion(lines, i + 1, a[0], namingLabels, constants, nicknameLabels)
          if (skipped != null) {
            if (!skipped.keepMessage && out.size >= 2 && out.last() == "waitmessage" && out[out.size - 2].startsWith("message ")) repeat(2) { out.removeAt(out.size - 1) }
            skipped.goto?.let { out += "goto $it" }
            skipped.call?.let { out += "call $it" }
            i = skipped.last
          } else {
            out += "yesnobox 0, 0"
            out += "ds_yesno ${a[0]}"
          }
        }
        // -- movement
        "ApplyMovement" -> {
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          if (target in IGNORED_OBJECTS) {} else out += "applymovement $target, ${a[1]}"
        }
        "WaitMovement" -> out += "waitmovement 0"
        "WaitTime", "Wait" -> out += "delay ${a.getOrElse(0) { "1" }}"
        "AddObject", "ShowPerson" -> if (a[0] !in IGNORED_OBJECTS) out += "addobject ${a[0]}"
        "RemoveObject", "HidePerson" -> if (a[0] !in IGNORED_OBJECTS) out += "removeobject ${a[0]}"
        // -- items, money, monsters
        "AddItem", "GiveItem" -> {
          out += "giveitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "RemoveItem", "TakeItem" -> {
          out += "removeitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "CheckItem", "HasItem" -> {
          out += "checkitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "CanFitItem", "HasSpaceForItem" -> {
          out += "checkitemspace ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "GivePokemon" -> {
          out += "givemon ${a[0]}, ${a[1]}, ${a.getOrElse(2) { "ITEM_NONE" }}"
          resultCopy(out, a.getOrNull(3))
        }
        "GiveMon" -> {
          out += "givemon ${a[0]}, ${a[1]}, ${a.getOrElse(2) { "ITEM_NONE" }}"
          resultCopy(out, a.getOrNull(5))
        }
        "GiveMoney", "AddMoney" -> out += "addmoney ${a[0]}, 0"
        "RemoveMoney", "SubMoneyImmediate" -> out += "removemoney ${a[0]}, 0"
        "CheckMoney", "HasEnoughMoneyImmediate" -> {
          out += "checkmoney ${a[0]}, 0"
          resultCopy(out, a.getOrNull(1))
        }
        "GetPlayerGender" -> {
          out += "checkplayergender"
          resultCopy(out, a.getOrNull(0))
        }
        "GetPartySize", "GetPartyCount" -> {
          out += "getpartysize"
          resultCopy(out, a.getOrNull(0))
        }
        // -- presentation with no server counterpart: dropped, the client owns audio and fades.
        // Buffer* fill text placeholders; the client formats DS text from message args, which
        // the server does not pass yet, so they are no-ops for now.
        in DROPPED -> {}
        "WaitForAnimation" -> {}
        else -> {
          // Unnamed engine commands: the ones that answer into a var get a zero, the rest vanish.
          if (name.startsWith("ScrCmd_")) a.lastOrNull { it.startsWith("VAR_") }?.let { out += "setvar $it, 0" }
          else out += "ds_${name.lowercase()}" + if (a.isEmpty()) "" else " " + a.joinToString(", ")
        }
      }
      }
      i++
    }
    if (out.isEmpty() || out.last() !in setOf("end", "return") && !out.last().startsWith("goto ")) out += "end"
    return out
  }

  /**
   * One message per player gender (checkplayergender leaves MALE = 0 in VAR_RESULT), as two called
   * blocks so the script carries on after it. The earlier goto-to-a-return form underflowed the call
   * stack in every top-level script (Elm's lab never reached its scene var).
   */
  private fun genderedMessage(out: MutableList<String>, extra: MutableList<Pair<String, List<String>>>, label: String, male: String, female: String) {
    if (male == female) {
      out += "message $male"
      out += "waitmessage"
      return
    }
    out += "checkplayergender"
    out += "compare VAR_RESULT, 1"
    out += "call_if_eq ${label}_female"
    out += "call_if_ne ${label}_male"
    extra += "${label}_female" to listOf("message $female", "waitmessage", "return")
    extra += "${label}_male" to listOf("message $male", "waitmessage", "return")
  }

  /**
   * The way past a dropped question: jump to [goto], or call [call] and carry on, or just carry on.
   * [keepMessage]: the question's text still shows (as a statement) - it is dropped only when the
   * question itself offers the missing thing ("Give it a nickname?"), never when it confirms
   * something and only its NO would have re-opened it ("So Silver was his name?").
   */
  private data class SkippedQuestion(val goto: String?, val call: String?, val last: Int, val keepMessage: Boolean)

  /** A line that only exists for a naming screen or a system PokeMMO does not have (Mom's bank). */
  private fun isDroppedSystemLine(line: List<String>): Boolean =
      line[0] in NAMING_COMMANDS || line[0] in BANK_COMMANDS || (line[0] == "SetFlag" && line.getOrNull(1) == "FLAG_SYS_MOMS_SAVINGS")

  /** The lines from [from] up to the block's next jump or end reach a dropped-system line. */
  private fun inlineReachesDroppedSystem(lines: List<List<String>>, from: Int): Boolean {
    for (k in from until lines.size) {
      if (isDroppedSystemLine(lines[k])) return true
      if (lines[k][0] in TERMINAL) return false
    }
    return false
  }

  /**
   * The branch lines after a yes/no into [answerVar] (answers 0 = yes, 1 = no): Platinum
   * `GoToIfEq VAR, MENU_NO, L`, HeartGold `Compare VAR, 0` + `GoToIfNe L` / `CallIfEq L`, then an
   * optional `GoTo L`. When exactly one answer leads to a naming screen or Mom's bank
   * ([avoidLabels], or the inline lines that follow), the question is skipped and the script takes
   * the other answer; else null.
   */
  private fun skippedQuestion(
      lines: List<List<String>>,
      start: Int,
      answerVar: String?,
      avoidLabels: Set<String>,
      constants: Map<String, Int>,
      namingOfferLabels: Set<String> = emptySet(),
  ): SkippedQuestion? {
    if (answerVar == null) return null
    // answer -> (label, isCall)
    val branches = HashMap<Int, Pair<String, Boolean>>()
    var trailingGoto: String? = null
    var last = start - 1
    var compared: Int? = null
    var k = start
    while (k < lines.size && k < start + 8) {
      val l = lines[k]
      when {
        l[0] in QUESTION_FILLER -> {}
        (l[0] == "GoToIfEq" || l[0] == "GoToIfNe") && l.size >= 4 && l[1] == answerVar -> {
          val value = constants[l[2]] ?: l[2].toIntOrNull() ?: return null
          branches.putIfAbsent(if (l[0] == "GoToIfEq") value else 1 - value, l[3] to false)
          last = k
        }
        (l[0] == "Compare" || l[0] == "CompareVarToValue") && l.size >= 3 && l[1] == answerVar -> {
          compared = constants[l[2]] ?: l[2].toIntOrNull() ?: return null
        }
        l[0] in setOf("GoToIfEq", "GoToIfNe", "CallIfEq", "CallIfNe") && l.size == 2 && compared != null -> {
          val value = compared!!
          branches.putIfAbsent(if (l[0].endsWith("Eq")) value else 1 - value, l[1] to l[0].startsWith("Call"))
          compared = null
          last = k
        }
        l[0] == "GoTo" && l.size >= 2 && branches.isNotEmpty() -> {
          trailingGoto = l[1]
          last = k
          break
        }
        else -> break
      }
      k++
    }
    if (branches.isEmpty() || branches.keys.any { it !in 0..1 }) return null
    fun path(answer: Int): Pair<String?, Boolean> = branches[answer] ?: (trailingGoto to false)
    fun avoided(answer: Int): Boolean {
      val (label, isCall) = path(answer)
      return if (label == null) inlineReachesDroppedSystem(lines, last + 1)
      else label in avoidLabels || (isCall && inlineReachesDroppedSystem(lines, last + 1))
    }
    val take =
        when {
          avoided(0) && !avoided(1) -> 1
          avoided(1) && !avoided(0) -> 0
          else -> return null
        }
    // The YES answer opens a naming screen: the offer itself is dropped. Anything else keeps its text.
    val offersNaming = take == 1 && path(0).first.let { it != null && it in namingOfferLabels }
    val (label, isCall) = path(take)
    return if (isCall) SkippedQuestion(null, label, last, !offersNaming) else SkippedQuestion(label, null, last, !offersNaming)
  }

  /**
   * The `Switch VAR` / `Case n, L` rows / `GoTo exit` after a menu into [menuVar]: when a case leads
   * into Mom's bank ([avoidLabels]), the exit label and the index of its line; else null.
   */
  private fun droppedMenuExit(lines: List<List<String>>, start: Int, menuVar: String, avoidLabels: Set<String>): Pair<String, Int>? {
    var k = start
    while (k < lines.size && lines[k][0] in QUESTION_FILLER) k++
    if (lines.getOrNull(k)?.let { it[0] == "Switch" && it.getOrNull(1) == menuVar } != true) return null
    k++
    val cases = mutableListOf<String>()
    while (k < lines.size && lines[k][0] == "Case" && lines[k].size >= 3) {
      cases += lines[k][2]
      k++
    }
    val exit = lines.getOrNull(k)?.takeIf { it[0] == "GoTo" && it.size >= 2 } ?: return null
    return if (cases.any { it in avoidLabels }) exit[1] to k else null
  }

  /** Gen 4 commands write their result into a named var; the GBA ones into VAR_RESULT. */
  private fun resultCopy(out: MutableList<String>, target: String?) {
    if (target == null || target == "VAR_RESULT" || target == "VAR_SPECIAL_RESULT" || target == "VAR_0x800C") return
    out += "copyvar $target, VAR_RESULT"
  }

  private fun transpileMovement(lines: List<List<String>>): List<String> {
    val out = mutableListOf<String>()
    for (line in lines) {
      val name = line[0]
      val count = line.getOrNull(1)?.toIntOrNull() ?: 1
      if (name == "EndMovement") break
      val step = MOVEMENT_STEPS[name] ?: continue
      repeat(count.coerceIn(1, 32)) { out += step }
    }
    out += "step_end"
    return out
  }

  // ---------------------------------------------------------------- dialects

  private class Platinum(private val root: File) : Dialect {
    private val machineAliases by lazy { gen4MachineAliases(File(root.absoluteFile.parentFile, "pokeheartgold"), PLATINUM_HM_MOVES) }

    override fun itemAliases(): Map<String, String> = machineAliases

    override val region = 3
    private val json = Json { ignoreUnknownKeys = true }
    override val scriptFiles: List<File> =
        File(root, "res/field/scripts").listFiles { f -> f.extension == "s" }?.sortedBy { it.name }.orEmpty()
    private val headerOrder: List<String> = File(root, "generated/map_headers.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
    private val textBankOrder: Map<String, Int> =
        File(root, "generated/text_banks.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .withIndex().associate { (i, name) -> name to i }
    private val textIds: Map<String, Int> by lazy { loadTexts() }
    private val chunkOffsets: Map<String, Int> by lazy {
      Regex("#define\\s+SCRIPT_ID_OFFSET_(\\w+)\\s+(\\d+)").findAll(File(root, "include/script_manager.h").readText())
          .associate { it.groupValues[1] to it.groupValues[2].toInt() }
    }

    override fun mapHeaders(): List<MapHeader> {
      val text = File(root, "include/data/map_headers.h").readText()
      val entry = Regex("\\[(MAP_HEADER_\\w+)\\]\\s*=\\s*\\{([^}]*)}")
      return entry.findAll(text).mapNotNull { m ->
        val name = m.groupValues[1]
        val id = headerOrder.indexOf(name).takeIf { it >= 0 } ?: return@mapNotNull null
        val body = m.groupValues[2]
        val scripts = Regex("\\.scriptsArchiveID\\s*=\\s*(\\w+)").find(body)?.groupValues?.get(1)
        val events = Regex("\\.eventsArchiveID\\s*=\\s*(\\w+)").find(body)?.groupValues?.get(1)
        val init = Regex("\\.initScriptsArchiveID\\s*=\\s*(\\w+)").find(body)?.groupValues?.get(1)
        MapHeader(id, name, scripts?.let { "$it.s" }, events?.let { "$it.json" }, initFile = init?.let { File(root, "res/field/scripts/$it.s") })
      }.toList()
    }

    override fun objectIdsFor(header: MapHeader): Map<String, Int> {
      val file = header.eventsFile?.let { File(root, "res/field/events/$it") } ?: return emptyMap()
      if (!file.isFile) return emptyMap()
      val objects = json.parseToJsonElement(file.readText()).jsonObject["object_events"]?.jsonArray ?: return emptyMap()
      val out = linkedMapOf<String, Int>()
      objects.forEachIndexed { i, o -> o.jsonObject["id"]?.jsonPrimitive?.content?.let { out[it] = i } }
      return out
    }

    private fun loadTexts(): Map<String, Int> {
      val out = HashMap<String, Int>()
      File(root, "res/text").listFiles { f -> f.extension == "json" }?.forEach { file ->
        val bank = textBankOrder["TEXT_BANK_" + file.nameWithoutExtension.uppercase()] ?: return@forEach
        val messages = runCatching { json.parseToJsonElement(file.readText()).jsonObject["messages"]?.jsonArray }.getOrNull() ?: return@forEach
        messages.forEachIndexed { i, m ->
          val id = m.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEachIndexed
          out[id] = (region shl 28) or (bank shl 16) or i
        }
      }
      return out
    }

    override fun textId(token: String): Int? = textIds[token]

    /** src/overlay005/field_menu.c FieldMenu_GetFloorsAbove: `case MAP_HEADER_X: floorsAbove = n;`. */
    override fun elevatorFloors(): Map<String, Int> {
      val file = File(root, "src/overlay005/field_menu.c")
      if (!file.isFile) return emptyMap()
      val body = Regex("FieldMenu_GetFloorsAbove\\(int location\\)(.*?)\\n}", RegexOption.DOT_MATCHES_ALL).find(file.readText())?.groupValues?.get(1) ?: return emptyMap()
      return Regex("case (MAP_HEADER_\\w+):\\s*floorsAbove = (\\d+);").findAll(body).associate { it.groupValues[1] to it.groupValues[2].toInt() }
    }

    /** res/field/events/<map>.json warp_events with dest_header_id MAP_HEADER_DYNAMIC, as (x, z). */
    override fun dynamicExits(header: MapHeader): List<Pair<Int, Int>> {
      val file = header.eventsFile?.let { File(root, "res/field/events/$it") } ?: return emptyList()
      if (!file.isFile) return emptyList()
      val warps = runCatching { json.parseToJsonElement(file.readText()).jsonObject["warp_events"]?.jsonArray }.getOrNull() ?: return emptyList()
      return warps.mapNotNull { w ->
        val o = w.jsonObject
        if (o["dest_header_id"]?.jsonPrimitive?.content != "MAP_HEADER_DYNAMIC") return@mapNotNull null
        val x = o["x"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        val z = o["z"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        x to z
      }
    }

    override fun constants(): Map<String, Int> {
      val out = HashMap(ConstantsIndex.build(root))
      File(root, "generated").listFiles { f -> f.extension == "txt" }?.sortedBy { it.name }?.forEach { out += enumFile(it, out) }
      // C enums in include/constants (NPC_TRADE_*): the #define index skips them.
      File(root, "include/constants").listFiles { f -> f.extension == "h" }?.forEach { out += enumConstants(it.readText(), out) }
      return out
    }

    /** include/data/mart_items.h: named shelves and the PokeMartSpecialties index over them. */
    override fun martTables(): Map<Int, List<String>> {
      val text = File(root, "include/data/mart_items.h").readText()
      val shelves = Regex("const u16 (\\w+)\\[\\] = \\{([^}]*)\\}").findAll(text).associate { m ->
        m.groupValues[1] to Regex("ITEM_\\w+").findAll(m.groupValues[2]).map { it.value }.toList()
      }
      val ids = constants()
      return Regex("\\[(MART_SPECIALTIES_ID_\\w+)\\]\\s*=\\s*(\\w+)").findAll(text).mapNotNull { m ->
        val id = ids[m.groupValues[1]] ?: return@mapNotNull null
        val shelf = shelves[m.groupValues[2]] ?: return@mapNotNull null
        id to shelf
      }.toMap()
    }

    /** SINGLE_BATTLES (3000) and DOUBLE_BATTLES (5000) carry the trainer id in the script id. */
    override fun trainerChunkSize(base: Int): Int? =
        if (base == chunkOffsets["SINGLE_BATTLES"] || base == chunkOffsets["DOUBLE_BATTLES"])
            File(root, "generated/trainers.txt").readLines().count { it.isNotBlank() }
        else null

    /** The `Common_*` helpers in scrcmd.inc: plain command bodies, no byte directives. */
    override fun macros(): Map<String, Macro> {
      val out = HashMap<String, Macro>()
      var name: String? = null
      var params = emptyList<String>()
      val body = mutableListOf<String>()
      for (raw in File(root, "asm/macros/scrcmd.inc").readLines()) {
        val line = raw.trim()
        when {
          line.startsWith(".macro ") -> {
            val parts = line.removePrefix(".macro").trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
            name = parts.firstOrNull()
            params = parts.drop(1).map { it.substringBefore('=') }
            body.clear()
          }
          line == ".endm" -> {
            val n = name
            if (n != null && (n.startsWith("Common_") || n.startsWith("PokeMart")) && body.none { it.startsWith(".") }) out[n] = Macro(params, body.toList())
            name = null
          }
          name != null && line.isNotEmpty() -> body += line
        }
      }
      return out
    }

    override fun chunkFiles(): Map<Int, String> {
      val table = File(root, "src/script_manager.c").readText()
      val entry = Regex("Entry\\(SCRIPT_ID_OFFSET_(\\w+),\\s*(\\w+),")
      return entry.findAll(table).mapNotNull { m ->
        val base = chunkOffsets[m.groupValues[1]] ?: return@mapNotNull null
        base to "${m.groupValues[2]}.s"
      }.toMap()
    }
  }

  private class HeartGold(private val root: File) : Dialect {
    private val machineAliases by lazy { gen4MachineAliases(root) }

    override fun itemAliases(): Map<String, String> = machineAliases

    override val region = 4
    private val json = Json { ignoreUnknownKeys = true }
    private val scriptDir = File(root, "files/fielddata/script/scr_seq")
    override val scriptFiles: List<File> =
        scriptDir.listFiles { f -> f.extension == "s" && !f.name.endsWith("_hdr.s") }?.sortedBy { it.name }.orEmpty()
    private val constants: Map<String, Int> by lazy { ConstantsIndex.build(root) }

    override fun mapHeaders(): List<MapHeader> {
      val text = File(root, "src/data/map_headers.h").readText()
      val entry = Regex("\\[(MAP_\\w+)\\]\\s*=\\s*\\{([^}]*)}")
      return entry.findAll(text).mapNotNull { m ->
        val name = m.groupValues[1]
        val id = constants[name] ?: return@mapNotNull null
        val body = m.groupValues[2]
        val scripts = Regex("\\.scriptsBank\\s*=\\s*NARC_scr_seq_(\\w+)_bin").find(body)?.groupValues?.get(1)
        val events = Regex("\\.eventsBank\\s*=\\s*NARC_zone_event_(\\w+)_bin").find(body)?.groupValues?.get(1)
        val msg = Regex("\\.msgBank\\s*=\\s*NARC_msg_msg_(\\d+)").find(body)?.groupValues?.get(1)?.toInt()
        val hdr = Regex("\\.scriptHeaderBank\\s*=\\s*NARC_scr_seq_(\\w+)_bin").find(body)?.groupValues?.get(1)
        MapHeader(id, name, scripts?.let { "$it.s" }, events?.let { "$it.json" }, msg, hdr?.let { File(scriptDir, "$it.s") })
      }.toList()
    }

    override fun objectIdsFor(header: MapHeader): Map<String, Int> {
      // The event header (`event_T01.h`) defines `obj_T01_gswoman1 0`; the json names it.
      val file = header.eventsFile?.let { File(root, "files/fielddata/eventdata/zone_event/$it") } ?: return emptyMap()
      if (!file.isFile) return emptyMap()
      val headerName = json.parseToJsonElement(file.readText()).jsonObject["header"]?.jsonPrimitive?.content ?: return emptyMap()
      val h = File(root, "files/$headerName")
      if (!h.isFile) return emptyMap()
      val define = Regex("#define\\s+(obj_\\w+)\\s+(\\d+)")
      return define.findAll(h.readText()).associate { it.groupValues[1] to it.groupValues[2].toInt() }
    }

    override fun textId(token: String): Int? {
      val m = MSG_TOKEN.matchEntire(token) ?: return null
      return (region shl 28) or (m.groupValues[1].toInt() shl 16) or m.groupValues[2].toInt()
    }

    /**
     * MapNumToFloorNo, read off asm/overlay_01_021EDAFC.s (0x021EE81C): the jump table and the
     * compare chains land on `mov r0, #n` returns. Every other map asserts and answers 0.
     */
    override fun elevatorFloors(): Map<String, Int> =
        mapOf(
            "MAP_GOLDENROD_RADIO_TOWER_5F" to 0, "MAP_GOLDENROD_RADIO_TOWER_OBSERVATION_DECK" to 1,
            "MAP_GOLDENROD_DEPARTMENT_STORE_BASEMENT" to 0, "MAP_GOLDENROD_DEPARTMENT_STORE_1F" to 1,
            "MAP_GOLDENROD_DEPARTMENT_STORE_2F" to 2, "MAP_GOLDENROD_DEPARTMENT_STORE_3F" to 3,
            "MAP_GOLDENROD_DEPARTMENT_STORE_4F" to 4, "MAP_GOLDENROD_DEPARTMENT_STORE_5F" to 5,
            "MAP_GOLDENROD_DEPARTMENT_STORE_6F" to 6,
            "MAP_OLIVINE_LIGHTHOUSE_1F" to 0, "MAP_OLIVINE_LIGHTHOUSE_LIGHT_ROOM" to 1,
            "MAP_CELADON_DEPARTMENT_STORE_1F" to 0, "MAP_CELADON_DEPARTMENT_STORE_2F" to 1,
            "MAP_CELADON_DEPARTMENT_STORE_3F" to 2, "MAP_CELADON_DEPARTMENT_STORE_4F" to 3,
            "MAP_CELADON_DEPARTMENT_STORE_5F" to 4, "MAP_CELADON_DEPARTMENT_STORE_ROOF" to 5,
            "MAP_CELADON_CONDOMINIUMS_1F" to 0, "MAP_CELADON_CONDOMINIUMS_2F" to 1,
            "MAP_CELADON_CONDOMINIUMS_3F" to 2, "MAP_CELADON_CONDOMINIUMS_ROOF" to 3,
            "MAP_SAFFRON_SILPH_CO_HQ" to 0, "MAP_SAFFRON_SILPH_CO_ROTOM_ROOM" to 1,
        )

    /** files/fielddata/eventdata/zone_event/<map>.json warps with header 4095 (dynamic), as (x, z). */
    override fun dynamicExits(header: MapHeader): List<Pair<Int, Int>> {
      val file = header.eventsFile?.let { File(root, "files/fielddata/eventdata/zone_event/$it") } ?: return emptyList()
      if (!file.isFile) return emptyList()
      val warps = runCatching { json.parseToJsonElement(file.readText()).jsonObject["warps"]?.jsonArray }.getOrNull() ?: return emptyList()
      return warps.mapNotNull { w ->
        val o = w.jsonObject
        if (o["header"]?.jsonPrimitive?.content?.toIntOrNull() != HEARTGOLD_DYNAMIC_HEADER) return@mapNotNull null
        val x = o["x"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        val z = o["z"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@mapNotNull null
        x to z
      }
    }

    /**
     * Script ids from 2000 up live in shared chunks: `_std_*` thresholds (include/constants/
     * std_script.h) name the base id and src/script_manager.c the scr_seq file; the entry index
     * is id minus base, the same NDS_CHUNK_<id> scheme Platinum uses.
     */
    override fun chunkFiles(): Map<Int, String> {
      val bases = Regex("#define\\s+(_std_\\w+)\\s+(\\d+)").findAll(File(root, "include/constants/std_script.h").readText())
          .associate { it.groupValues[1] to it.groupValues[2].toInt() }
      val table = File(root, "src/script_manager.c").readText()
      val row = Regex("\\{\\s*(\\w+),\\s*NARC_scr_seq_(scr_seq_\\d+)_bin")
      val files = scriptDir.listFiles()?.map { it.name }.orEmpty()
      return row.findAll(table).mapNotNull { m ->
        val base = bases[m.groupValues[1]] ?: m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val prefix = m.groupValues[2]
        val file = files.firstOrNull { it == "$prefix.s" || (it.startsWith(prefix + "_") && it.endsWith(".s") && !it.endsWith("_hdr.s")) } ?: return@mapNotNull null
        base to file
      }.toMap()
    }

    /** src/scrcmd_mart.c: the anonymous shelves and the _0210FA3C index SpecialMartBuy reads. */
    override fun martTables(): Map<Int, List<String>> {
      val text = File(root, "src/scrcmd_mart.c").readText()
      val shelves = Regex("const u16 (_[0-9A-F]+)\\[\\] = \\{([^}]*)\\}").findAll(text).associate { m ->
        m.groupValues[1] to Regex("ITEM_\\w+").findAll(m.groupValues[2]).map { it.value }.toList()
      }
      val order = Regex("const u16 \\*_0210FA3C\\[\\] = \\{([^}]*)\\}").find(text)?.groupValues?.get(1) ?: return emptyMap()
      return Regex("_[0-9A-F]{8}").findAll(order).map { it.value }.withIndex().mapNotNull { (i, name) -> shelves[name]?.let { i to it } }.toMap()
    }

    override fun constants(): Map<String, Int> = constants
  }

  /**
   * Unova has no decomp; the ROM's script archive (/a/0/5/7) is disassembled by tools/nds/Dis5
   * into `nds-scripts-2.txt` (`file;entry;offset;Name args`, `mv;file;offset;type,len ...`) and
   * the map headers into `nds-headers-2.txt`. The Gen 5 engine is a small stack machine
   * (SetStackVar / SetStackDerefVar / StoreFlag push, Condition op, When skips unless true) which
   * this folds into the Gen 4 Compare/GoToIf vocabulary the transpiler already handles. Map text
   * is the header's bank in the second text archive, which the client's game bit selects.
   */
  private class Unova(private val root: File) : Dialect {
    override val region = 2
    /**
     * The client's own item names by Gen 5 index (decomp/pokeblack/item_names.json, the table the
     * item catalogue is generated from), so a script's numeric item becomes the ITEM_ constant of
     * the item the client already has - never a new item. Same mangling as ItemRegistry.
     */
    private val itemNames: List<String> by lazy {
      val json = File(root.parentFile, "decomp/pokeblack/item_names.json").readText()
      Regex("\"([^\"]*)\"").findAll(json).map { it.groupValues[1] }.toList()
    }
    fun itemConstant(index: Int): String? =
        itemNames.getOrNull(index)?.takeIf { it.isNotEmpty() && it != "None" }?.let { name ->
          "ITEM_" +
              java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                  .replace(Regex("\\p{Mn}+"), "")
                  .uppercase(java.util.Locale.ROOT)
                  .replace(Regex("[^A-Z0-9]+"), "_")
                  .trim('_')
        }
    override val scriptFiles: List<File> = emptyList()
    private val headerRows: List<IntArray> by lazy {
      File(root, "nds-headers-2.txt").readLines().filter { it.startsWith("hdr;") }.map { l -> l.split(';').drop(1).map { it.toInt() }.toIntArray() }
    }
    /** script file -> text bank of the first header using it. */
    private val bankByFile: Map<Int, Int> by lazy {
      val out = HashMap<Int, Int>()
      for (h in headerRows) out.putIfAbsent(h[4], h[6])
      // The shared routines belong to no header: CallStd's file 862 (2805 bag check, 2811 obtain
      // item) and the item balls' file 864 speak from story bank 283 - "{0} obtained {1}!" (0/1/3),
      // "found" (4-6), "no more room" (7/8), "put the {1} in the {2} Case" (10/11), the entries
      // they index. Left at bank 0 every item gift ended in the gourmet maid's and the ore
      // collector's lines (Mom's Xtransceiver, 2026-09-19).
      out.putIfAbsent(862, 283)
      out.putIfAbsent(864, 283)
      // The Pokemon Center nurse, file 855, speaks from bank 346 - the bank the hardcoded stand-in
      // already used and the owner already play-tested (0 greeting, 6 "I will take your Pokemon",
      // 7 restored, 8 the send-off), and the script asks for entry 6 at the counter. Left at bank 0
      // she came out as the GOURMET MAID, sniffing and asking for an ingredient - the same failure
      // as the item gifts above, one file later (owner, 2026-09-21).
      out.putIfAbsent(855, 346)
      out
    }

    override fun mapHeaders(): List<MapHeader> =
        headerRows.map { h -> MapHeader(h[3], "unova_${h[3]}", "U${h[4]}", null, h[6]) }

    override fun objectIdsFor(header: MapHeader): Map<String, Int> {
      val out = HashMap<String, Int>()
      val prefix = "obj;2;${header.id and 0xFF};${header.id shr 8};"
      File(root, "nds-npcs-2.txt").forEachLine { l ->
        if (l.startsWith(prefix)) {
          val p = l.split(';')
          out["OBJ_${p[5]}"] = p[4].toInt()
        }
      }
      // Ids a script creates with MakeNPC exist on no map's event list, and the one that makes an
      // actor is often not the one that moves it (Nuvema's file 778 makes 240 in its exit trigger,
      // then entry 14 and Route 1's own script walk it). Declared here so those movements resolve:
      // the actor's definition arrives at runtime (ScriptMovementService.makeNdsNpc).
      for (id in SCRIPT_ACTOR_IDS) out.putIfAbsent("OBJ_$id", id)
      return out
    }

    override fun textId(token: String): Int? {
      val m = Regex("^T(\\d+)_(\\d+)$").matchEntire(token) ?: return null
      return (region shl 28) or (1 shl 27) or (m.groupValues[1].toInt() shl 16) or m.groupValues[2].toInt()
    }

    override fun constants(): Map<String, Int> = emptyMap()

    /**
     * Script ids outside map files: 7000+ item balls (file 864: 306 entries for ids 7000-7305, all
     * on sprite 110), 2800+ the standard routines CallStd names (file 862: 2805 bag-space check,
     * 2811 obtain item), 3001-5449 trainers. Id 2000 EXACTLY is
     * the "no talk script" placeholder 355 story actors carry (Mom outside the lab, Cheren, gym
     * props): bound to file 864 it made Mom run item ball 0 - "raket received raket" - while the
     * real item balls fell into the trainer range (2026-09-20).
     */
    override fun chunkFiles(): Map<Int, String> = mapOf(2800 to "U862", 3000 to "UTR", 7000 to "U864")

    /**
     * 10000+ is the engine's own standard-script space, NOT file 865: 865 is the hidden-item file
     * (entry N = give item, set flag), it stops at entry 176, and the band bound to it handed the
     * Pokemon Center counter npcs (10100, 10105 on all fourteen Centers) a one-off item each and
     * made the 55 `CallStd 10110` sites give item 88. Hidden items are bg events, which are not
     * extracted at all yet, so nothing wants that binding today.
     */
    override fun unboundStdCalls(): IntRange = 10000..19999

    /**
     * The Pokemon Center nurse, script id 2100 on all fourteen of them (sprite 78). Her script is
     * file 855 entry 0 - the one with HealPokemon - and it is the ONLY thing that sets VAR 16507 to
     * 2, which is the Accumula tour hand-off, so the hardcoded stand-in soft-locked the story at the
     * first heal (owner, 2026-09-21).
     *
     * The mart (2101) is deliberately NOT bound yet: it is a different file, 856, the one with
     * MoneyBox, and which of its two entries the id wants is unconfirmed. The hardcoded clerk still
     * serves it.
     */
    override fun soloChunks(): Map<Int, Pair<String, Int>> = mapOf(2100 to ("U855" to 0))

    /** Trainer npcs carry script 3000 + trainer id; one synthetic battle script serves them all. */
    override fun trainerChunkSize(base: Int): Int? = if (base == 3000) 616 else null

    private class Cmd(val entry: Int, val offset: Int, val name: String, val args: List<String>)

    override fun preparsed(): Map<String, ParsedFile> {
      val lines = File(root, "nds-scripts-2.txt").readLines()
      val cmdsByFile = HashMap<Int, TreeMap<Int, Cmd>>()
      val entriesByFile = HashMap<Int, HashMap<Int, Int>>()
      val movesByFile = HashMap<Int, HashMap<Int, List<String>>>()
      for (l in lines) {
        val p = l.split(';')
        if (p[0] == "mv") {
          val steps = p[3].trim().split(' ').filter { it.isNotEmpty() }
          movesByFile.getOrPut(p[1].toInt()) { HashMap() }[p[2].toInt()] = steps
          continue
        }
        if (p.size < 4) continue
        val file = p[0].toInt()
        val entry = p[1].toInt()
        val off = p[2].toInt()
        val parts = p[3].trim().split(' ')
        cmdsByFile.getOrPut(file) { TreeMap() }[off] = Cmd(entry, off, parts[0], parts.drop(1))
      }
      // Entry offsets: the lowest offset seen per entry index (entries decode from their start).
      for ((file, cmds) in cmdsByFile) for (c in cmds.values) {
        val e = entriesByFile.getOrPut(file) { HashMap() }
        e[c.entry] = minOf(e[c.entry] ?: Int.MAX_VALUE, c.offset)
      }
      val out = LinkedHashMap<String, ParsedFile>()
      for ((file, cmds) in cmdsByFile) {
        val bank = bankByFile[file] ?: 0
        fun lab(off: Int) = "U${file}_$off"
        val targets = HashSet<Int>()
        entriesByFile[file]?.values?.forEach { targets += it }
        for (c in cmds.values) for (a in c.args) if (a.startsWith("@")) targets += a.drop(1).toInt()
        val blocks = mutableListOf<Block>()
        var current: Block? = null
        val stack = ArrayDeque<String>()
        var lastOp = 1
        // Multi x, y, ?, ?, cancel, var opens a choice list; each SetTextScriptMessage text, 0xFFFF,
        // value is one row; CloseMulti shows it. Emitted as one ds_menu, like HeartGold's MenuExec.
        var menuVar: String? = null
        val menuItems = mutableListOf<Pair<String, String>>()
        // The starter-select app's result var, from its open (0xB4 331 339 var) to its close (CMD_1AF).
        var starterVar: String? = null
        // Door objects a script made (CMD_127 handle var -> its tile), until it frees them (CMD_128).
        val doors = HashMap<String, Pair<String, String>>()
        val list = cmds.values.toList()
        for ((ci, c) in list.withIndex()) {
          // A command whose last (var) argument the table lacks leaves it decoded as a bare
          // CMD_80xx "opcode" right after it (0x8010 = VAR_0x8010); the real command reads it here.
          val trailingVar = list.getOrNull(ci + 1)?.name?.takeIf { it.matches(Regex("CMD_80[0-9A-F]{2}")) }?.let { "VAR_0x" + it.removePrefix("CMD_") }
          if (c.offset in targets || current == null) {
            // A block that runs into the next label falls through on the ROM; the transpiler ends
            // every block, so it gets the explicit goto the decomp parser adds (parseScriptFile).
            // Without it the gift box's Oshawott branch (file 782 @700 -> @723) ended the script
            // before the mon was given (2026-09-19).
            current?.let { prev ->
              val last = prev.lines.lastOrNull()?.firstOrNull()
              if (last !in TERMINAL) prev.lines += listOf("GoTo", lab(c.offset))
            }
            current = Block(lab(c.offset), false, mutableListOf()).also { blocks += it }
            stack.clear()
          }
          val b = current
          val a = c.args
          fun t(i: Int) = a.getOrElse(i) { "0" }
          fun v(i: Int) = "VAR_0x" + t(i).toInt().toString(16).uppercase()
          // Mom's Running Shoes scene on Route 2 (file 638 entry 7: her yes/no, then SetFlag 16)
          // is the only place in the whole game that touches ROM flag 16, so it IS the running
          // flag. Named here, it rides the same synthetic key the decomp regions use and the
          // server mirrors to the client id the client's own Yb0 gate reads.
          fun fl(i: Int) = "FLAG_" + t(i)
          fun isVarArg(token: String) = (token.toIntOrNull() ?: 0) >= 0x4000
          /** A value-or-var argument: Gen 5 passes vars (0x4000+) where a value is expected. */
          fun tv(i: Int) = if ((t(i).toIntOrNull() ?: 0) >= 0x4000) v(i) else t(i)
          /** An item argument: a var stays a var, a Gen 5 index becomes the client item's ITEM_ constant. */
          fun item(i: Int) = if ((t(i).toIntOrNull() ?: 0) >= 0x4000) v(i) else itemConstant(t(i).toInt()) ?: t(i)
          fun jump(i: Int) = lab(t(i).drop(1).toInt())
          fun text(i: Int) = "T%04d_%05d".format(bank, t(i).toInt())
          fun cond(code: Int, negate: Boolean): String {
            val names = listOf("Lt", "Eq", "Gt", "Le", "Ge", "Ne")
            val neg = listOf(4, 5, 3, 2, 0, 1)
            val k = code.coerceIn(0, 5)
            return names[if (negate) neg[k] else k]
          }
          // The bare var "opcodes" (see trailingVar) belong to the command before them.
          if (c.name.matches(Regex("CMD_80[0-9A-F]{2}"))) continue
          when (c.name) {
            "SetStackVar" -> stack.addLast(t(0))
            "SetStackDerefVar" -> stack.addLast(v(0))
            "StoreFlag" -> stack.addLast(fl(0))
            "Condition" -> {
              lastOp = t(0).toInt()
              val rhs = stack.removeLastOrNull() ?: "0"
              val lhs = stack.removeLastOrNull() ?: "0"
              when {
                lhs.startsWith("FLAG_") -> {
                  b.lines += listOf("CheckFlagVar", lhs, "VAR_RESULT")
                  b.lines += listOf("Compare", "VAR_RESULT", rhs)
                }
                lhs.startsWith("VAR_") -> b.lines += listOf("Compare", lhs, rhs)
                rhs.startsWith("VAR_") -> {
                  b.lines += listOf("Compare", rhs, lhs)
                  lastOp = listOf(2, 1, 0, 4, 3, 5).getOrElse(lastOp) { lastOp }
                }
                else -> {
                  b.lines += listOf("SetVar", "VAR_RESULT", lhs)
                  b.lines += listOf("Compare", "VAR_RESULT", rhs)
                }
              }
              // 7 negates the condition just computed; 6 combines with the previous one (kept as is).
              if (t(0).toInt() == 7) lastOp = listOf(4, 5, 3, 2, 0, 1).getOrElse(lastOp) { lastOp }
              else if (t(0).toInt() > 7) b.lines += listOf("Ds5Condition", t(0))
            }
            "Compare" -> {
              b.lines += listOf("Compare", v(0), tv(1))
              lastOp = -1
            }
            "When", "If" -> {
              val verb = if (c.name == "When") "GoToIf" else "CallIf"
              val k = t(0).toInt()
              val name = if (lastOp >= 0) cond(lastOp, negate = k == 255) else cond(k, negate = false)
              b.lines += listOf(verb + name, jump(1))
            }
            "GetStackVar" -> b.lines += listOf("SetVar", v(0), stack.removeLastOrNull() ?: "0")
            "PopStack", "AddStackVar" -> {}
            "End" -> b.lines += listOf("End")
            "EndRoutine", "ReturnStd" -> b.lines += listOf("Return")
            // CheckItemBagNumber item, count, result var (the table had two args until 2026-09-20;
            // the var then rode along as a bare CMD_80xx).
            "CheckItemBagNumber" -> b.lines += listOf("GetItemQuantity", item(0), v(2))
            "Multi" -> {
              menuVar = v(5)
              menuItems.clear()
            }
            "SetTextScriptMessage" -> if (menuVar != null) menuItems += (textId(text(0))?.toString() ?: "0") to t(2)
            "CloseMulti" -> {
              val target = menuVar
              if (target != null && menuItems.isNotEmpty()) b.lines += listOf("Menu", target, "0") + menuItems.flatMap { listOf(it.first, it.second) }
              else if (target != null) b.lines += listOf("SetVar", target, "127")
              menuVar = null
              menuItems.clear()
            }
            // The trainer is marked fought so the sight line no longer triggers (same flag the
            // battle sets on a win).
            "DeactivateTrainerID" -> b.lines += listOf("SetTrainerFlag", tv(0))
            // Scripted wild battle: CMD_178 species, level, flagsVar; CMD_17B var = 1 when won;
            // CMD_17C var = the outcome code; CMD_179 / CMD_17A = the won / not-won epilogues.
            // Rides Platinum's StartLegendaryBattle so the owner's story-legendary rule applies.
            "CMD_178" -> b.lines += listOf("StartLegendaryBattle", tv(0), tv(1))
            "CMD_17B" -> b.lines += listOf("WildOutcome", v(0), "won")
            "CMD_17C" -> b.lines += listOf("WildOutcome", v(0), "outcome")
            "CMD_179", "CMD_17A" -> {}
            // Whole-game pass: flag/trainer queries and warps on existing commands; unknown
            // stores answer zero; date, music, money box and the musical/party-poke buffers vanish.
            "SetVarFlagStatus" -> b.lines += listOf("CheckFlagVar", fl(0), v(1))
            "StoreActiveTrainerID" -> b.lines += listOf("CheckTrainerFlag", tv(0), v(1))
            "TeleportWarpNPC" -> b.lines += listOf("Warp", t(0), t(1), t(2), t(3))
            // DoubleTrainerBattle ally, opponent, opponent, ?: no doubles engine yet, the first opponent fights.
            // partner, enemy1, enemy2: the player faces BOTH enemies at once (Wellspring Cave with
            // Cheren: 56 = Cheren, 62 and 63 = the grunts). It used to take tv(1) alone as a single.
            "DoubleTrainerBattle" -> b.lines += listOf("DsDoubleTrainerBattle", tv(0), tv(1), tv(2))
            // Opcode 0x103 (sized to two halfwords 2026-09-23 from the binary's command table and
            // its 31 uses): VAR, mode - the party count into VAR. Mode 0 every slot, 2 the usable
            // ones (the Route 3 twins want two, the cave heals you when none stand); 1 and 4 seen
            // only in the unbound std files.
            "StorePartyCount" -> b.lines += listOf("DsStorePartyCount", tv(0), t(1))
            "StoreVar_CD", "StoreVar_CE", "Unknown_0D", "Unknown_0E", "Unknown_12", "Unknown_16" ->
                b.lines += listOf("SetVar", v(0), "0")
            // The nurse compares the date against the player's birthday. Both used to stub to 0, which
            // made them EQUAL - she would have wished the owner happy birthday on every single heal.
            // The date is real (WorldClock, the owner's game clock); the birthday answers 0, and a
            // real month is 1-12, so the greeting simply never fires until birthdays exist.
            "StoreDate" -> b.lines += listOf("StoreDate2", v(0), v(1))
            "StoreBirthDay" -> b.lines += listOf("StoreBirthDay2", v(0), v(1))
            "MoneyBox", "MusicalMessage", "PlayTrainerMusic" -> {}
            // Nickname prompts are never asked in story (owner's rule): declined, answer 0.
            // Dis5 could not read on from here (an opcode the table lacks; 85 entries): the script
            // ENDS. A block without a terminator gets a fall-through goto into the next label, and
            // for a truncated entry that label is a stranger's script - leaving Nuvema ran on into
            // the post-game scenes filed after it (Looker, Cedric Juniper's National Dex, Black
            // City, Cheren and the Seven Sages; 2026-09-20).
            "DecodeStopped" -> b.lines += listOf("End")
            "RenamePokemon" -> b.lines += listOf("SetVar", v(0), "0")
            // Opcode 0x110 (table name "StorePokemonSex"): result, party slot, screen - the naming
            // app Juniper opens in her lab (after CMD_1AD; the script branches on result == 1).
            "StorePokemonSex" -> b.lines += listOf("SetVar", v(0), "0")
            // Presentation and engine calls with no server counterpart on the opening route
            // (contexts read in the corpus 2026-09-19): item-obtained fanfare/pocket (CMD_240),
            // type-name text buffer (SetVarType; DS buffers are all no-ops for now), relocator,
            // camera/screen effects, the Interpoke/PC, save prompts, badge case, and the like.
            "CMD_240", "CMD_21C", "ActivateRelocator", "CMD_02D", "CMD_0D8", "CMD_0DA",
            "CMD_0FF", "CMD_208", "CMD_24F", "CMD_250", "CMD_252", "CMD_A3", "CMD_A5", "GetDerefVar07", "CMD_01B",
            "CMD_1B2", "CMD_1D1", "CMD_23A", "CMD_25F", "CMD_6F", "DVar92", "Unknown_13",
            "CMD_15A", "CMD_13C", "CMD_11F", "CMD_13A", "CMD_137", "CMD_1DE", "CMD_01A" -> {}
            "CMD_146", "CMD_400", "CMD_190", "CMD_78", "CMD_1B5", "CMD_9F", "CMD_220",
            "CMD_1F0", "CMD_24C", "GetDerefVar06", "CMD_1A8", "CMD_144", "CMD_248" -> {}
            // The map's own gimmick handler: the client picks an f/vc0 subclass by ROM header at
            // map load (Striaton Gym 7 -> f/iu5) and s2c 0xB6 action 6 hands it [opcode, args] -
            // the ROM's own opcodes. 0x188 k opens curtain k, 0x187/0x189 work its switches,
            // 0x186 restores the open ones. Stubbed out, the gym curtains never moved.
            // Mom's Route 2 scene (file 638 entry 0) hands over the Running Shoes: her fanfare,
            // the grey "received" box, then this. The client gates running on ITS flag 2403, which
            // no script in the game sets - the engine does it here. Bound to this one script, not
            // to the opcode everywhere: its only other use is in the opening chunk, which would
            // hand the shoes over before she ever speaks (owner, 2026-09-21).
            "CMD_E3" -> if (file == 638) b.lines += listOf("SetFlag", "FLAG_DS_RUNNING_SHOES")
            "CMD_186" -> b.lines += listOf("DsMapGimmick", "390", tv(0))
            "CMD_187" -> b.lines += listOf("DsMapGimmick", "391", tv(0))
            "CMD_188" -> b.lines += listOf("DsMapGimmick", "392", tv(0))
            "CMD_189" -> b.lines += listOf("DsMapGimmick", "393", tv(0))
            "SetVarItem", "SetVarItem2" -> b.lines += listOf("BufferItemName", t(0), item(1))
            // Text slots that were no-ops - and an unfilled slot shows the PLAYER'S name on the
            // client ("What are you and raket going to do?" for the starter's name, 2026-09-20):
            // a party monster's nickname / species, a move, a type (Gen 5 type numbers).
            "SetVarPartyPokemonNick" -> b.lines += listOf("BufferUnovaText", t(0), "partynick", tv(1))
            "SetVarPartyPoke" -> b.lines += listOf("BufferUnovaText", t(0), "partyspecies", tv(1))
            "SetVarMove" -> b.lines += listOf("BufferUnovaText", t(0), "move", tv(1))
            "SetVarType" -> b.lines += listOf("BufferUnovaText", t(0), "type", tv(1))
            // The shared obtain-item routine (file 862, read 2026-09-19 after "raket received the
            // raket ... put the raket in the raket Case"): CMD_4E slot, item, count, flag is the
            // item name of messages 0/5/10/11; CMD_BB item -> var is the item's POCKET (the routine
            // branches on 2 TMs & HMs / 4 Key Items, the ROM item table's own numbers) and
            // SetVarBag slot, pocket var names it; SetVarItem3 slot, item is a TM's move name;
            // opcode 0xB5 ("Screen_B5") item, count, result is the add-item itself.
            // The scripted door: CMD_127 handle, kind, x, y makes a map object (kind 1 = a door, 21 of
            // 29 uses; 5/6/8 are other effects and stay silent), CMD_129 handle, 0|1 opens / closes
            // it, CMD_12A waits for the animation, CMD_128 frees it. The client plays it through
            // s2c 0x1F with the same 0 = open / 1 = close (its own door code: kind 0, arg 0 / 1) -
            // probe-verified on Juniper's lab door 2026-09-20. Until now all four were no-ops, so
            // Cheren and Bianca walked into a shut lab and Cilan into a shut gym.
            "CMD_127" -> if (t(1) == "1") doors[v(0)] = tv(2) to tv(3)
            "CMD_129" -> doors[v(0)]?.let { (x, y) -> b.lines += listOf("DsDoor", t(1), x, y) }
            "CMD_12A" -> if (v(0) in doors) b.lines += listOf("WaitTime", "20")
            // CMD_103 var: the PARTY COUNT. At the end of Route 1 Bianca reads it and branches on
            // `<= 1`; as a no-op the var stayed 0, so she always took the "only your starter" line
            // however many the owner was carrying (2026-09-20).
            "CMD_103" -> b.lines += listOf("GetPartySize", v(0))
            // The Xtransceiver call: the client has the window and reads the call from the ROM.
            "OpenInterpoke" -> b.lines += listOf("DsXtransceiver", tv(0))
            "CMD_4E" -> b.lines += listOf("BufferItemName", t(0), item(1))
            "SetVarItem3" -> b.lines += listOf("BufferUnovaTmMove", t(0), item(1))
            "SetVarBag" -> b.lines += listOf("BufferUnovaPocket", t(0), tv(1))
            "CMD_BB" -> b.lines += listOf("UnovaItemPocket", v(1), item(0))
            "Screen_B5" -> b.lines += listOf("GiveItem", item(0), tv(1), v(2))
            "CloseShowMessageAt" -> b.lines += listOf("CloseMessage")
            "CMD_6A", "CMD_19F" -> {}
            "CMD_BA" -> b.lines += listOf("CheckItem", v(0), v(1), v(3))
            // ShowMessageAt text, OBJECT, position, window: a bubble over one actor, closed by
            // window id (CloseShowMessageAt). Speaker-less it inherited the last Message's
            // speaker, so Accumula's crowd chatter came out of Ghetsis (owner, 2026-09-21).
            "ShowMessageAt" -> {
              val who = t(1).toIntOrNull() ?: 0
              b.lines += listOf("DsSpeaker", if (who >= 0x4000) v(1) else if (who < 252) "OBJ_$who" else "none")
              b.lines += listOf("Message", text(0))
            }
            "SetBadge" -> b.lines += listOf("GiveBadge", t(0))
            "CMD_128" -> doors.remove(v(0))
            "CMD_11E", "CMD_107", "Xtransciever4", "Xtransciever5", "Xtransciever7" -> {}
            "SetVarStoreValue5C" -> b.lines += listOf("BufferNumber", t(0), v(1))
            "CallRoutine" -> b.lines += listOf("Call", jump(0))
            "Jump" -> b.lines += listOf("GoTo", jump(0))
            "ReturnAfterDelay" -> b.lines += listOf("WaitTime", t(0))
            // A flag id held in a VAR is the shared routines' way of naming the actor's own flag
            // (the item ball sets its own before removing itself). Written out as FLAG_32784 it
            // was a flag that does not exist; the removal persists the right one by itself.
            "SetFlag" -> if (!isVarArg(t(0))) b.lines += listOf("SetFlag", fl(0))
            "ClearFlag" -> if (!isVarArg(t(0))) b.lines += listOf("ClearFlag", fl(0))
            "StoreValueInVar" -> b.lines += listOf("SetVar", v(0), t(1))
            // The second argument is a var OR a literal (StoreDerefVarInVar VAR_0x8000, 504 puts the
            // item index in the var); read as a var it pointed at nothing and hidden items gave item 0.
            "StoreVarInVar", "StoreDerefVarInVar" -> b.lines += if ((t(1).toIntOrNull() ?: 0) >= 0x4000) listOf("CopyVar", v(0), v(1)) else listOf("SetVar", v(0), t(1))
            "AddVars" -> b.lines += listOf("AddVar", v(0), t(1))
            "SubVars" -> b.lines += listOf("SubVar", v(0), t(1))
            "LockAll" -> b.lines += listOf("LockAll")
            "ReleaseAll" -> b.lines += listOf("ReleaseAll")
            "WaitButton" -> b.lines += listOf("WaitButton")
            "FacePlayer" -> b.lines += listOf("FacePlayer")
            // Message 0, 4, entry, SPEAKER, position, type: the fourth argument is the object whose
            // line it is (bedroom scene: 0 Cheren, 1 Bianca; a var holds one too). Dropped, every
            // scene line went out speaker-less and the client hung the box on the player
            // (2026-09-20). Message2 is the talked-to npc's own line - its speaker is the script's.
            "Message" -> {
              val who = t(3).toIntOrNull() ?: 0
              b.lines += listOf("DsSpeaker", if (who >= 0x4000) v(3) else if (who < 252) "OBJ_$who" else "none")
              b.lines += listOf("Message", text(2))
            }
            "Message2", "Message3" -> b.lines += listOf("Message", text(2))
            "BubbleMessage", "EventGreyMessage", "BorderedMessage", "AngryMessage" -> b.lines += listOf("Message", text(0))
            "CloseMessageKP", "CloseMessageKP2", "CloseEventGreyMessage", "CloseBorderedMessage", "CloseAngryMessage", "CloseMusicalMessage" -> b.lines += listOf("CloseMessage")
            "YesNoBox" -> b.lines += listOf("YesNo", v(0))
            // StoreBadge VAR, badge - the var it answers into comes FIRST (every use in the game
            // reads `StoreBadge 32776 0`, and 32776 is the 0x8000 var band). Passed through in
            // ROM order these were swapped, so the answer went into VAR_0x0 and the gym scripts
            // branched on whatever 32776 still held: Striaton greeted the owner as though he had
            // already won the badge (2026-09-21).
            "StoreBadge" -> b.lines += listOf("CheckBadge", t(1), v(0))
            // The game version, which White's map-load scripts branch on to pick the version's
            // sprite for a var-drawn object (var 0x4020: the museum's stone is 144 in White, 143 in
            // Black); answered 0, every branch took Black's side (2026-09-23).
            "StoreVersion" -> b.lines += listOf("DsGameVersion", v(0))
            "Store_D2" -> b.lines += listOf("SetVar", v(0), "0")
            "DoubleMessage" -> b.lines += listOf("Message", text(3))
            "CloseBubbleMessage" -> b.lines += listOf("CloseMessage")
            // SetVarPoke slot, species (a literal or a var): the species name in a text slot.
            "SetVarPoke" -> b.lines += listOf("BufferSpeciesName", t(0), tv(1))
            // Opcode 0xB4 (table name "ResetScreen") is a function-call family: (40, var, value)
            // stores, (260, 169, 1300) sits between a fade and WaitFanfare. Nuvema's gift box
            // (file 782 entry 8) uses two of them around the Gen 5 starter-select app: CMD_1AE +
            // (331, 339, 0x8020) runs the app, leaving the pick 0/1/2 in the var, and CMD_1AF +
            // (9, 0x8020, 8) reads it back for the Condition that follows with NO pushes of its
            // own (@612: == 0 is Snivy; @652 pushes the var itself and tests == 1, Tepig; @700 is
            // Oshawott). The app becomes the species picker the Platinum briefcase and Elm's lab
            // already use here; the read-back pushes the var and 0 for that first compare.
            // The app covers the field from its open to its close (CMD_1AF); the SetOWPositions
            // in between (Cheren and Bianca step up beside the box) happen unseen on the cartridge,
            // so here the screen fades out at the open and back in at the close, and the picker
            // follows - the same fade the script itself uses around its later repositions.
            // (Opcode 0xB4 "ResetScreen" takes NO arguments: sized HHH it swallowed the next command -
            // the app call below read as `ResetScreen 331 339 var`, the pick's own compare as
            // `ResetScreen 9 var 8`, the player's SetOWPosition as `ResetScreen 109 255 4`.)
            // CMD_153 var runs the starter-select app and leaves the pick 0/1/2 in the var.
            "ResetScreen" -> {}
            "CMD_153" -> { starterVar = v(0); b.lines += listOf("ScreenFade", "1") }
            "CMD_1AF" -> starterVar?.let { b.lines += listOf("ScreenFade", "0"); b.lines += listOf("ChooseUnovaStarter", it); starterVar = null }
            // type, from, to, speed: 0 -> 16 darkens, 16 -> 0 restores. Dropped, every reposition
            // the games hide behind a fade was a visible jump (Nuvema's gift box, 2026-09-19).
            "FadeScreen" -> b.lines += listOf("ScreenFade", if (t(2) == "16") "1" else "0")
            "StoreVarItem", "CMD_243", "CMD_13D", "CMD_17E", "CMD_1AE", "CMD_12B", "CMD_1A9", "CMD_1AD", "CMD_1B1" -> {}
            // 250 and 251 are ACTORS a script makes, not camera slots: both carry a sprite in
            // MakeNPC (Nuvema makes Cheren as 250 sprite 7 and Bianca as 240 at the town edge) and
            // both get walks. Dropped as camera, Cheren never appeared on Route 1 (2026-09-20).
            // 252-254 are never made and stay dropped; 255 is the player.
            // 255 = player; 250-254 = camera/follower slots the server does not animate.
            // Object 32785 (VAR 0x8011) is the npc the script belongs to - the one being talked to.
            // The nurse moves herself with it, and her sibling script uses it as the message
            // speaker, which is the same thing the GBA side already calls VAR_LAST_TALKED.
            "ApplyMovement" ->
                if (t(0).toInt() in 252..254) {} else
                    b.lines +=
                        listOf(
                            "ApplyMovement",
                            when (t(0)) {
                              "255" -> "obj_player"
                              SELF_OBJECT.toString() -> "VAR_LAST_TALKED"
                              else -> "OBJ_" + t(0)
                            },
                            "M${file}_" + t(1).drop(1))
            "WaitMovement" -> b.lines += listOf("WaitMovement")
            "SingleTrainerBattle", "TrainerBattle" -> b.lines += listOf("TrainerBattle", tv(0), "0", "0", "0")
            "StoreBattleResult" -> b.lines += listOf("CheckBattleWon", v(0))
            "SetVarHero" -> b.lines += listOf("BufferPlayerName", t(0))
            "SetVarColoredItem" -> b.lines += listOf("BufferItemName", t(0), v(1))
            "SetVarNumberBound" -> b.lines += listOf("BufferNumber", t(0), v(1))
            "StoreHeroPosition", "StoreHeroPosition_66" -> b.lines += listOf("GetPlayerMapPos", v(0), v(1))
            "StoreHeroOrientation" -> b.lines += listOf("GetPlayerDir", v(0))
            "StoreRandomNumber" -> b.lines += listOf("GetRandom", v(0), t(1))
            "StoreGender" -> b.lines += listOf("GetPlayerGender", v(0))
            "StoreDay" -> b.lines += listOf("GetWeekday", v(0))
            "HealPokemon" -> b.lines += listOf("HealParty")
            // Gen 5: result var, species, item, level (the monkeys `0x8010 511 0 10`, Magikarp
            // `0x8010 129 0 5`, the starter `0x8010 0x8021 0 5`). Read in Gen 4 order the starter
            // was species 0 at level 495 and never arrived (2026-09-19).
            "GivePokemon" -> b.lines += listOf("GivePokemon", tv(1), tv(3), if (t(2) == "0") "ITEM_NONE" else item(2), v(0))
            "TakeMoney" -> b.lines += listOf("RemoveMoney", tv(0))
            "CheckMoney" -> b.lines += listOf("CheckMoney", tv(0), v(1))
            "CheckItemBagSpace" -> b.lines += listOf("CanFitItem", item(0), tv(1), v(2))
            // item, count, result var - the item by the client's own name, never a raw Gen 5 index.
            "TakeItem" -> b.lines += listOf("TakeItem", item(0), tv(1), v(2))
            "GiveItem", "AddItem" -> b.lines += listOf("GiveItem", item(0), tv(1), v(2))
            "StoreHeroGender" -> b.lines += listOf("GetPlayerGender", v(0))
            // header, x, y. Nuvema's room scene ends with `FallWarp 0 101 100` - header 0 is no map
            // (a same-map reload on the cartridge); warped there the owner stood in a blue void
            // (2026-09-19). Only a real header goes out.
            "FallWarp" -> if (t(0) != "0") b.lines += listOf("Warp", t(0), t(1), t(2), t(3))
            // MakeNPC x, y, dir, id, sprite, ?: the script makes its own actor (Cheren and Bianca
            // at Nuvema's Route 1 exit, Juniper on Route 1). Only ids under 250 are addressable
            // objects; 250/251 are the camera and follower slots the server does not animate.
            "MakeNPC" -> if ((t(3).toIntOrNull() ?: 255) < 252) b.lines += listOf("DsMakeNpc", t(3), t(4), t(0), t(1), t(2))
            "ShowDiploma", "Unknown_0F", "StoreVar_CF" -> {}
            // SELF_OBJECT is how the shared chunk scripts name the actor that was talked to, and
            // the item-ball routine (file 864) ends `SetFlag <its own flag>; RemoveNPC SELF`.
            // Dropped for not being under 252, the ball stayed on the ground and could be picked
            // up again and again (owner, 2026-09-21).
            "RemoveNPC" ->
                if (t(0) == SELF_OBJECT.toString()) b.lines += listOf("RemoveObject", "VAR_LAST_TALKED")
                else if (t(0).toInt() < 252) b.lines += listOf("RemoveObject", "OBJ_" + t(0))
            "AddNPC" ->
                if (t(0) == SELF_OBJECT.toString()) b.lines += listOf("AddObject", "VAR_LAST_TALKED")
                else if (t(0).toInt() < 252) b.lines += listOf("AddObject", "OBJ_" + t(0))
            // obj, x, z, y, facing (the corpus: z is 0/1/3 everywhere, facing 0-3; Nuvema's gift
            // box puts Cheren at 6,6 and Bianca at 4,6). Reading z as y put them on row 0.
            // 255 is the player, placed under the same fades (4,6 facing east for the battles after
            // the starter pick); the facing rides along for the player only.
            "SetOWPosition" ->
                if (t(0) == "255") b.lines += listOf("SetObjectEventPos", "LOCALID_PLAYER", t(1), t(3), t(4))
                else if (t(0).toInt() < 252) b.lines += listOf("SetObjectEventPos", "OBJ_" + t(0), t(1), t(3))
            // Opcode 0x1EC (sized 2026-09-23): the same placement with the facing SECOND - obj,
            // facing, x, z, y. Map-load scripts use it to stand actors where the story var says
            // they walked to (Route 3: Bianca and the little girl by the Wellspring var).
            "SetOWPositionFacing" ->
                if (t(0) == "255") b.lines += listOf("SetObjectEventPos", "LOCALID_PLAYER", t(2), t(4), t(1))
                else if (t(0).toInt() < 252) b.lines += listOf("SetObjectEventPos", "OBJ_" + t(0), t(2), t(4))
            "FastWarp", "TeleportWarp" -> b.lines += listOf("Warp", t(0), t(1), t(2), t(3))
            "CallStd" -> b.lines += listOf("CallStd", t(0))
            "ShowMoneyBox", "CloseMoneyBox", "UpdateMoneyBox" -> {}
            // An opcode the table only numbers (CMD_xxx) goes out as ScrCmd_xxx with its var
            // arguments marked, so the transpiler treats it the way it treats Platinum's unnamed
            // engine calls: a zero into the var it answers, nothing otherwise. Named commands the
            // dialect does not handle keep their name and reach the report as ds_<name>.
            else ->
                if (c.name.startsWith("CMD_")) b.lines += listOf("ScrCmd_" + c.name.removePrefix("CMD_")) + a.indices.map { tv(it) }
                else b.lines += listOf(c.name) + a.map { it.removePrefix("@") }
          }
        }
        // Movement tables: Gen 5 (type, count) pairs into the Gen 4 macro names the transpiler maps.
        movesByFile[file]?.forEach { (off, steps) ->
          val block = Block("M${file}_$off", true, mutableListOf())
          for (s in steps) {
            val pair = s.split(',').map { it.toInt() }
            val type = pair[0]
            val n = pair.getOrElse(1) { 1 }
            val dir = listOf("North", "South", "West", "East")[type and 3]
            // Gen 5 kept Gen 4's movement action numbers (pokeplatinum generated/
            // movement_actions.txt): every code read off the corpus by hand matched it - 32-35
            // turn in place, 60-63 delays, 69/70 hide/show, 75 the "!" bubble. The hand-made table
            // this replaces still had 24-31 (turns) as waits, 44-59 (jumps) as walks and DROPPED
            // 76-99, which are real steps (Cheren's `79 76` = east, north): actors ended scenes
            // on the wrong tile (Bianca beside Mom outside the lab, 2026-09-20). Names are the
            // Gen 4 macro names MOVEMENT_STEPS already maps; unmapped ones (lock dir, warp) skip.
            val name =
                when (type) {
                  in 0..99 -> GEN4_DIRECTIONAL.getOrNull(type / 4)?.let { it + dir } ?: GEN4_SINGLE[type] ?: continue
                  else -> GEN4_SINGLE[type] ?: continue
                }
            block.lines += listOf(name, n.toString())
          }
          block.lines += listOf("EndMovement")
          blocks += block
        }
        val entries = (entriesByFile[file] ?: HashMap()).toSortedMap().values.map { lab(it) }
        out["U$file"] = ParsedFile(entries, blocks)
      }
      // Level scripts: type 2 (enter) and 4 (load) run on arrival; the var table is the frame table.
      val hdrLines = File(root, "nds-headers-2.txt").readLines()
      val init = LinkedHashMap<Int, MutableList<String>>()
      val frame = LinkedHashMap<Int, MutableList<List<String>>>()
      fun entryLabel(header: Int, idx: Int): String? {
        val row = headerRows.firstOrNull { it[3] == header } ?: return null
        val entries = (entriesByFile[row[4]] ?: HashMap()).toSortedMap().values.toList()
        return entries.getOrNull(idx - 1)?.let { "U${row[4]}_$it" }
      }
      for (l in hdrLines) {
        val p = l.split(';')
        if (p[0] == "lvl" && (p[3] == "2" || p[3] == "4")) entryLabel(p[2].toInt(), p[4].toInt())?.let { init.getOrPut(p[2].toInt()) { mutableListOf() } += it }
        if (p[0] == "lvlvar") entryLabel(p[2].toInt(), p[5].toInt())?.let { target ->
          val f = frame.getOrPut(p[2].toInt()) { mutableListOf() }
          f += listOf("Compare", "VAR_0x" + p[3].toInt().toString(16).uppercase(), p[4])
          f += listOf("GoToIfEq", target)
        }
      }
      val initBlocks = mutableListOf<Block>()
      // One program PER on-load entry. They used to be chained as `call A; call B; end`, but a ROM
      // entry finishes with End, and `end` inside a call ends the whole script - so only the FIRST
      // entry of a map ever ran. Nuvema Town has two (type 2 -> entry 17, type 4 -> entry 13), and
      // the second is the one that stands Bianca by the player's house: she walked home from the
      // lab instead (2026-09-20). The first keeps the old label; the rest are _TRANSITION_1, _2...
      for ((h, targets) in init) targets.forEachIndexed { i, target ->
        val label = if (i == 0) "NDS_INIT_${h}_TRANSITION" else "NDS_INIT_${h}_TRANSITION_$i"
        initBlocks += Block(label, false, mutableListOf(listOf("GoTo", target), listOf("End")))
      }
      for ((h, lines) in frame) initBlocks += Block("NDS_INIT_${h}_FRAME", false, lines.toMutableList().also { it += listOf("End") })
      out["UINIT"] = ParsedFile(emptyList(), initBlocks)
      // Gen 5 runs trainer npcs (script 3000 + id) inside the engine: intro speech from the ROM
      // trainer message table, the battle, the defeated flag; a beaten trainer repeats their
      // post-battle line. Written here in the Gen 4 command names the transpiler already maps.
      val trainer =
          Block(
              "UTR_0", false,
              mutableListOf(
                  listOf("LockAll"), listOf("FacePlayer"),
                  listOf("GoToIfDefeated", "VAR_0x8004", "UTR_0_AGAIN"),
                  listOf("PrintTrainerDialogue", "VAR_0x8004", "0"), listOf("CloseMessage"),
                  listOf("TrainerBattle", "VAR_0x8004", "0", "0", "0"),
                  listOf("CheckWonBattle", "VAR_RESULT"), listOf("Compare", "VAR_RESULT", "0"), listOf("GoToIfEq", "UTR_0_LOST"),
                  listOf("SetTrainerFlag", "VAR_0x8004"), listOf("ReleaseAll"), listOf("End")))
      val again = Block("UTR_0_AGAIN", false, mutableListOf(listOf("PrintTrainerDialogue", "VAR_0x8004", "2"), listOf("WaitButton"), listOf("CloseMessage"), listOf("ReleaseAll"), listOf("End")))
      val lost = Block("UTR_0_LOST", false, mutableListOf(listOf("ReleaseAll"), listOf("End")))
      out["UTR"] = ParsedFile(listOf("UTR_0"), listOf(trainer, again, lost))
      return out
    }
  }

  private companion object {
    val LABEL_LINE = Regex("^(\\w+):\\s*$")
    val LOCAL_LABEL = Regex("^_[0-9A-Fa-f]{3,5}$")
    val MSG_TOKEN = Regex("^msg_(\\d+)(?:_\\w+?)?_(\\d{5})$")
    /** HeartGold's standard menu bank (files/msgdata/msg/msg_0191.gmm): floors, ROOF, EXIT, ... */
    const val HEARTGOLD_STD_MENU_BANK = 191
    /** A HeartGold warp whose destination header is 4095 goes to the dynamic warp (an elevator exit). */
    const val HEARTGOLD_DYNAMIC_HEADER = 4095
    val BLOCK_COMMENT = Regex("/\\*.*?\\*/")
    /** Platinum's CommonScript_TrySaveGame and HeartGold's std_prompt_save body (scr_seq_0003 _0646): VAR_RESULT 1 = saved. */
    val SILENT_SAVE_ROUTINES = setOf("CommonScript_TrySaveGame", "scr_seq_0003__0646")
    val MESSAGE_COMMANDS = setOf("Message", "MessageInstant", "MessageNoSkip", "MessageSynchronized", "NPCMessage", "EventMessage", "NPCMsg", "NonNPCMsg", "SimpleNPCMsg", "GenderMsgBox")
    /** Object ids a Gen 5 script can create with MakeNPC (252-254 are engine slots, 255 the player). */
    val SCRIPT_ACTOR_IDS = 224..251

    /** VAR 0x8011: the npc a script belongs to, which the GBA side already calls VAR_LAST_TALKED. */
    const val SELF_OBJECT = 32785

    /** Nuvema's walk up Route 1 (U778 entry 13 = NDS_389_14) ends by entering Juniper's lesson. */
    const val SCENE_CHAIN_FILE = "U778"
    const val SCENE_CHAIN_ENTRY = 13
    const val SCENE_CHAIN_TARGET = "NDS_317_1"

    /** Gen 4 movement actions 0-99 in groups of four (north, south, west, east); null = not directional. */
    val GEN4_DIRECTIONAL: List<String?> =
        listOf(
            "Face", "WalkSlower", "WalkSlow", "WalkNormal", "WalkFast", "WalkFaster",
            "WalkOnSpotSlower", "WalkOnSpotSlow", "WalkOnSpotNormal", "WalkOnSpotFast", "WalkOnSpotFaster",
            "JumpOnSpotSlow", "JumpOnSpotFast", "JumpNearFast", "JumpFar",
            null, null, null, null, // 60-75: delays, warp, visibility, direction lock, emote
            "WalkSlightlyFast", "WalkSlightlyFaster", "WalkFastest", "Run",
            null, // 92-95: west/east-only jumps
            "WalkEverSoSlightlyFast")
    /** The non-directional Gen 4 movement actions the transpiler has a step for. */
    val GEN4_SINGLE: Map<Int, String> =
        mapOf(
            60 to "Delay1", 61 to "Delay2", 62 to "Delay4", 63 to "Delay8", 64 to "Delay15", 65 to "Delay16", 66 to "Delay32",
            69 to "SetInvisible", 70 to "SetVisible", 75 to "EmoteExclamationMark",
            92 to "JumpNearSlowWest", 93 to "JumpNearSlowEast", 94 to "JumpFartherWest", 95 to "JumpFartherEast")
    val TERMINAL = setOf("End", "Return", "GoTo", "EndMovement")
    /** ov01_022067C8 in pokeheartgold src/field/scrcmd_message.c. */
    val STD_MSG_BANKS = mapOf(0 to 752, 1 to 211, 2 to 30, 3 to 435)
    val YESNO_COMMANDS = setOf("ShowYesNoMenu", "YesNo", "GetMenuChoice")
    val PROMPT_FILLER = setOf("WaitButton", "WaitABPress", "TouchscreenMenuHide", "TouchscreenMenuShow", "PlaySE")
    val COND_CODES = mapOf("0" to "lt", "1" to "eq", "2" to "gt", "3" to "le", "4" to "ge", "5" to "ne", "TRUE" to "eq", "FALSE" to "ne")
    val OBJECT_ALIASES = mapOf("obj_player" to "LOCALID_PLAYER", "OBJ_PLAYER" to "LOCALID_PLAYER", "OBJ_EVENT_ID_PLAYER" to "LOCALID_PLAYER")
    val FACE_MOVEMENTS = mapOf("DIR_NORTH" to "NDS_FACE_UP", "DIR_SOUTH" to "NDS_FACE_DOWN", "DIR_WEST" to "NDS_FACE_LEFT", "DIR_EAST" to "NDS_FACE_RIGHT",
        "0" to "NDS_FACE_UP", "1" to "NDS_FACE_DOWN", "2" to "NDS_FACE_LEFT", "3" to "NDS_FACE_RIGHT")
    /** Queries with a fixed answer on this server: the var they fill and the value. */
    /** Commands that open the naming keyboard: the questions leading to them are skipped. */
    val NICKNAME_COMMANDS = setOf("OpenPokemonNamingScreen", "NicknameInput", "RenamePokemon")
    val NAMING_COMMANDS = NICKNAME_COMMANDS + "NameRival"

    /** One Pokegear call line per gender, plus a follow-up line (Mom's answer to her own question). */
    data class ScriptedCall(val male: String, val female: String, val followUp: String? = null)

    /**
     * HeartGold `PhoneCall contact, 2, n`: src/application/pokegear/phone/scripts - Elm's
     * sPhoneCallData_ProfElm_MapScripts[n] -> PHONE_SCRIPT_002..006, Oak's scripted call is
     * PHONE_SCRIPT_082, Baoba's PHONE_SCRIPT_141 (phone_script_defs.c msgIds, banks per contact
     * in src/phonebook_dat.c). Mom's scripted call prints msg_0664 entry 22 and asks about her
     * savings; there is no bank here, so her "I won't save your money" answer (26) follows.
     */
    val HEARTGOLD_SCRIPTED_CALLS: Map<String, List<ScriptedCall>> =
        mapOf(
            "PHONE_CONTACT_PROF__ELM" to
                listOf(33 to 34, 35 to 36, 37 to 38, 39 to 40, 41 to 42).map { (m, f) -> ScriptedCall("msg_0716_%05d".format(m), "msg_0716_%05d".format(f)) },
            "PHONE_CONTACT_PROF__OAK" to listOf(ScriptedCall("msg_0666_00012", "msg_0666_00012")),
            "PHONE_CONTACT_MOTHER" to listOf(ScriptedCall("msg_0664_00022", "msg_0664_00022", "msg_0664_00026")),
            "PHONE_CONTACT_BAOBA" to listOf(ScriptedCall("msg_0667_00002", "msg_0667_00003")),
        )
    /** HeartGold's Mom's bank (scr_seq_0845_T20R0201.s): PokeMMO has no such bank. */
    val BANK_COMMANDS = setOf("BankTransaction", "CheckBankBalance", "BankOrWalletIsFull")
    /** Lines between a question and the branches on its answer that change nothing about them. */
    val QUESTION_FILLER = setOf("CloseMsg", "CloseMessage", "TouchscreenMenuShow", "TouchscreenMenuHide")
    /** Text commands: a yes/no prompt belongs to the last of them before it. */
    val PROMPT_MESSAGES = setOf("Message", "MessageInstant", "MessageNoSkip", "MessageSynchronized", "NPCMessage", "EventMessage",
        "NPCMsg", "NonNPCMsg", "MessageVar", "NPCMsgVar", "NonNPCMsgVar")
    /**
     * Pokedex_GetRatingMessageID_National's answers in order (src/unk_0205DFC4.c): caught bands
     * under 40, 40, 60, 90, 120, 150, 190, 230, 270, 310, 350, 380; 410 male / female; 430, 450,
     * 460, 470, 476; complete male / female.
     */
    val NATIONAL_DEX_RATING_TEXTS =
        listOf("Under40", "40", "60", "90", "120", "150", "190", "230", "270", "310", "350", "380", "410_Male", "410_Female", "430", "450", "460", "470", "476")
            .map { "PokedexRatings_Text_OakPokemonCaught$it" } +
            listOf("PokedexRatings_Text_OakCompleteNationalDex_Male", "PokedexRatings_Text_OakCompleteNationalDex_Female")
    // CheckPartyPokerus: no Pokerus on this server (IsPokerusInParty answers 0 in Kanto too).
    // GetSwarmMapAndSpecies: no daily swarms are modelled; the species var reads 0.
    val STUB_QUERIES = mapOf(
        "CheckPartyPokerus" to 0, "GetSwarmMapAndSpecies" to 0, "PartyHasPokerus" to 0,
        // HeartGold: trainer card stars start at 0; no Shiny Leaves; Kenya carries mail, which the
        // catalogue does not have (KenyaCheckPartyOrMailbox VAR / GetShinyLeafCount slot, VAR /
        // CheckReturnLoanMon trade, slot, VAR all answer in their last var).
        "GetTrcardStars" to 0, "GetShinyLeafCount" to 0, "KenyaCheckPartyOrMailbox" to 0, "CheckReturnLoanMon" to 0,
        // No distribution events, no bad eggs, no contest photos, no unused trade flag.
        "CheckDistributionEvent" to 0, "CheckPartyHasBadEgg" to 0, "ContestPhotoHasData" to 0, "GetNPCTradeUnusedFlag" to 0,
        // No Poketch, no bad eggs, no apricorn box, no coins to fill, no Battle Points to spend,
        // no follower-event monsters: the checks answer no (CheckGiveCoins: room for more, yes).
        "CheckPoketchEnabled" to 0, "PartyLegalCheck" to 0, "GetTotalApricornCount" to 0, "CheckBattlePoints" to 0,
        "FollowerPokeIsEventTrigger" to 0, "CheckGiveCoins" to 1,
        "DressUpPhotoHasData" to 0,
        "CheckTVInterviewEligible" to 0, "ScrCmd_729" to 0, "GetItemPocket" to 0, "GetTrainerCardLevel" to 0, "CheckItemIsPlate" to 0, "GetTimeOfDay" to 1, "CheckPartyHasSpecies" to 0, "CheckPoketchAppRegistered" to 0, "GetTrCardStars" to 0, "CountAliveMonsExcept" to 1, "GetMovementType" to 0, "CheckIsTrainerDoubleBattle" to 0, "CheckHasTwoAliveMons" to 1, "PhotoAlbumIsFull" to 0, "GetPlayerState" to 0,
        "CheckPlayerOnBike" to 0, "PlayerOnBikeCheck" to 0, "CheckRegisteredPhoneNumber" to 0, "GetPhoneBookRematch" to 0,
        "GetRematchTrainerID" to 0, "IsItemTMHM" to 0, "ItemIsTMOrHM" to 0, "GetCoinsAmount" to 0, "GetCoinAmount" to 0,
    )
    // LOCALID_DP_FOLLOWER (254, scripts_route_201.s): the partner walking behind the player.
    val IGNORED_OBJECTS = setOf("obj_partner_poke", "LOCALID_FOLLOWER", "obj_follower", "OBJ_FOLLOWER", "LOCALID_DP_FOLLOWER")
    val DROPPED =
        setOf(
            "PlaySE", "StopSE", "WaitSE", "PlayCry", "WaitCry", "PlayFanfare", "WaitFanfare", "PlayMusic", "StopMusic",
            "PlayDefaultMusic", "PlayBGM", "StopBGM", "ResetBGM", "FadeOutBGM", "FadeInBGM", "TempBGM", "SetSpecialBGM",
            "SetBGM", "SetBGMFixed", "SetBGMPlayerPaused", "FadeScreen", "FadeScreenOut", "FadeScreenIn", "WaitFadeScreen",
            "WaitFade", "BufferPlayerName", "BufferRivalName", "BufferCounterpartName", "BufferPlayersName", "BufferRivalsName",
            "BufferFriendsName", "BufferPartyMonSpecies", "BufferItemName", "BufferPocketName", "BufferMoveName",
            "BufferNumber", "BufferPartyMonNickname", "BufferMonSpeciesName", "BufferSpeciesNameFromVar", "BufferMapName",
            "BufferTMHMMoveName", "BufferTrainerClassName", "BufferPoketchAppName", "TouchscreenMenuHide",
            "TouchscreenMenuShow", "ToggleFollowingPokemonMovement", "WaitFollowingPokemonMovement",
            "FollowingPokemonMovement", "ReturnToField", "RestoreOverworld", "Noop", "Dummy", "SetObjectFlagIsPersistent",
            // Gen 5 (disassembly names): sound, camera, waits with no server counterpart. SetStatusCG
            // is the C-Gear activation (Fennel, after the Dream Mist) - a DS wireless feature an MMO
            // has no use for; the recovered branch it sits on rejected her whole script (2026-09-22).
            "WaitMoment", "Nop", "Nop2", "Cry", "FadeToDefaultMusic", "SetStatusCG",
            "StartCameraEvent", "StopCameraEvent", "LockCamera", "ReleaseCamera", "MoveCamera", "EndCameraEvent", "ResetCamera",
            "CallStart", "CallEnd", "EndBattle", "DisableTrainer", "ChangeMusicVolume", "SetTextScriptMessage", "CloseMulti",
            // HeartGold opens most npc scripts with this argument-less command; nothing observable follows it.
            "ScrCmd_609", "CameronPhoto", "RecordHeapMemory", "CreateJournalEvent", "ActivateRegiRuinsDot", "LoadDoorAnimation",
            "InitTurnbackCave", "InitPersistedMapFeaturesForDistortionWorld", "ShowDressUpPhoto", "SetWarpEventPos",
            "CallBattleTowerFunction", "ClearHasPartner", "LoadTVInterviewMessage", "SetObjectEventMovementType", "SetMovementType",
            "ScriptOverlayCmd", "ShowMoney", "HideMoney", "ShowMoneyBox", "HideMoneyBox", "UpdateMoneyDisplay", "UpdateMoneyBox",
            "ShowCoins", "HideCoins", "UpdateCoinDisplay", "TrySetUnusedCollectedOrbFlag", "PlayDoorOpenAnimation", "PlayDoorCloseAnimation",
            "RegisterGearNumber", "ScreenShake", "SetBikeStateLock", "MoveGreatMarshTram", "SetSubScene63",
            // A lost battle already whited the player out server-side; trainer intro text and music
            // come from ROM tables not bound yet.
            "BlackOutFromBattle", "Whiteout", "WhiteOut", "PlayTrainerEncounterBGM", "SetMoveCodeForFacingDirection",
            // A lost battle already whited the player out server-side.
            "BlackoutFromBattle", "Whiteout",
            // Platinum presentation with no server counterpart: the hand-over pose, camera and
            // volume, animation unloads, the menu's side, a dummy command, game records.
            "SetPlayerState", "ChangePlayerState", "AddFreeCamera", "ApplyFreeCameraMovement", "RestoreCamera",
            "AddCameraOverrideObject", "RemoveCameraOverrideObject", "SetInitialVolumeForSequence", "UnloadAnimation",
            "SetMenuXOriginToRight", "Dummy1F9", "IncrementGameRecord", "PlayPokecenterHealingAnimation",
            // Systems PokeMMO does not have: the Poketch, the journal, accessories and contest
            // backdrops, swarm news. The follow-partner flags only steer the rival walking behind.
            "RegisterPoketchApp", "GiveJournal", "AddAccessory", "AddContestBackdrop", "EnableSwarms",
            "SetHasPartner", "SetStepFlag",
            // The catching demonstration is skipped like Wally's and the Viridian old man's; the
            // Pokedex itself is FLAG_HAS_POKEDEX, which the script sets right after GivePokedex.
            "StartCatchingTutorial", "GivePokedex",
            // HeartGold: the same pose, healing-machine and catching-tutorial presentation; the ball
            // placement on Elm's desk; the respawn point is the nurse's here (GBA setrespawn too).
            "SetAvatarBits", "UpdateAvatarState", "PokeCenAnim", "DebugWatch", "CatchingTutorial",
            "PlaceStarterBallsInElmsLab", "SetSpawn",
            // HeartGold systems PokeMMO does not have: the Pokegear phone and its cards, Shiny Leaf
            // crowns and the certificate screen, ribbons, Kenya's mail and the loan Spearow's return.
            "UnsetPhoneCallTrigger", "RegisterPokegearCard", "TryGiveShinyLeafCrown", "ShowCertificate",
            "GiveRibbon", "MonGiveMail", "ReturnLoanMon",
            // Bookkeeping and presentation with no server side: game records and scores, the saving
            // and waiting icons, the save-info window, the follower's inhibit state, the platform
            // lift's persisted state, the warp-tile move (Radio Tower) - and coins/Pal Park, which
            // PokeMMO does not have.
            "IncrementTrainerScore", "IncrementTrainerScore2", "AddSpecialGameStat", "AddSpecialGameStat2", "NopVar490",
            "ShowSavingIcon", "HideSavingIcon", "OpenSaveInfo", "CloseSaveInfo", "AddWaitingIcon", "SetFollowMonInhibitState",
            "InitPersistedMapFeaturesForPlatformLift", "MoveWarp", "TakeCoins", "PalParkAction", "ShowSaveStats", "HideSaveStats",
            // Save-data and link housekeeping, the HM cut-in, the transition wait, a bg event's tile,
            // Team Rocket's costume flag (the disguise scene's look).
            "SaveExtraData", "ClearReceivedTempDataAllPlayers", "PlayHMCutIn", "WaitForTransition", "SetBgEventPos",
            "RocketCostumeFlagAction",
        )
    val MOVEMENT_STEPS: Map<String, String> = buildMap {
      val dirs = mapOf("North" to "up", "South" to "down", "West" to "left", "East" to "right")
      for ((suffix, d) in dirs) {
        put("Face$suffix", "face_$d")
        put("WalkNormal$suffix", "walk_$d")
        put("WalkSlow$suffix", "walk_slow_$d")
        put("WalkSlower$suffix", "walk_slow_$d")
        put("WalkFast$suffix", "walk_fast_$d")
        put("WalkFaster$suffix", "walk_fast_$d")
        put("WalkFastest$suffix", "walk_fast_$d")
        put("WalkSlightlyFast$suffix", "walk_$d")
        put("WalkSlightlyFaster$suffix", "walk_fast_$d")
        put("WalkEverSoSlightlyFast$suffix", "walk_$d")
        put("Run$suffix", "walk_fast_$d")
        put("WalkOnSpotNormal$suffix", "walk_in_place_$d")
        put("WalkOnSpotSlow$suffix", "walk_in_place_$d")
        put("WalkOnSpotSlower$suffix", "walk_in_place_$d")
        put("WalkOnSpotFast$suffix", "walk_in_place_$d")
        put("WalkOnSpotFaster$suffix", "walk_in_place_$d")
        put("JumpOnSpotSlow$suffix", "walk_in_place_$d")
        put("JumpOnSpotFast$suffix", "walk_in_place_$d")
        put("JumpNearFast$suffix", "walk_$d")
        put("JumpNearSlow$suffix", "walk_$d")
        put("JumpFar$suffix", "walk_$d")
        put("JumpFarther$suffix", "walk_$d")
      }
      put("Delay1", "delay_1"); put("Delay2", "delay_2"); put("Delay4", "delay_4"); put("Delay8", "delay_8")
      put("Delay15", "delay_16"); put("Delay16", "delay_16"); put("Delay32", "delay_16")
      put("SetInvisible", "set_invisible")
      put("EmoteExclamationMark", "emote_exclamation_mark"); put("EmoteExclamation2", "emote_exclamation_mark")
      put("EmoteDoubleExclamationMark", "emote_exclamation_mark"); put("EmoteQuestionMark", "emote_question_mark")
      put("NurseJoyBow", "nurse_joy_bow"); put("PokecenterNurseBow", "nurse_joy_bow")
    }

    /** `NAME` or `NAME = expr` lines: C-enum numbering, one value per line. */
    fun enumFile(file: File, known: Map<String, Int>): Map<String, Int> {
      if (!file.isFile) return emptyMap()
      val out = LinkedHashMap<String, Int>()
      var next = 0
      for (raw in file.readLines()) {
        val line = raw.substringBefore("//").trim()
        if (line.isEmpty()) continue
        val name = line.substringBefore('=').trim()
        val expr = if ('=' in line) line.substringAfter('=').trim() else null
        val value =
            when {
              expr == null -> next
              expr.startsWith("0x") -> expr.removePrefix("0x").toIntOrNull(16)
              expr.toIntOrNull() != null -> expr.toInt()
              else -> out[expr] ?: known[expr]
            } ?: continue
        out[name] = value
        next = value + 1
      }
      return out
    }
  }
}

/** A badge token as its number, so the eight synthetic flags can be counted. */
/**
 * Gen 4 machines by number (pokeheartgold include/constants/items.h `#define TM_FLASH ITEM_TM70`);
 * Diamond/Pearl/Platinum and HeartGold/SoulSilver share the TM01-92 list. The catalogue resolves a
 * machine by its move, while its numbered lookup follows the Gen 3 list.
 */
private fun gen4MachineAliases(heartGoldRoot: File, hmMoves: List<String>? = null): Map<String, String> {
  val file = File(heartGoldRoot, "include/constants/items.h")
  if (!file.isFile) return emptyMap()
  val text = file.readText()
  val out = HashMap<String, String>()
  Regex("#define\\s+TM_(\\w+)\\s+ITEM_TM(\\d\\d)\\b").findAll(text).forEach { out["ITEM_TM${it.groupValues[2]}"] = "ITEM_TM_${it.groupValues[1]}" }
  // HMs differ between the two games (HeartGold HM05 Whirlpool, Platinum HM05 Defog): the
  // header's HM_ defines for HeartGold, the caller's list for Platinum. The client keeps each
  // game's own HM tools (tool-moves.csv 8420-8427 Sinnoh, 9420-9427 Johto), found by move.
  if (hmMoves == null) Regex("#define\\s+HM_(\\w+)\\s+ITEM_HM(\\d\\d)\\b").findAll(text).forEach { out["ITEM_HM${it.groupValues[2]}"] = "ITEM_HM_${it.groupValues[1]}" }
  else hmMoves.forEachIndexed { i, move -> out["ITEM_HM%02d".format(i + 1)] = "ITEM_HM_$move" }
  return out
}

/**
 * PokeMMO's story legendaries (national dex ids): "boss" for the two story bosses (Ho-Oh 250,
 * Giratina 487) and Zekrom's required catch (644), "none" for every other legendary or mythical of
 * Gens 1-5 (no battle in the story), null for an ordinary species.
 */
private fun legendaryKind(species: Int?): String? =
    when (species) {
      null -> null
      250, 487, 644 -> "boss"
      in 144..146, 150, 151, in 243..245, 249, 251, in 377..386, in 480..493, 494, in 638..649 -> "none"
      else -> null
    }

/** The members of every `enum { A = 0, B, ... }` in [text], counting the way C does; known names win. */
private fun enumConstants(text: String, known: Map<String, Int>): Map<String, Int> {
  val out = HashMap<String, Int>()
  for (body in Regex("enum\\s*\\w*\\s*\\{([^}]*)\\}").findAll(text).map { it.groupValues[1] }) {
    var next = 0
    for (raw in body.split(',')) {
      val entry = raw.substringBefore("//").trim()
      if (entry.isEmpty()) continue
      val name = entry.substringBefore('=').trim()
      if (!Regex("[A-Za-z_]\\w*").matches(name)) continue
      val value = entry.substringAfter('=', "").trim().let { v -> if (v.isEmpty()) next else (v.toIntOrNull() ?: v.removePrefix("0x").toIntOrNull(16) ?: known[v] ?: out[v] ?: next) }
      if (name !in known) out[name] = value
      next = value + 1
    }
  }
  return out
}

/** Platinum's HM01-HM08 moves (the client's Sinnoh HM tools 8420-8427 carry the same order). */
private val PLATINUM_HM_MOVES = listOf("CUT", "FLY", "SURF", "STRENGTH", "DEFOG", "ROCK_SMASH", "WATERFALL", "ROCK_CLIMB")

private fun badge(token: String, constants: Map<String, Int>): String = (constants[token] ?: token.toIntOrNull())?.toString() ?: token
