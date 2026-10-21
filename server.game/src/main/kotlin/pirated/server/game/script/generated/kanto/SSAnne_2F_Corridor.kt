package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SSAnne_2F_Corridor
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SSAnne_2F_Corridor_EventScript_Sailor : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_2F_Corridor.ThisShipIsLuxuryLiner)
}

internal val SSAnne_2F_CorridorScripts: Map<String, Script> =
    mapOf(
        "SSAnne_2F_Corridor_EventScript_Sailor" to SSAnne_2F_Corridor_EventScript_Sailor,
    )
