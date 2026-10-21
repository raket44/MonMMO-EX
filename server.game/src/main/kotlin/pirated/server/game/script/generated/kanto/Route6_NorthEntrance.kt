package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route6_NorthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route6_NorthEntrance_EventScript_Guard : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(Route6_NorthEntrance.HiHowsItGoing)
}

internal val Route6_NorthEntranceScripts: Map<String, Script> =
    mapOf(
        "Route6_NorthEntrance_EventScript_Guard" to Route6_NorthEntrance_EventScript_Guard,
    )
