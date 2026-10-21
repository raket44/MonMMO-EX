package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SSAnne_1F_Room1
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SSAnne_1F_Room1_EventScript_Gentleman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_1F_Room1.ImAGlobalPoliceAgent)
}

internal val SSAnne_1F_Room1Scripts: Map<String, Script> =
    mapOf(
        "SSAnne_1F_Room1_EventScript_Gentleman" to SSAnne_1F_Room1_EventScript_Gentleman,
    )
