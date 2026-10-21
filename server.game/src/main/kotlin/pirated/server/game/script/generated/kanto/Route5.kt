package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route5
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route5_EventScript_UndergroundPathSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Route5.UndergroundPathSign)
}

internal val Route5Scripts: Map<String, Script> =
    mapOf(
        "Route5_EventScript_UndergroundPathSign" to Route5_EventScript_UndergroundPathSign,
    )
