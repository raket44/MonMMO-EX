package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.MossdeepCity_PokemonCenter_2F
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object MossdeepCity_PokemonCenter_2F_EventScript_Woman5 : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(MossdeepCity_PokemonCenter_2F.Woman5)
}

internal val MossdeepCity_PokemonCenter_2FScripts: Map<String, Script> =
    mapOf(
        "MossdeepCity_PokemonCenter_2F_EventScript_Woman5" to
            MossdeepCity_PokemonCenter_2F_EventScript_Woman5,
    )
