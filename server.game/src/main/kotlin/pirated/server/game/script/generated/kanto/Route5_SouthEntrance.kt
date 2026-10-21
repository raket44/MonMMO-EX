package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route5_SouthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route5_SouthEntrance_EventScript_Guard : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(Route5_SouthEntrance.HiHowsItGoing)
}

internal val Route5_SouthEntranceScripts: Map<String, Script> =
    mapOf(
        "Route5_SouthEntrance_EventScript_Guard" to Route5_SouthEntrance_EventScript_Guard,
    )
