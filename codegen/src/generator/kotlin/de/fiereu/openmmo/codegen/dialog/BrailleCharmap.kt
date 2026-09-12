package de.fiereu.openmmo.codegen.dialog

/**
 * The GBA braille encoder: the bytes a `.braille "..."` literal assembles to (tools/preproc
 * asm_file.cpp ReadBrailleString over include/characters.h BRAILLE_CHAR_*, identical in FireRed and
 * Emerald). The client draws a braille sign straight from these ROM bytes, so the dialog table
 * needs their exact offset just like a `.string` text.
 */
object BrailleCharmap {
  private const val SPACE = 0x00
  private const val COMMA = 0x04
  private const val COLON = 0x0C
  private const val APOSTROPHE = 0x10
  private const val SLASH = 0x12
  private const val SEMICOLON = 0x14
  private const val EXCL_MARK = 0x1C
  private const val PERIOD = 0x2C
  private const val HYPHEN = 0x30
  private const val QUESTION_MARK = 0x34
  private const val NUMBER = 0x3A
  private const val PAREN = 0x3C
  private const val NEWLINE = 0xFE
  private const val EOS = 0xFF

  /** BRAILLE_CHAR_A..Z, the font's own order (not the dot pattern). */
  private val letters =
      intArrayOf(
          0x01, 0x05, 0x03, 0x0B, 0x09, 0x07, 0x0F, 0x0D, 0x06, 0x0E, 0x11, 0x15, 0x13, 0x1B, 0x19, 0x17, 0x1F, 0x1D, 0x16,
          0x1E, 0x31, 0x35, 0x2E, 0x33, 0x3B, 0x39)

  /** Digits 1..9,0 are BRAILLE_CHAR_A..J behind a number sign. */
  private fun digit(c: Char): Int = letters[(c - '0' + 9) % 10]

  private val marks =
      mapOf(
          ' ' to SPACE, ',' to COMMA, '.' to PERIOD, '?' to QUESTION_MARK, '!' to EXCL_MARK, ':' to COLON,
          ';' to SEMICOLON, '-' to HYPHEN, '/' to SLASH, '(' to PAREN, ')' to PAREN, '\'' to APOSTROPHE)

  /** Encodes the literal content of one or more `.braille` lines (escapes `\n`, terminator `$`). */
  fun encode(text: String): ByteArray? {
    val out = ArrayList<Byte>(text.length + 1)
    var inNumber = false
    var i = 0
    while (i < text.length) {
      val c = text[i]
      when {
        c == '\\' && i + 1 < text.length && text[i + 1] == 'n' -> {
          out.add(NEWLINE.toByte())
          i += 2
          continue
        }
        c == '$' -> out.add(EOS.toByte())
        c.isLetter() && c.uppercaseChar() in 'A'..'Z' -> out.add(letters[c.uppercaseChar() - 'A'].toByte())
        c.isDigit() -> {
          if (!inNumber) {
            out.add(NUMBER.toByte())
            inNumber = true
          }
          out.add(digit(c).toByte())
        }
        else -> {
          val code = marks[c] ?: return null
          if (code == SPACE) inNumber = false
          out.add(code.toByte())
        }
      }
      i++
    }
    return out.toByteArray()
  }
}
