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

    /** Convenience macros the scripts use as commands, expanded to their plain-command bodies. */
    fun macros(): Map<String, Macro> = emptyMap()

    /** For a chunk whose ids are base + trainer id: the number of trainer ids, else null. */
    fun trainerChunkSize(base: Int): Int? = null
  }

  class Macro(val params: List<String>, val body: List<String>)

  /** One ROM map header: its id as the client numbers it and the files it references. */
  data class MapHeader(val id: Int, val name: String, val scriptFile: String?, val eventsFile: String?, val msgBank: Int? = null, val initFile: File? = null)

  fun build(spec: ScriptCorpusSpec): BuiltScriptCorpus {
    val dialect: Dialect =
        when (spec.source) {
          "platinum" -> Platinum(spec.decompDir)
          "heartgold" -> HeartGold(spec.decompDir)
          else -> error("Unknown DS script source ${spec.source}")
        }
    val constants = HashMap(dialect.constants())
    constants["OBJ_EVENT_ID_PLAYER"] = 0xFF
    constants["TRUE"] = 1
    constants["FALSE"] = 0
    constants["VAR_RESULT"] = 0x800C

    val failures = linkedMapOf<String, Int>()
    val samples = linkedMapOf<String, MutableList<String>>()
    fun failure(category: String, detail: String) {
      failures[category] = failures.getOrDefault(category, 0) + 1
      samples.getOrPut(category) { mutableListOf() }.let { if (it.size < 5) it += detail }
    }

    val headers = dialect.mapHeaders()
    val headersByScript = headers.filter { it.scriptFile != null }.groupBy { it.scriptFile!! }

    val programs = mutableListOf<ScriptCorpusProgramRecord>()
    val movements = mutableListOf<ScriptCorpusMovementRecord>()
    val referencedText = linkedSetOf<String>()
    val interactable = linkedSetOf<String>()
    val referencedTokens = HashSet<String>()
    val tokenPattern = Regex("[A-Za-z_]\\w*")
    var indexedLabels = 0

    val parsedByFile = HashMap<String, ParsedFile>()
    for (file in dialect.scriptFiles) {
      val parsed = parseScriptFile(file, dialect)
      parsedByFile[file.name] = parsed
      val owners = headersByScript[file.name].orEmpty()
      val objectIds = owners.firstOrNull()?.let(dialect::objectIdsFor).orEmpty()
      val sourceFile = "decomp/${spec.decompDir.name}/${file.relativeTo(spec.decompDir).invariantSeparatorsPath}"
      indexedLabels += parsed.blocks.size

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
        val commands = transpile(block.lines, dialect, block.label, extra, owners.firstOrNull()?.msgBank, constants)
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
      // Chunk bindings (Platinum common scripts etc.): global ids -> entry label.
      dialect.chunkFiles().filterValues { it == file.name }.keys.forEach { base ->
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
    )
  }

  // ---------------------------------------------------------------- source parsing

  private class Block(val label: String, var movement: Boolean, val lines: MutableList<List<String>>)

  private class ParsedFile(val entries: List<String>, val blocks: List<Block>)

  /**
   * Splits a script file into labelled blocks of `Command arg, arg` lines. Local labels (`_0033`)
   * are namespaced by file so the corpus stays label-unique; a block that falls through into the
   * next label gets an explicit `goto` so the interpreter never runs off the end.
   */
  private fun parseScriptFile(file: File, dialect: Dialect): ParsedFile {
    val stem = file.nameWithoutExtension
    // Local labels get the file as namespace; the Gen 4 result var takes the GBA name the
    // interpreter's own commands write to (vars are keyed by token, not number).
    fun localize(token: String) =
        when {
          LOCAL_LABEL.matches(token) -> "${stem}_$token"
          token == "VAR_SPECIAL_RESULT" || token == "VAR_0x800C" -> "VAR_RESULT"
          else -> token
        }
    val entries = mutableListOf<String>()
    val blocks = mutableListOf<Block>()
    var current: Block? = null
    val macros = dialect.macros()
    val source = ArrayDeque(file.readLines())
    while (source.isNotEmpty()) {
      val raw = source.removeFirst()
      val line = raw.substringBefore("//").substringBefore(';').trim()
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
  ): List<String> {
    val out = mutableListOf<String>()
    // HeartGold standard messages: GetStdMsgNaix puts a message file id in a var, MsgBoxExtern
    // shows an entry of it; folded here into the same ROM text ids as every other message.
    val stdMsg = HashMap<String, Int>()
    var i = 0
    while (i < lines.size) {
      val line = lines[i]
      val name = line[0]
      // A bare number in a message command indexes the owning map header's own text bank.
      val a = line.drop(1).map { t -> if (msgBank != null && name in MESSAGE_COMMANDS && t.toIntOrNull() != null) "msg_%04d_MAP_%05d".format(msgBank, t.toInt()) else t }
      val next = lines.getOrNull(i + 1)
      when {
        name.startsWith("Buffer") -> {}
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
        "SetVar", "SetVarFromValue" -> out += "setvar ${a[0]}, ${a[1]}"
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
          if (dialect.textId(a[0]) == null) out += "ds_${name.lowercase()} ${a.joinToString(", ")}"
          else {
            // A yes/no prompt right after the text (Platinum ShowYesNoMenu, HeartGold's {YESNO}
            // text + GetMenuChoice) is one GBA MSGBOX_YESNO; Gen 4 answers 0 = yes, 1 = no.
            val prompt = (1..3).firstOrNull { k ->
              val l = lines.getOrNull(i + k) ?: return@firstOrNull false
              if (l[0] in YESNO_COMMANDS) true else if (l[0] in PROMPT_FILLER) false else return@firstOrNull false
            }
            if (prompt != null) {
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
          out += "yesnobox 0, 0"
          out += "ds_yesno ${a[0]}"
        }
        "GetPlayerMapPos", "GetPlayerCoords" -> out += "getplayerxy ${a[0]}, ${a[1]}"
        "SetObjectEventPos" -> out += "setobjectxy ${a[0]}, ${a[1]}, ${a[2]}"
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
        "GoToIfBadgeAcquired" -> out += "goto_if_set FLAG_DS_BADGE_${a[0]}, ${a[1]}"
        "CheckBadge" -> out += "ds_flagtovar FLAG_DS_BADGE_${a[0]}, ${a[1]}"
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
          out += "setwildbattle ${a[0]}, ${a.getOrElse(1) { "5" }}, ITEM_NONE"
          out += "dowildbattle"
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
        "SetObjectEventDir" -> {
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
        "GenderMsgBox" -> {
          // Two texts, one per player gender; checkplayergender leaves MALE = 0 in VAR_RESULT.
          val female = "${label}_female"
          val done = "${label}_gender_done"
          out += "checkplayergender"
          out += "compare VAR_RESULT, 1"
          out += "goto_if_eq $female"
          out += "message ${a[0]}"
          out += "waitmessage"
          out += "goto $done"
          extra += female to listOf("message ${a[1]}", "waitmessage", "goto $done")
          extra += done to listOf("return")
          out += "end"
        }
        in STUB_QUERIES.keys -> {
          val target = a.lastOrNull { it.startsWith("VAR_") }
          if (target != null) out += "setvar $target, ${STUB_QUERIES.getValue(name)}"
        }
        "GetPlayerDir", "GetPlayerFacing" -> out += "ds_getplayerdir ${a[0]}"
        "GetWeekday" -> out += "ds_getweekday ${a[0]}"
        "CallCommonScript" -> out += "call NDS_CHUNK_${a[0].removePrefix("0x").toIntOrNull(if (a[0].startsWith("0x")) 16 else 10) ?: a[0]}"
        "CallStd" -> out += "call NDS_CHUNK_${constants[a[0]] ?: a[0]}"
        "GiveItemNoCheck" -> {
          out += "giveitem ${a[0]}, ${a.getOrElse(1) { "1" }}"
          resultCopy(out, a.getOrNull(2))
        }
        "RestartCurrentScript" -> out += "end"
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
          out += "yesnobox 0, 0"
          out += "ds_yesno ${a[0]}"
        }
        // -- movement
        "ApplyMovement" -> {
          val target = OBJECT_ALIASES[a[0]] ?: a[0]
          if (target in IGNORED_OBJECTS) {} else out += "applymovement $target, ${a[1]}"
        }
        "WaitMovement" -> out += "waitmovement 0"
        "WaitTime", "Wait" -> out += "delay ${a.getOrElse(0) { "1" }}"
        "AddObject", "ShowPerson" -> out += "addobject ${a[0]}"
        "RemoveObject", "HidePerson" -> out += "removeobject ${a[0]}"
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

    override fun constants(): Map<String, Int> {
      val out = HashMap(ConstantsIndex.build(root))
      File(root, "generated").listFiles { f -> f.extension == "txt" }?.sortedBy { it.name }?.forEach { out += enumFile(it, out) }
      return out
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
            if (n != null && n.startsWith("Common_") && body.none { it.startsWith(".") }) out[n] = Macro(params, body.toList())
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

    override fun constants(): Map<String, Int> = constants
  }

  private companion object {
    val LABEL_LINE = Regex("^(\\w+):\\s*$")
    val LOCAL_LABEL = Regex("^_[0-9A-Fa-f]{3,5}$")
    val MSG_TOKEN = Regex("^msg_(\\d+)(?:_\\w+?)?_(\\d{5})$")
    val MESSAGE_COMMANDS = setOf("Message", "MessageInstant", "MessageNoSkip", "MessageSynchronized", "NPCMessage", "EventMessage", "NPCMsg", "NonNPCMsg", "SimpleNPCMsg", "GenderMsgBox")
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
    val STUB_QUERIES = mapOf(
        "GetNationalDexEnabled" to 1, "GetGameVersion" to 0, "GetPartyLeadAlive" to 1, "DressUpPhotoHasData" to 0,
        "CheckTVInterviewEligible" to 0, "ScrCmd_729" to 0, "GetItemPocket" to 0, "GetTrainerCardLevel" to 0, "CheckItemIsPlate" to 0, "GetTimeOfDay" to 1, "CheckPartyHasSpecies" to 0, "CheckPoketchAppRegistered" to 0, "GetTrCardStars" to 0, "CountAliveMonsExcept" to 1, "GetMovementType" to 0, "CheckIsTrainerDoubleBattle" to 0, "CheckHasTwoAliveMons" to 1, "PhotoAlbumIsFull" to 0, "GetPlayerState" to 0,
        "CheckPlayerOnBike" to 0, "PlayerOnBikeCheck" to 0, "CheckRegisteredPhoneNumber" to 0, "GetPhoneBookRematch" to 0,
        "GetRematchTrainerID" to 0, "IsItemTMHM" to 0, "ItemIsTMOrHM" to 0, "GetCoinsAmount" to 0, "GetCoinAmount" to 0,
    )
    val IGNORED_OBJECTS = setOf("obj_partner_poke", "LOCALID_FOLLOWER", "obj_follower", "OBJ_FOLLOWER")
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
            // HeartGold opens most npc scripts with this argument-less command; nothing observable follows it.
            "ScrCmd_609", "CameronPhoto", "RecordHeapMemory", "CreateJournalEvent", "ActivateRegiRuinsDot", "LoadDoorAnimation",
            "InitTurnbackCave", "InitPersistedMapFeaturesForDistortionWorld", "ShowDressUpPhoto", "SetWarpEventPos",
            "CallBattleTowerFunction", "ClearHasPartner", "LoadTVInterviewMessage", "SetObjectEventMovementType", "SetMovementType",
            "ScriptOverlayCmd", "ShowMoney", "HideMoney", "ShowMoneyBox", "HideMoneyBox", "UpdateMoneyDisplay", "UpdateMoneyBox",
            "ShowCoins", "HideCoins", "UpdateCoinDisplay", "TrySetUnusedCollectedOrbFlag", "PlayDoorOpenAnimation", "PlayDoorCloseAnimation",
            "RegisterGearNumber", "ScreenShake", "SetBikeStateLock", "MoveGreatMarshTram", "SetSubScene63",
            // A lost battle already whited the player out server-side; trainer intro text and music
            // come from ROM tables not bound yet.
            "BlackOutFromBattle", "Whiteout", "WhiteOut", "PlayTrainerEncounterBGM", "GetTrainerMessageTypes", "GetTrainerRematchMessageTypes",
            "PrintTrainerDialogue", "SetMoveCodeForFacingDirection",
            // A lost battle already whited the player out server-side.
            "BlackoutFromBattle", "Whiteout",
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
