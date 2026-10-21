package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.MauvilleCity_PokemonCenter_2F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object MauvilleCity_PokemonCenter_2F_EventScript_Youngster : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(MauvilleCity_PokemonCenter_2F.Youngster)
}

internal val MauvilleCity_PokemonCenter_2FScripts: Map<String, Script> =
    mapOf(
        "MauvilleCity_PokemonCenter_2F_EventScript_Youngster" to
            MauvilleCity_PokemonCenter_2F_EventScript_Youngster,
    )
