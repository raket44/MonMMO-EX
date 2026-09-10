package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.openmmo.common.enums.Direction

import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.script.FlagArg
import de.fiereu.openmmo.script.IntArg
import de.fiereu.openmmo.script.LabelArg
import de.fiereu.openmmo.script.MovementArg
import de.fiereu.openmmo.script.MovementProgram
import de.fiereu.openmmo.script.ObjectArg
import de.fiereu.openmmo.script.ScriptArg
import de.fiereu.openmmo.script.ScriptInstruction
import de.fiereu.openmmo.script.ScriptProgram
import de.fiereu.openmmo.script.SymbolArg
import de.fiereu.openmmo.script.TextArg
import de.fiereu.openmmo.script.TrainerArg
import de.fiereu.openmmo.script.VarArg
import de.fiereu.openmmo.net.game.packets.dialog.DialogMessageArg
import de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.script.MovementStep
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.session.DialogMessageMode
import de.fiereu.openmmo.server.game.session.DialogTextColor
import de.fiereu.openmmo.trainer.TrainerDef
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.ArrayDeque
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay

private val log = KotlinLogging.logger {}


class UnsupportedScriptCommandException(
    scriptId: String,
    command: String,
    sourceLine: String,
) : IllegalStateException("Unsupported script command in $scriptId: $command from `$sourceLine`")

class UnsupportedMovementActionException(
    movementProgramId: String,
    action: String,
    sourceLine: String,
) :
    IllegalStateException(
        "Unsupported movement action in $movementProgramId: $action from `$sourceLine`")

class InterpretedScript(
    val program: ScriptProgram,
    internal val textBindings: Map<String, DialogLine>,
    internal val movementPrograms: Map<String, MovementProgram> = emptyMap(),
    internal val programLibrary: Map<String, ScriptProgram> = emptyMap(),
    /** The game's scripted menus as option texts, by constant (corpus MenuIndex). */
    internal val menus: Map<String, List<String>> = emptyMap(),
    /** DS elevator floor by map header id (decimal key), from the game's own table (corpus). */
    internal val headerFloors: Map<String, Int> = emptyMap(),
) : Script {
  override suspend fun run(ctx: ScriptContext) {
    log.info { "[Interpreter] Running ${program.id.stable}" }
    if (program.id.source == "firered") {
      // ProcessPlayerFieldInput resets these before dispatching a new FRLG field script.
      ctx.setDialogMessageMode(DialogMessageMode.NORMAL)
      ctx.setDialogTextColor(DialogTextColor.DEFAULT)
    }
    val state = RuntimeState(activeProgram = program)
    var steps = 0
    while (state.pc in state.activeProgram.instructions.indices) {
      check(++steps <= MAX_STEPS) { "Script ${program.id.stable} exceeded $MAX_STEPS steps" }
      val instruction = state.activeProgram.instructions[state.pc]
      ctx.traceInterpreter {
        "${program.id.stable} pc=${state.pc} command=${instruction.command} " +
            "source=`${instruction.sourceLine}`"
      }
      when (instruction.command) {
        "msgbox" -> {
          // The HM "used" box is the client's banner here, sent at the dofieldeffect that follows.
          if (textArg(instruction, 0).token !in InterpreterSupport.USED_MOVE_TEXTS) {
            state.currentMessage = tracedWait(ctx, "dialog") { runMsgbox(ctx, instruction) }
          }
          state.pc++
        }
        "message" -> {
          // A multichoice right after re-presents the same text with the choice attached, so
          // showing it here too would put the identical dialog up twice in a row.
          if (multichoiceFollows(state)) {
            state.currentMessage = textLine(textArg(instruction, 0).token, instruction)
            // Nothing was shown, so the waitmessage between here and the multichoice would
            // wait forever on a dialog that never opened.
            state.skipNextWaitMessage = true
          } else {
            state.currentMessage = runMessage(ctx, instruction)
          }
          state.pc++
        }
        "lock" -> {
          ctx.lock()
          state.pc++
        }
        "lockall" -> {
          ctx.lockAll()
          state.pc++
        }
        "release" -> {
          ctx.release()
          state.pc++
        }
        "releaseall" -> {
          ctx.releaseAll()
          state.pc++
        }
        "closemessage" -> {
          ctx.closeMessage()
          state.pc++
        }
        "waitmessage" -> {
          if (state.skipNextWaitMessage) state.skipNextWaitMessage = false
          else tracedWait(ctx, "dialog") { ctx.waitMessage() }
          state.pc++
        }
        "waitbuttonpress" -> {
          requireNoArgs(instruction)
          tracedWait(ctx, "dialog button") { ctx.waitButtonPress() }
          state.pc++
        }
        "yesnobox" -> {
          tracedWait(ctx, "dialog choice") { runYesNoBox(ctx, state, instruction) }
          state.pc++
        }
        // Gen 4 yes/no writes 0 for YES and 1 for NO into a named var; yesnobox leaves 1 for yes.
        "ds_yesno" -> {
          val yes = ctx.getVar(namespaced("VAR_RESULT")) == 1
          ctx.setVar(namespaced(varArg(instruction, 0).token), if (yes) 0 else 1)
          state.pc++
        }
        // Gen 4 facing codes: 0 up, 1 down, 2 left, 3 right.
        "ds_getplayerdir" -> {
          val code =
              when (ctx.facingDirection) {
                Direction.UP -> 0
                Direction.DOWN -> 1
                Direction.LEFT -> 2
                else -> 3
              }
          ctx.setVar(namespaced(varArg(instruction, 0).token), code)
          state.pc++
        }
        // Gen 4 weekdays count from Sunday = 0.
        "ds_getweekday" -> {
          ctx.setVar(namespaced(varArg(instruction, 0).token), java.time.LocalDate.now().dayOfWeek.value % 7)
          state.pc++
        }
        "ds_flagtovar" -> {
          val set = ctx.isFlagSet(namespaced(flagArg(instruction, 0).token))
          ctx.setVar(namespaced(varArg(instruction, 1).token), if (set) 1 else 0)
          state.pc++
        }
        // Gen 4 trainer battles name the trainer by ROM id (a constant or, for Platinum's shared
        // battle script, VAR_0x8004). The region's trainer table resolves it; a win sets the
        // synthetic defeated flag and VAR_RESULT = 1, anything else 0.
        "ds_trainerbattle" -> {
          val arg = instruction.arg(0)
          val id = if (arg is VarArg) ctx.getVar(namespaced(arg.token)) else (arg as IntArg).value
          val trainer = ctx.resolveTrainerById(id)
          // The in-battle defeat line (kind 1) rides the battle end packet like the GBA macro's.
          val result = tracedWait(ctx, "ds_trainerbattle $id") { ctx.trainerBattle(trainer, trainerMessage(trainer, 1)) }
          val won = result == BattleResult.VICTORY
          if (won) ctx.setFlag(namespaced("FLAG_DS_TRAINER_$id"))
          ctx.setVar(namespaced("VAR_RESULT"), if (won) 1 else 0)
          state.pc++
        }
        // DS trainer speech: the ROM trainer message table, (trainer, kind) to a client text id.
        "ds_trainermsg" -> {
          val trainer = ctx.resolveTrainerById(value(ctx, instruction.arg(0)))
          val kind = value(ctx, instruction.arg(1))
          val textId = trainerMessage(trainer, kind)
          if (textId == null) log.warn { "Script ${program.id.stable}: trainer ${trainer.id} has no speech of kind $kind" }
          else tracedWait(ctx, "trainer dialog") { ctx.say(TrainerLine(textId)) }
          state.pc++
        }
        // The shared battle script asks which kinds apply (pre, post, not-enough-mons): singles
        // 0/2/0, the first double partner 3/5/6; rematch intros 17 and 18. The trainer is the
        // chunk binding's VAR_0x8004.
        "ds_trainermsgtypes",
        "ds_trainermsgtypes_rematch" -> {
          val trainer = ctx.resolveTrainerById(ctx.getVar(namespaced("VAR_0x8004")))
          val rematch = instruction.command.endsWith("_rematch")
          val kinds =
              if (!trainer.doubleBattle) Triple(if (rematch) 17 else 0, if (rematch) 0 else 2, 0)
              else Triple(if (rematch) 18 else 3, if (rematch) 0 else 5, 6)
          ctx.setVar(namespaced(varArg(instruction, 0).token), kinds.first)
          ctx.setVar(namespaced(varArg(instruction, 1).token), kinds.second)
          ctx.setVar(namespaced(varArg(instruction, 2).token), kinds.third)
          state.pc++
        }
        "ds_checktrainerflag" -> {
          val arg = instruction.arg(0)
          val id = if (arg is VarArg) ctx.getVar(namespaced(arg.token)) else (arg as IntArg).value
          val set = ctx.isFlagSet(namespaced("FLAG_DS_TRAINER_$id"))
          ctx.setVar(namespaced(varArg(instruction, 1).token), if (set) 1 else 0)
          state.pc++
        }
        "ds_settrainerflag",
        "ds_cleartrainerflag" -> {
          val arg = instruction.arg(0)
          val id = if (arg is VarArg) ctx.getVar(namespaced(arg.token)) else (arg as IntArg).value
          if (instruction.command == "ds_settrainerflag") ctx.setFlag(namespaced("FLAG_DS_TRAINER_$id"))
          else ctx.clearFlag(namespaced("FLAG_DS_TRAINER_$id"))
          state.pc++
        }
        // Gen 4 marts. The common shelf is the badge-tiered table both games share; a specialty
        // shelf lists the game's own item indexes. Wire ids are region * 1000 + index.
        "ds_martcommon",
        "ds_pokemart" -> {
          val region = if (program.id.source == "heartgold") 4 else 3
          val ids =
              if (instruction.command == "ds_pokemart") instruction.args.mapNotNull { (it as? IntArg)?.value }
              else {
                val badges = (0 until 16).count { ctx.isFlagSet(namespaced("FLAG_DS_BADGE_$it")) }
                val tier = when (badges) { 0 -> 1; 1, 2 -> 2; 3, 4 -> 3; 5, 6 -> 4; 7 -> 5; else -> 6 }
                DS_COMMON_MART.filter { it.second <= tier }.map { it.first }
              }
          val shelf = ids.mapNotNull { ctx.resolveItemWire(region * 1000 + it) }
          if (shelf.isNotEmpty()) ctx.pokemart(*shelf.toTypedArray())
          else log.warn { "Script ${program.id.stable}: DS mart shelf resolved no items ($ids)" }
          return
        }
        // Gen 4 Buffer*: fill text slot N. Kinds: player/rival name (raw string 5), item name by
        // wire id (25), number (raw string of the value).
        "ds_buffer" -> {
          val slot = (instruction.arg(0) as IntArg).value
          val region = if (program.id.source == "heartgold") 4 else 3
          fun valueOf(arg: ScriptArg): Int = if (arg is VarArg) ctx.getVar(namespaced(arg.token)) else (arg as? IntArg)?.value ?: 0
          val arg: DialogMessageArg? =
              when (instruction.arg(1).token) {
                "player" -> RawMessageArg(slot.toByte(), 5, text = ctx.playerName)
                "rival" -> RawMessageArg(slot.toByte(), 5, text = if (region == 4) "Silver" else "Barry")
                "item" -> RawMessageArg(slot.toByte(), 25, shorts = listOf((region * 1000 + valueOf(instruction.arg(2))).toShort()))
                "number" -> RawMessageArg(slot.toByte(), 5, text = valueOf(instruction.arg(2)).toString())
                else -> null
              }
          arg?.let { ctx.setMessageArg(slot, it) }
          state.pc++
        }
        "ds_countbadges" -> {
          ctx.setVar(namespaced(varArg(instruction, 0).token), (0 until 16).count { ctx.isFlagSet(namespaced("FLAG_DS_BADGE_$it")) })
          state.pc++
        }
        // A DS map header id is the client's bank (low byte) and map (high byte).
        // ds_menu VAR, cursor, (textId, value)+: a DS scripted menu as the client's text-button list.
        "ds_menu" -> {
          tracedWait(ctx, "dialog choice") { runDsMenu(ctx, state, instruction) }
          state.pc++
        }
        // ds_setdynamicwarp header, x, y: SetDynamicWarp / SetSpecialLocation - where the elevator lets out.
        "ds_setdynamicwarp" -> {
          val header = value(ctx, instruction.arg(0))
          ctx.setDynamicWarp(dsRegion(), header and 0xFF, header shr 8, value(ctx, instruction.arg(1)), value(ctx, instruction.arg(2)), de.fiereu.openmmo.common.enums.Direction.DOWN)
          state.pc++
        }
        // ds_dynamicwarpfloor VAR: GetFloorsAbove / GetDynamicWarpFloorNo - the floor the dynamic warp names.
        "ds_dynamicwarpfloor" -> {
          // Platinum answers 1 for a map outside its table (the macro's own doc); HeartGold asserts and gives 0.
          val fallback = if (program.id.source == "platinum") 1 else 0
          ctx.setVar(namespaced(instruction.arg(0).token), ctx.dsDynamicWarpFloor(dsRegion(), headerFloors, fallback))
          state.pc++
        }
        "ds_warp" -> {
          val header = (instruction.arg(0) as IntArg).value
          val region = if (program.id.source == "heartgold") 4 else 3
          tracedWait(ctx, "ds_warp") {
            ctx.rawWarp(region, header and 0xFF, header shr 8, (instruction.arg(1) as IntArg).value, (instruction.arg(2) as IntArg).value)
          }
          state.pc++
        }
        "textcolor" -> {
          runTextColor(ctx, instruction)
          state.pc++
        }
        "signmsg" -> {
          requireNoArgs(instruction)
          if (program.id.source == "firered") {
            ctx.setDialogMessageMode(DialogMessageMode.SIGN)
          }
          state.pc++
        }
        "normalmsg" -> {
          requireNoArgs(instruction)
          if (program.id.source == "firered") {
            ctx.setDialogMessageMode(DialogMessageMode.NORMAL)
          }
          state.pc++
        }
        "applymovement" -> runApplyMovement(ctx, state, instruction)
        "waitmovement" -> runWaitMovement(ctx, state, instruction)
        "faceplayer" -> {
          check(instruction.args.isEmpty()) {
            "Script ${program.id.stable} expected no arguments in `${instruction.sourceLine}`"
          }
          ctx.facePlayer()
          state.pc++
        }
        "trainerbattle_single" -> {
          if (!runTrainerBattle(ctx, state, instruction, rematch = false)) return
        }
        "trainerbattle_rematch" -> {
          if (!runTrainerBattle(ctx, state, instruction, rematch = true)) return
        }
        "trainerbattle_double" -> {
          if (!runTrainerBattleDouble(ctx, state, instruction, rematch = false)) return
        }
        "trainerbattle_rematch_double" -> {
          if (!runTrainerBattleDouble(ctx, state, instruction, rematch = true)) return
        }
        "pokemart" -> {
          openMart(ctx, state, instruction)
          // Vanilla blocks until the shop window closes; nothing on the wire reports that
          // close, so the script ends with the shelf open and the come-again line is skipped.
          return
        }
        "mart_item" ->
            error(
                "Script ${program.id.stable} executed a mart data row at " +
                    "`${instruction.sourceLine}`")
        "setflag",
        // Marks a location visited on the Town Map - a persisted flag like any other. Leaving it
        // unsupported failed every town's on-enter script whole, Vermilion City's included.
        "setworldmapflag" -> {
          ctx.setFlag(namespaced(flagArg(instruction, 0).token))
          state.pc++
        }
        "clearflag" -> {
          val flag = namespaced(flagArg(instruction, 0).token)
          ctx.clearFlag(flag)
          // The GBA object system brings a hidden npc back once its flag clears - scenes rely on
          // it after removeobject + setobjectxyperm (Oak reappearing behind his desk).
          ctx.respawnNpcForClearedHideFlag(flag)
          state.pc++
        }
        "setvar" -> {
          val target = namespaced(varArg(instruction, 0).token)
          val source = instruction.arg(1)
          if (source is FlagArg) {
            // The var carries a flag for a special that sets it by id (SetHiddenItemFlag): our
            // flags are named, so the name rides beside the var and the var reads non-zero.
            ctx.rememberFlagInVar(target, namespaced(source.token))
            ctx.setVar(target, 1)
          } else {
            ctx.setVar(target, value(ctx, source))
          }
          state.pc++
        }
        "copyvar" -> {
          val source = ctx.getVar(namespaced(varArg(instruction, 1).token))
          ctx.setVar(namespaced(varArg(instruction, 0).token), source)
          state.pc++
        }
        "setorcopyvar" -> {
          ctx.setVar(
              namespaced(varArg(instruction, 0).token),
              gbaValue(value(ctx, instruction.arg(1))),
          )
          state.pc++
        }
        "addvar" -> {
          val variable = namespaced(varArg(instruction, 0).token)
          ctx.setVar(
              variable,
              gbaValue(ctx.getVar(variable) + immediateValue(instruction.arg(1), instruction)),
          )
          state.pc++
        }
        "subvar" -> {
          val variable = namespaced(varArg(instruction, 0).token)
          ctx.setVar(
              variable,
              gbaValue(ctx.getVar(variable) - value(ctx, instruction.arg(1))),
          )
          state.pc++
        }
        "compare" -> {
          state.comparisonResult =
              compareValues(
                  value(ctx, instruction.arg(0)),
                  value(ctx, instruction.arg(1)),
              )
          state.pc++
        }
        "call" -> {
          state.callStack.addLast(ScriptLocation(state.activeProgram, state.pc + 1))
          jumpTo(state, instruction, labelArg(instruction, 0))
        }
        "goto" -> jumpTo(state, instruction, labelArg(instruction, 0))
        "goto_if_eq",
        "goto_if_ne",
        "goto_if_lt",
        "goto_if_le",
        "goto_if_gt",
        "goto_if_ge" -> runConditional(ctx, state, instruction, call = false)
        "call_if_eq",
        "call_if_ne",
        "call_if_lt",
        "call_if_le",
        "call_if_gt",
        "call_if_ge" -> runConditional(ctx, state, instruction, call = true)
        "goto_if_set" -> runFlagConditional(ctx, state, instruction, expectSet = true, call = false)
        "goto_if_unset" ->
            runFlagConditional(ctx, state, instruction, expectSet = false, call = false)
        "call_if_set" -> runFlagConditional(ctx, state, instruction, expectSet = true, call = true)
        "call_if_unset" ->
            runFlagConditional(ctx, state, instruction, expectSet = false, call = true)
        "return" -> {
          check(state.callStack.isNotEmpty()) {
            "Script ${program.id.stable} call stack underflow at `${instruction.sourceLine}`"
          }
          val target = state.callStack.removeLast()
          state.activeProgram = target.program
          state.pc = target.pc
        }
        "end" -> return
        "setflashlevel" -> {
          // The ROM's flash level: 0 is fully lit. The client keeps darkness per map (f/tM.Xl1),
          // so this lights the map the player stands in until the next map load, as the GBA does.
          ctx.lightMap(value(ctx, instruction.arg(0)))
          state.pc++
        }
        "checkpartymove" -> {
          val moveId = (instruction.arg(0) as IntArg).value
          ctx.setVar(namespaced("VAR_RESULT"), ctx.partyIndexWithMove(moveId))
          state.pc++
        }
        "checkmoney" -> {
          val amount = value(ctx, instruction.arg(0))
          ctx.setVar(namespaced("VAR_RESULT"), if (ctx.money() >= amount) 1 else 0)
          state.pc++
        }
        "addmoney" -> {
          ctx.addMoney(value(ctx, instruction.arg(0)))
          state.pc++
        }
        "removemoney" -> {
          ctx.addMoney(-value(ctx, instruction.arg(0)))
          state.pc++
        }
        "getpartysize" -> {
          ctx.setVar(namespaced("VAR_RESULT"), ctx.partySize())
          state.pc++
        }
        "setwildbattle" -> {
          state.wildSpecies = value(ctx, instruction.arg(0))
          state.wildLevel = value(ctx, instruction.arg(1))
          state.pc++
        }
        "dowildbattle" -> {
          check(state.wildSpecies > 0) { "Script ${program.id.stable} has no setwildbattle before `${instruction.sourceLine}`" }
          val result = tracedWait(ctx, "wild battle ${state.wildSpecies}") { ctx.wildBattle(state.wildSpecies, state.wildLevel) }
          // The field's objects reload after a battle on the cartridge, so an npc the script hid
          // just before (Route 12's Snorlax: setflag, then dowildbattle) is gone when the screen returns.
          ctx.despawnHiddenNpcs()
          state.lastBattleOutcome =
              when (result) {
                BattleResult.VICTORY -> B_OUTCOME_WON
                BattleResult.DEFEAT -> B_OUTCOME_LOST
                BattleResult.CAUGHT -> B_OUTCOME_CAUGHT
                else -> B_OUTCOME_RAN
              }
          if (result == BattleResult.DEFEAT || result == BattleResult.DISCONNECTED) return
          state.pc++
        }
        "trainerbattle_earlyrival" -> {
          if (!runTrainerBattle(ctx, state, instruction, rematch = false, whiteoutOnDefeat = false, earlyRival = true)) return
        }
        "givemon" -> {
          val species = value(ctx, instruction.arg(0))
          val level = value(ctx, instruction.arg(1))
          val result = tracedWait(ctx, "givemon $species") { ctx.giveMonster(species, level) }
          ctx.setVar(namespaced("VAR_RESULT"), result)
          state.pc++
        }
        "braillemessage" -> {
          // Braille signs read as plain sign text on this client.
          val line = textLine(textArg(instruction, 0).token, instruction)
          tracedWait(ctx, "dialog") { ctx.sign(line) }
          state.pc++
        }
        "copyobjectxytoperm" -> {
          val target =
              resolveMovementTarget(ctx, state.activeProgram, instruction, ObjectArg(instruction.arg(0).token), true)
          if (target is MovementTarget.Npc) ctx.copyNpcXyToPerm(target.localId)
          state.pc++
        }
        "map_script",
        "map_script_2" ->
            error("Script ${program.id.stable} executed a map script table row at `${instruction.sourceLine}`")
        "dofieldeffect" -> {
          // Surf is the one field effect with a server-side state; the rest (Cut's swing, the
          // flash, the rock smash) are client visuals this dialog channel cannot trigger yet, and
          // the scripts around them already carry the outcome (removeobject, the message).
          // The pose (900 ms) and the banner's slide in, hold and slide out run on the client
          // before the move lands.
          if (ctx.fieldMoveBanner(instruction.arg(0).token)) delay(de.fiereu.openmmo.server.game.services.FieldMoveBanners.HOLD_MILLIS)
          when (instruction.arg(0).token) {
            "FLDEFF_USE_SURF" -> tracedWait(ctx, "surf") { ctx.startSurfing() }
            "FLDEFF_USE_WATERFALL" -> tracedWait(ctx, "waterfall") { ctx.climbWaterfall() }
            "FLDEFF_USE_DIVE" -> tracedWait(ctx, "dive") { ctx.dive() }
          }
          state.pc++
        }
        "multichoicedefault",
        "multichoicegrid" -> {
          tracedWait(ctx, "dialog choice") { runMultichoice(ctx, state, instruction) }
          state.pc++
        }
        in InterpreterSupport.BUFFER_COMMANDS -> {
          // ROM string variables ride the dialog as raw text arguments (slot = the variable).
          val variable = stringVariable(instruction.arg(0).token)
          val text =
              if (variable == null) null
              else
                  when (instruction.command) {
                    "bufferpartymonnick" -> ctx.partyNickname(value(ctx, instruction.arg(1)))
                    "buffermovename" -> ctx.moveName(value(ctx, instruction.arg(1)))
                    "bufferspeciesname" -> ctx.speciesName(value(ctx, instruction.arg(1)))
                    "bufferleadmonspeciesname" -> ctx.leadSpeciesName()
                    "buffernumberstring" -> value(ctx, instruction.arg(1)).toString()
                    else -> null
                  }
          if (variable != null && text != null) ctx.bufferText(variable, text)
          state.pc++
        }
        "setmetatile" -> {
          ctx.setMetatile(
              value(ctx, instruction.arg(0)),
              value(ctx, instruction.arg(1)),
              value(ctx, instruction.arg(2)),
              value(ctx, instruction.arg(3)) != 0)
          state.pc++
        }
        in InterpreterSupport.NOOP_COMMANDS -> state.pc++
        "delay" -> {
          val frames = (instruction.arg(0) as? IntArg)?.value ?: 0
          delay((frames * FRAME_MILLIS).coerceAtMost(MAX_DELAY_MILLIS))
          state.pc++
        }
        "special" -> {
          when (val function = instruction.arg(0).token) {
            "HealPlayerParty" -> ctx.healParty()
            "ListMenu" -> runListMenu(ctx, state)
            // src/safari_zone.c: the Safari Game on the client's own safari counters.
            // FlagSet(gSpecialVar_0x8004): the Silph Co. doors after the Card Key opens them.
            "SetHiddenItemFlag" -> ctx.setFlagRememberedInVar(namespaced("VAR_0x8004"))
            "EnterSafariMode" -> ctx.enterSafari()
            "ExitSafariMode" -> ctx.exitSafari()
            // src/field_specials.c ChoosePartyMon: VAR_0x8004 = the party slot, PARTY_SIZE when closed.
            "ChoosePartyMon" -> ctx.setVar(namespaced("VAR_0x8004"), tracedWait(ctx, "party pick") { ctx.choosePartyMember() })
            // The in-game trade: VAR_0x8004 = the trade, VAR_0x8005 = the party slot given away. The
            // monster is built and swapped in one go at the scene; the create step has nothing left to do.
            "CreateInGameTradePokemon" -> {}
            "DoInGameTradeScene" -> {
              val done = tracedWait(ctx, "npc trade") { ctx.inGameTrade(ctx.getVar(namespaced("VAR_0x8004")), ctx.getVar(namespaced("VAR_0x8005"))) }
              check(done) { "Script ${program.id.stable} could not complete its in-game trade" }
            }
            "GetMagikarpSizeRecordInfo" -> ctx.bufferMagikarpRecord()
            "CompareMagikarpSize" -> ctx.setVar(namespaced("VAR_RESULT"), ctx.compareMagikarpSize(ctx.getVar(namespaced("VAR_RESULT"))))
            // src/field_specials.c: the floor the elevator stands on, read from the dynamic warp.
            "GetElevatorFloor" -> ctx.setVar(namespaced("VAR_ELEVATOR_FLOOR"), ctx.elevatorFloor())
            // Pokemon Tower's ghost: the wild battle setwildbattle named, uncatchable. FireRed's
            // CB2_EndMarowakBattle leaves VAR_RESULT FALSE only for a win; anything else keeps it
            // TRUE and the trigger walks the player back from the stairs.
            "StartMarowakBattle" -> {
              check(state.wildSpecies > 0) { "Script ${program.id.stable} has no setwildbattle before `${instruction.sourceLine}`" }
              // Without the Silph Scope the cartridge lets the unidentified ghost scare every
              // attack off until the player runs; here the fight is skipped outright - the grey
              // box says "Haunted by the ghosts." and VAR_RESULT stays TRUE, so the trigger walks
              // the player back from the stairs.
              val scope = ctx.itemByScriptConstant("ITEM_SILPH_SCOPE")
              if (scope == null || ctx.itemCount(scope) == 0) {
                ctx.send(de.fiereu.openmmo.net.game.packets.ServerMessagePacket(HAUNTED_BY_GHOSTS_STRING, emptyList(), showOnMap = true, mode = null))
                ctx.setVar(namespaced("VAR_RESULT"), 1)
              } else {
                val result = tracedWait(ctx, "ghost battle ${state.wildSpecies}") { ctx.ghostBattle(state.wildSpecies, state.wildLevel) }
                state.lastBattleOutcome = if (result == BattleResult.VICTORY) B_OUTCOME_WON else B_OUTCOME_RAN
                if (result == BattleResult.DEFEAT || result == BattleResult.DISCONNECTED) return
                ctx.setVar(namespaced("VAR_RESULT"), if (result == BattleResult.VICTORY) 0 else 1)
              }
            }
            // Rock Smash encounters: the retail tables carry no Rock Smash entries, so no monster
            // ever hides under a rock here - the truthful answer, not a shortcut.
            "RockSmashWildEncounter" -> ctx.setVar(namespaced("VAR_RESULT"), 0)
            "SetVermilionTrashCans" -> {
              // src/field_specials.c: the live can is random, the second switch sits beside it
              // (right or below when there is room, else left or above) in the 5x3 grid.
              val idx = 1 + kotlin.random.Random.nextInt(15)
              val right = idx % 5 != 0
              val below = idx <= 10
              val second =
                  when {
                    right && below -> if (kotlin.random.Random.nextBoolean()) idx + 1 else idx + 5
                    right -> idx + 1
                    below -> idx + 5
                    else -> idx - 1
                  }
              ctx.setVar(namespaced("VAR_0x8004"), idx)
              ctx.setVar(namespaced("VAR_0x8005"), second)
            }
            "ChooseMonForMoveTutor" -> {
              val taught =
                  tracedWait(ctx, "move tutor") {
                    ctx.chooseMonForMoveTutor(ctx.getVar(namespaced("VAR_0x8005")))
                  }
              ctx.setVar(namespaced("VAR_RESULT"), if (taught) 1 else 0)
            }
            "DoSSAnneDepartureCutscene" -> {
              // src/ss_anne.c: the boat object (local id 1) slides west until it is off screen.
              val boat =
                  state.activeProgram.objectIds["LOCALID_SS_ANNE"] ?: program.objectIds["LOCALID_SS_ANNE"] ?: 0
              tracedWait(ctx, "S.S. Anne departure") { ctx.sailBoatAway(boat) }
            }
            in InterpreterSupport.NOOP_SPECIALS -> Unit
            else ->
                throw UnsupportedScriptCommandException(
                    program.id.stable, "special $function", instruction.sourceLine)
          }
          state.pc++
        }
        "specialvar" -> {
          val function = instruction.arg(1).token
          val result =
              if (function == "GetBattleOutcome") state.lastBattleOutcome
              else if (function == "IsPlayerLeftOfVermilionSailor") {
                // field_specials.c: player x below the sailor's x.
                val sailor = state.activeProgram.objectIds["LOCALID_VERMILION_FERRY_SAILOR"] ?: program.objectIds["LOCALID_VERMILION_FERRY_SAILOR"] ?: 5
                if (ctx.isPlayerLeftOfNpc(sailor)) 1 else 0
              }
              else if (function == "InitElevatorFloorSelectMenuPos") ctx.elevatorMenuPosition()
              else if (function == "IsThereRoomInAnyBoxForMorePokemon") (if (ctx.pcHasRoom()) 1 else 0)
              else if (function == "DoesPlayerPartyContainSpecies") (if (ctx.partyContainsSpecies(ctx.getVar(namespaced("VAR_0x8004")))) 1 else 0)
              else if (function == "GetPokedexCount") {
                // src/prof_pc.c: VAR_0x8004 0 = the Kanto dex, else national; 0x8005 seen, 0x8006
                // owned; the answer itself is IsNationalPokedexEnabled. Oak's aides read 0x8006.
                val kantoOnly = ctx.getVar(namespaced("VAR_0x8004")) == 0
                val (seen, owned) = ctx.dexCounts(kantoOnly)
                ctx.setVar(namespaced("VAR_0x8005"), seen)
                ctx.setVar(namespaced("VAR_0x8006"), owned)
                0
              }
              // src/trade_scene.c: the in-game trade NPCs of the Pokemon Lab lounge and friends.
              else if (function == "GetInGameTradeSpeciesInfo") ctx.inGameTradeInfo(ctx.getVar(namespaced("VAR_0x8004")))
              else if (function == "GetTradeSpecies") ctx.partySpecies(ctx.getVar(namespaced("VAR_0x8005")))
              else InterpreterSupport.SPECIALVAR_RESULTS[function]
                  ?: throw UnsupportedScriptCommandException(
                      program.id.stable, "specialvar $function", instruction.sourceLine)
          ctx.setVar(namespaced(varArg(instruction, 0).token), result)
          state.pc++
        }
        "multichoice" -> {
          tracedWait(ctx, "dialog choice") { runMultichoice(ctx, state, instruction) }
          state.pc++
        }
        "checkplayergender" -> {
          ctx.setVar(namespaced("VAR_RESULT"), ctx.playerGender())
          state.pc++
        }
        "getplayerxy" -> {
          val (x, y) = ctx.playerXy() ?: (0 to 0)
          ctx.setVar(namespaced(varArg(instruction, 0).token), x)
          ctx.setVar(namespaced(varArg(instruction, 1).token), y)
          state.pc++
        }
        "removeobject",
        "addobject" -> {
          val target =
              resolveMovementTarget(
                  ctx, state.activeProgram, instruction, objectArg(instruction, 0), true)
          if (target is MovementTarget.Npc) {
            // pokefirered ScrCmd_removeobject -> RemoveObjectEventByLocalIdAndMap: FlagSet on the
            // object's own flag, then removal - so a taken starter ball stays gone across visits.
            // Objects with no flag (cut trees, flag 0) come back with the map, as on the GBA.
            // addobject (TrySpawnObjectEvent) spawns regardless of the flag and clears nothing.
            if (instruction.command == "removeobject") ctx.removeNpc(target.localId, persist = true)
            else ctx.addNpc(target.localId)
          }
          state.pc++
        }
        in InterpreterSupport.ITEM_COMMANDS -> {
          runItemCommand(ctx, instruction)
          state.pc++
        }
        "random" -> {
          val bound = ((instruction.arg(0) as IntArg).value).coerceAtLeast(1)
          ctx.setVar(namespaced("VAR_RESULT"), kotlin.random.Random.nextInt(bound))
          state.pc++
        }
        "giveitem_msg" -> {
          runGiveItemMsg(ctx, state, instruction)
          state.pc++
        }
        // msgreceiveditem TEXT, ITEM: the "received" line alone (STD_RECEIVED_ITEM buffers the item
        // name into STR_VAR_2); the fishing gurus add the rod with additem first.
        "msgreceiveditem" -> {
          runReceivedItemMsg(ctx, state, instruction)
          state.pc++
        }
        "setobjectxy" -> {
          val target =
              resolveMovementTarget(
                  ctx, state.activeProgram, instruction, objectArg(instruction, 0), true)
          if (target is MovementTarget.Npc) {
            ctx.repositionNpc(
                target.localId,
                (instruction.arg(1) as IntArg).value,
                (instruction.arg(2) as IntArg).value,
            )
          }
          state.pc++
        }
        // Overrides the npc's template tile: ON_TRANSITION scripts place story npcs with it (the
        // Viridian old man). Persisted, so every later spawn of this map uses the new tile.
        "setobjectmovementtype" -> {
          val target =
              resolveMovementTarget(
                  ctx, state.activeProgram, instruction, ObjectArg(instruction.arg(0).token), true)
          val type =
              de.fiereu.openmmo.common.enums.MovementType.entries.firstOrNull {
                it.name == instruction.arg(1).token.removePrefix("MOVEMENT_TYPE_")
              }
          if (target is MovementTarget.Npc && type != null) ctx.setNpcMovementType(target.localId, type)
          state.pc++
        }
        "setobjectxyperm" -> {
          val target =
              resolveMovementTarget(
                  ctx, state.activeProgram, instruction, objectArg(instruction, 0), true)
          if (target is MovementTarget.Npc) {
            ctx.setNpcXyOverride(
                target.localId,
                (instruction.arg(1) as IntArg).value,
                (instruction.arg(2) as IntArg).value,
            )
          }
          state.pc++
        }
        // The map argument is dropped: scripts all but always address the map they run on, and
        // showing/hiding is per-player visual state here, not global object state.
        "showobjectat",
        "hideobjectat" -> {
          val target =
              resolveMovementTarget(
                  ctx, state.activeProgram, instruction, objectArg(instruction, 0), true)
          if (target is MovementTarget.Npc) {
            if (instruction.command == "hideobjectat") ctx.removeNpc(target.localId, persist = true)
            else ctx.showNpc(target.localId)
          }
          state.pc++
        }
        in InterpreterSupport.DEFEATED_BRANCHES -> runDefeatedBranch(ctx, state, instruction)
        "warp" -> {
          // MAP_ constants pack the destination as num | group << 8; group is the server bank.
          val packed = (instruction.arg(0) as IntArg).value
          val region =
              when (program.id.source) {
                "firered" -> 0
                "emerald" -> 1
                else ->
                    error("Script ${program.id.stable} cannot warp for source " + program.id.source)
              }
          tracedWait(ctx, "warp") {
            ctx.warp(
                region,
                packed shr 8,
                packed and 0xFF,
                // `warp map, x, y` or `warp map, warpId, x, y` (asm/macros/event.inc: map, a, b, c).
                value(ctx, instruction.arg(instruction.args.size - 2)),
                value(ctx, instruction.arg(instruction.args.size - 1)),
                de.fiereu.openmmo.common.enums.Direction.DOWN,
            )
          }
          state.pc++
        }
        "trainerbattle_no_intro" -> {
          if (!runTrainerBattleNoIntro(ctx, state, instruction)) return
        }
        // setdynamicwarp MAP, warpId, x, y: where the map's MAP_DYNAMIC warps (an elevator's exit)
        // send the player. The map constant packs num | group << 8 like `warp`.
        "setdynamicwarp" -> {
          val packed = (instruction.arg(0) as IntArg).value
          val region =
              when (program.id.source) {
                "firered" -> 0
                "emerald" -> 1
                else -> error("Script ${program.id.stable} cannot set a dynamic warp for source " + program.id.source)
              }
          ctx.setDynamicWarp(
              region,
              packed shr 8,
              packed and 0xFF,
              value(ctx, instruction.arg(2)),
              value(ctx, instruction.arg(3)),
              de.fiereu.openmmo.common.enums.Direction.DOWN,
          )
          state.pc++
        }
        else ->
            throw UnsupportedScriptCommandException(
                program.id.stable, instruction.command, instruction.sourceLine)
      }
    }
  }

  private suspend fun runApplyMovement(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    check(instruction.args.size == 2) {
      "Script ${program.id.stable} supports current-map applymovement only in " +
          "`${instruction.sourceLine}`"
    }
    val resolved =
        resolveMovementTarget(ctx, state.activeProgram, instruction, objectArg(instruction, 0), true)
    // The GBA camera object (Bill's teleporter pan) has nothing to drive here: skip the walk.
    if (resolved == null && objectArg(instruction, 0).token == "LOCALID_CAMERA") {
      state.pc++
      return
    }
    val target =
        checkNotNull(resolved) {
          "Script ${program.id.stable} cannot apply movement to LOCALID_NONE from " +
              "`${instruction.sourceLine}`"
        }
    val movementLabel = movementArg(instruction, 1).token
    val movement =
        movementPrograms[movementLabel]
            ?: error(
                "Script ${program.id.stable} has no movement $movementLabel from " +
                    "`${instruction.sourceLine}`")
    if (movement.actions.any { MovementStep.isRuntimeResolved(it.command) }) {
      runSegmentedMovement(ctx, state, target, movement)
      return
    }
    val steps = movement.actions.map { action -> movementStep(movement, action) }
    val operation =
        try {
          when (target) {
            MovementTarget.Player -> ctx.applyPlayerMovement(steps)
            is MovementTarget.Npc -> ctx.applyNpcMovement(target.localId, steps)
          }
        } catch (cause: IllegalStateException) {
          throw IllegalStateException(
              "Script ${program.id.stable} cannot apply movement to ${target.display} from " +
                  "`${instruction.sourceLine}`: ${cause.message}",
              cause,
          )
        }
    state.pendingMovements[target] = operation
    state.lastMovementTarget = target
    state.pc++
  }

  /**
   * face_player has no fixed action byte - the direction exists only when the step runs - so a
   * sequence containing it runs synchronously in segments around it, facing the talked-to NPC at
   * the marker. face_original_direction is skipped: the stale facing lasts one interaction and is
   * cosmetic. Nothing is left pending, so a following waitmovement passes straight through.
   */
  private suspend fun runSegmentedMovement(
      ctx: ScriptContext,
      state: RuntimeState,
      target: MovementTarget,
      movement: MovementProgram,
  ) {
    val segment = mutableListOf<MovementStep>()
    suspend fun flush() {
      if (segment.isEmpty()) return
      val steps = segment.toList()
      segment.clear()
      val operation =
          when (target) {
            MovementTarget.Player -> ctx.applyPlayerMovement(steps)
            is MovementTarget.Npc -> ctx.applyNpcMovement(target.localId, steps)
          }
      tracedWait(ctx, "movement ${target.display}") { operation.await() }
    }
    for (action in movement.actions) {
      if (!MovementStep.isRuntimeResolved(action.command)) {
        segment += movementStep(movement, action)
        continue
      }
      flush()
      if (action.command == "face_player" &&
          target is MovementTarget.Npc &&
          target.localId == ctx.interactingLocalId()) {
        ctx.facePlayer()
      }
    }
    flush()
    state.lastMovementTarget = target
    state.pc++
  }

  private suspend fun runWaitMovement(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    check(instruction.args.size <= 1) {
      "Script ${program.id.stable} supports current-map waitmovement only in " +
          "`${instruction.sourceLine}`"
    }
    val requested =
        instruction.args.firstOrNull()?.let {
          resolveMovementTarget(
              ctx,
              state.activeProgram,
              instruction,
              objectArg(instruction, 0),
              allowNone = true,
          )
        }
    val target = requested ?: state.lastMovementTarget
    if (target != null) {
      val pending = state.pendingMovements.remove(target)
      if (pending != null) tracedWait(ctx, "movement ${target.display}") { pending.await() }
    }
    state.pc++
  }

  private fun movementStep(
      movement: MovementProgram,
      action: de.fiereu.openmmo.script.MovementAction,
  ): MovementStep {
    if (action.args.isNotEmpty()) {
      throw UnsupportedMovementActionException(
          movement.id.stable, action.command, action.sourceLine)
    }
    return MovementStep.fromPretCommand(action.command)
        ?: throw UnsupportedMovementActionException(
            movement.id.stable, action.command, action.sourceLine)
  }

  private fun resolveMovementTarget(
      ctx: ScriptContext,
      activeProgram: ScriptProgram,
      instruction: ScriptInstruction,
      arg: ObjectArg,
      allowNone: Boolean,
  ): MovementTarget? {
    val token = arg.token
    if (token == "LOCALID_NONE") return null
    // The camera object only exists for GBA panning; its movements have nothing to drive here.
    if (token == "LOCALID_CAMERA") return null
    if (token == "LOCALID_PLAYER") return MovementTarget.Player
    // Shared scripts (data/scripts) address a map's objects through the script that called them,
    // so the root program's table and then any library program's table are consulted too.
    (activeProgram.objectIds[token]
            ?: program.objectIds[token]
            ?: programLibrary.values.firstNotNullOfOrNull { it.objectIds[token] })
        ?.let {
          return MovementTarget.Npc(it)
        }
    if (token == "VAR_LAST_TALKED") {
      return MovementTarget.Npc(
          ctx.interactingLocalId()
              ?: error(
                  "Script ${program.id.stable} cannot resolve VAR_LAST_TALKED from " +
                      "`${instruction.sourceLine}`"))
    }
    val sourceLocalId =
        when {
          token.startsWith("VAR_") -> ctx.getVar(namespaced(token))
          else -> sourceInt(token)
        }
            ?: error(
                "Script ${program.id.stable} cannot resolve object id $token from " +
                    "`${instruction.sourceLine}`")
    if (sourceLocalId == LOCALID_NONE) {
      check(allowNone) {
        "Script ${program.id.stable} cannot use LOCALID_NONE in `${instruction.sourceLine}`"
      }
      return null
    }
    if (sourceLocalId == LOCALID_PLAYER) return MovementTarget.Player
    check(sourceLocalId > LOCALID_NONE) {
      "Script ${program.id.stable} has invalid object id $sourceLocalId from " +
          "`${instruction.sourceLine}`"
    }
    return MovementTarget.Npc(sourceLocalId - PRET_LOCAL_ID_OFFSET)
  }

  private fun sourceInt(token: String): Int? =
      if (token.startsWith("0x", ignoreCase = true)) token.drop(2).toIntOrNull(16)
      else token.toIntOrNull()

  private fun runConditional(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
      call: Boolean,
  ) {
    val comparison =
        when (instruction.args.size) {
          1 ->
              state.comparisonResult
                  ?: error(
                      "Script ${program.id.stable} has no comparison result for " +
                          "`${instruction.sourceLine}`")
          3 ->
              compareValues(value(ctx, instruction.arg(0)), value(ctx, instruction.arg(1))).also {
                state.comparisonResult = it
              }
          else ->
              error(
                  "Script ${program.id.stable} expected 1 or 3 arguments in " +
                      "`${instruction.sourceLine}`")
        }
    val condition = ComparisonCondition.fromCommand(instruction.command)
    if (!condition.matches(comparison)) {
      state.pc++
      return
    }

    val labelIndex = if (instruction.args.size == 1) 0 else 2
    if (call) state.callStack.addLast(ScriptLocation(state.activeProgram, state.pc + 1))
    jumpTo(state, instruction, labelArg(instruction, labelIndex))
  }

  private fun runFlagConditional(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
      expectSet: Boolean,
      call: Boolean,
  ) {
    val isSet = ctx.isFlagSet(namespaced(flagArg(instruction, 0).token))
    state.comparisonResult = if (isSet) ComparisonResult.EQUAL else ComparisonResult.LESS
    if (isSet != expectSet) {
      state.pc++
      return
    }

    if (call) state.callStack.addLast(ScriptLocation(state.activeProgram, state.pc + 1))
    jumpTo(state, instruction, labelArg(instruction, 1))
  }

  private suspend fun runMsgbox(
      ctx: ScriptContext,
      instruction: ScriptInstruction,
  ): DialogLine {
    val label = textArg(instruction, 0).token
    val line = textLine(label, instruction)
    InterpreterSupport.CLIENT_STRING_OVERRIDES[label]?.let { stringId ->
      ctx.clientMessage(stringId)
      return line
    }
    when (instruction.args.getOrNull(1)?.token) {
      "MSGBOX_SIGN" -> ctx.sign(line)
      // The prompt shows the text with yes/no attached; the answer lands in VAR_RESULT exactly
      // like the GBA msgbox macro's.
      "MSGBOX_YESNO",
      "5" -> ctx.setVar(namespaced("VAR_RESULT"), if (ctx.askYesNo(line)) 1 else 0)
      else -> ctx.say(line)
    }
    return line
  }

  /**
   * The bag commands. All of them leave the GBA truth in VAR_RESULT. finditem also makes the item
   * ball vanish for good: despawn now, and persist its hide flag so it never respawns.
   */
  private suspend fun runItemCommand(ctx: ScriptContext, instruction: ScriptInstruction) {
    val token = instruction.arg(0).token
    // DS item balls carry the item and count in vars; the value is the game's own item index,
    // which the client knows as region * 1000 + index.
    val item =
        if (instruction.arg(0) is VarArg) {
          val region = if (program.id.source == "heartgold") 4 else 3
          val index = ctx.getVar(namespaced(token))
          ctx.resolveItemWire(region * 1000 + index)
              ?: error("Script ${program.id.stable} var $token holds unknown item $index (region $region)")
        } else {
          ctx.resolveItem(token)
              ?: error(
                  "Script ${program.id.stable} references unknown item $token from " +
                      "`${instruction.sourceLine}`")
        }
    val quantity =
        when (val q = instruction.args.getOrNull(1)) {
          is IntArg -> q.value
          is VarArg -> ctx.getVar(namespaced(q.token))
          else -> 1
        }
    val result =
        when (instruction.command) {
          "giveitem" ->
              ctx.giveItem(item, quantity).also { if (it) announceItem(ctx, item, quantity) }
          // additem: into the bag without the "received" fanfare; the script announces it itself.
          "additem" -> ctx.giveItem(item, quantity)
          "removeitem" -> ctx.takeItem(item, quantity)
          "checkitem" -> ctx.itemCount(item) >= quantity
          // The server bag has no slot cap for scripts to overflow.
          "checkitemspace" -> true
          "finditem" -> {
            val obtained = ctx.giveItem(item, quantity)
            if (obtained) {
              announceItem(ctx, item, quantity)
              // Persisting the ball's hide flag means it never respawns.
              ctx.interactingLocalId()?.let { ctx.removeNpc(it, persist = true) }
            }
            obtained
          }
          else ->
              error(
                  "Script ${program.id.stable} cannot run ${instruction.command} from " +
                      "`${instruction.sourceLine}`")
        }
    ctx.setVar(namespaced("VAR_RESULT"), if (result) 1 else 0)
  }

  /** FireRed's combined receive-item macro: show the received-item text, then hand it over. */
  private suspend fun runGiveItemMsg(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    val line = textLine(textArg(instruction, 0).token, instruction)
    val token = instruction.arg(1).token
    val item =
        ctx.resolveItem(token)
            ?: error(
                "Script ${program.id.stable} references unknown item $token from " +
                    "`${instruction.sourceLine}`")
    val quantity = (instruction.args.getOrNull(2) as? IntArg)?.value ?: 1
    val obtained = ctx.giveItem(item, quantity)
    tracedWait(ctx, "dialog") { ctx.say(line) }
    state.currentMessage = line
    ctx.setVar(namespaced("VAR_RESULT"), if (obtained) 1 else 0)
  }

  private suspend fun runReceivedItemMsg(ctx: ScriptContext, state: RuntimeState, instruction: ScriptInstruction) {
    val line = textLine(textArg(instruction, 0).token, instruction)
    val token = instruction.arg(1).token
    val item = ctx.resolveItem(token) ?: error("Script ${program.id.stable} references unknown item $token from `${instruction.sourceLine}`")
    ctx.bufferText(2, item.name)
    tracedWait(ctx, "dialog") { ctx.say(line) }
    state.currentMessage = line
  }

  private fun runDefeatedBranch(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    val trainer = resolveTrainer(ctx, trainerArg(instruction, 0), instruction)
    val defeated = ctx.isFlagSet(TrainerStoryState.defeated(program.storyNamespace, trainer.id))
    if (defeated != !instruction.command.contains("not_")) {
      state.pc++
      return
    }
    if (instruction.command.startsWith("call")) {
      state.callStack.addLast(ScriptLocation(state.activeProgram, state.pc + 1))
    }
    jumpTo(state, instruction, labelArg(instruction, 1))
  }

  /**
   * A battle with no intro dialog (rival fights mid-cutscene). On victory the script continues with
   * the next instruction; a loss or disconnect ends it like the other battle macros.
   */
  private suspend fun runTrainerBattleNoIntro(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ): Boolean {
    val trainer = resolveTrainer(ctx, trainerArg(instruction, 0), instruction)
    // The in-battle defeat speech, shown by the battle end packet before the prize money.
    val defeat = textLine(textArg(instruction, 1).token, instruction)
    val defeatedKey = TrainerStoryState.defeated(program.storyNamespace, trainer.id)
    if (ctx.isFlagSet(defeatedKey)) {
      state.pc++
      return true
    }
    return when (val result =
        tracedWait(ctx, "battle ${trainer.constant}") {
          ctx.trainerBattle(trainer, defeat.textId)
        }) {
      BattleResult.VICTORY -> {
        ctx.setFlag(defeatedKey)
        state.pc++
        true
      }
      BattleResult.DEFEAT -> {
        state.lastBattleOutcome = B_OUTCOME_LOST
        false
      }
      BattleResult.DISCONNECTED -> false
      BattleResult.FAILED,
      BattleResult.FLED,
      BattleResult.CAUGHT ->
          error(
              "Script ${program.id.stable} trainer battle ${trainer.constant} ended with " +
                  "$result at `${instruction.sourceLine}`")
    }
  }

  /**
   * The obtained-item announcement as the client's own on-screen popup. String ids from the client
   * string table, probe-verified: 6063 "You found a {00}!", 6066 "You found {00} {01}(s)!" with
   * type-5 string arguments.
   */
  private fun announceItem(
      ctx: ScriptContext,
      item: de.fiereu.openmmo.items.ItemDef,
      quantity: Int,
  ) = ctx.announceItem(item, quantity)

  /**
   * special ListMenu (src/field_specials.c): the scrolling list VAR_0x8004 names (LISTMENU_*),
   * drawn as the client's text-button list over the current message; VAR_RESULT takes the
   * 0-based pick, as the cartridge's list returns it. Silph Co's elevator is the story use; the
   * waitstate that follows is a no-op here since the pick is already in.
   */
  private suspend fun runListMenu(ctx: ScriptContext, state: RuntimeState) {
    val id = ctx.getVar(namespaced("VAR_0x8004"))
    val line = checkNotNull(state.currentMessage) { "Script ${program.id.stable} has no current message for special ListMenu" }
    val list = menus["LISTMENU#$id"]?.let(InterpreterSupport::dsTextList)
    val pick =
        if (list == null) {
          log.warn { "Script ${program.id.stable}: list menu $id has no client labels, answering B" }
          MULTI_B_PRESSED
        } else ctx.dsTextListMenu(line, list, preselected = ctx.getVar(namespaced("VAR_RESULT")))
    ctx.setVar(namespaced("VAR_RESULT"), pick)
  }

  /** The client's wire region of this DS script's game (ds_warp's convention). */
  private fun dsRegion(): Int =
      when (program.id.source) {
        "heartgold" -> 4
        "white" -> 2
        else -> 3
      }

  /**
   * ds_menu VAR, cursor, (textId, value)+: the entries are DS text ids (region << 28 | bank << 16 |
   * entry) of one bank, exactly what the client's text-button list draws from; the pick's value
   * goes to VAR, as the games' ShowMenu / MenuExec write the chosen entry's index there.
   */
  private suspend fun runDsMenu(ctx: ScriptContext, state: RuntimeState, instruction: ScriptInstruction) {
    val target = namespaced(instruction.arg(0).token)
    val cursor = value(ctx, instruction.arg(1))
    val items = (2 until instruction.args.size step 2).map { value(ctx, instruction.arg(it)) to value(ctx, instruction.arg(it + 1)) }
    val region = items.first().first ushr 28
    val bank = (items.first().first shr 16) and 0xFFF
    if (items.any { (it.first ushr 28) != region || ((it.first shr 16) and 0xFFF) != bank }) {
      log.warn { "Script ${program.id.stable}: menu entries span text banks, answering 127 - `${instruction.sourceLine}`" }
      ctx.setVar(target, MULTI_B_PRESSED)
      return
    }
    val line = checkNotNull(state.currentMessage) { "Script ${program.id.stable} has no current message for `${instruction.sourceLine}`" }
    val list = InterpreterSupport.DsTextList(region, bank, items.map { it.first and 0xFFFF })
    val pick = ctx.dsTextListMenu(line, list, cursor).coerceIn(0, items.size - 1)
    ctx.setVar(target, items[pick].second)
  }

  /** Only the yes/no menu is modeled; the analyzer admits no other multichoice. */
  private suspend fun runMultichoice(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    val menu = instruction.arg(2).token
    val line = state.currentMessage
    // Multichoice answers are list indices: YES is 0, NO is 1. The client renders the ROM's
    // yes/no box and its own registry menus (BUILTIN_MENUS); a menu it has no set for behaves as
    // if B was pressed (MULTI_B_PRESSED) so the script takes its cancel path instead of dying
    // at resolution time and taking the whole npc with it.
    val builtin = InterpreterSupport.BUILTIN_MENUS[menu]
    val dsList = menus[menu]?.let(InterpreterSupport::dsTextList)
    val result =
        when {
          menu == "MULTICHOICE_YES_NO" || menu == "MULTI_YESNO" -> {
            checkNotNull(line) { "Script ${program.id.stable} has no current message for `${instruction.sourceLine}`" }
            if (ctx.askYesNo(line)) 0 else 1
          }
          builtin != null && line != null -> {
            val pick = ctx.builtinMenu(line, builtin)
            if (pick <= 0) MULTI_B_PRESSED else pick - 1
          }
          dsList != null && line != null -> {
            // multichoicedefault's fourth argument is the pre-selected row. The client's text-button
            // list answers with the 0-based index of the button, which is the multichoice result.
            val preselected = if (instruction.command == "multichoicedefault") value(ctx, instruction.arg(3)) else 0
            ctx.dsTextListMenu(line, dsList, preselected)
          }
          else -> {
            log.warn { "Script ${program.id.stable}: no client menu for $menu, answering as B pressed" }
            MULTI_B_PRESSED
          }
        }
    ctx.setVar(namespaced("VAR_RESULT"), result)
  }

  /**
   * Whether the next thing shown after this `message` is a multichoice over the same text. Looks
   * past commands that touch no dialog (waits, variable moves, compares) and through a switch when
   * every case leads to a multichoice - the elevators pick their menu's preselected row that way.
   * The parser lowers `switch` to copyvar and `case` to compare + goto_if_eq (PretScriptParser).
   */
  private fun multichoiceFollows(state: RuntimeState): Boolean =
      multichoiceFollows(state.activeProgram, state.pc + 1, HashSet())

  /** A branch target: a label of [program] itself, or another program of the corpus at its start. */
  private fun branchTarget(program: ScriptProgram, label: String): Pair<ScriptProgram, Int>? =
      program.labels[label]?.let { program to it } ?: programLibrary[label]?.let { it to 0 }

  private fun multichoiceFollows(program: ScriptProgram, start: Int, visited: MutableSet<String>): Boolean {
    var pc = start
    var caseTargets = 0
    while (pc in program.instructions.indices && visited.add("${program.id.stable}:$pc")) {
      val instruction = program.instructions[pc]
      when (instruction.command) {
        "waitmessage", "setvar", "copyvar", "specialvar", "compare" -> pc++
        "multichoice", "multichoicedefault", "multichoicegrid", "ds_menu" -> return true
        "special" -> return if (instruction.arg(0).token == "ListMenu") true else caseTargets > 0
        "goto_if_eq", "goto_if_ne", "goto_if_lt", "goto_if_le", "goto_if_gt", "goto_if_ge" -> {
          val (targetProgram, target) = branchTarget(program, instruction.arg(0).token) ?: return false
          if (!multichoiceFollows(targetProgram, target, visited)) return false
          caseTargets++
          pc++
        }
        "goto" -> {
          val (targetProgram, target) = branchTarget(program, instruction.arg(0).token) ?: return false
          return multichoiceFollows(targetProgram, target, visited)
        }
        else -> return caseTargets > 0
      }
    }
    return false
  }

  private fun runMessage(ctx: ScriptContext, instruction: ScriptInstruction): DialogLine {
    check(instruction.args.size == 1) {
      "Script ${program.id.stable} expected one argument in `${instruction.sourceLine}`"
    }
    val line = textLine(textArg(instruction, 0).token, instruction)
    ctx.showMessage(line)
    return line
  }

  private suspend fun runYesNoBox(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
  ) {
    check(instruction.args.size == 2) {
      "Script ${program.id.stable} expected two arguments in `${instruction.sourceLine}`"
    }
    byteArgument(instruction, 0)
    byteArgument(instruction, 1)
    val line =
        state.currentMessage
            ?: error(
                "Script ${program.id.stable} has no current message for " +
                    "`${instruction.sourceLine}`")
    ctx.setVar(namespaced("VAR_RESULT"), if (ctx.askYesNo(line)) 1 else 0)
  }

  private fun runTextColor(ctx: ScriptContext, instruction: ScriptInstruction) {
    check(instruction.args.size == 1) {
      "Script ${program.id.stable} expected one argument in `${instruction.sourceLine}`"
    }
    if (program.id.source != "firered") return

    val value =
        when (val arg = instruction.arg(0)) {
          is IntArg -> arg.value
          is SymbolArg ->
              when (arg.token) {
                "NPC_TEXT_COLOR_MALE" -> 0
                "NPC_TEXT_COLOR_FEMALE" -> 1
                "NPC_TEXT_COLOR_MON" -> 2
                "NPC_TEXT_COLOR_NEUTRAL" -> 3
                "NPC_TEXT_COLOR_DEFAULT" -> 0xFF
                else -> unsupportedPresentation(instruction, arg.token)
              }
          else -> unsupportedPresentation(instruction, arg.token)
        }
    val color =
        DialogTextColor.fromGbaValue(value)
            ?: unsupportedPresentation(instruction, value.toString())
    ctx.setDialogTextColor(color)
  }

  private fun byteArgument(instruction: ScriptInstruction, index: Int): Int {
    val value =
        (instruction.arg(index) as? IntArg)?.value
            ?: unsupportedPresentation(instruction, instruction.arg(index).token)
    check(value in 0..0xFF) {
      "Script ${program.id.stable} has invalid byte value $value for ${instruction.command} " +
          "from `${instruction.sourceLine}`"
    }
    return value
  }

  private fun unsupportedPresentation(instruction: ScriptInstruction, value: String): Nothing =
      error(
          "Script ${program.id.stable} cannot apply ${instruction.command} value $value from " +
              "`${instruction.sourceLine}`")

  private fun requireNoArgs(instruction: ScriptInstruction) {
    check(instruction.args.isEmpty()) {
      "Script ${program.id.stable} expected no arguments in `${instruction.sourceLine}`"
    }
  }

  private suspend fun <T> tracedWait(
      ctx: ScriptContext,
      operation: String,
      block: suspend () -> T,
  ): T {
    ctx.traceInterpreter { "${program.id.stable} suspended for $operation" }
    return try {
      block()
    } finally {
      ctx.traceInterpreter { "${program.id.stable} resumed after $operation" }
    }
  }

  /**
   * Runs the two ordinary single-trainer macros supported in this phase. The first battle ends the
   * source script unless trainerbattle_single supplied its explicit victory continuation; an
   * already-defeated or unavailable-rematch check falls through to the following instruction.
   */
  private suspend fun runTrainerBattle(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
      rematch: Boolean,
      whiteoutOnDefeat: Boolean = true,
      earlyRival: Boolean = false,
  ): Boolean {
    // trainerbattle_earlyrival: trainer, battle kind, defeat speech, the rival's victory speech.
    val expectedArgs = if (rematch) setOf(3) else if (earlyRival) setOf(4) else setOf(3, 4, 5)
    check(instruction.args.size in expectedArgs) {
      "Script ${program.id.stable} expected ${expectedArgs.joinToString(" or ")} arguments in " +
          "`${instruction.sourceLine}`"
    }

    val base = resolveTrainer(ctx, trainerArg(instruction, 0), instruction)
    val intro = if (earlyRival) null else textLine(textArg(instruction, 1).token, instruction)
    // The trainerbattle macro's third argument is the trainer's IN-BATTLE defeat speech - the
    // GBA engine shows it in the battle screen before control returns to the script. The battle
    // end packet carries its ROM dialog id and the client renders it before the prize money.
    val defeat = textLine(textArg(instruction, 2).token, instruction)
    instruction.args.getOrNull(4)?.let { music ->
      check(music.token in setOf("NO_MUSIC", "FALSE", "TRUE")) {
        "Script ${program.id.stable} cannot resolve trainer battle music ${music.token} from " +
            "`${instruction.sourceLine}`"
      }
    }

    val baseDefeated = TrainerStoryState.defeated(program.storyNamespace, base.id)
    if (!rematch && ctx.isFlagSet(baseDefeated)) {
      state.pc++
      return true
    }

    val readyKey = TrainerStoryState.rematchReady(program.storyNamespace, base.id)
    if (rematch && ctx.getVar(readyKey) == 0) {
      state.pc++
      return true
    }

    val opponent = if (rematch) resolveRematchTrainer(ctx, base, instruction) else base
    if (intro != null) tracedWait(ctx, "trainer dialog") { ctx.say(intro) }
    return when (val result =
        tracedWait(ctx, "battle ${opponent.constant}") {
          ctx.trainerBattle(opponent, defeat.textId, whiteoutOnDefeat)
        }) {
      BattleResult.VICTORY -> {
        state.lastBattleOutcome = B_OUTCOME_WON
        ctx.setFlag(TrainerStoryState.defeated(program.storyNamespace, opponent.id))
        if (rematch) ctx.setVar(readyKey, 0)
        val continuation =
            if (earlyRival) null
            else (instruction.args.getOrNull(3) as? LabelArg)?.takeUnless { it.token == "FALSE" }
        if (continuation == null) {
          false
        } else {
          jumpTo(state, instruction, continuation)
          true
        }
      }
      BattleResult.DEFEAT -> {
        state.lastBattleOutcome = B_OUTCOME_LOST
        // The early rival: the script carries on after a loss (Oak's lab, Route 22).
        if (whiteoutOnDefeat) false else { state.pc++; true }
      }
      BattleResult.DISCONNECTED -> false
      BattleResult.FAILED,
      BattleResult.FLED,
      BattleResult.CAUGHT ->
          error(
              "Script ${program.id.stable} trainer battle ${opponent.constant} ended with " +
                  "$result at `${instruction.sourceLine}`")
    }
  }

  /**
   * The decomp pokemart: resolves the shelf's data label (kept as synthetic mart_item rows by the
   * parser) and opens the client's own mart window on it.
   */
  private fun openMart(ctx: ScriptContext, state: RuntimeState, instruction: ScriptInstruction) {
    val label = labelArg(instruction, 0).token
    val holder =
        if (state.activeProgram.labels.containsKey(label)) state.activeProgram
        else
            programLibrary[label]
                ?: error(
                    "Script ${program.id.stable} has no mart shelf $label from " +
                        "`${instruction.sourceLine}`")
    val start =
        holder.labels[label]
            ?: error(
                "Script ${program.id.stable} has no mart shelf $label from " +
                    "`${instruction.sourceLine}`")
    val shelf = mutableListOf<de.fiereu.openmmo.items.ItemDef>()
    var index = start
    shelfWalk@ while (index < holder.instructions.size &&
        holder.instructions[index].command == "mart_item") {
      for (arg in holder.instructions[index].args) {
        val token = arg.token
        if (token == "ITEM_NONE" || token == "0") break@shelfWalk
        // Mail is not in the retail catalogue; the shelf omits it like PokeMMO's own marts.
        if (token.endsWith("_MAIL")) continue
        shelf +=
            ctx.resolveItem(token)
                ?: error("Script ${program.id.stable} mart shelf $label offers unknown item $token")
      }
      index++
    }
    check(shelf.isNotEmpty()) { "Script ${program.id.stable} mart shelf $label is empty" }
    ctx.pokemart(*shelf.toTypedArray())
  }

  /**
   * The double-battle macros: same lifecycle as [runTrainerBattle] plus the vanilla two-able-mons
   * gate (fewer shows the NotEnoughMons text and no battle happens). The FIGHT itself currently
   * runs in the single format against the pair's whole team - the client renders true 2v2 (it is
   * the retail battle engine), but the server's turn engine tracks one active slot per side; the
   * doubles format is engine work, tracked separately. Continuation rides at arg 4, music at 5.
   */
  private suspend fun runTrainerBattleDouble(
      ctx: ScriptContext,
      state: RuntimeState,
      instruction: ScriptInstruction,
      rematch: Boolean,
  ): Boolean {
    val expectedArgs = if (rematch) setOf(4) else setOf(4, 5, 6)
    check(instruction.args.size in expectedArgs) {
      "Script ${program.id.stable} expected ${expectedArgs.joinToString(" or ")} arguments in " +
          "`${instruction.sourceLine}`"
    }

    val base = resolveTrainer(ctx, trainerArg(instruction, 0), instruction)
    val intro = textLine(textArg(instruction, 1).token, instruction)
    val defeat = textLine(textArg(instruction, 2).token, instruction)
    val needTwoMons = textLine(textArg(instruction, 3).token, instruction)
    instruction.args.getOrNull(5)?.let { music ->
      check(music.token in setOf("NO_MUSIC", "FALSE", "TRUE")) {
        "Script ${program.id.stable} cannot resolve trainer battle music ${music.token} from " +
            "`${instruction.sourceLine}`"
      }
    }

    val baseDefeated = TrainerStoryState.defeated(program.storyNamespace, base.id)
    if (!rematch && ctx.isFlagSet(baseDefeated)) {
      state.pc++
      return true
    }
    val readyKey = TrainerStoryState.rematchReady(program.storyNamespace, base.id)
    if (rematch && ctx.getVar(readyKey) == 0) {
      state.pc++
      return true
    }

    // Vanilla gates a double battle on two able monsters - with fewer the pair shows their
    // "come back with two" line and no battle fires; the trainer stays undefeated.
    if (ctx.ablePartyCount() < 2) {
      tracedWait(ctx, "need-two-mons dialog") { ctx.say(needTwoMons) }
      return false
    }

    val opponent = if (rematch) resolveRematchTrainer(ctx, base, instruction) else base
    tracedWait(ctx, "trainer dialog") { ctx.say(intro) }
    return when (val result =
        tracedWait(ctx, "battle ${opponent.constant}") {
          ctx.trainerBattle(opponent, defeat.textId)
        }) {
      BattleResult.VICTORY -> {
        ctx.setFlag(TrainerStoryState.defeated(program.storyNamespace, opponent.id))
        if (rematch) ctx.setVar(readyKey, 0)
        val continuation =
            (instruction.args.getOrNull(4) as? LabelArg)?.takeUnless { it.token == "FALSE" }
        if (continuation == null) {
          false
        } else {
          jumpTo(state, instruction, continuation)
          true
        }
      }
      BattleResult.DEFEAT,
      BattleResult.DISCONNECTED -> false
      BattleResult.FAILED,
      BattleResult.FLED,
      BattleResult.CAUGHT ->
          error(
              "Script ${program.id.stable} trainer battle ${opponent.constant} ended with " +
                  "$result at `${instruction.sourceLine}`")
    }
  }

  private fun resolveTrainer(
      ctx: ScriptContext,
      arg: TrainerArg,
      instruction: ScriptInstruction,
  ): TrainerDef =
      try {
        ctx.resolveTrainer(arg.token)
      } catch (cause: IllegalStateException) {
        throw IllegalStateException(
            "Script ${program.id.stable} cannot resolve trainer ${arg.token} from " +
                "`${instruction.sourceLine}`: ${cause.message}",
            cause,
        )
      }

  private fun resolveRematchTrainer(
      ctx: ScriptContext,
      base: TrainerDef,
      instruction: ScriptInstruction,
  ): TrainerDef {
    check(base.rematchIds.size > 1) {
      "Script ${program.id.stable} trainer ${base.constant} has no rematch chain in " +
          "`${instruction.sourceLine}`"
    }
    val candidates = base.rematchIds.withIndex().drop(1).filter { it.value != null }
    check(candidates.isNotEmpty()) {
      "Script ${program.id.stable} trainer ${base.constant} has no rematch opponent in " +
          "`${instruction.sourceLine}`"
    }
    var selected =
        candidates.firstOrNull { (_, id) ->
          !ctx.isFlagSet(TrainerStoryState.defeated(program.storyNamespace, checkNotNull(id)))
        } ?: candidates.last()

    // FireRed limits its six Vs Seeker stages by story progression and falls back through SKIP
    // slots. Emerald's Match Call table has no equivalent stage gates.
    if (program.id.source == "firered") {
      while (selected.index > 0) {
        val requiredFlag = FIRE_RED_REMATCH_GATES[selected.index]
        if (requiredFlag == null || ctx.isFlagSet(namespaced(requiredFlag))) break
        selected = candidates.lastOrNull { it.index < selected.index } ?: IndexedValue(0, base.id)
      }
    }

    val selectedId = checkNotNull(selected.value)
    return base.takeIf { it.id == selectedId }
        ?: try {
          ctx.resolveTrainerById(selectedId)
        } catch (cause: IllegalStateException) {
          throw IllegalStateException(
              "Script ${program.id.stable} cannot resolve rematch trainer id $selectedId from " +
                  "`${instruction.sourceLine}`: ${cause.message}",
              cause,
          )
        }
  }

  /** STR_VAR_1..3 -> 1..3; anything else is not a string variable. */
  private fun stringVariable(token: String): Int? =
      if (token.startsWith("STR_VAR_")) token.removePrefix("STR_VAR_").toIntOrNull() else null

  private fun textLine(label: String, instruction: ScriptInstruction): DialogLine =
      textBindings[label]
          ?: error(
              "Script ${program.id.stable} references unknown text label $label from " +
                  "`${instruction.sourceLine}`")

  private fun value(ctx: ScriptContext, arg: ScriptArg): Int =
      when (arg) {
        is IntArg -> arg.value
        is VarArg -> ctx.getVar(namespaced(arg.token))
        is SymbolArg ->
            when (arg.token) {
              "TRUE" -> 1
              "FALSE" -> 0
              in InterpreterSupport.MOVE_TUTOR_INDEXES -> InterpreterSupport.MOVE_TUTOR_INDEXES.getValue(arg.token)
              // A map-local object id used as a value (setvar VAR_LAST_TALKED, LOCALID_X): the
              // pret local id, one above the object index, so a later applymovement finds it.
              else ->
                  (program.objectIds[arg.token]
                          ?: programLibrary.values.firstNotNullOfOrNull { it.objectIds[arg.token] })
                      ?.let { it + PRET_LOCAL_ID_OFFSET }
                      ?: error("Script ${program.id.stable} cannot resolve ${arg.token}")
            }
        else -> error("Script ${program.id.stable} expected a value, got ${arg.token}")
      }

  private fun immediateValue(arg: ScriptArg, instruction: ScriptInstruction): Int =
      when (arg) {
        is IntArg -> arg.value
        is SymbolArg -> valueForSymbol(arg)
        else ->
            error(
                "Script ${program.id.stable} expected an immediate value in " +
                    "`${instruction.sourceLine}`")
      }

  private fun valueForSymbol(arg: SymbolArg): Int =
      when (arg.token) {
        "TRUE" -> 1
        "FALSE" -> 0
        else -> error("Script ${program.id.stable} cannot resolve ${arg.token}")
      }

  private fun gbaValue(value: Int): Int = value and GBA_VALUE_MASK

  private fun compareValues(left: Int, right: Int): ComparisonResult {
    val gbaLeft = left and GBA_VALUE_MASK
    val gbaRight = right and GBA_VALUE_MASK
    return when {
      gbaLeft < gbaRight -> ComparisonResult.LESS
      gbaLeft == gbaRight -> ComparisonResult.EQUAL
      else -> ComparisonResult.GREATER
    }
  }

  private fun jumpTo(
      state: RuntimeState,
      instruction: ScriptInstruction,
      arg: LabelArg,
  ) {
    val local = state.activeProgram.labels[arg.token]
    if (local != null) {
      state.pc = local
      return
    }
    val dependency =
        programLibrary[arg.token]
            ?: error(
                "Script ${program.id.stable} has no label ${arg.token} from " +
                    "`${instruction.sourceLine}`")
    check(
        dependency.id.source == program.id.source &&
            dependency.id.gameCode == program.id.gameCode) {
          "Script ${program.id.stable} cannot jump across game sources to ${dependency.id.stable}"
        }
    state.activeProgram = dependency
    state.pc = dependency.labels[arg.token] ?: 0
  }

  private fun namespaced(token: String): String =
      if ('/' in token) token else "${program.storyNamespace}/$token"

  private fun ScriptInstruction.arg(index: Int): ScriptArg =
      args.getOrNull(index)
          ?: error("Script ${program.id.stable} missing argument $index in `$sourceLine`")

  private fun flagArg(instruction: ScriptInstruction, index: Int): FlagArg =
      instruction.arg(index) as? FlagArg
          ?: error("Script ${program.id.stable} expected flag in `${instruction.sourceLine}`")

  private fun varArg(instruction: ScriptInstruction, index: Int): VarArg =
      instruction.arg(index) as? VarArg
          ?: error("Script ${program.id.stable} expected var in `${instruction.sourceLine}`")

  private fun textArg(instruction: ScriptInstruction, index: Int): TextArg =
      instruction.arg(index) as? TextArg
          ?: error("Script ${program.id.stable} expected text label in `${instruction.sourceLine}`")

  private fun trainerArg(instruction: ScriptInstruction, index: Int): TrainerArg =
      instruction.arg(index) as? TrainerArg
          ?: error("Script ${program.id.stable} expected trainer in `${instruction.sourceLine}`")

  private fun objectArg(instruction: ScriptInstruction, index: Int): ObjectArg =
      instruction.arg(index) as? ObjectArg
          ?: error("Script ${program.id.stable} expected object id in `${instruction.sourceLine}`")

  private fun movementArg(instruction: ScriptInstruction, index: Int): MovementArg =
      instruction.arg(index) as? MovementArg
          ?: error(
              "Script ${program.id.stable} expected movement label in " +
                  "`${instruction.sourceLine}`")

  private fun labelArg(instruction: ScriptInstruction, index: Int): LabelArg =
      instruction.arg(index) as? LabelArg
          ?: error("Script ${program.id.stable} expected label in `${instruction.sourceLine}`")

  /** A trainer speech line: only its client text id matters to the dialog service. */
  private class TrainerLine(override val textId: Int) : DialogLine

  /**
   * The trainer message of [kind], or the double-battle / rematch twin of it when the trainer only
   * carries those (Gen 4 partner trainers have no single-battle lines).
   */
  private fun trainerMessage(trainer: TrainerDef, kind: Int): Int? =
      trainer.messages[kind] ?: TRAINER_MESSAGE_FALLBACKS[kind]?.firstNotNullOfOrNull { trainer.messages[it] }

  private companion object {
    val TRAINER_MESSAGE_FALLBACKS = mapOf(0 to listOf(3, 7, 17), 1 to listOf(4, 8), 2 to listOf(5, 9), 17 to listOf(0, 18, 19), 3 to listOf(0), 5 to listOf(2), 4 to listOf(1))
    const val MAX_STEPS = 10_000
    const val GBA_VALUE_MASK = 0xFFFF
    const val FRAME_MILLIS = 17L
    const val MAX_DELAY_MILLIS = 5_000L
    const val LOCALID_NONE = 0
    const val LOCALID_PLAYER = 255
    const val PRET_LOCAL_ID_OFFSET = 1
    val FIRE_RED_REMATCH_GATES =
        mapOf(
            1 to "FLAG_GOT_VS_SEEKER",
            2 to "FLAG_WORLD_MAP_CELADON_CITY",
            3 to "FLAG_WORLD_MAP_FUCHSIA_CITY",
            4 to "FLAG_SYS_GAME_CLEAR",
            5 to "FLAG_SYS_CAN_LINK_WITH_RS",
        )
  }

  private data class RuntimeState(
      var activeProgram: ScriptProgram,
      var pc: Int = 0,
      var comparisonResult: ComparisonResult? = null,
      val callStack: ArrayDeque<ScriptLocation> = ArrayDeque(),
      val pendingMovements: MutableMap<MovementTarget, Deferred<Unit>> = linkedMapOf(),
      var lastMovementTarget: MovementTarget? = null,
      var currentMessage: DialogLine? = null,
      var skipNextWaitMessage: Boolean = false,
      /** GetBattleOutcome after the last scripted battle (B_OUTCOME_*). */
      var lastBattleOutcome: Int = B_OUTCOME_WON,
      var wildSpecies: Int = 0,
      var wildLevel: Int = 0,
  )

  private data class ScriptLocation(
      val program: ScriptProgram,
      val pc: Int,
  )

  private sealed interface MovementTarget {
    val display: String

    data object Player : MovementTarget {
      override val display = "LOCALID_PLAYER"
    }

    data class Npc(val localId: Int) : MovementTarget {
      override val display = "npc $localId"
    }
  }

  private enum class ComparisonResult {
    LESS,
    EQUAL,
    GREATER,
  }

  private enum class ComparisonCondition {
    EQ,
    NE,
    LT,
    LE,
    GT,
    GE;

    fun matches(result: ComparisonResult): Boolean =
        when (this) {
          EQ -> result == ComparisonResult.EQUAL
          NE -> result != ComparisonResult.EQUAL
          LT -> result == ComparisonResult.LESS
          LE -> result != ComparisonResult.GREATER
          GT -> result == ComparisonResult.GREATER
          GE -> result != ComparisonResult.LESS
        }

    companion object {
      fun fromCommand(command: String): ComparisonCondition =
          when (command.substringAfterLast('_')) {
            "eq" -> EQ
            "ne" -> NE
            "lt" -> LT
            "le" -> LE
            "gt" -> GT
            "ge" -> GE
            else -> error("Unknown comparison command $command")
          }
    }
  }
}

/**
 * Stable keys layered on the existing StoryService store, not a separate trainer-state database.
 */
internal object TrainerStoryState {
  fun defeated(namespace: String, trainerId: Int): String = "$namespace/trainer/$trainerId/defeated"

  fun rematchReady(namespace: String, baseTrainerId: Int): String =
      "$namespace/trainer/$baseTrainerId/rematch-ready"
}

/** The GBA answer when a multichoice is cancelled with B. */
private const val MULTI_B_PRESSED = 127

private const val B_OUTCOME_WON = 1

// Client string 11204, "Haunted by the ghosts.": the grey box shown when the Silph Scope is missing.
private const val HAUNTED_BY_GHOSTS_STRING = 11204
private const val B_OUTCOME_LOST = 2
private const val B_OUTCOME_RAN = 4
private const val B_OUTCOME_CAUGHT = 7

/** The Gen 4 common mart shelf: (item index, badge tier) - identical in Platinum and HeartGold. */
private val DS_COMMON_MART: List<Pair<Int, Int>> =
    listOf(4 to 1, 3 to 3, 2 to 4, 17 to 1, 26 to 2, 25 to 4, 24 to 5, 23 to 6, 28 to 3, 18 to 1, 22 to 1, 21 to 2, 19 to 2, 20 to 2, 27 to 4, 78 to 2, 79 to 2, 83 to 3, 84 to 4)
