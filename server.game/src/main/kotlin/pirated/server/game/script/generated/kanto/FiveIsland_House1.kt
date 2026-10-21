package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.FiveIsland_House1
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object FiveIsland_House1_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(FiveIsland_House1.HusbandWentOffFishing)
}

internal val FiveIsland_House1Scripts: Map<String, Script> =
    mapOf(
        "FiveIsland_House1_EventScript_Woman" to FiveIsland_House1_EventScript_Woman,
    )
