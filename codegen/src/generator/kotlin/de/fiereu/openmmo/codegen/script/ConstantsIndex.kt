package de.fiereu.openmmo.codegen.script

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Integer constants scripts reference by name: the object-like `#define`s under include/constants
 * plus the MAP_ ids the pret build synthesizes from data/maps (value = num | group << 8). These let
 * the parser turn `setvar VAR_X, SOME_STATE` and `warp MAP_X, ...` into plain numbers.
 */
object ConstantsIndex {
  private val define = Regex("^#define\\s+(\\w+)([^\\w(].*)?$")

  fun build(decompDir: File): Map<String, Int> {
    val resolved = HashMap<String, Int>()
    val pending = mutableListOf<Pair<String, String>>()

    val constantsDir = File(decompDir, "include/constants")
    if (constantsDir.isDirectory) {
      constantsDir
          .walkTopDown()
          .filter { it.isFile && it.extension == "h" }
          .forEach { file ->
            for (raw in file.readLines()) {
              val line = raw.substringBefore("//").trim()
              val match = define.matchEntire(line) ?: continue
              val expr = match.groupValues[2].trim()
              if (expr.isNotEmpty()) pending += match.groupValues[1] to expr
            }
          }
    }

    // Definitions reference each other in either direction, so evaluate to a fixpoint.
    var progressed = true
    while (progressed && pending.isNotEmpty()) {
      progressed = false
      val iterator = pending.iterator()
      while (iterator.hasNext()) {
        val (name, expr) = iterator.next()
        val value = ExpressionParser(expr, resolved).parseOrNull() ?: continue
        if (name !in resolved) resolved[name] = value
        iterator.remove()
        progressed = true
      }
    }

    loadMapIds(decompDir, resolved)
    return resolved
  }

  private fun loadMapIds(decompDir: File, out: MutableMap<String, Int>) {
    val groupsFile = File(decompDir, "data/maps/map_groups.json")
    if (!groupsFile.isFile) return
    val groups = Json.parseToJsonElement(groupsFile.readText()).jsonObject
    val order = groups["group_order"]?.jsonArray ?: return
    order.forEachIndexed { groupIndex, groupName ->
      val maps = groups[groupName.jsonPrimitive.content]?.jsonArray ?: return@forEachIndexed
      maps.forEachIndexed { mapIndex, dirName ->
        val mapJson = File(decompDir, "data/maps/${dirName.jsonPrimitive.content}/map.json")
        if (!mapJson.isFile) return@forEachIndexed
        val id =
            Json.parseToJsonElement(mapJson.readText()).jsonObject["id"]?.jsonPrimitive?.content
                ?: return@forEachIndexed
        out[id] = mapIndex or (groupIndex shl 8)
      }
    }
  }

  /**
   * Recursive-descent evaluator for the expression subset constants headers actually use: integers,
   * resolved identifiers, parens, unary minus/complement, and | & ^ << >> + - *.
   */
  private class ExpressionParser(
      source: String,
      private val names: Map<String, Int>,
  ) {
    private val tokens = tokenize(source)
    private var index = 0

    fun parseOrNull(): Int? =
        try {
          if (tokens == null) null else parseOr().also { check(index == tokens.size) }
        } catch (_: Exception) {
          null
        }

    private fun parseOr(): Int {
      var value = parseXor()
      while (peek() == "|") {
        next()
        value = value or parseXor()
      }
      return value
    }

    private fun parseXor(): Int {
      var value = parseAnd()
      while (peek() == "^") {
        next()
        value = value xor parseAnd()
      }
      return value
    }

    private fun parseAnd(): Int {
      var value = parseShift()
      while (peek() == "&") {
        next()
        value = value and parseShift()
      }
      return value
    }

    private fun parseShift(): Int {
      var value = parseAdditive()
      while (peek() == "<<" || peek() == ">>") {
        val op = next()
        val operand = parseAdditive()
        value = if (op == "<<") value shl operand else value shr operand
      }
      return value
    }

    private fun parseAdditive(): Int {
      var value = parseMultiplicative()
      while (peek() == "+" || peek() == "-") {
        val op = next()
        val operand = parseMultiplicative()
        value = if (op == "+") value + operand else value - operand
      }
      return value
    }

    private fun parseMultiplicative(): Int {
      var value = parseUnary()
      while (peek() == "*") {
        next()
        value *= parseUnary()
      }
      return value
    }

    private fun parseUnary(): Int =
        when (peek()) {
          "-" -> {
            next()
            -parseUnary()
          }
          "~" -> {
            next()
            parseUnary().inv()
          }
          "(" -> {
            next()
            val value = parseOr()
            check(next() == ")")
            value
          }
          else -> {
            val token = next()
            when {
              token.startsWith("0x") || token.startsWith("0X") -> token.drop(2).toInt(16)
              token.first().isDigit() -> token.toInt()
              else -> names[token] ?: error("unresolved $token")
            }
          }
        }

    private fun peek(): String? = tokens?.getOrNull(index)

    private fun next(): String = checkNotNull(tokens)[index++]

    private fun tokenize(source: String): List<String>? {
      val out = mutableListOf<String>()
      var i = 0
      while (i < source.length) {
        val c = source[i]
        when {
          c.isWhitespace() -> i++
          c.isLetterOrDigit() || c == '_' -> {
            val start = i
            while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
            out += source.substring(start, i)
          }
          c == '<' || c == '>' -> {
            if (i + 1 >= source.length || source[i + 1] != c) return null
            out += "$c$c"
            i += 2
          }
          c in "|&^+-*~()" -> {
            out += c.toString()
            i++
          }
          else -> return null
        }
      }
      return out.takeIf { it.isNotEmpty() }
    }
  }
}
