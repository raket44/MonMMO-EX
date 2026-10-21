package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SixIsland_House
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SixIsland_House_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SixIsland_House.GoodPlaceForNatureAndHistory)
}

internal val SixIsland_HouseScripts: Map<String, Script> =
    mapOf(
        "SixIsland_House_EventScript_OldMan" to SixIsland_House_EventScript_OldMan,
    )
