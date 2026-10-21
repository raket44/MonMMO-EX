@import java.util.List

@param List<List<String>> chunks
package pirated.maps.generated

import pirated.maps.MapManager

internal object GeneratedMaps {
  fun loadInto(mgr: MapManager) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(mgr)
@endfor
  }

@for(int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++)
  private fun registerChunk${chunkIndex}(mgr: MapManager) {
@for(String ref : chunks.get(chunkIndex))
    mgr.register(${ref})
@endfor
  }

@endfor
}
