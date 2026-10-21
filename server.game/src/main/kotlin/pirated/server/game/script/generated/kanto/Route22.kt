package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route22
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route22_EventScript_LeagueGateSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Route22.LeagueGateSign)
}

internal val Route22Scripts: Map<String, Script> =
    mapOf(
        "Route22_EventScript_LeagueGateSign" to Route22_EventScript_LeagueGateSign,
    )
