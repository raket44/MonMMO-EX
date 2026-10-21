package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.Underwater_SeafloorCavern
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object Underwater_SeafloorCavern_EventScript_CheckStolenSub : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(Underwater_SeafloorCavern.SubExplorer1)
}

internal val Underwater_SeafloorCavernScripts: Map<String, Script> =
    mapOf(
        "Underwater_SeafloorCavern_EventScript_CheckStolenSub" to
            Underwater_SeafloorCavern_EventScript_CheckStolenSub,
    )
