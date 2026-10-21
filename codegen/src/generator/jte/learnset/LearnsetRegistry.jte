@import java.util.List
@import pirated.codegen.learnset.ParsedLearnset
@import pirated.codegen.learnset.RenderUtil

@param List<List<ParsedLearnset>> chunks
package pirated.pokemon.generated

import pirated.pokemon.LearnsetRegistry
import pirated.pokemon.LevelUpMove

internal object GeneratedLearnsets {
  fun loadInto(reg: LearnsetRegistry) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(reg)
@endfor
  }

@for(int c = 0; c < chunks.size(); c++)
  private fun registerChunk${c}(reg: LearnsetRegistry) {
@for(ParsedLearnset l : chunks.get(c))
    ${RenderUtil.INSTANCE.learnset(l)}
@endfor
  }

@endfor
}
