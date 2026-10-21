@import java.util.List
@import pirated.codegen.trainer.ParsedTrainer
@import pirated.codegen.trainer.RenderUtil

@param String pkg
@param String objectName
@param String region
@param List<List<ParsedTrainer>> chunks
package ${pkg}

import pirated.common.enums.Region
import pirated.trainer.TrainerDef
import pirated.trainer.TrainerMon
import pirated.trainer.TrainerRegistry

internal object ${objectName} {
  fun loadInto(reg: TrainerRegistry) {
@for(int i = 0; i < chunks.size(); i++)
    registerChunk${i}(reg)
@endfor
  }

@for(int c = 0; c < chunks.size(); c++)
  private fun registerChunk${c}(reg: TrainerRegistry) {
@for(ParsedTrainer t : chunks.get(c))
    ${RenderUtil.INSTANCE.trainer(region, t)}
@endfor
  }

@endfor
}
