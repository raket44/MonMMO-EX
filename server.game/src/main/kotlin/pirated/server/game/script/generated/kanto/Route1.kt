package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route1
import pirated.items.generated.Items
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext
import pirated.story.generated.kanto.KantoFlags

internal object Route1_EventScript_MartClerk : Script {
  override suspend fun run(ctx: ScriptContext) {
    if (ctx.isFlagSet(KantoFlags.FLAG_GOT_POTION_ON_ROUTE_1)) {
      return ctx.say(Route1.ComeSeeUsIfYouNeedPokeBalls)
    }
    ctx.say(Route1.WorkAtPokeMartTakeSample)
    ctx.giveItem(Items.POTION)
    ctx.say(Route1.PutPotionAway)
    ctx.setFlag(KantoFlags.FLAG_GOT_POTION_ON_ROUTE_1)
  }
}

internal object Route1_EventScript_Boy : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(Route1.CanJumpFromLedges)
}

internal object Route1_EventScript_RouteSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Route1.RouteSign)
}

internal val Route1Scripts: Map<String, Script> =
    mapOf(
        "Route1_EventScript_MartClerk" to Route1_EventScript_MartClerk,
        "Route1_EventScript_Boy" to Route1_EventScript_Boy,
        "Route1_EventScript_RouteSign" to Route1_EventScript_RouteSign,
    )
