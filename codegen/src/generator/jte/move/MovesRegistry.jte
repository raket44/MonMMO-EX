@import java.util.List
@import pirated.codegen.move.ParsedMove
@import pirated.codegen.move.RenderUtil

@param List<List<ParsedMove>> chunks
package pirated.moves.generated

import pirated.common.enums.DamageCategory
import pirated.common.enums.MoveAdditionalEffect
import pirated.common.enums.MoveEffect
import pirated.common.enums.MoveFlag
import pirated.common.enums.MoveTarget
import pirated.common.enums.PokemonType
import pirated.moves.AdditionalEffect
import pirated.moves.MoveArgument
import pirated.moves.MoveDef
import pirated.moves.MoveRegistry

internal object GeneratedMoves {
  fun loadInto(reg: MoveRegistry) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(reg)
@endfor
  }

@for(int c = 0; c < chunks.size(); c++)
  private fun registerChunk${c}(reg: MoveRegistry) {
@for(ParsedMove m : chunks.get(c))
    reg.register(${RenderUtil.INSTANCE.move(m)})
@endfor
  }

@endfor
}
