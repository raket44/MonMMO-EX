package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.SSAnne_1F_Room3
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SSAnne_1F_Room3_EventScript_LittleGirl : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_1F_Room3.AlwaysTravelWithWigglytuff)
}

/**
 * Not ported yet. Decomp body:
 * ```
 * lock
 * faceplayer
 * waitse
 * playmoncry SPECIES_WIGGLYTUFF, CRY_MODE_NORMAL
 * msgbox SSAnne_1F_Room3_Text_Wigglytuff
 * waitmoncry
 * release
 * end
 * ```
 */
internal object SSAnne_1F_Room3_EventScript_Wigglytuff : Script {
  override suspend fun run(ctx: ScriptContext) = TODO("port SSAnne_1F_Room3_EventScript_Wigglytuff")
}

internal object SSAnne_1F_Room3_EventScript_BaldingMan : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(SSAnne_1F_Room3.CruiseIsElegantAndCozy)
}

internal val SSAnne_1F_Room3Scripts: Map<String, Script> =
    mapOf(
        "SSAnne_1F_Room3_EventScript_LittleGirl" to SSAnne_1F_Room3_EventScript_LittleGirl,
        "SSAnne_1F_Room3_EventScript_Wigglytuff" to SSAnne_1F_Room3_EventScript_Wigglytuff,
        "SSAnne_1F_Room3_EventScript_BaldingMan" to SSAnne_1F_Room3_EventScript_BaldingMan,
    )
