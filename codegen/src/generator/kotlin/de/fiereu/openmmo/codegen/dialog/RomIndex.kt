package de.fiereu.openmmo.codegen.dialog

import java.io.File

/**
 * A retail GBA ROM located by its header game code, used to resolve a text's file offset. The ROM
 * is byte-identical to the pret decomp, so the offset of an encoded string is the value PokeMMO
 * packs into a dialog textId.
 */
class RomIndex private constructor(private val latin1: String) {

  /**
   * The offset of [bytes] in the ROM, or -1 if absent. When the same encoded string occurs more
   * than once, the client only accepts the copy the game actually points at, so occurrences whose
   * GBA pointer (0x08000000 + offset, little-endian) appears in the ROM win over earlier ones.
   */
  fun offsetOf(bytes: ByteArray): Int {
    val needle = String(bytes, Charsets.ISO_8859_1)
    val first = latin1.indexOf(needle)
    if (first < 0) return first
    var offset = first
    while (offset >= 0) {
      if (isPointerReferenced(offset)) return offset
      offset = latin1.indexOf(needle, offset + 1)
    }
    return first
  }

  private fun isPointerReferenced(offset: Int): Boolean {
    val address = 0x08000000 + offset
    val pointer =
        String(
            byteArrayOf(
                (address and 0xFF).toByte(),
                ((address shr 8) and 0xFF).toByte(),
                ((address shr 16) and 0xFF).toByte(),
                ((address shr 24) and 0xFF).toByte(),
            ),
            Charsets.ISO_8859_1)
    return latin1.contains(pointer)
  }

  companion object {
    private const val GAME_CODE_OFFSET = 0xAC

    private const val VERSION_OFFSET = 0xBC

    /**
     * Finds the ROM in [romsDir] whose header game code equals [gameCode] (e.g. "BPEE"), and whose
     * header version byte equals [version] when one is asked for (FireRed v1.0 = 0, v1.1 = 1).
     */
    fun find(romsDir: File, gameCode: String, version: Int? = null): RomIndex? {
      val rom =
          romsDir
              .listFiles { f -> f.isFile }
              ?.firstOrNull { f ->
                f.length() > VERSION_OFFSET + 1 &&
                    readGameCode(f) == gameCode &&
                    (version == null || readVersion(f) == version)
              } ?: return null
      return RomIndex(String(rom.readBytes(), Charsets.ISO_8859_1))
    }

    private fun readGameCode(file: File): String =
        file.inputStream().use { s ->
          s.skip(GAME_CODE_OFFSET.toLong())
          val b = ByteArray(4)
          if (s.read(b) != 4) "" else String(b, Charsets.US_ASCII)
        }

    private fun readVersion(file: File): Int =
        file.inputStream().use { s ->
          s.skip(VERSION_OFFSET.toLong())
          s.read()
        }
  }
}
