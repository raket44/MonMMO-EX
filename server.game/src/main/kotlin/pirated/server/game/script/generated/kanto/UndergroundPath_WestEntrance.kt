package pirated.server.game.script.generated.kanto

import pirated.dialog.generated.kanto.UndergroundPath_WestEntrance
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object UndergroundPath_WestEntrance_EventScript_BaldingMan : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(UndergroundPath_WestEntrance.SleepyMonNearCeladon)
}

internal val UndergroundPath_WestEntranceScripts: Map<String, Script> =
    mapOf(
        "UndergroundPath_WestEntrance_EventScript_BaldingMan" to
            UndergroundPath_WestEntrance_EventScript_BaldingMan,
    )
