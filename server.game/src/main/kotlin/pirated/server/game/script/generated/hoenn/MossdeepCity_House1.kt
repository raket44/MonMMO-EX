package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.MossdeepCity_House1
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

/**
 * Not ported yet. Decomp body:
 * ```
 * lock
 * faceplayer
 * bufferleadmonspeciesname STR_VAR_1
 * msgbox MossdeepCity_House1_Text_HmmYourPokemon, MSGBOX_DEFAULT
 * specialvar VAR_RESULT, GetPokeblockNameByMonNature
 * goto_if_eq VAR_RESULT, 0, MossdeepCity_House1_EventScript_NeutralNature
 * msgbox MossdeepCity_House1_Text_ItLikesXPokeblocks, MSGBOX_DEFAULT
 * release
 * end
 * ```
 */
internal object MossdeepCity_House1_EventScript_BlackBelt : Script {
  override suspend fun run(ctx: ScriptContext) =
      TODO("port MossdeepCity_House1_EventScript_BlackBelt")
}

internal object MossdeepCity_House1_EventScript_Woman : Script {
  override suspend fun run(ctx: ScriptContext) =
      ctx.say(MossdeepCity_House1.HusbandCanTellPokeblockMonLikes)
}

internal val MossdeepCity_House1Scripts: Map<String, Script> =
    mapOf(
        "MossdeepCity_House1_EventScript_BlackBelt" to MossdeepCity_House1_EventScript_BlackBelt,
        "MossdeepCity_House1_EventScript_Woman" to MossdeepCity_House1_EventScript_Woman,
    )
