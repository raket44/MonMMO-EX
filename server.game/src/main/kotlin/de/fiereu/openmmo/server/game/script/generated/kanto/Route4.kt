package de.fiereu.openmmo.server.game.script.generated.kanto

import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.dialog.generated.kanto.Misc
import de.fiereu.openmmo.dialog.generated.kanto.Route4
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.story.generated.kanto.KantoFlags

/** Our imported TM items (monmmo/imported-items.csv) for the two Gen 1 techniques. */
private const val TM_MEGA_PUNCH = 20000
private const val TM_MEGA_KICK = 20011

/**
 * Retail's Route 4 karate brothers: no party menu here - the one you side with hands over the
 * TM of his technique, once, and the other refuses you afterwards. The ROM's own lines carry
 * the exchange (the "which POKeMON?" line is skipped, there is nothing to pick).
 */
private suspend fun karateBrother(
    ctx: ScriptContext,
    teach: DialogLine,
    declined: DialogLine,
    taught: DialogLine,
    myFlag: String,
    rivalFlag: String,
    tmItemId: Int,
) {
  if (ctx.isFlagSet(myFlag)) return ctx.say(taught)
  if (ctx.isFlagSet(rivalFlag)) return ctx.say(declined)
  if (!ctx.askYesNo(teach)) return ctx.say(declined)
  val tm = ctx.resolveItemWire(tmItemId) ?: return ctx.say(declined)
  if (!ctx.giveItem(tm)) return ctx.say(declined)
  ctx.announceItem(tm)
  ctx.setFlag(myFlag)
  ctx.say(taught)
}

internal object Route4_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(Route4.TrippedOverGeodude)
}

/**
 * Not ported yet. Decomp body:
 * ```
 * trainerbattle_single TRAINER_LASS_CRISSY, Route4_Text_CrissyIntro, Route4_Text_CrissyDefeat
 * specialvar VAR_RESULT, ShouldTryRematchBattle
 * goto_if_eq VAR_RESULT, TRUE, Route4_EventScript_CrissyRematch
 * msgbox Route4_Text_CrissyPostBattle, MSGBOX_AUTOCLOSE
 * end
 * ```
 */
internal object Route4_EventScript_Crissy : Script {
  override suspend fun run(ctx: ScriptContext) = TODO("port Route4_EventScript_Crissy")
}

/**
 * Not ported yet. Decomp body:
 * ```
 * finditem ITEM_TM05
 * end
 * ```
 */
internal object Route4_EventScript_ItemTM05 : Script {
  override suspend fun run(ctx: ScriptContext) = TODO("port Route4_EventScript_ItemTM05")
}

/**
 * Not ported yet. Decomp body:
 * ```
 * lock
 * faceplayer
 * famechecker FAMECHECKER_BROCK, 3
 * msgbox Route4_Text_PeopleLikeAndRespectBrock
 * release
 * end
 * ```
 */
internal object Route4_EventScript_Boy : Script {
  override suspend fun run(ctx: ScriptContext) = TODO("port Route4_EventScript_Boy")
}

internal object Route4_EventScript_MegaPunchTutor : Script {
  override suspend fun run(ctx: ScriptContext) =
      karateBrother(
          ctx,
          teach = Misc.Text_MegaPunchTeach,
          declined = Misc.Text_MegaPunchDeclined,
          taught = Misc.Text_MegaPunchTaught,
          myFlag = KantoFlags.FLAG_TUTOR_MEGA_PUNCH,
          rivalFlag = KantoFlags.FLAG_TUTOR_MEGA_KICK,
          tmItemId = TM_MEGA_PUNCH)
}

internal object Route4_EventScript_MegaKickTutor : Script {
  override suspend fun run(ctx: ScriptContext) =
      karateBrother(
          ctx,
          teach = Misc.Text_MegaKickTeach,
          declined = Misc.Text_MegaKickDeclined,
          taught = Misc.Text_MegaKickTaught,
          myFlag = KantoFlags.FLAG_TUTOR_MEGA_KICK,
          rivalFlag = KantoFlags.FLAG_TUTOR_MEGA_PUNCH,
          tmItemId = TM_MEGA_KICK)
}

internal object Route4_EventScript_MtMoonSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Route4.MtMoonEntrance)
}

internal object Route4_EventScript_RouteSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Route4.RouteSign)
}

internal val Route4Scripts: Map<String, Script> =
    mapOf(
        "Route4_EventScript_Woman" to Route4_EventScript_Woman,
        "Route4_EventScript_Crissy" to Route4_EventScript_Crissy,
        "Route4_EventScript_ItemTM05" to Route4_EventScript_ItemTM05,
        "Route4_EventScript_Boy" to Route4_EventScript_Boy,
        "Route4_EventScript_MegaPunchTutor" to Route4_EventScript_MegaPunchTutor,
        "Route4_EventScript_MegaKickTutor" to Route4_EventScript_MegaKickTutor,
        "Route4_EventScript_MtMoonSign" to Route4_EventScript_MtMoonSign,
        "Route4_EventScript_RouteSign" to Route4_EventScript_RouteSign,
    )
