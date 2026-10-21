@import java.util.List
@import pirated.codegen.pokemon.ParsedSpecies
@import pirated.codegen.pokemon.RenderUtil

@param List<List<ParsedSpecies>> chunks
package pirated.pokemon.generated

import pirated.common.enums.Ability
import pirated.common.enums.BodyColor
import pirated.common.enums.EggGroup
import pirated.common.enums.GrowthRate
import pirated.common.enums.PokemonType
import pirated.pokemon.SpeciesDef
import pirated.pokemon.SpeciesRegistry

internal object GeneratedSpecies {
  fun loadInto(reg: SpeciesRegistry) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(reg)
@endfor
  }

@for(int c = 0; c < chunks.size(); c++)
  private fun registerChunk${c}(reg: SpeciesRegistry) {
@for(ParsedSpecies s : chunks.get(c))
    reg.register(${RenderUtil.INSTANCE.species(s)})
@endfor
  }

@endfor
}
