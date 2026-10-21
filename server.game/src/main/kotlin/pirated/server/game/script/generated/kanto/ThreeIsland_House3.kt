package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.ThreeIsland_House3
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object ThreeIsland_House3_EventScript_Beauty : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(ThreeIsland_House3.WantedToLiveSomewhereQuiet)
}

internal val ThreeIsland_House3Scripts: Map<String, Script> =
    mapOf(
        "ThreeIsland_House3_EventScript_Beauty" to ThreeIsland_House3_EventScript_Beauty,
    )
