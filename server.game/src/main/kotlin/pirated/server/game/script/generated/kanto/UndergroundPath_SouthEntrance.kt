package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.UndergroundPath_SouthEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object UndergroundPath_SouthEntrance_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(UndergroundPath_SouthEntrance.PeopleLoseThingsInTheDarkness)
}

internal val UndergroundPath_SouthEntranceScripts: Map<String, Script> =
    mapOf(
        "UndergroundPath_SouthEntrance_EventScript_Woman" to
            UndergroundPath_SouthEntrance_EventScript_Woman,
    )
