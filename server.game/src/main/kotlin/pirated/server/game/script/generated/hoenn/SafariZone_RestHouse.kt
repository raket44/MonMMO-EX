package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.SafariZone_RestHouse
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SafariZone_RestHouse_EventScript_Youngster : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SafariZone_RestHouse.Youngster)
}

internal object SafariZone_RestHouse_EventScript_PsychicM : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SafariZone_RestHouse.PsychicM)
}

internal object SafariZone_RestHouse_EventScript_FatMan : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SafariZone_RestHouse.FatMan)
}

internal val SafariZone_RestHouseScripts: Map<String, Script> =
    mapOf(
        "SafariZone_RestHouse_EventScript_Youngster" to SafariZone_RestHouse_EventScript_Youngster,
        "SafariZone_RestHouse_EventScript_PsychicM" to SafariZone_RestHouse_EventScript_PsychicM,
        "SafariZone_RestHouse_EventScript_FatMan" to SafariZone_RestHouse_EventScript_FatMan,
    )
