package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.SootopolisCity_House7
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object SootopolisCity_House7_EventScript_OldMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SootopolisCity_House7.CityFromEruptedVolcano)
}

internal object SootopolisCity_House7_EventScript_PokefanF : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(SootopolisCity_House7.CaveMadeToKeepSomething)
}

internal val SootopolisCity_House7Scripts: Map<String, Script> =
    mapOf(
        "SootopolisCity_House7_EventScript_OldMan" to SootopolisCity_House7_EventScript_OldMan,
        "SootopolisCity_House7_EventScript_PokefanF" to SootopolisCity_House7_EventScript_PokefanF,
    )
