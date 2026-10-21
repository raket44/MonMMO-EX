package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.SlateportCity_House
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SlateportCity_House_EventScript_PokefanM : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SlateportCity_House.NatureToDoWithStatGains)
}

internal object SlateportCity_House_EventScript_Girl : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SlateportCity_House.MustBeGoingToBattleTent)
}

internal val SlateportCity_HouseScripts: Map<String, Script> =
    mapOf(
        "SlateportCity_House_EventScript_PokefanM" to SlateportCity_House_EventScript_PokefanM,
        "SlateportCity_House_EventScript_Girl" to SlateportCity_House_EventScript_Girl,
    )
