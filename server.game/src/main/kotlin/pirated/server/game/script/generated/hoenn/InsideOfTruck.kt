package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.InsideOfTruck
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object InsideOfTruck_EventScript_MovingBox : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(InsideOfTruck.BoxPrintedWithMonLogo)
}

internal val InsideOfTruckScripts: Map<String, Script> =
    mapOf(
        "InsideOfTruck_EventScript_MovingBox" to InsideOfTruck_EventScript_MovingBox,
    )
