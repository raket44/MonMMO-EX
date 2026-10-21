package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route12_NorthEntrance_1F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route12_NorthEntrance_1F_EventScript_Guard : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route12_NorthEntrance_1F.LookoutSpotUpstairs)
}

internal val Route12_NorthEntrance_1FScripts: Map<String, Script> =
    mapOf(
        "Route12_NorthEntrance_1F_EventScript_Guard" to Route12_NorthEntrance_1F_EventScript_Guard,
    )
