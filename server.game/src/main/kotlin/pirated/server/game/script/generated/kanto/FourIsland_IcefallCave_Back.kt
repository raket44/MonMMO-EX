package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.FourIsland_IcefallCave_Back
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object FourIsland_IcefallCave_Back_EventScript_Lorelei : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(FourIsland_IcefallCave_Back.ThankYouThisIsAwful)
}

internal val FourIsland_IcefallCave_BackScripts: Map<String, Script> =
    mapOf(
        "FourIsland_IcefallCave_Back_EventScript_Lorelei" to
            FourIsland_IcefallCave_Back_EventScript_Lorelei,
    )
