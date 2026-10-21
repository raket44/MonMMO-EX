package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.OneIsland_House1
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object OneIsland_House1_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(OneIsland_House1.GazeUponVolcanoOnSunnyDays)
}

internal object OneIsland_House1_EventScript_OldWoman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(OneIsland_House1.LastTimeMtEmberErupted)
}

internal val OneIsland_House1Scripts: Map<String, Script> =
    mapOf(
        "OneIsland_House1_EventScript_OldMan" to OneIsland_House1_EventScript_OldMan,
        "OneIsland_House1_EventScript_OldWoman" to OneIsland_House1_EventScript_OldWoman,
    )
