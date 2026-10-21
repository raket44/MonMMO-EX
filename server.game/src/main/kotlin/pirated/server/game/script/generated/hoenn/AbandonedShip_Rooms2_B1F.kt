package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.AbandonedShip_Rooms2_B1F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object AbandonedShip_Rooms2_B1F_EventScript_Camper : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(AbandonedShip_Rooms2_B1F.PerfectPlaceToGoExploring)
}

/**
 * Not ported yet. Decomp body:
 * ```
 * finditem ITEM_DIVE_BALL
 * end
 * ```
 */
internal object AbandonedShip_Rooms2_B1F_EventScript_ItemDiveBall : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port AbandonedShip_Rooms2_B1F_EventScript_ItemDiveBall")
}

internal val AbandonedShip_Rooms2_B1FScripts: Map<String, Script> =
    mapOf(
        "AbandonedShip_Rooms2_B1F_EventScript_Camper" to
            AbandonedShip_Rooms2_B1F_EventScript_Camper,
        "AbandonedShip_Rooms2_B1F_EventScript_ItemDiveBall" to
            AbandonedShip_Rooms2_B1F_EventScript_ItemDiveBall,
    )
