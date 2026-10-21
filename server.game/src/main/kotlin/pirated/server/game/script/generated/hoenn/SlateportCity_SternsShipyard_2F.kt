package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.SlateportCity_SternsShipyard_2F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SlateportCity_SternsShipyard_2F_EventScript_Scientist1 : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SlateportCity_SternsShipyard_2F.ShipDesignMoreLikeBuilding)
}

internal object SlateportCity_SternsShipyard_2F_EventScript_Scientist2 : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SlateportCity_SternsShipyard_2F.FloatsBecauseBuoyancy)
}

internal val SlateportCity_SternsShipyard_2FScripts: Map<String, Script> =
    mapOf(
        "SlateportCity_SternsShipyard_2F_EventScript_Scientist1" to
            SlateportCity_SternsShipyard_2F_EventScript_Scientist1,
        "SlateportCity_SternsShipyard_2F_EventScript_Scientist2" to
            SlateportCity_SternsShipyard_2F_EventScript_Scientist2,
    )
