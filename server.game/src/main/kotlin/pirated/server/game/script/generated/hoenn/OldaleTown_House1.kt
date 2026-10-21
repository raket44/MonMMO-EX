package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.OldaleTown_House1
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object OldaleTown_House1_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.say(OldaleTown_House1.LeftPokemonGoesOutFirst)
}

internal val OldaleTown_House1Scripts: Map<String, Script> =
    mapOf(
        "OldaleTown_House1_EventScript_Woman" to OldaleTown_House1_EventScript_Woman,
    )
