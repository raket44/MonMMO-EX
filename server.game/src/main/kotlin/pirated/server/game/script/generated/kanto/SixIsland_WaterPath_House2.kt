package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SixIsland_WaterPath_House2
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SixIsland_WaterPath_House2_EventScript_Man : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SixIsland_WaterPath_House2.MyNeighborMeasuredMe)
}

internal val SixIsland_WaterPath_House2Scripts: Map<String, Script> =
    mapOf(
        "SixIsland_WaterPath_House2_EventScript_Man" to SixIsland_WaterPath_House2_EventScript_Man,
    )
