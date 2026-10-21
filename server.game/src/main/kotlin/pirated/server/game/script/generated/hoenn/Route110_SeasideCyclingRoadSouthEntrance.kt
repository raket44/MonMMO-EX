package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.Route110_SeasideCyclingRoadSouthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route110_SeasideCyclingRoadSouthEntrance_EventScript_Clerk : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route110_SeasideCyclingRoadSouthEntrance.GoAllOutOnCyclingRoad)
}

internal val Route110_SeasideCyclingRoadSouthEntranceScripts: Map<String, Script> =
    mapOf(
        "Route110_SeasideCyclingRoadSouthEntrance_EventScript_Clerk" to
            Route110_SeasideCyclingRoadSouthEntrance_EventScript_Clerk,
    )
