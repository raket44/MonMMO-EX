package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SSAnne_2F_Room6
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SSAnne_2F_Room6_EventScript_Woman2 : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_2F_Room6.ManyPeopleGetSeasick)
}

internal object SSAnne_2F_Room6_EventScript_Woman1 : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_2F_Room6.CaptainIsAwfullySick)
}

internal val SSAnne_2F_Room6Scripts: Map<String, Script> =
    mapOf(
        "SSAnne_2F_Room6_EventScript_Woman2" to SSAnne_2F_Room6_EventScript_Woman2,
        "SSAnne_2F_Room6_EventScript_Woman1" to SSAnne_2F_Room6_EventScript_Woman1,
    )
