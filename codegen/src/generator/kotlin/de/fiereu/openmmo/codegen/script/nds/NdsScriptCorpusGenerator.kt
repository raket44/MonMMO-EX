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
    )
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
    var lastVar8004: Int? = null
    // A scripted menu builds up item by item: Platinum Init*TextMenu / AddMenuEntry* / ShowMenu,
    // HeartGold MenuInit(StdGmm) / MenuItemAdd / MenuExec. Emitted as one ds_menu.
    var menuVar: String? = null
    var menuCursor = "0"
    var menuStdBank = false
    val menuItems = mutableListOf<Pair<String, String>>()
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
              "BufferRivalName", "BufferRivalsName", "BufferCounterpartName" -> out += "ds_buffer ${a[0]}, rival"
              "BufferItemName", "BufferItemNameWithArticle", "BufferItemNamePlural", "BufferItemNameIndef" ->
                  if (a.size >= 2) out += "ds_buffer ${a[0]}, item, ${a[1]}"
              "BufferNumber", "BufferInt", "BufferFloorNumber", "BufferDeptStoreFloorNo" ->
                  if (a.size >= 2) out += "ds_buffer ${a[0]}, number, ${a[1]}"
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
          out += "setvar ${a[0]}, ${a[1]}"
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
        "GoToIfBadgeAcquired" -> out += "goto_if_set FLAG_DS_BADGE_${badge(a[0], constants)}, ${a[1]}"
        "CheckBadge", "CheckBadgeAcquired" -> out += "ds_flagtovar FLAG_DS_BADGE_${badge(a[0], constants)}, ${a[1]}"
        "CheckFlagVar" -> out += "ds_flagtovar ${a[0]}, ${a[1]}"
        "GiveBadge" -> out += "setflag FLAG_DS_BADGE_${badge(a[0], constants)}"
        "CountBadgesAcquired" -> out += "ds_countbadges ${a[0]}"
        "HealParty" -> out += "special HealPlayerParty"
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
        "CallStd" -> {
          val id = constants[a[0]] ?: a[0].toIntOrNull()
          when (id) {
            2011 -> out += "ds_martcommon"
            2052 -> {
              // std_special_mart reads the shelf index the caller put in VAR_SPECIAL_x8004.
              val items = lastVar8004?.let { dialect.martTables()[it] }
              if (items == null) out += "ds_pokemartspecialties ${lastVar8004 ?: "?"}" else out += "ds_pokemart ${items.joinToString(", ")}"
            }
            else -> out += "call NDS_CHUNK_${id ?: a[0]}"
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
          if (target != null && menuItems.isNotEmpty()) out += "ds_menu $target, $menuCursor, " + menuItems.joinToString(", ") { "${it.first}, ${it.second}" }
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
    override val scriptFiles: List<File> = emptyList()
    private val headerRows: List<IntArray> by lazy {
      File(root, "nds-headers-2.txt").readLines().filter { it.startsWith("hdr;") }.map { l -> l.split(';').drop(1).map { it.toInt() }.toIntArray() }
    }
    /** script file -> text bank of the first header using it. */
    private val bankByFile: Map<Int, Int> by lazy {
      val out = HashMap<Int, Int>()
      for (h in headerRows) out.putIfAbsent(h[4], h[6])
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
      return out
    }

    override fun textId(token: String): Int? {
      val m = Regex("^T(\\d+)_(\\d+)$").matchEntire(token) ?: return null
      return (region shl 28) or (1 shl 27) or (m.groupValues[1].toInt() shl 16) or m.groupValues[2].toInt()
    }

    override fun constants(): Map<String, Int> = emptyMap()

    /**
     * Script ids outside map files: 2000+ item balls (file 864, one entry per ball), 2800+ the
     * standard routines CallStd names (file 862: 2805 bag-space check, 2811 obtain item),
     * 10000+ hidden items (file 865).
     */
    override fun chunkFiles(): Map<Int, String> = mapOf(2000 to "U864", 2800 to "U862", 3000 to "UTR", 10000 to "U865")

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
        for (c in cmds.values) {
          if (c.offset in targets || current == null) {
            current = Block(lab(c.offset), false, mutableListOf()).also { blocks += it }
            stack.clear()
          }
          val b = current
          val a = c.args
          fun t(i: Int) = a.getOrElse(i) { "0" }
          fun v(i: Int) = "VAR_0x" + t(i).toInt().toString(16).uppercase()
          fun fl(i: Int) = "FLAG_" + t(i)
          /** A value-or-var argument: Gen 5 passes vars (0x4000+) where a value is expected. */
          fun tv(i: Int) = if ((t(i).toIntOrNull() ?: 0) >= 0x4000) v(i) else t(i)
          fun jump(i: Int) = lab(t(i).drop(1).toInt())
          fun text(i: Int) = "T%04d_%05d".format(bank, t(i).toInt())
          fun cond(code: Int, negate: Boolean): String {
            val names = listOf("Lt", "Eq", "Gt", "Le", "Ge", "Ne")
            val neg = listOf(4, 5, 3, 2, 0, 1)
            val k = code.coerceIn(0, 5)
            return names[if (negate) neg[k] else k]
          }
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
            "CheckItemBagNumber" -> b.lines += listOf("GetItemQuantity", tv(0), v(1))
            "Screen_B5", "CMD_146", "CMD_400", "CMD_103", "CMD_127", "CMD_190", "CMD_78", "CMD_1B5", "CMD_9F", "CMD_220",
            "CMD_1F0", "CMD_24C", "CMD_4E", "GetDerefVar06", "CMD_1A8", "CMD_129", "CMD_12A", "CMD_144", "CMD_248", "CMD_187", "CMD_189" -> {}
            "SetVarItem", "SetVarItem2", "SetVarItem3" -> b.lines += listOf("BufferItemName", t(0), tv(1))
            "CloseShowMessageAt" -> b.lines += listOf("CloseMessage")
            "SetVarBag", "CMD_6A", "CMD_19F" -> {}
            "CMD_BB" -> b.lines += listOf("GetItemPocket", v(0), v(1))
            "CMD_BA" -> b.lines += listOf("CheckItem", v(0), v(1), v(3))
            "ShowMessageAt" -> b.lines += listOf("Message", text(0))
            "SetBadge" -> b.lines += listOf("GiveBadge", t(0))
            "CMD_128", "CMD_11E", "CMD_107", "Xtransciever4", "Xtransciever5", "Xtransciever7" -> {}
            "SetVarStoreValue5C" -> b.lines += listOf("BufferNumber", t(0), v(1))
            "CallRoutine" -> b.lines += listOf("Call", jump(0))
            "Jump" -> b.lines += listOf("GoTo", jump(0))
            "ReturnAfterDelay" -> b.lines += listOf("WaitTime", t(0))
            "SetFlag" -> b.lines += listOf("SetFlag", fl(0))
            "ClearFlag" -> b.lines += listOf("ClearFlag", fl(0))
            "StoreValueInVar" -> b.lines += listOf("SetVar", v(0), t(1))
            "StoreVarInVar", "StoreDerefVarInVar" -> b.lines += listOf("CopyVar", v(0), v(1))
            "AddVars" -> b.lines += listOf("AddVar", v(0), t(1))
            "SubVars" -> b.lines += listOf("SubVar", v(0), t(1))
            "LockAll" -> b.lines += listOf("LockAll")
            "ReleaseAll" -> b.lines += listOf("ReleaseAll")
            "WaitButton" -> b.lines += listOf("WaitButton")
            "FacePlayer" -> b.lines += listOf("FacePlayer")
            "Message2", "Message", "Message3" -> b.lines += listOf("Message", text(2))
            "BubbleMessage", "EventGreyMessage", "BorderedMessage", "AngryMessage" -> b.lines += listOf("Message", text(0))
            "CloseMessageKP", "CloseMessageKP2", "CloseEventGreyMessage", "CloseBorderedMessage", "CloseAngryMessage", "CloseMusicalMessage" -> b.lines += listOf("CloseMessage")
            "YesNoBox" -> b.lines += listOf("YesNo", v(0))
            "StoreBadge" -> b.lines += listOf("CheckBadge", t(0), v(1))
            "StoreVersion" -> b.lines += listOf("GetGameVersion", v(0))
            "Store_D2" -> b.lines += listOf("SetVar", v(0), "0")
            "DoubleMessage" -> b.lines += listOf("Message", text(3))
            "CloseBubbleMessage" -> b.lines += listOf("CloseMessage")
            "StoreVarItem", "SetVarPoke", "SetVarPartyPokemonNick", "CMD_243", "CMD_13D", "CMD_17E", "CMD_1AE", "CMD_12B", "CMD_1A9", "CMD_1AD", "CMD_1B1" -> {}
            // 255 = player; 250-254 = camera/follower slots the server does not animate.
            "ApplyMovement" -> if (t(0).toInt() in 250..254) {} else b.lines += listOf("ApplyMovement", if (t(0) == "255") "obj_player" else "OBJ_" + t(0), "M${file}_" + t(1).drop(1))
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
            "GivePokemon" -> b.lines += listOf("GivePokemon", tv(0), tv(1), tv(2), v(3))
            "TakeMoney" -> b.lines += listOf("RemoveMoney", tv(0))
            "CheckMoney" -> b.lines += listOf("CheckMoney", tv(0), v(1))
            "CheckItemBagSpace" -> b.lines += listOf("CanFitItem", tv(0), tv(1), v(2))
            "StoreHeroGender" -> b.lines += listOf("GetPlayerGender", v(0))
            "FallWarp" -> b.lines += listOf("Warp", t(0), t(1), t(2), t(3))
            "MakeNPC", "ShowDiploma", "Unknown_0F", "StoreVar_CF" -> {}
            "RemoveNPC" -> if (t(0).toInt() < 250) b.lines += listOf("RemoveObject", "OBJ_" + t(0))
            "AddNPC" -> if (t(0).toInt() < 250) b.lines += listOf("AddObject", "OBJ_" + t(0))
            "SetOWPosition" -> if (t(0).toInt() < 250) b.lines += listOf("SetObjectEventPos", "OBJ_" + t(0), t(1), t(2))
            "FastWarp", "TeleportWarp" -> b.lines += listOf("Warp", t(0), t(1), t(2), t(3))
            "CallStd" -> b.lines += listOf("CallStd", t(0))
            "ShowMoneyBox", "CloseMoneyBox", "UpdateMoneyBox" -> {}
            else -> b.lines += listOf(c.name) + a.map { it.removePrefix("@") }
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
            val name =
                when (type) {
                  in 0..3 -> "Face$dir"
                  in 4..7 -> "WalkSlow$dir"
                  in 8..15 -> "WalkNormal$dir"
                  in 16..23 -> "WalkFast$dir"
                  in 24..43 -> "Delay8"
                  in 44..59 -> "WalkNormal$dir"
                  else -> continue
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
      for ((h, targets) in init) initBlocks += Block("NDS_INIT_${h}_TRANSITION", false, targets.map { listOf("Call", it) }.toMutableList<List<String>>().also { it += listOf("End") })
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
            // Gen 5 (disassembly names): sound, camera, waits with no server counterpart.
            "WaitMoment", "Nop", "Nop2", "PlaySound", "WaitSound", "WaitSoundA7", "Cry", "ChangeMusic", "FadeToDefaultMusic",
            "StartCameraEvent", "StopCameraEvent", "LockCamera", "ReleaseCamera", "MoveCamera", "EndCameraEvent", "ResetCamera",
            "CallStart", "CallEnd", "ResetScreen", "EndBattle", "DisableTrainer", "ChangeMusicVolume", "SetTextScriptMessage", "CloseMulti",
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
private fun badge(token: String, constants: Map<String, Int>): String = (constants[token] ?: token.toIntOrNull())?.toString() ?: token
