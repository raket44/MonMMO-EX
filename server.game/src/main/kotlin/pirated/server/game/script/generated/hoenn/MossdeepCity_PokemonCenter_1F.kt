package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.MossdeepCity_PokemonCenter_1F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

/**
 * Not ported yet. Decomp body:
 * ```
 * setvar VAR_0x800B, LOCALID_MOSSDEEP_NURSE
 * call Common_EventScript_PkmnCenterNurse
 * waitmessage
 * waitbuttonpress
 * release
 * end
 * ```
 */
internal object MossdeepCity_PokemonCenter_1F_EventScript_Nurse : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port MossdeepCity_PokemonCenter_1F_EventScript_Nurse")
}

internal object MossdeepCity_PokemonCenter_1F_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(MossdeepCity_PokemonCenter_1F.GymLeaderDuoFormidable)
}

internal object MossdeepCity_PokemonCenter_1F_EventScript_Girl : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(MossdeepCity_PokemonCenter_1F.AbilitiesMightChangeMoves)
}

internal val MossdeepCity_PokemonCenter_1FScripts: Map<String, Script> =
    mapOf(
        "MossdeepCity_PokemonCenter_1F_EventScript_Nurse" to
            MossdeepCity_PokemonCenter_1F_EventScript_Nurse,
        "MossdeepCity_PokemonCenter_1F_EventScript_Woman" to
            MossdeepCity_PokemonCenter_1F_EventScript_Woman,
        "MossdeepCity_PokemonCenter_1F_EventScript_Girl" to
            MossdeepCity_PokemonCenter_1F_EventScript_Girl,
    )
