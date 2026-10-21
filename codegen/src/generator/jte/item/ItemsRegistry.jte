@import java.util.List
@import pirated.codegen.item.ParsedItem
@import pirated.codegen.item.RenderUtil

@param List<List<ParsedItem>> chunks
package pirated.items.generated

import pirated.items.ItemRegistry

internal object GeneratedItems {
  fun loadInto(reg: ItemRegistry) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(reg)
@endfor
  }

@for(int c = 0; c < chunks.size(); c++)
  private fun registerChunk${c}(reg: ItemRegistry) {
@for(ParsedItem item : chunks.get(c))
    ${RenderUtil.INSTANCE.register(item)}
@endfor
  }

@endfor
}
