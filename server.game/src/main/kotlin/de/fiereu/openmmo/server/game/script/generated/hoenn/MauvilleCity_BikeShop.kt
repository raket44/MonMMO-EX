package de.fiereu.openmmo.server.game.script.generated.hoenn

import de.fiereu.openmmo.dialog.generated.hoenn.MauvilleCity_BikeShop
import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext

/**
 * Not ported yet. Decomp body:
 * ```
 * lock
 * faceplayer
 * goto_if_set FLAG_RECEIVED_BIKE, MauvilleCity_BikeShop_EventScript_AskSwitchBikes
 * goto_if_set FLAG_DECLINED_BIKE, MauvilleCity_BikeShop_EventScript_SkipGreeting
 * msgbox MauvilleCity_BikeShop_Text_RydelGreeting, MSGBOX_DEFAULT
 * msgbox MauvilleCity_BikeShop_Text_DidYouComeFromFarAway, MSGBOX_YESNO
 * goto_if_eq VAR_RESULT, YES, MauvilleCity_BikeShop_EventScript_YesFar
 * goto_if_eq VAR_RESULT, NO, MauvilleCity_BikeShop_EventScript_NotFar
 * end
 * ```
 */
/**
 * Rydel's Cycles. The ROM offers Mach or Acro through a shop menu the client cannot draw; this
 * client rides one Bicycle whichever the key item, so a far-away traveller gets the Mach Bike
 * straight away (and with it the client's Bicycle, through the regional-bike rule). The switch
 * counter keeps the ROM's Mach/Acro swap.
 */
internal object MauvilleCity_BikeShop_EventScript_Rydel : Script {
  override suspend fun run(ctx: ScriptContext) {
    if (ctx.isFlagSet(HoennFlags.FLAG_RECEIVED_BIKE)) return switchBikes(ctx)
    if (!ctx.isFlagSet(HoennFlags.FLAG_DECLINED_BIKE)) ctx.say(MauvilleCity_BikeShop.RydelGreeting)
    if (!ctx.askYesNo(MauvilleCity_BikeShop.DidYouComeFromFarAway)) {
      ctx.setFlag(HoennFlags.FLAG_DECLINED_BIKE)
      return ctx.say(MauvilleCity_BikeShop.GuessYouDontNeedBike)
    }
    ctx.setFlag(HoennFlags.FLAG_RECEIVED_BIKE)
    ctx.say(MauvilleCity_BikeShop.ChoseMachBike)
    ctx.resolveItem("ITEM_MACH_BIKE")?.let { if (ctx.giveItem(it)) ctx.announceItem(it) }
    ctx.say(MauvilleCity_BikeShop.ComeBackToSwitchBikes)
  }

  private suspend fun switchBikes(ctx: ScriptContext) {
    if (!ctx.askYesNo(MauvilleCity_BikeShop.WantToSwitchBikes)) return ctx.say(MauvilleCity_BikeShop.HappyYouLikeIt)
    ctx.say(MauvilleCity_BikeShop.IllSwitchBikes)
    val mach = ctx.resolveItem("ITEM_MACH_BIKE")
    val acro = ctx.resolveItem("ITEM_ACRO_BIKE")
    if (mach == null || acro == null) return ctx.say(MauvilleCity_BikeShop.OhYourBikeIsInPC)
    when {
      ctx.itemCount(acro) > 0 -> {
        ctx.say(MauvilleCity_BikeShop.ExchangedAcroForMach)
        ctx.takeItem(acro)
        ctx.giveItem(mach)
      }
      ctx.itemCount(mach) > 0 -> {
        ctx.say(MauvilleCity_BikeShop.ExchangedMachForAcro)
        ctx.takeItem(mach)
        ctx.giveItem(acro)
      }
      else -> return ctx.say(MauvilleCity_BikeShop.OhYourBikeIsInPC)
    }
    ctx.say(MauvilleCity_BikeShop.ComeBackToSwitchBikes)
  }
}

internal object MauvilleCity_BikeShop_EventScript_Assistant : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(MauvilleCity_BikeShop.HandbooksAreInBack)
}

/**
 * Not ported yet. Decomp body:
 * ```
 * message MauvilleCity_BikeShop_Text_MachHandbookWhichPage
 * waitmessage
 * goto MauvilleCity_BikeShop_EventScript_ChooseMachHandbookPage
 * end
 * ```
 */
internal object MauvilleCity_BikeShop_EventScript_MachBikeHandbook : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port MauvilleCity_BikeShop_EventScript_MachBikeHandbook")
}

/**
 * Not ported yet. Decomp body:
 * ```
 * message MauvilleCity_BikeShop_Text_AcroHandbookWhichPage
 * waitmessage
 * goto MauvilleCity_BikeShop_EventScript_ChooseAcroHandbookPage
 * end
 * ```
 */
internal object MauvilleCity_BikeShop_EventScript_AcroBikeHandbook : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port MauvilleCity_BikeShop_EventScript_AcroBikeHandbook")
}

internal val MauvilleCity_BikeShopScripts: Map<String, Script> =
    mapOf(
        "MauvilleCity_BikeShop_EventScript_Rydel" to MauvilleCity_BikeShop_EventScript_Rydel,
        "MauvilleCity_BikeShop_EventScript_Assistant" to
            MauvilleCity_BikeShop_EventScript_Assistant,
        "MauvilleCity_BikeShop_EventScript_MachBikeHandbook" to
            MauvilleCity_BikeShop_EventScript_MachBikeHandbook,
        "MauvilleCity_BikeShop_EventScript_AcroBikeHandbook" to
            MauvilleCity_BikeShop_EventScript_AcroBikeHandbook,
    )
