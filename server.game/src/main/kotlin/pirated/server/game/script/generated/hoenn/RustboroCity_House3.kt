package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.RustboroCity_House3
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object RustboroCity_House3_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(RustboroCity_House3.IGivePerfectlySuitedNicknames)
}

internal object RustboroCity_House3_EventScript_OldWoman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(RustboroCity_House3.NamingPikachuPekachu)
}

/**
 * Not ported yet. Decomp body:
 * ```
 * lock
 * faceplayer
 * waitse
 * playmoncry SPECIES_PIKACHU, CRY_MODE_NORMAL
 * msgbox RustboroCity_House3_Text_Pekachu, MSGBOX_DEFAULT
 * waitmoncry
 * release
 * end
 * ```
 */
internal object RustboroCity_House3_EventScript_Pekachu : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port RustboroCity_House3_EventScript_Pekachu")
}

internal val RustboroCity_House3Scripts: Map<String, Script> =
    mapOf(
        "RustboroCity_House3_EventScript_OldMan" to RustboroCity_House3_EventScript_OldMan,
        "RustboroCity_House3_EventScript_OldWoman" to RustboroCity_House3_EventScript_OldWoman,
        "RustboroCity_House3_EventScript_Pekachu" to RustboroCity_House3_EventScript_Pekachu,
    )
