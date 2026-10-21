package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.FiveIsland
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object FiveIsland_EventScript_Fisher : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(FiveIsland.BeenGettingBusierAroundHere)
}

internal object FiveIsland_EventScript_BugCatcher : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(FiveIsland.WeirdBuildingInMeadow)
}

internal object FiveIsland_EventScript_IslandSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(FiveIsland.IslandSign)
}

internal val FiveIslandScripts: Map<String, Script> =
    mapOf(
        "FiveIsland_EventScript_Fisher" to FiveIsland_EventScript_Fisher,
        "FiveIsland_EventScript_BugCatcher" to FiveIsland_EventScript_BugCatcher,
        "FiveIsland_EventScript_IslandSign" to FiveIsland_EventScript_IslandSign,
    )
