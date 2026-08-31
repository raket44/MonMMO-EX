package de.fiereu.openmmo.codegen.move

import java.io.File

/**
 * The Expansion's build settings, read from its own headers.
 *
 * Move data is not written as plain values. Numbers hide behind `B_UPDATED_MOVE_DATA >= GEN_n ? new
 * : old`, and flags behind expressions like `(B_UPDATED_MOVE_FLAGS >= GEN_6) ||
 * (B_UPDATED_MOVE_FLAGS < GEN_3)`. Choosing those settings is how a checkout picks which
 * generation's rules it wants, so they are resolved rather than assumed.
 */
class ExpansionConfig private constructor(private val defines: Map<String, String>) {

  /** Follows aliases: `GEN_LATEST` to `GEN_9` to 9. */
  fun value(symbol: String): Int? = resolve(symbol, 0)

  private fun resolve(symbol: String, depth: Int): Int? {
    if (depth > MAX_DEPTH) return null
    val token = symbol.trim()
    token.toIntOrNull()?.let {
      return it
    }
    if (token.startsWith("GEN_")) {
      token.removePrefix("GEN_").toIntOrNull()?.let {
        return it
      }
    }
    val target = defines[token] ?: return null
    return resolve(target, depth + 1)
  }

  /**
   * Evaluates `TRUE`, a bare setting, a comparison against a generation, and those joined by `||`.
   * Anything outside that grammar is unknown rather than false, so a caller can tell them apart.
   */
  fun boolean(expression: String): Boolean? {
    var sawUnknown = false
    expression.split("||").forEach { part ->
      when (term(part.trim().removeSurrounding("(", ")").trim())) {
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
    return value(text)?.let { it != 0 }
  }

  companion object {
    private const val MAX_DEPTH = 8
    private val COMPARISON =
        Regex("""([A-Za-z_][A-Za-z0-9_]*)\s*(>=|<=|==|!=|>|<)\s*([A-Za-z0-9_]+)""")
    private val DEFINE = Regex("""(?m)^\s*#define\s+([A-Za-z_][A-Za-z0-9_]*)\s+([^/\r\n]+)""")

    fun read(rootDir: File): ExpansionConfig {
      val defines = linkedMapOf("TRUE" to "1", "FALSE" to "0")
      File(rootDir, "include/config")
          .listFiles { file -> file.name.endsWith(".h") }
          ?.sortedBy { it.name }
          ?.forEach { file ->
            DEFINE.findAll(file.readText()).forEach { match ->
              defines.putIfAbsent(match.groupValues[1], match.groupValues[2].trim())
            }
          }
      return ExpansionConfig(defines)
    }
  }
}
