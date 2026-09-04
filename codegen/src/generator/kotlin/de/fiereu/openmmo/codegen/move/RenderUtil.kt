package de.fiereu.openmmo.codegen.move

object RenderUtil {

  fun move(m: ParsedMove): String {
    val flags = if (m.flags.isEmpty()) "emptySet()" else m.flags.joinToString(", ", "setOf(", ")")
    val argument =
        if (m.argumentKind == null || m.argument == null) ""
        else ", argument = MoveArgument(${escapeString(m.argumentKind)}, ${escapeString(m.argument)})"
    val extras =
        if (m.additionalEffects.isEmpty()) ""
        else
            m.additionalEffects.joinToString(", ", ", additionalEffects = listOf(", ")") { e ->
              val stats =
                  if (e.stats.isEmpty()) ""
                  else
                      e.stats.joinToString(", ", ", stats = listOf(", ")") { (stat, stages) ->
                        "${escapeString(stat)} to $stages"
                      }
              "AdditionalEffect(MoveAdditionalEffect.${additionalEffectName(e.effect)}, ${e.chance}, ${e.self}$stats)"
            }
    val strikes = if (m.strikeCount > 0) ", strikeCount = ${m.strikeCount}" else ""
    val crit = if (m.criticalHitStage > 0) ", criticalHitStage = ${m.criticalHitStage}" else ""
    return "MoveDef(id = ${m.id}, name = ${escapeString(m.name)}, effect = ${m.effect}," +
        " power = ${m.power}, type = ${m.type}, accuracy = ${m.accuracy}, pp = ${m.pp}," +
        " secondaryEffectChance = ${m.secondaryEffectChance}, target = ${m.target}," +
        " priority = ${m.priority}, flags = $flags$argument$extras$strikes$crit)"
  }

  /** The enum constant for a `MOVE_EFFECT_*` token; anything the enum lacks renders as UNKNOWN. */
  private fun additionalEffectName(token: String): String {
    val name = token.removePrefix("MOVE_EFFECT_")
    return if (name in KNOWN_ADDITIONAL_EFFECTS) name else "UNKNOWN"
  }

  private val KNOWN_ADDITIONAL_EFFECTS: Set<String> =
      de.fiereu.openmmo.common.enums.MoveAdditionalEffect.entries.map { it.name }.toSet()

  private fun escapeString(s: String): String =
      "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
