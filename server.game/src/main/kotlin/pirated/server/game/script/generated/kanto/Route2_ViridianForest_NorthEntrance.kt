package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.Route2_ViridianForest_NorthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Route2_ViridianForest_NorthEntrance_EventScript_Youngster : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route2_ViridianForest_NorthEntrance.ManyMonsOnlyInForests)
}

internal object Route2_ViridianForest_NorthEntrance_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route2_ViridianForest_NorthEntrance.CanCutSkinnyTrees)
}

internal object Route2_ViridianForest_NorthEntrance_EventScript_CooltrainerF : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(Route2_ViridianForest_NorthEntrance.CanCancelEvolution)
}

internal val Route2_ViridianForest_NorthEntranceScripts: Map<String, Script> =
    mapOf(
        "Route2_ViridianForest_NorthEntrance_EventScript_Youngster" to
            Route2_ViridianForest_NorthEntrance_EventScript_Youngster,
        "Route2_ViridianForest_NorthEntrance_EventScript_OldMan" to
            Route2_ViridianForest_NorthEntrance_EventScript_OldMan,
        "Route2_ViridianForest_NorthEntrance_EventScript_CooltrainerF" to
            Route2_ViridianForest_NorthEntrance_EventScript_CooltrainerF,
    )
