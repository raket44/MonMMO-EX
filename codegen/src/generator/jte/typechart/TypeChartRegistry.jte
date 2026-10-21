@import java.util.List
@import pirated.codegen.typechart.ParsedMatchup

@param List<ParsedMatchup> matchups
package pirated.typechart.generated

import pirated.common.enums.PokemonType
import pirated.typechart.TypeChart

internal object GeneratedTypeChart {
  fun loadInto(chart: TypeChart) {
@for(ParsedMatchup m : matchups)
    chart.register(${m.getAttacker()}, ${m.getDefender()}, ${m.getMultiplier()})
@endfor
  }
}
