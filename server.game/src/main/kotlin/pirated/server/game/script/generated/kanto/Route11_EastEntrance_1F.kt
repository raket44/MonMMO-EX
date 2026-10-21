package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route11_EastEntrance_1F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route11_EastEntrance_1F_EventScript_TopGuard : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route11_EastEntrance_1F.ManInLavenderRatesNames)
}

internal object Route11_EastEntrance_1F_EventScript_BottomGuard : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route11_EastEntrance_1F.RockTunnelToReachLavender)
}

internal val Route11_EastEntrance_1FScripts: Map<String, Script> =
    mapOf(
        "Route11_EastEntrance_1F_EventScript_TopGuard" to
            Route11_EastEntrance_1F_EventScript_TopGuard,
        "Route11_EastEntrance_1F_EventScript_BottomGuard" to
            Route11_EastEntrance_1F_EventScript_BottomGuard,
    )
