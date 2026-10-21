package pirated.server.game.script.generated.hoenn

import pirated.dialog.generated.hoenn.EverGrandeCity
import pirated.server.game.script.Script
import pirated.server.game.script.ScriptContext

internal object EverGrandeCity_EventScript_VictoryRoadSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(EverGrandeCity.EnteringVictoryRoad)
}

internal object EverGrandeCity_EventScript_CitySign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(EverGrandeCity.CitySign)
}

internal object EverGrandeCity_EventScript_PokemonLeagueSign : Script {
  override suspend fun run(ctx: ScriptContext) = ctx.sign(EverGrandeCity.EnteringPokemonLeague)
}

internal val EverGrandeCityScripts: Map<String, Script> =
    mapOf(
        "EverGrandeCity_EventScript_VictoryRoadSign" to EverGrandeCity_EventScript_VictoryRoadSign,
        "EverGrandeCity_EventScript_CitySign" to EverGrandeCity_EventScript_CitySign,
        "EverGrandeCity_EventScript_PokemonLeagueSign" to
            EverGrandeCity_EventScript_PokemonLeagueSign,
    )
