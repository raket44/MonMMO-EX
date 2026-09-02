package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * Chooses animation donors from the Expansion's OWN animation scripts instead of a stat heuristic
 * (operator-directed after the type/power picks landed as thoughtless).
 *
 * `data/battle_anim_scripts.s` holds a `gBattleAnimMove_<Name>::` block for every move, and the
 * blocks for new moves are built from the same primitives as the old ones: Moonblast's script
 * spawns `gMistBallSpriteTemplate` and runs `AnimTask_MoonlightEndFade` - the Expansion itself says
 * it is Mist Ball's projectile with Moonlight's staging. Many scripts even `goto` another move's
 * script outright. So the donor choice reads those signals in order:
 * 1. A `goto gBattleAnimMove_X` - the animation IS X's, use X (chained until a playable id).
 * 2. Highest primitive overlap with a retail script: shared sprite templates weigh most, then
 *    shared visual tasks and backgrounds, then shared sound effects; ties break to the same type
 *    and closest power.
 *
 * Every choice is also written to a review file so the curation is inspectable and overridable line
 * by line.
 */
object AnimScriptMatcher {
  data class AnimScript(
      val labels: List<String>,
      val templates: Set<String>,
      val tasks: Set<String>,
      val backgrounds: Set<String>,
      val sounds: Set<String>,
      val gotos: List<String>,
  )

  data class Choice(val donor: Int, val reason: String)

  private val BLOCK_LABEL = Regex("""^gBattleAnimMove_(\w+)::""", RegexOption.MULTILINE)
  private val TEMPLATE = Regex("""createsprite (g\w+)""")
  private val TASK = Regex("""createvisualtask (\w+)""")
  private val BACKGROUND = Regex("""fadetobg (\w+)""")
  private val SOUND = Regex("""(?:playsewithpan|loopsewithpan|panse) (SE_\w+)""")
  private val GOTO = Regex("""goto gBattleAnimMove_(\w+)""")

  fun matches(
      expansionRoot: Path,
      moves: List<MoveText.Move>,
  ): Map<Int, Choice> {
    val text = Files.readString(expansionRoot.resolve("data/battle_anim_scripts.s"))
    val idsByConstant = MoveText.ids(expansionRoot)

    // PascalCase label to MOVE_ constant: split at lower-to-upper and letter-to-digit seams.
    fun labelToConstant(label: String): String =
        "MOVE_" +
            label
                .replace(Regex("""(?<=[a-z0-9])(?=[A-Z])"""), "_")
                .replace(Regex("""(?<=[A-Za-z])(?=\d)"""), "_")
                .uppercase()

    // Blocks run from one gBattleAnimMove label to the next; stacked labels share a body.
    val labelMatches = BLOCK_LABEL.findAll(text).toList()
    val scripts = mutableListOf<AnimScript>()
    var index = 0
    while (index < labelMatches.size) {
      val labels = mutableListOf(labelMatches[index].groupValues[1])
      var end = index + 1
      while (end < labelMatches.size &&
          text
              .substring(labelMatches[end - 1].range.last, labelMatches[end].range.first)
              .isBlank()) {
        labels += labelMatches[end].groupValues[1]
        end++
      }
      val bodyEnd = if (end < labelMatches.size) labelMatches[end].range.first else text.length
      val body = text.substring(labelMatches[index].range.last, bodyEnd)
      scripts +=
          AnimScript(
              labels = labels,
              templates = TEMPLATE.findAll(body).map { it.groupValues[1] }.toSet(),
              tasks = TASK.findAll(body).map { it.groupValues[1] }.toSet(),
              backgrounds = BACKGROUND.findAll(body).map { it.groupValues[1] }.toSet(),
              sounds = SOUND.findAll(body).map { it.groupValues[1] }.toSet(),
              gotos = GOTO.findAll(body).map { it.groupValues[1] }.toList(),
          )
      index = end
    }

    val scriptByMoveId = mutableMapOf<Int, AnimScript>()
    val moveIdByLabel = mutableMapOf<String, Int>()
    scripts.forEach { script ->
      script.labels.forEach { label ->
        idsByConstant[labelToConstant(label)]?.let { id ->
          scriptByMoveId.putIfAbsent(id, script)
          moveIdByLabel[label] = id
        }
      }
    }

    val movesById = moves.associateBy { it.id }
    val retailScripts = scriptByMoveId.filterKeys { it in 1..559 }

    fun resolveGoto(script: AnimScript, depth: Int): Int? {
      if (depth > 4) return null
      script.gotos.forEach { targetLabel ->
        val target = moveIdByLabel[targetLabel] ?: return@forEach
        if (target in 1..732) return target
        val next = scriptByMoveId[target] ?: return@forEach
        resolveGoto(next, depth + 1)?.let {
          return it
        }
      }
      return null
    }

    fun structuralDonor(moveId: Int, script: AnimScript): Choice? {
      val move = movesById[moveId]
      val scored =
          retailScripts.mapNotNull { (retailId, retail) ->
            val shared = (script.templates intersect retail.templates).size
            val score =
                shared * 5 +
                    (script.tasks intersect retail.tasks).size * 3 +
                    (script.backgrounds intersect retail.backgrounds).size * 4 +
                    (script.sounds intersect retail.sounds).size
            if (score <= 0) null else Triple(retailId, score, shared)
          }
      if (scored.isEmpty()) return null
      val best =
          scored
              .sortedWith(
                  compareByDescending<Triple<Int, Int, Int>> { it.second }
                      .thenByDescending { movesById[it.first]?.type == move?.type }
                      .thenBy {
                        kotlin.math.abs((movesById[it.first]?.power ?: 0) - (move?.power ?: 0))
                      }
                      .thenBy { it.first })
              .first()
      return Choice(best.first, "overlap score=${best.second} sharedTemplates=${best.third}")
    }

    val choices = mutableMapOf<Int, Choice>()
    movesById.keys
        .filter { it > 559 }
        .forEach { moveId ->
          val script = scriptByMoveId[moveId] ?: return@forEach
          resolveGoto(script, 0)?.let { target ->
            choices[moveId] = Choice(target, "script goto")
            return@forEach
          }
          structuralDonor(moveId, script)?.let { choices[moveId] = it }
        }
    return choices
  }
}
