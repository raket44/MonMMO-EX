package pirated.server.game.script.generated.kanto

import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

/**
 * Not ported yet. Decomp body:
 * ```
 * call TrainerTower_EventScript_SpeakToOwner
 * end
 * ```
 */
internal object TrainerTower_EventScript_Owner : Script {
  override suspend fun run(ctx: ScriptContext) = TODO("port TrainerTower_EventScript_Owner")
}

internal val TrainerTower_2FScripts: Map<String, Script> =
    mapOf(
        "TrainerTower_EventScript_Owner" to TrainerTower_EventScript_Owner,
    )
