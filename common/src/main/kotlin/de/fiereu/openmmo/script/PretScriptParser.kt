package de.fiereu.openmmo.script

/** Minimal parser for the human-readable pret GBA script assembly used by FireRed/Emerald. */
object PretScriptParser {
  private val labelLine = Regex("^(\\w+)::?\\s*$")
  private val comparisonBranches =
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
  private val flagBranches = setOf("goto_if_set", "goto_if_unset", "call_if_set", "call_if_unset")
  private val defeatedBranches =
      setOf(
          "goto_if_defeated",
          "goto_if_not_defeated",
          "call_if_defeated",
          "call_if_not_defeated",
      )

  fun parse(
      id: ScriptId,
      storyNamespace: String,
      sourceFile: String,
      lines: List<String>,
      objectIds: Map<String, Int> = emptyMap(),
      constants: Map<String, Int> = emptyMap(),
  ): ScriptProgram {
    val labels = linkedMapOf<String, Int>()
    val instructions = mutableListOf<ScriptInstruction>()
    for (raw in lines) {
      val line = raw.substringBefore('@').trim()
      if (line.isEmpty()) continue
      val label = labelLine.matchEntire(line)
      if (label != null) {
        labels[label.groupValues[1]] = instructions.size
        continue
      }
      if (line.startsWith(".")) continue
      val command = line.substringBefore(' ').substringBefore('\t').trim()
      val rest = line.removePrefix(command).trim()
      instructions += parseInstructions(command, splitArgs(rest), line, constants)
    }
    return ScriptProgram(id, storyNamespace, sourceFile, labels, instructions, objectIds)
  }

  private fun parseInstructions(
      command: String,
      args: List<String>,
      sourceLine: String,
      constants: Map<String, Int>,
  ): List<ScriptInstruction> =
      when (command) {
        "switch" -> {
          requireArgs(command, args, expected = 1, sourceLine)
          listOf(instruction("copyvar", listOf("VAR_0x8000", args[0]), sourceLine, constants))
        }
        "case" -> {
          // FireRed's pkmn_center_nurse.inc has `case 1 EventScript_...` with the comma missing,
          // and the games' own assembler accepts it. Repair that one shape instead of dropping
          // every Pokemon Center nurse behind it.
          val repaired =
              if (args.size == 1 && ' ' in args[0]) args[0].split(Regex("\\s+"), limit = 2)
              else args
          requireArgs(command, repaired, expected = 2, sourceLine)
          listOf(
              instruction("compare", listOf("VAR_0x8000", repaired[0]), sourceLine, constants),
              instruction("goto_if_eq", listOf(repaired[1]), sourceLine, constants),
          )
        }
        else -> listOf(instruction(command, args, sourceLine, constants))
      }

  private fun instruction(
      command: String,
      args: List<String>,
      sourceLine: String,
      constants: Map<String, Int>,
  ): ScriptInstruction = ScriptInstruction(command, parseArgs(command, args, constants), sourceLine)

  private fun requireArgs(
      command: String,
      args: List<String>,
      expected: Int,
      sourceLine: String,
  ) =
      require(args.size == expected) {
        "$command expected $expected arguments, got ${args.size} in `$sourceLine`"
      }

  private fun parseArgs(
      command: String,
      args: List<String>,
      constants: Map<String, Int>,
  ): List<ScriptArg> =
      args.mapIndexed { index, token ->
        parseArg(command, index, token.trim(), args.size, constants)
      }

  private fun parseArg(
      command: String,
      index: Int,
      token: String,
      argumentCount: Int,
      constants: Map<String, Int>,
  ): ScriptArg =
      when {
        token.isEmpty() -> SymbolArg(token)
        command in setOf("msgbox", "message") && index == 0 -> TextArg(token)
        command == "applymovement" && index == 0 -> ObjectArg(token)
        command == "applymovement" && index == 1 -> MovementArg(token)
        command == "waitmovement" && index == 0 -> ObjectArg(token)
        command in
            setOf(
                "removeobject",
                "addobject",
                "setobjectxy",
                "setobjectxyperm",
                "showobjectat",
                "hideobjectat") && index == 0 -> ObjectArg(token)
        command == "giveitem_msg" && index == 0 -> TextArg(token)
        command in defeatedBranches && index == 0 -> TrainerArg(token)
        command in defeatedBranches && index == 1 -> LabelArg(token)
        command in TRAINER_BATTLE_COMMANDS && index == 0 -> TrainerArg(token)
        command in TRAINER_BATTLE_COMMANDS && index in 1..2 -> TextArg(token)
        command == "trainerbattle_single" && index == 3 -> LabelArg(token)
        // The double macros carry the NotEnoughMons text at 3 and the continuation at 4.
        command in DOUBLE_BATTLE_COMMANDS && index == 3 -> TextArg(token)
        command == "trainerbattle_double" && index == 4 -> LabelArg(token)
        command in setOf("setflag", "clearflag") + flagBranches && index == 0 -> FlagArg(token)
        command in setOf("setvar", "compare", "setorcopyvar", "addvar", "subvar") && index == 0 ->
            VarArg(token)
        command == "copyvar" -> VarArg(token)
        command in setOf("goto", "call") -> LabelArg(token)
        command in comparisonBranches && argumentCount == 1 && index == 0 -> LabelArg(token)
        command in comparisonBranches && argumentCount == 3 && index == 0 -> VarArg(token)
        command in comparisonBranches && argumentCount == 3 && index == 2 -> LabelArg(token)
        command in flagBranches && index == 1 -> LabelArg(token)
        token.startsWith("FLAG_") -> FlagArg(token)
        token.startsWith("VAR_") -> VarArg(token)
        intValue(token) != null -> IntArg(intValue(token)!!, token)
        constants[token] != null -> IntArg(constants[token]!!, token)
        else -> SymbolArg(token)
      }

  private fun intValue(token: String): Int? =
      when (token) {
        "TRUE",
        "YES" -> 1
        "FALSE",
        "NO" -> 0
        // FireRed multichoice cancel sentinel (include/constants/menu.h).
        "SCR_MENU_CANCEL" -> 127
        else ->
            if (token.startsWith("0x", ignoreCase = true)) token.drop(2).toIntOrNull(16)
            else token.toIntOrNull()
      }

  private fun splitArgs(rest: String): List<String> {
    if (rest.isBlank()) return emptyList()
    val out = mutableListOf<String>()
    val current = StringBuilder()
    var parens = 0
    for (ch in rest) {
      when (ch) {
        '(' -> {
          parens++
          current.append(ch)
        }
        ')' -> {
          parens--
          current.append(ch)
        }
        ',' -> {
          if (parens == 0) {
            out += current.toString().trim()
            current.clear()
          } else {
            current.append(ch)
          }
        }
        else -> current.append(ch)
      }
    }
    out += current.toString().trim()
    return out.filter { it.isNotEmpty() }
  }

  private val DOUBLE_BATTLE_COMMANDS = setOf("trainerbattle_double", "trainerbattle_rematch_double")

  private val TRAINER_BATTLE_COMMANDS =
      setOf("trainerbattle_single", "trainerbattle_rematch", "trainerbattle_no_intro") +
          DOUBLE_BATTLE_COMMANDS
}
