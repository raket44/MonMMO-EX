package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.Route110_SeasideCyclingRoadNorthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route110_SeasideCyclingRoadNorthEntrance_EventScript_Clerk : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route110_SeasideCyclingRoadNorthEntrance.GoAllOutOnCyclingRoad)
}

internal val Route110_SeasideCyclingRoadNorthEntranceScripts: Map<String, Script> =
    mapOf(
        "Route110_SeasideCyclingRoadNorthEntrance_EventScript_Clerk" to
            Route110_SeasideCyclingRoadNorthEntrance_EventScript_Clerk,
    )
