package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.PewterCity_House2
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object PewterCity_House2_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(PewterCity_House2.MonsLearnTechniquesAsTheyGrow)
}

internal object PewterCity_House2_EventScript_LittleBoy : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(PewterCity_House2.MonsEasierCatchIfStatused)
}

internal val PewterCity_House2Scripts: Map<String, Script> =
    mapOf(
        "PewterCity_House2_EventScript_OldMan" to PewterCity_House2_EventScript_OldMan,
        "PewterCity_House2_EventScript_LittleBoy" to PewterCity_House2_EventScript_LittleBoy,
    )
