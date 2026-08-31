package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * The Expansion's build settings, resolved from its own headers.
 *
 * Move data is not written as plain numbers. Power, accuracy and PP are gated behind
 * `B_UPDATED_MOVE_DATA >= GEN_n ? new : old` on 334 moves, and the flags behind expressions like
 * `(B_UPDATED_MOVE_FLAGS >= GEN_6) || (B_UPDATED_MOVE_FLAGS < GEN_3)`. Reading the first token off
 * the line gives the wrong answer - Flying Press arrives with no power that way.
 *
 * Choosing those settings is how a checkout decides which generation's rules it wants, so they are
 * read rather than assumed.
 */
class ExpansionConfig private constructor(private val defines: Map<String, String>) {

  /** Resolves a symbol to a number, following aliases: `GEN_LATEST` to `GEN_9` to 9. */
  fun value(symbol: String): Int? = resolve(symbol, 0)

  private fun resolve(symbol: String, depth: Int): Int? {
    if (depth > MAX_DEPTH) return null
    val token = symbol.trim()
    token.toIntOrNull()?.let {
      return it
    }
    if (token.startsWith("GEN_"))
        token.removePrefix("GEN_").toIntOrNull()?.let {
          return it
        }
    val target = defines[token] ?: return null
    return resolve(target, depth + 1)
  }

  /**
   * Evaluates the boolean expressions the move table uses: `TRUE`, a bare setting, a comparison
   * against a generation, and those joined by `||`. Anything outside that grammar is unknown rather
   * than false, so a caller can tell the difference.
   */
  fun boolean(expression: String): Boolean? {
    val terms = expression.split("||").map { it.trim().removeSurrounding("(", ")").trim() }
    var sawUnknown = false
    terms.forEach { term ->
      when (val result = term(term)) {
        true -> return true
        null -> sawUnknown = true
        false -> Unit
      }
    }
    return if (sawUnknown) null else false
  }

  private fun term(text: String): Boolean? {
    val comparison = COMPARISON.matchEntire(text)
    if (comparison != null) {
      val (left, operator, right) = comparison.destructured
      val a = value(left) ?: return null
      val b = value(right) ?: return null
      return when (operator) {
        ">=" -> a >= b
        "<=" -> a <= b
        ">" -> a > b
        "<" -> a < b
        "==" -> a == b
        else -> a != b
      }
    }
    return when (val resolved = value(text)) {
      null -> null
      else -> resolved != 0
    }
  }

  companion object {
    private const val MAX_DEPTH = 8
    private val COMPARISON =
        Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*(>=|<=|==|!=|>|<)\s*([A-Za-z0-9_]+)""")
    private val DEFINE = Regex("""(?m)^\s*#define\s+([A-Za-z_][A-Za-z0-9_]*)\s+([^/\r\n]+)""")

    fun read(expansionRoot: Path): ExpansionConfig {
      val defines = linkedMapOf("TRUE" to "1", "FALSE" to "0")
      val configs = expansionRoot.resolve("include/config")
      Files.list(configs).use { files ->
        files
            .filter { it.fileName.toString().endsWith(".h") }
            .sorted()
            .forEach { file ->
              DEFINE.findAll(Files.readString(file)).forEach { match ->
                // First definition wins, matching the include order closely enough for settings.
                defines.putIfAbsent(match.groupValues[1], match.groupValues[2].trim())
              }
            }
      }
      return ExpansionConfig(defines)
    }
  }
}
