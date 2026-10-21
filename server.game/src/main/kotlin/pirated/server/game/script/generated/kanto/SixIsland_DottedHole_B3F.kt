package pirated.server.game.script.generated.kanto

import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

/**
 * Not ported yet. Decomp body:
 * ```
 * lockall
 * braillemessage Braille_Text_Right
 * waitbuttonpress
 * releaseall
 * end
 * ```
 */
internal object SixIsland_DottedHole_B3F_EventScript_BrailleRight : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port SixIsland_DottedHole_B3F_EventScript_BrailleRight")
}

internal val SixIsland_DottedHole_B3FScripts: Map<String, Script> =
    mapOf(
        "SixIsland_DottedHole_B3F_EventScript_BrailleRight" to
            SixIsland_DottedHole_B3F_EventScript_BrailleRight,
    )
