package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.script.FlagArg
import de.fiereu.openmmo.script.IntArg
import de.fiereu.openmmo.script.LabelArg
import de.fiereu.openmmo.script.MovementArg
import de.fiereu.openmmo.script.ObjectArg
import de.fiereu.openmmo.script.ScriptInstruction
import de.fiereu.openmmo.script.ScriptProgram
import de.fiereu.openmmo.script.SymbolArg
import de.fiereu.openmmo.script.TextArg
import de.fiereu.openmmo.script.TrainerArg
import de.fiereu.openmmo.script.VarArg
import de.fiereu.openmmo.server.game.script.MovementStep
import de.fiereu.openmmo.trainer.TrainerRegistry
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

data class ScriptSupport(
    val complete: Boolean,
    val reason: String? = null,
) {
  companion object {
    val COMPLETE = ScriptSupport(complete = true)

    fun incomplete(reason: String) = ScriptSupport(complete = false, reason = reason)
  }
}

/**
 * Conservatively proves that every instruction reachable from a program's entry point has the
 * commands and source resources the current interpreter needs. It never executes script logic.
 */
class ScriptSupportAnalyzer(
    private val trainers: TrainerRegistry = TrainerRegistry(),
    private val items: de.fiereu.openmmo.items.ItemRegistry =
        de.fiereu.openmmo.items.ItemRegistry(),
) {
  private val cache = ConcurrentHashMap<InterpretedScript, ScriptSupport>()

  fun analyze(script: InterpretedScript): ScriptSupport =
      cache.computeIfAbsent(script, ::analyzeUncached)

  private fun analyzeUncached(script: InterpretedScript): ScriptSupport {
    val root = script.program
    if (root.instructions.isEmpty()) return ScriptSupport.incomplete("program has no instructions")

    val pending = ArrayDeque<ProgramLocation>()
    val visited = mutableSetOf<Pair<String, Int>>()
    pending.add(ProgramLocation(root, 0))
    while (pending.isNotEmpty()) {
      val location = pending.removeFirst()
      val activeProgram = location.program
      val pc = location.pc
      if (pc == activeProgram.instructions.size || !visited.add(activeProgram.id.stable to pc))
          continue
      if (pc !in activeProgram.instructions.indices) {
        return ScriptSupport.incomplete(
            "control flow reaches invalid instruction $pc in ${activeProgram.id.stable}")
      }
      val instruction = activeProgram.instructions[pc]
      validateInstruction(script, activeProgram, instruction)?.let {
        return ScriptSupport.incomplete(it)
      }
      successors(script, activeProgram, pc, instruction)
          .fold(
              onSuccess = { pending.addAll(it) },
              onFailure = {
                return ScriptSupport.incomplete(checkNotNull(it.message))
              },
          )
    }
    return ScriptSupport.COMPLETE
  }

  private fun validateInstruction(
      script: InterpretedScript,
      activeProgram: ScriptProgram,
      instruction: ScriptInstruction,
  ): String? {
    if (instruction.command !in SUPPORTED_COMMANDS) {
      return sourceReason(instruction, "unsupported command ${instruction.command}")
    }

    instruction.args.filterIsInstance<TextArg>().forEach { text ->
      if (text.token !in script.textBindings) {
        return sourceReason(instruction, "unresolved text ${text.token}")
      }
    }
    instruction.args.filterIsInstance<MovementArg>().forEach { movementRef ->
      val movement =
          script.movementPrograms[movementRef.token]
              ?: return sourceReason(instruction, "unresolved movement ${movementRef.token}")
      movement.actions.forEach { action ->
        if (action.args.isNotEmpty() ||
            (MovementStep.fromPretCommand(action.command) == null &&
                !MovementStep.isRuntimeResolved(action.command))) {
          return "unsupported movement ${movement.id.stable}:${action.command} from " +
              "`${action.sourceLine}`"
        }
      }
    }
    instruction.args.filterIsInstance<ObjectArg>().forEach { objectRef ->
      if (!canResolveObject(activeProgram, objectRef.token) &&
          !canResolveObject(script.program, objectRef.token) &&
          script.programLibrary.values.none { objectRef.token in it.objectIds }) {
        return sourceReason(instruction, "unresolved object ${objectRef.token}")
      }
    }
    instruction.args.filterIsInstance<TrainerArg>().forEach { trainerRef ->
      validateTrainer(script, instruction, trainerRef)?.let {
        return it
      }
    }
    if (instruction.command == "pokemart") {
      validateMart(script, activeProgram, instruction)?.let {
        return it
      }
    }

    return validateShape(script, instruction) ?: validateTypes(instruction)
  }

  /** Proves the pokemart shelf label resolves to mart_item rows this build can all sell. */
  private fun validateMart(
      script: InterpretedScript,
      activeProgram: ScriptProgram,
      instruction: ScriptInstruction,
  ): String? {
    val label =
        (instruction.args.getOrNull(0) as? LabelArg)?.token
            ?: return sourceReason(instruction, "pokemart requires a shelf label")
    val holder =
        if (activeProgram.labels.containsKey(label)) activeProgram
        else
            script.programLibrary[label]
                ?: return sourceReason(instruction, "unresolved mart shelf $label")
    val start =
        holder.labels[label] ?: return sourceReason(instruction, "unresolved mart shelf $label")
    var index = start
    var count = 0
    while (index < holder.instructions.size && holder.instructions[index].command == "mart_item") {
      for (arg in holder.instructions[index].args) {
        val token = arg.token
        if (token == "ITEM_NONE" || token == "0") {
          return if (count > 0) null else sourceReason(instruction, "mart shelf $label is empty")
        }
        // Mail does not exist in the retail catalogue - the retail-accurate shelf simply
        // omits it, the way PokeMMO's own marts do.
        if (token.endsWith("_MAIL")) continue
        if (items.byScriptConstant(token) == null) {
          return sourceReason(instruction, "mart shelf $label offers unknown item $token")
        }
        count++
      }
      index++
    }
    return if (count > 0) null else sourceReason(instruction, "mart shelf $label has no items")
  }

  private fun validateTrainer(
      script: InterpretedScript,
      instruction: ScriptInstruction,
      trainerRef: TrainerArg,
  ): String? {
    val region =
        when (script.program.id.source) {
          "firered" -> Region.KANTO
          "emerald" -> Region.HOENN
          else ->
              return sourceReason(instruction, "unknown trainer source ${script.program.id.source}")
        }
    val trainer =
        trainers.get(region, trainerRef.token)
            ?: return sourceReason(instruction, "unresolved trainer ${trainerRef.token}")
    if (instruction.command !in setOf("trainerbattle_rematch", "trainerbattle_rematch_double")) {
      return null
    }
    val rematches = trainer.rematchIds.drop(1).filterNotNull()
    if (rematches.isEmpty()) {
      return sourceReason(instruction, "trainer ${trainerRef.token} has no rematch chain")
    }
    val missing = rematches.firstOrNull { trainers.get(region, it) == null }
    return missing?.let { sourceReason(instruction, "unresolved rematch trainer id $it") }
  }

  private fun validateShape(script: InterpretedScript, instruction: ScriptInstruction): String? {
    val args = instruction.args
    val expected =
        when (instruction.command) {
          in InterpreterSupport.NOOP_COMMANDS -> true
          in InterpreterSupport.ITEM_COMMANDS -> args.size in 1..2
          "delay",
          "special",
          "removeobject",
          "addobject" -> args.size == 1
          "random" -> args.size == 1
          "specialvar",
          "getplayerxy",
          "trainerbattle_no_intro",
          "showobjectat",
          "hideobjectat",
          "setobjectmovementtype" -> args.size == 2
          in InterpreterSupport.DEFEATED_BRANCHES -> args.size == 2
          "giveitem_msg", "msgreceiveditem" -> args.size in 2..4
          "setobjectxy",
          "setobjectxyperm",
          "warp" -> args.size in 3..4
          "setdynamicwarp" -> args.size == 4
          "multichoice" -> args.size == 4
          "multichoicedefault",
          "multichoicegrid" -> args.size == 5
          "checkpartymove" -> args.size == 1
          "setflashlevel" -> args.size == 1
          "setmetatile" -> args.size == 4
          "checkmoney" -> args.size in 1..2
          "addmoney",
          "removemoney" -> args.size in 1..2
          "givemon" -> args.size in 2..3
          "braillemessage" -> args.size == 1
          "getpartysize",
          "dowildbattle" -> args.isEmpty()
          "setwildbattle" -> args.size in 2..3
          "trainerbattle_earlyrival" -> args.size == 4
          "copyobjectxytoperm" -> args.size == 1
          "map_script" -> args.size == 2
          "map_script_2" -> args.size == 3
          in InterpreterSupport.BUFFER_COMMANDS -> args.size == 2
          "checkplayergender" -> args.isEmpty()
          "end",
          "return",
          "lock",
          "lockall",
          "release",
          "releaseall",
          "closemessage",
          "waitmessage",
          "waitbuttonpress",
          "signmsg",
          "normalmsg",
          "faceplayer" -> args.isEmpty()
          "message",
          "textcolor",
          "setflag",
          "setworldmapflag",
          "clearflag",
          "goto",
          "call" -> args.size == 1
          "msgbox" -> args.size in 1..2
          "yesnobox",
          "setvar",
          "copyvar",
          "setorcopyvar",
          "addvar",
          "subvar",
          "compare",
          "applymovement",
          "goto_if_set",
          "goto_if_unset",
          "call_if_set",
          "call_if_unset" -> args.size == 2
          "waitmovement" -> args.size <= 1
          "ds_yesno", "ds_getplayerdir", "ds_getweekday" -> args.size == 1
          "ds_flagtovar" -> args.size == 2
          "ds_warp" -> args.size == 3
          // ds_menu VAR, cursor, (textId, value)+ ; ds_setdynamicwarp header, x, y ; ds_dynamicwarpfloor VAR
          "ds_menu" -> args.size >= 4 && args.size % 2 == 0
          "ds_setdynamicwarp" -> args.size == 3
          "ds_dynamicwarpfloor" -> args.size == 1
          "ds_trainerbattle", "ds_settrainerflag", "ds_cleartrainerflag" -> args.size == 1
          "ds_checktrainerflag", "ds_trainermsg" -> args.size == 2
          "ds_trainermsgtypes", "ds_trainermsgtypes_rematch" -> args.size == 3
          "ds_martcommon" -> args.isEmpty()
          "ds_buffer" -> args.size in 2..3
          "ds_pokemart" -> args.isNotEmpty()
          "ds_countbadges" -> args.size == 1
          "trainerbattle_single" -> args.size in setOf(3, 4, 5)
          "trainerbattle_rematch" -> args.size == 3
          "trainerbattle_double" -> args.size in setOf(4, 5, 6)
          "trainerbattle_rematch_double" -> args.size == 4
          "pokemart" -> args.size == 1
          "mart_item" -> args.isNotEmpty()
          in COMPARISON_BRANCHES -> args.size == 1 || args.size == 3
          else -> false
        }
    if (!expected) return sourceReason(instruction, "unsupported argument shape")

    if (instruction.command == "yesnobox") {
      val invalid = args.filterIsInstance<IntArg>().firstOrNull { it.value !in 0..0xFF }
      if (invalid != null || args.size != args.filterIsInstance<IntArg>().size) {
        return sourceReason(instruction, "yesnobox requires two byte arguments")
      }
    }
    if (instruction.command == "special" &&
        args[0].token !in InterpreterSupport.SUPPORTED_SPECIALS) {
      return sourceReason(instruction, "unsupported special ${args[0].token}")
    }
    if (instruction.command == "specialvar" &&
        args[1].token !in InterpreterSupport.SPECIALVAR_RESULTS &&
        args[1].token !in InterpreterSupport.IMPLEMENTED_SPECIALVARS) {
      return sourceReason(instruction, "unsupported specialvar ${args[1].token}")
    }
    if (instruction.command in InterpreterSupport.ITEM_COMMANDS) {
      // DS item balls pass the item and count in vars (VAR_0x8008/9); those resolve at run time.
      if (args[0] !is VarArg && items.byScriptConstant(args[0].token) == null) {
        return sourceReason(instruction, "unresolved item ${args[0].token}")
      }
      if (args.size == 2 && args[1] !is IntArg && args[1] !is VarArg) {
        return sourceReason(instruction, "unsupported item count ${args[1].token}")
      }
    }
    if (instruction.command == "giveitem_msg" || instruction.command == "msgreceiveditem") {
      if (items.byScriptConstant(args[1].token) == null) {
        return sourceReason(instruction, "unresolved item ${args[1].token}")
      }
      if (args.size == 3 && args[2] !is IntArg) {
        return sourceReason(instruction, "unsupported item count ${args[2].token}")
      }
    }
    if (instruction.command == "msgbox" && args.size == 2) {
      val supported = args[1].token in SUPPORTED_MSGBOX_TYPES
      if (!supported) return sourceReason(instruction, "unsupported msgbox type ${args[1].token}")
    }
    if (instruction.command == "textcolor" && script.program.id.source == "firered") {
      val supported =
          args.single().token in
              setOf(
                  "NPC_TEXT_COLOR_MALE",
                  "NPC_TEXT_COLOR_FEMALE",
                  "NPC_TEXT_COLOR_MON",
                  "NPC_TEXT_COLOR_NEUTRAL",
                  "NPC_TEXT_COLOR_DEFAULT",
                  "0",
                  "1",
                  "2",
                  "3",
                  "255",
                  "0xFF",
              )
      if (!supported)
          return sourceReason(instruction, "unsupported textcolor ${args.single().token}")
    }
    return null
  }

  private fun validateTypes(instruction: ScriptInstruction): String? {
    val args = instruction.args
    val valid =
        when (instruction.command) {
          "msgbox",
          "message" -> args[0] is TextArg
          "setflag",
          "setworldmapflag",
          "clearflag" -> args[0] is FlagArg
          "delay" -> args[0] is IntArg
          "specialvar" -> args[0] is VarArg
          "getplayerxy" -> args.all { it is VarArg }
          "random" -> args[0] is IntArg
          "removeobject",
          "addobject",
          "showobjectat",
          "hideobjectat" -> args[0] is ObjectArg
          "setobjectxy",
          "setobjectxyperm" -> args[0] is ObjectArg && args[1] is IntArg && args[2] is IntArg
          // The parser types the first argument as an object only for a fixed command list; the
          // interpreter rebuilds the ObjectArg from the token (as copyobjectxytoperm does).
          "setobjectmovementtype" -> args.size == 2
          "warp" -> args.all { it is IntArg }
          "setdynamicwarp" -> args.all { it is IntArg }
          "trainerbattle_no_intro" -> args[0] is TrainerArg && args[1] is TextArg
          "trainerbattle_earlyrival" -> args[0] is TrainerArg && args[2] is TextArg
          "givemon" -> isValue(args[0]) && isValue(args[1])
          "braillemessage" -> args[0] is TextArg
          "copyobjectxytoperm" -> args[0] is ObjectArg || (args[0] is SymbolArg && args[0].token.startsWith("LOCALID_"))
          "checkmoney",
          "addmoney",
          "removemoney" -> isValue(args[0])
          "setwildbattle" -> isValue(args[0]) && isValue(args[1]) && (args.size < 3 || isValue(args[2]))
          in InterpreterSupport.DEFEATED_BRANCHES -> args[0] is TrainerArg && args[1] is LabelArg
          // A flag symbol is a value too: FireRed's Silph doors park the door's flag in VAR_0x8004
          // for `special SetHiddenItemFlag` (silphco_doors.inc).
          "setvar",
          "setorcopyvar" -> args[0] is VarArg && (isValue(args[1]) || args[1] is FlagArg)
          "subvar" -> args[0] is VarArg && isValue(args[1])
          "copyvar" -> args.all { it is VarArg }
          "addvar" -> args[0] is VarArg && isImmediate(args[1])
          "compare" -> args.all(::isValue)
          "goto",
          "call" -> args[0] is LabelArg
          in COMPARISON_BRANCHES ->
              if (args.size == 1) args[0] is LabelArg
              else isValue(args[0]) && isValue(args[1]) && args[2] is LabelArg
          in FLAG_BRANCHES -> args[0] is FlagArg && args[1] is LabelArg
          "applymovement" -> args[0] is ObjectArg && args[1] is MovementArg
          "waitmovement" -> args.isEmpty() || args[0] is ObjectArg
          "trainerbattle_single" ->
              args[0] is TrainerArg &&
                  args[1] is TextArg &&
                  args[2] is TextArg &&
                  (args.size < 4 || args[3] is LabelArg) &&
                  (args.size < 5 || args[4].token in SUPPORTED_TRAINER_MUSIC)
          "trainerbattle_rematch" ->
              args[0] is TrainerArg && args[1] is TextArg && args[2] is TextArg
          "trainerbattle_double" ->
              args[0] is TrainerArg &&
                  args[1] is TextArg &&
                  args[2] is TextArg &&
                  args[3] is TextArg &&
                  (args.size < 5 || args[4] is LabelArg) &&
                  (args.size < 6 || args[5].token in SUPPORTED_TRAINER_MUSIC)
          "trainerbattle_rematch_double" ->
              args[0] is TrainerArg &&
                  args[1] is TextArg &&
                  args[2] is TextArg &&
                  args[3] is TextArg
          "pokemart" -> args[0] is LabelArg
          else -> true
        }
    return if (valid) null else sourceReason(instruction, "unsupported argument types")
  }

  private fun isValue(arg: de.fiereu.openmmo.script.ScriptArg): Boolean =
      arg is IntArg ||
          arg is VarArg ||
          // A map-local object id as a value (setvar VAR_LAST_TALKED, LOCALID_X): the executor
          // resolves it through the program's object table.
          (arg is SymbolArg && (arg.token in BOOLEAN_SYMBOLS || arg.token.startsWith("LOCALID_")))

  private fun isImmediate(arg: de.fiereu.openmmo.script.ScriptArg): Boolean =
      arg is IntArg || (arg is SymbolArg && arg.token in BOOLEAN_SYMBOLS)

  private fun successors(
      script: InterpretedScript,
      activeProgram: ScriptProgram,
      pc: Int,
      instruction: ScriptInstruction,
  ): Result<List<ProgramLocation>> = runCatching {
    val next = ProgramLocation(activeProgram, pc + 1)
    when (instruction.command) {
      "end",
      "return" -> emptyList()
      "goto" -> listOf(target(script, activeProgram, instruction, 0))
      "call" -> listOf(target(script, activeProgram, instruction, 0), next)
      in COMPARISON_BRANCHES ->
          listOf(
              target(
                  script,
                  activeProgram,
                  instruction,
                  if (instruction.args.size == 1) 0 else 2,
              ),
              next,
          )
      in FLAG_BRANCHES -> listOf(target(script, activeProgram, instruction, 1), next)
      in InterpreterSupport.DEFEATED_BRANCHES ->
          listOf(target(script, activeProgram, instruction, 1), next)
      "trainerbattle_single" -> {
        val continuation = instruction.args.getOrNull(3) as? LabelArg
        if (continuation == null || continuation.token == "FALSE") listOf(next)
        else listOf(target(script, activeProgram, instruction, 3), next)
      }
      "trainerbattle_double" -> {
        val continuation = instruction.args.getOrNull(4) as? LabelArg
        if (continuation == null || continuation.token == "FALSE") listOf(next)
        else listOf(target(script, activeProgram, instruction, 4), next)
      }
      else -> listOf(next)
    }
  }

  private fun target(
      script: InterpretedScript,
      activeProgram: ScriptProgram,
      instruction: ScriptInstruction,
      index: Int,
  ): ProgramLocation {
    val label =
        instruction.args.getOrNull(index) as? LabelArg
            ?: error(
                "${instruction.command} has no label argument from `${instruction.sourceLine}`")
    activeProgram.labels[label.token]?.let {
      return ProgramLocation(activeProgram, it)
    }
    val dependency =
        script.programLibrary[label.token]
            ?: error("unresolved label ${label.token} from `${instruction.sourceLine}`")
    check(
        dependency.id.source == script.program.id.source &&
            dependency.id.gameCode == script.program.id.gameCode) {
          "cross-source label ${label.token} from `${instruction.sourceLine}`"
        }
    return ProgramLocation(dependency, dependency.labels[label.token] ?: 0)
  }

  private fun canResolveObject(program: ScriptProgram, token: String): Boolean =
      token in setOf("LOCALID_NONE", "LOCALID_PLAYER", "LOCALID_CAMERA", "VAR_LAST_TALKED") ||
          token.startsWith("VAR_") ||
          token in program.objectIds ||
          sourceInt(token) != null

  private fun sourceInt(token: String): Int? =
      if (token.startsWith("0x", ignoreCase = true)) token.drop(2).toIntOrNull(16)
      else token.toIntOrNull()

  private fun sourceReason(instruction: ScriptInstruction, reason: String): String =
      "$reason from `${instruction.sourceLine}`"

  private data class ProgramLocation(
      val program: ScriptProgram,
      val pc: Int,
  )

  private companion object {
    val COMPARISON_BRANCHES =
        setOf(
            "goto_if_eq",
            "goto_if_ne",
            "goto_if_lt",
            "goto_if_le",
            "goto_if_gt",
            "goto_if_ge",
            "call_if_eq",
            "call_if_ne",
            "call_if_lt",
            "call_if_le",
            "call_if_gt",
            "call_if_ge",
        )
    val FLAG_BRANCHES = setOf("goto_if_set", "goto_if_unset", "call_if_set", "call_if_unset")
    val BOOLEAN_SYMBOLS = setOf("TRUE", "FALSE")
    val SUPPORTED_MSGBOX_TYPES =
        setOf(
            "MSGBOX_NPC",
            "MSGBOX_SIGN",
            "MSGBOX_DEFAULT",
            "MSGBOX_AUTOCLOSE",
            "MSGBOX_YESNO",
            "2",
            "4",
            "5",
            "6",
        )
    val SUPPORTED_TRAINER_MUSIC = setOf("NO_MUSIC", "FALSE", "TRUE")
    val SUPPORTED_COMMANDS =
        setOf(
            "ds_yesno",
            "ds_getplayerdir",
            "ds_getweekday",
            "ds_flagtovar",
            "ds_warp",
            "ds_menu",
            "ds_setdynamicwarp",
            "ds_dynamicwarpfloor",
            "ds_trainerbattle",
            "ds_trainermsg",
            "ds_trainermsgtypes",
            "ds_trainermsgtypes_rematch",
            "ds_martcommon",
            "ds_buffer",
            "ds_pokemart",
            "ds_countbadges",
            "ds_checktrainerflag",
            "ds_settrainerflag",
            "ds_cleartrainerflag",
            "msgbox",
            "message",
            "lock",
            "lockall",
            "release",
            "releaseall",
            "closemessage",
            "waitmessage",
            "waitbuttonpress",
            "yesnobox",
            "textcolor",
            "signmsg",
            "normalmsg",
            "applymovement",
            "waitmovement",
            "faceplayer",
            "trainerbattle_single",
            "trainerbattle_rematch",
            "trainerbattle_double",
            "trainerbattle_rematch_double",
            "pokemart",
            "mart_item",
            "setflag",
            "setworldmapflag",
            "clearflag",
            "setvar",
            "copyvar",
            "setorcopyvar",
            "addvar",
            "subvar",
            "compare",
            "call",
            "goto",
            "return",
            "end",
            "delay",
            "special",
            "specialvar",
            "multichoice",
            "multichoicedefault",
            "multichoicegrid",
            "checkpartymove",
            "setflashlevel",
            "setmetatile",
            "checkmoney",
            "addmoney",
            "removemoney",
            "givemon",
            "braillemessage",
            "getpartysize",
            "setwildbattle",
            "dowildbattle",
            "trainerbattle_earlyrival",
            "copyobjectxytoperm",
            "map_script",
            "map_script_2",
            "removeobject",
            "addobject",
            "checkplayergender",
            "getplayerxy",
            "random",
            "giveitem_msg",
            "msgreceiveditem",
            "setobjectxy",
            "setobjectxyperm",
            "setobjectmovementtype",
            "showobjectat",
            "hideobjectat",
            "trainerbattle_no_intro",
            "warp",
            "setdynamicwarp",
        ) +
            InterpreterSupport.DEFEATED_BRANCHES +
            COMPARISON_BRANCHES +
            FLAG_BRANCHES +
            InterpreterSupport.NOOP_COMMANDS +
            InterpreterSupport.BUFFER_COMMANDS +
            InterpreterSupport.ITEM_COMMANDS
  }
}
