package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.CeladonCity_Condominiums_Roof
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object CeladonCity_Condominiums_Roof_EventScript_Sign : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.sign(CeladonCity_Condominiums_Roof.IKnowEverything)
}

internal val CeladonCity_Condominiums_RoofScripts: Map<String, Script> =
    mapOf(
        "CeladonCity_Condominiums_Roof_EventScript_Sign" to
            CeladonCity_Condominiums_Roof_EventScript_Sign,
    )
