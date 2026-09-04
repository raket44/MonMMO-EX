package de.fiereu.openmmo.codegen.move

import java.io.File

/**
 * Reads the Expansion's move table.
 *
 * The Emerald decomp stops at 354 moves, so a server built from it cannot describe anything from
 * Gen 4 onward: an imported move arrives with no power, no type and no PP, and the give path fills
 * the gap with a placeholder. This reads all of them from the Expansion instead.
 *
 * Three things in that file lose data silently if you read it naively, and all three cost a debug
 * cycle before they were found:
 * 1. Power, accuracy and PP are gated behind `B_UPDATED_MOVE_DATA >= GEN_n ? new : old` on 334
 *    moves. The first number on the line is neither value.
 * 2. `#if` / `#else` blocks appear in 64 entries. Low Kick keeps a Gen 1 flinch behind an `#else`.
 * 3. `MOVE_POWER_SHIFT` closes its brace at column 0 while every other entry indents it, so a
 *    pattern anchored on the closing brace swallows the entry that follows it.
 */
class ExpansionMoveParser(private val rootDir: File) {

  fun parseAll(): List<ParsedMove> {
    val ids = readMoveIds()
    val config = ExpansionConfig.read(rootDir)
    val updatedThrough = config.value("B_UPDATED_MOVE_DATA") ?: LATEST_GENERATION
    val text = File(rootDir, "src/data/moves_info.h").readText()

    return ENTRY.findAll(text)
        .mapNotNull { match ->
          val symbol = "MOVE_" + match.groupValues[1]
          val id = ids[symbol] ?: return@mapNotNull null
          val body = preprocess(match.groupValues[2], config)
          val name = NAME.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
          val declared = resolveToken(raw(body, "effect") ?: "EFFECT_HIT", config)
          val effect = statChangeEffect(body, damaging = declared == "EFFECT_HIT") ?: declared
          ParsedMove(
              id = id,
              name = clean(name),
              effect = effectRef(effect),
              power = numeric(body, "power", updatedThrough) ?: 0,
              type = typeRef(resolveToken(raw(body, "type") ?: "TYPE_NORMAL", config)),
              accuracy = numeric(body, "accuracy", updatedThrough) ?: 0,
              pp = numeric(body, "pp", updatedThrough) ?: 0,
              secondaryEffectChance = secondaryChance(body, updatedThrough),
              target = targetRef(target(body, config)),
              priority = numeric(body, "priority", updatedThrough) ?: 0,
              flags = flagRefs(flags(body, config).joinToString("|")),
              argumentKind = argument(body, config, updatedThrough)?.first,
              argument = argument(body, config, updatedThrough)?.second,
              additionalEffects = additionalEffects(body, config, updatedThrough),
              strikeCount = numeric(body, "strikeCount", updatedThrough) ?: 0,
              criticalHitStage = numeric(body, "criticalHitStage", updatedThrough) ?: 0,
          )
        }
        .toList()
  }

  private fun readMoveIds(): Map<String, Int> {
    val file = File(rootDir, "include/constants/moves.h")
    require(file.exists()) { "Expansion not initialised at $rootDir (missing ${file.path})" }
    var next = 0
    val ids = linkedMapOf<String, Int>()
    // The character class allows a digit after MOVE_, because MOVE_10000000_VOLT_THUNDERBOLT
    // exists and a letters-only pattern drops it.
    DECLARATION.findAll(file.readText()).forEach { match ->
      val id = match.groupValues[2].trim().toIntOrNull() ?: next
      ids["MOVE_" + match.groupValues[1]] = id
      next = id + 1
    }
    return ids
  }

  /** The server's target vocabulary, which uses the decomp's older `MOVE_TARGET_` spelling. */
  private fun target(body: String, config: ExpansionConfig): String {
    val token = raw(body, "target")?.let { resolveToken(it, config) } ?: "TARGET_SELECTED"
    return "MOVE_TARGET_" + token.removePrefix("TARGET_")
  }

  /** A ternary whose branches are symbols rather than numbers, as a few targets are. */
  private fun resolveToken(expression: String, config: ExpansionConfig): String {
    val ternary = SYMBOL_TERNARY.find(expression) ?: return expression.trim()
    val generation = ternary.groupValues[2].toInt()
    val setting = config.value(ternary.groupValues[1]) ?: LATEST_GENERATION
    return if (setting >= generation) ternary.groupValues[3] else ternary.groupValues[4]
  }

  private fun flags(body: String, config: ExpansionConfig): List<String> {
    fun on(name: String) = config.boolean(raw(body, name) ?: "FALSE") == true
    return buildList {
      if (on("makesContact")) add("FLAG_MAKES_CONTACT")
      // The Expansion states the exceptions; the server states the rule.
      if (!on("ignoresProtect")) add("FLAG_PROTECT_AFFECTED")
      if (on("magicCoatAffected")) add("FLAG_MAGIC_COAT_AFFECTED")
      if (on("snatchAffected")) add("FLAG_SNATCH_AFFECTED")
      if (!on("mirrorMoveBanned")) add("FLAG_MIRROR_MOVE_AFFECTED")
      if (!on("ignoresKingsRock")) add("FLAG_KINGS_ROCK_AFFECTED")
      // The Expansion's per-move booleans the battle engine branches on.
      if (on("multiHit")) add("FLAG_MULTI_HIT")
      if (on("thawsUser")) add("FLAG_THAWS_USER")
      if (on("alwaysCriticalHit")) add("FLAG_ALWAYS_CRIT")
      if (on("powderMove")) add("FLAG_POWDER")
      if (on("soundMove")) add("FLAG_SOUND")
      if (on("healingMove")) add("FLAG_HEALING")
      if (on("punchingMove")) add("FLAG_PUNCH")
      if (on("bitingMove")) add("FLAG_BITE")
      if (on("minimizeDoubleDamage")) add("FLAG_MINIMIZE_DOUBLE_DAMAGE")
    }
  }

  /**
   * The specific stat effect behind the Expansion's generic one.
   *
   * The Emerald decomp had an effect per stat and direction - `EFFECT_ATTACK_DOWN` - and the battle
   * engine branches on exactly those. The Expansion replaced them with `EFFECT_STAT_CHANGE` plus
   * the stat in the additional effects, so importing its names verbatim silently disables every
   * stat move the engine actually implements. Growl stopped lowering Attack, and only a test
   * noticed.
   *
   * Returns null when the shape is anything the engine has no branch for, so the Expansion's own
   * effect is kept and the move is honestly recorded as unimplemented.
   */
  private fun statChangeEffect(body: String, damaging: Boolean): String? {
    val start = body.indexOf("ADDITIONAL_EFFECTS(")
    if (start < 0) return null
    val entry = EFFECT_ENTRY.find(body, start)?.groupValues?.get(1) ?: return null
    val direction =
        STAT_DIRECTION.find(entry)?.groupValues?.get(2)?.let { if (it == "PLUS") "UP" else "DOWN" }
            ?: return null
    val stat =
        STATS.firstNotNullOfOrNull { (field, name) ->
          Regex("""\.$field\s*=\s*(\d+)""").find(entry)?.let { name to it.groupValues[1].toInt() }
        } ?: return null
    val stages = if (stat.second >= 2) "_2" else ""
    val suffix = if (damaging) "_HIT" else ""
    val candidate = "${stat.first}_$direction$stages$suffix"
    return if (candidate in ENGINE_STAT_EFFECTS) "EFFECT_$candidate" else null
  }

  /**
   * The `.argument = { .kind = value }` pair. Numeric ternaries resolve like every other number;
   * the ice-weather preference resolves to hail, the Gen 3 weather the client draws. Multi-line
   * arguments (Counter's reflectDamage struct) are left out.
   */
  private fun argument(
      body: String,
      config: ExpansionConfig,
      updatedThrough: Int,
  ): Pair<String, String>? {
    val match = ARGUMENT.find(body) ?: return null
    val kind = match.groupValues[1]
    var value = match.groupValues[2].trim().trimEnd(',').trim()
    if ("B_PREFERRED_ICE_WEATHER" in value) return kind to "BATTLE_WEATHER_HAIL"
    NUMBER_TERNARY.find(value)?.let { ternary ->
      val generation = ternary.groupValues[1].toInt()
      value =
          if (updatedThrough >= generation) ternary.groupValues[2] else ternary.groupValues[3]
    }
    return kind to resolveToken(value, config)
  }

  /** Every `ADDITIONAL_EFFECTS` entry, in order; a missing chance means the effect always lands. */
  private fun additionalEffects(
      body: String,
      config: ExpansionConfig,
      updatedThrough: Int,
  ): List<ParsedAdditionalEffect> {
    val start = body.indexOf("ADDITIONAL_EFFECTS(")
    if (start < 0) return emptyList()
    val end = body.indexOf("})", start).takeIf { it >= 0 } ?: return emptyList()
    val block = body.substring(start, end + 1)
    return EFFECT_ENTRY.findAll(block)
        .mapNotNull { entry ->
          val text = entry.groupValues[1]
          val token = raw(text, "moveEffect")?.let { resolveToken(it, config) } ?: return@mapNotNull null
          val effect =
              when {
                token.endsWith("_PLUS") -> "MOVE_EFFECT_STAT_PLUS"
                token.endsWith("_MINUS") -> "MOVE_EFFECT_STAT_MINUS"
                else -> token
              }
          val stats =
              STATS.mapNotNull { (field, name) ->
                Regex("""\.$field\s*=\s*(\d+)""").find(text)?.let { name to it.groupValues[1].toInt() }
              }
          ParsedAdditionalEffect(
              effect = effect,
              chance = numeric(text, "chance", updatedThrough)?.takeIf { it > 0 } ?: 100,
              self = config.boolean(raw(text, "self") ?: "FALSE") == true,
              stats = stats,
          )
        }
        .toList()
  }

  /** The chance on the first additional effect, which is what the old table recorded. */
  private fun secondaryChance(body: String, updatedThrough: Int): Int {
    val start = body.indexOf("ADDITIONAL_EFFECTS(")
    if (start < 0) return 0
    val first = EFFECT_ENTRY.find(body, start) ?: return 0
    return numeric(first.groupValues[1], "chance", updatedThrough) ?: 0
  }

  private fun raw(body: String, name: String): String? =
      Regex("""\.$name\s*=\s*([^,\n]+)""").find(body)?.groupValues?.get(1)?.trim()

  private fun numeric(body: String, name: String, updatedThrough: Int): Int? {
    val value = raw(body, name) ?: return null
    value.toIntOrNull()?.let {
      return it
    }
    val ternary = NUMBER_TERNARY.find(value) ?: return null
    val generation = ternary.groupValues[1].toInt()
    return if (updatedThrough >= generation) ternary.groupValues[2].toInt()
    else ternary.groupValues[3].toInt()
  }

  /** Keeps only the branches the configured settings actually compile. */
  private fun preprocess(body: String, config: ExpansionConfig): String {
    val frames = ArrayDeque<Frame>()
    val kept = StringBuilder()
    body.lineSequence().forEach { line ->
      val trimmed = line.trim()
      when {
        trimmed.startsWith("#if") -> {
          val parentActive = frames.lastOrNull()?.active ?: true
          val taken =
              config.boolean(trimmed.removePrefix("#ifdef").removePrefix("#if").trim()) ?: true
          frames.addLast(Frame(taken, parentActive && taken, parentActive))
        }
        trimmed.startsWith("#elif") ->
            frames.lastOrNull()?.let { frame ->
              val value = config.boolean(trimmed.removePrefix("#elif").trim()) ?: false
              frame.active = frame.parentActive && !frame.taken && value
              if (value) frame.taken = true
            }
        trimmed.startsWith("#else") ->
            frames.lastOrNull()?.let { frame ->
              frame.active = frame.parentActive && !frame.taken
              frame.taken = true
            }
        trimmed.startsWith("#endif") -> frames.removeLastOrNull()
        else -> if (frames.all { it.active }) kept.appendLine(line)
      }
    }
    return kept.toString()
  }

  private class Frame(var taken: Boolean, var active: Boolean, val parentActive: Boolean)

  private fun clean(text: String): String =
      WHITESPACE.replace(CONTROL.replace(text, " ").replace("\\", ""), " ").trim()

  private companion object {
    /** GEN_LATEST, when the config does not say otherwise. */
    const val LATEST_GENERATION = 9

    /** Each entry runs to the next entry's header - see the class comment for why. */
    val ENTRY =
        Regex(
            """\[MOVE_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[MOVE_[A-Z0-9_]+]\s*=|\n};)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    val DECLARATION =
        Regex("""^\s*MOVE_([A-Z0-9][A-Z0-9_]*)\s*(?:=\s*([^,]+))?,""", RegexOption.MULTILINE)
    val NAME = Regex("""\.name\s*=\s*(?:COMPOUND_STRING|_)\("([^"]*)"\)""")
    val NUMBER_TERNARY =
        Regex("""B_UPDATED_MOVE_DATA\s*>=\s*GEN_(\d+)\s*\?\s*(-?\d+)\s*:\s*(-?\d+)""")
    val SYMBOL_TERNARY =
        Regex("""(\w+)\s*>=\s*GEN_(\d+)\s*\?\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)""")
    val STAT_DIRECTION =
        Regex("""\.moveEffect\s*=\s*(STAT_CHANGE_EFFECT|MOVE_EFFECT_STAT)_(PLUS|MINUS)""")

    /** Expansion field name to the server's stat name. */
    val STATS =
        listOf(
            "attack" to "ATTACK",
            "defense" to "DEFENSE",
            "speed" to "SPEED",
            "spAtk" to "SPECIAL_ATTACK",
            "spDef" to "SPECIAL_DEFENSE",
            "accuracy" to "ACCURACY",
            "evasion" to "EVASION",
        )

    /**
     * The stat effects TurnEngine actually resolves. Anything else stays as the Expansion has it.
     */
    val ENGINE_STAT_EFFECTS =
        setOf(
            "ACCURACY_DOWN",
            "ACCURACY_DOWN_2",
            "ACCURACY_DOWN_HIT",
            "ACCURACY_UP",
            "ACCURACY_UP_2",
            "ATTACK_DOWN",
            "ATTACK_DOWN_2",
            "ATTACK_DOWN_HIT",
            "ATTACK_UP",
            "ATTACK_UP_2",
            "ATTACK_UP_HIT",
            "DEFENSE_DOWN",
            "DEFENSE_DOWN_2",
            "DEFENSE_DOWN_HIT",
            "DEFENSE_UP",
            "DEFENSE_UP_2",
            "DEFENSE_UP_HIT",
            "EVASION_DOWN",
            "EVASION_DOWN_2",
            "EVASION_DOWN_HIT",
            "EVASION_UP",
            "EVASION_UP_2",
            "SPECIAL_ATTACK_DOWN",
            "SPECIAL_ATTACK_DOWN_2",
            "SPECIAL_ATTACK_DOWN_HIT",
            "SPECIAL_ATTACK_UP",
            "SPECIAL_ATTACK_UP_2",
            "SPECIAL_DEFENSE_DOWN",
            "SPECIAL_DEFENSE_DOWN_2",
            "SPECIAL_DEFENSE_DOWN_HIT",
            "SPECIAL_DEFENSE_UP",
            "SPECIAL_DEFENSE_UP_2",
            "SPEED_DOWN",
            "SPEED_DOWN_2",
            "SPEED_DOWN_HIT",
            "SPEED_UP",
            "SPEED_UP_2",
        )
    val EFFECT_ENTRY = Regex("""\{([^{}]*)}""")
    val ARGUMENT = Regex("""\.argument\s*=\s*\{\s*\.(\w+)\s*=\s*([^{}\n]+?)\s*,?\s*}""")
    val CONTROL = Regex("""\\+[nlp]""")
    val WHITESPACE = Regex("""\s+""")
  }
}
