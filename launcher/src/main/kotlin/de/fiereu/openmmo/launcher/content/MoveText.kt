package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * Move definitions read from the Expansion's own move table.
 *
 * The client resolves a move's name from string id `110000 + moveId` and its description from
 * `120000 + moveId`, and consults the string table for both - PokeMMO overrides Hail's name to
 * "Snowscape" at 110258 that way. Move ids past what the ROMs provide are unused, so imported moves
 * can occupy them.
 */
object MoveText {
  const val NAME_BASE = 110000
  const val DESCRIPTION_BASE = 120000

  data class Move(
      val id: Int,
      val name: String,
      val description: String,
      val type: String,
      val power: Int,
      val accuracy: Int,
      val pp: Int,
      val category: String,
      val flags: Set<String>,
      val recoilPercent: Int?,
      val absorbPercent: Int?,
      val additionalEffects: List<AdditionalEffect>,
      val effect: String,
      val nonVolatileStatus: String?,
  )

  /**
   * One move entry, taken from its header up to the next one.
   *
   * Matching the closing brace instead looks tidier and is wrong: `MOVE_POWER_SHIFT` closes at
   * column 0 rather than indented, so a terminator-based pattern runs straight through it and
   * swallows `MOVE_STONE_AXE`, which then goes missing with no error anywhere.
   */
  private val ENTRY =
      Regex(
          """\[MOVE_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[MOVE_[A-Z0-9_]+]\s*=|\n};)""",
          RegexOption.DOT_MATCHES_ALL,
      )
  private val NAME = Regex("""\.name\s*=\s*(?:COMPOUND_STRING|_)\("([^"]*)"\)""")
  private val DESCRIPTION =
      Regex(
          """\.description\s*=\s*(?:COMPOUND_STRING|_)\((.*?)\),\n""", RegexOption.DOT_MATCHES_ALL)
  private val LITERAL = Regex("\"([^\"]*)\"")
  /**
   * A move constant. The character class allows a digit after `MOVE_`, because
   * `MOVE_10000000_VOLT_THUNDERBOLT` exists and a letters-only pattern loses it - and with it the
   * move it names, which then reads as absent from the table rather than as a parse failure.
   */
  private val DECLARATION =
      Regex("""^\s*MOVE_([A-Z0-9][A-Z0-9_]*)\s*(?:=\s*([^,]+))?,""", RegexOption.MULTILINE)
  private val CONTROL = Regex("""\\+[nlp]""")
  private val WHITESPACE = Regex("\\s+")

  fun ids(expansionRoot: Path): Map<String, Int> {
    val header = Files.readString(expansionRoot.resolve("include/constants/moves.h"))
    var next = 0
    val ids = linkedMapOf<String, Int>()
    DECLARATION.findAll(header).forEach { match ->
      val id = match.groupValues[2].trim().toIntOrNull() ?: next
      ids["MOVE_" + match.groupValues[1]] = id
      next = id + 1
    }
    return ids
  }

  fun parse(expansionRoot: Path, ids: Map<String, Int>): List<Move> {
    val text = Files.readString(expansionRoot.resolve("src/data/moves_info.h"))
    val updatedThrough = updatedMoveDataGeneration(expansionRoot)
    val updatedTypesThrough = updatedMoveTypesGeneration(expansionRoot)
    val config = ExpansionConfig.read(expansionRoot)
    return ENTRY.findAll(text)
        .mapNotNull { match ->
          val id = ids["MOVE_" + match.groupValues[1]] ?: return@mapNotNull null
          val body = preprocess(match.groupValues[2], config)
          val name = NAME.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
          val description =
              DESCRIPTION.find(body)?.groupValues?.get(1)?.let { raw ->
                LITERAL.findAll(raw).joinToString("") { it.groupValues[1] }
              }
          Move(
              id = id,
              name = clean(name),
              description = clean(description.orEmpty()).ifEmpty { "--" },
              type = resolvedType(body, updatedTypesThrough) ?: "TYPE_NORMAL",
              power = numeric(body, "power", updatedThrough) ?: 0,
              accuracy = numeric(body, "accuracy", updatedThrough) ?: 0,
              pp = numeric(body, "pp", updatedThrough) ?: 0,
              category = field(body, "category") ?: "DAMAGE_CATEGORY_STATUS",
              flags = flags(body, config),
              recoilPercent = argument(body, "recoilPercentage", updatedThrough),
              absorbPercent = argument(body, "absorbPercentage", updatedThrough),
              additionalEffects = additionalEffects(body, updatedThrough),
              effect = field(body, "effect") ?: "EFFECT_HIT",
              nonVolatileStatus = argumentSymbol(body, "nonVolatileStatus"),
          )
        }
        .toList()
  }

  /**
   * Power, accuracy and PP are written as `B_UPDATED_MOVE_DATA >= GEN_n ? new : old` on 334 of the
   * Expansion's moves, so reading the first number out of the line is not enough. The setting is
   * resolved from the Expansion's own config rather than assumed, since changing it there is
   * exactly how a build chooses which generation's numbers it wants.
   */
  private val TERNARY =
      Regex("""B_UPDATED_MOVE_DATA\s*>=\s*GEN_(\d+)\s*\?\s*(-?\d+)\s*:\s*(-?\d+)""")

  private fun numeric(body: String, name: String, updatedThrough: Int): Int? {
    val raw =
        Regex("""\.$name\s*=\s*([^,\n]+)""").find(body)?.groupValues?.get(1)?.trim() ?: return null
    raw.toIntOrNull()?.let {
      return it
    }
    val ternary = TERNARY.find(raw) ?: return null
    val generation = ternary.groupValues[1].toInt()
    return if (updatedThrough >= generation) ternary.groupValues[2].toInt()
    else ternary.groupValues[3].toInt()
  }

  /**
   * Move TYPES carry their own generation gate: `.type = B_UPDATED_MOVE_TYPES >= GEN_6 ? TYPE_FAIRY
   * : TYPE_NORMAL` on the retyped classics (Sweet Kiss, Charm, Karate Chop...). Same
   * resolve-from-config rule as the numeric gate - the raw token is never a type name.
   */
  private val TYPE_TERNARY =
      Regex("""B_UPDATED_MOVE_TYPES\s*>=\s*GEN_(\d+)\s*\?\s*(\w+)\s*:\s*(\w+)""")

  fun resolvedType(body: String, updatedTypesThrough: Int): String? {
    val raw =
        Regex("""\.type\s*=\s*([^,\n]+)""").find(body)?.groupValues?.get(1)?.trim() ?: return null
    val ternary = TYPE_TERNARY.find(raw) ?: return raw
    val generation = ternary.groupValues[1].toInt()
    return if (updatedTypesThrough >= generation) ternary.groupValues[2] else ternary.groupValues[3]
  }

  /** The generation whose move TYPES this Expansion checkout is configured to use. */
  fun updatedMoveTypesGeneration(expansionRoot: Path): Int {
    val battle = Files.readString(expansionRoot.resolve("include/config/battle.h"))
    val symbol =
        Regex("""#define\s+B_UPDATED_MOVE_TYPES\s+(\w+)""").find(battle)?.groupValues?.get(1)
            ?: return LATEST_GENERATION
    return resolveGeneration(expansionRoot, symbol, 0)
  }

  /** The generation whose move numbers this Expansion checkout is configured to use. */
  fun updatedMoveDataGeneration(expansionRoot: Path): Int {
    val battle = Files.readString(expansionRoot.resolve("include/config/battle.h"))
    val symbol =
        Regex("""#define\s+B_UPDATED_MOVE_DATA\s+(\w+)""").find(battle)?.groupValues?.get(1)
            ?: return LATEST_GENERATION
    return resolveGeneration(expansionRoot, symbol, 0)
  }

  private fun resolveGeneration(expansionRoot: Path, symbol: String, depth: Int): Int {
    if (depth > MAX_ALIAS_DEPTH) return LATEST_GENERATION
    symbol.removePrefix("GEN_").toIntOrNull()?.let {
      return it
    }
    val general = Files.readString(expansionRoot.resolve("include/config/general.h"))
    val target =
        Regex("""#define\s+$symbol\s+(\w+)""").find(general)?.groupValues?.get(1)
            ?: return LATEST_GENERATION
    return resolveGeneration(expansionRoot, target, depth + 1)
  }

  /** One entry of `.additionalEffects = ADDITIONAL_EFFECTS({ ... })`. */
  data class AdditionalEffect(val effect: String, val chance: Int)

  /** The boolean flags the client has an attribute line for. */
  val FLAG_NAMES =
      listOf(
          "makesContact",
          "ignoresProtect",
          "magicCoatAffected",
          "snatchAffected",
          "mirrorMoveBanned",
          "soundMove",
          "ignoresSubstitute",
          "slicingMove",
          "windMove",
          "thawsUser",
          "minimizeDoubleDamage",
          "alwaysHitsInRain",
          "alwaysHitsInHailSnow",
          "damagesUnderground",
          "damagesAirborneDoubleDamage",
          "damagesAirborne",
          "damagesUnderwater",
          "alwaysHitsOnSameType",
      )

  private val ARGUMENT = Regex("""\.argument\s*=\s*\{([^}]*)}""")
  private val EFFECT_ENTRY = Regex("""\{([^{}]*)}""")
  private val MOVE_EFFECT = Regex("""\.moveEffect\s*=\s*([A-Za-z0-9_]+)""")

  /**
   * Resolves the `#if` / `#elif` / `#else` branches inside a move entry, keeping only the code the
   * configured settings actually compile.
   *
   * 64 moves are written this way. Low Kick keeps its Gen 1 flinch behind an `#else`, so reading
   * the whole entry gives it a 30% flinch it has not had since Gen 2. An expression outside the
   * grammar keeps its first branch, which is the modern one throughout this file.
   */
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
        trimmed.startsWith("#elif") -> {
          frames.lastOrNull()?.let { frame ->
            val value = config.boolean(trimmed.removePrefix("#elif").trim()) ?: false
            frame.active = frame.parentActive && !frame.taken && value
            if (value) frame.taken = true
          }
        }
        trimmed.startsWith("#else") -> {
          frames.lastOrNull()?.let { frame ->
            frame.active = frame.parentActive && !frame.taken
            frame.taken = true
          }
        }
        trimmed.startsWith("#endif") -> frames.removeLastOrNull()
        else -> if (frames.all { it.active }) kept.appendLine(line)
      }
    }
    return kept.toString()
  }

  private class Frame(var taken: Boolean, var active: Boolean, val parentActive: Boolean)

  private fun raw(body: String, name: String): String? =
      Regex("""\.$name\s*=\s*([^,\n]+)""").find(body)?.groupValues?.get(1)?.trim()

  private fun flags(body: String, config: ExpansionConfig): Set<String> =
      FLAG_NAMES.filterTo(linkedSetOf()) { name ->
        config.boolean(raw(body, name) ?: "FALSE") == true
      }

  /** The percentage inside `.argument = { .recoilPercentage = ... }`, ternaries resolved. */
  private fun argument(body: String, name: String, updatedThrough: Int): Int? {
    val inner = ARGUMENT.find(body)?.groupValues?.get(1) ?: return null
    return numeric(inner, name, updatedThrough)
  }

  /** A symbol inside `.argument = { ... }`, such as `.nonVolatileStatus = MOVE_EFFECT_POISON`. */
  private fun argumentSymbol(body: String, name: String): String? {
    val inner = ARGUMENT.find(body)?.groupValues?.get(1) ?: return null
    return Regex("""\.$name\s*=\s*([A-Za-z0-9_]+)""").find(inner)?.groupValues?.get(1)
  }

  private fun additionalEffects(body: String, updatedThrough: Int): List<AdditionalEffect> {
    val start = body.indexOf("ADDITIONAL_EFFECTS(")
    if (start < 0) return emptyList()
    var depth = 0
    var end = start
    for (index in start until body.length) {
      when (body[index]) {
        '(' -> depth++
        ')' -> {
          depth--
          if (depth == 0) {
            end = index
            break
          }
        }
      }
    }
    return EFFECT_ENTRY.findAll(body.substring(start, end))
        .mapNotNull { entry ->
          val text = entry.groupValues[1]
          val effect = MOVE_EFFECT.find(text)?.groupValues?.get(1) ?: return@mapNotNull null
          // A missing or zero chance means the effect is certain, which the client spells 100.
          val chance = numeric(text, "chance", updatedThrough) ?: 0
          AdditionalEffect(effect, if (chance == 0) CERTAIN_CHANCE else chance)
        }
        .toList()
  }

  private fun field(body: String, name: String): String? =
      Regex("""\.$name\s*=\s*([A-Za-z0-9_]+)""").find(body)?.groupValues?.get(1)

  private fun clean(text: String): String =
      WHITESPACE.replace(CONTROL.replace(text, " ").replace("\\", ""), " ").trim()
}

/** GEN_LATEST, when the Expansion config does not say otherwise. */
private const val LATEST_GENERATION = 9

/** GEN_LATEST points at GEN_9, so one hop; a few more in case a checkout adds its own alias. */
private const val MAX_ALIAS_DEPTH = 4

/** The decomp writes a guaranteed secondary effect as chance 0; the client shows it as 100%. */
private const val CERTAIN_CHANCE = 100
