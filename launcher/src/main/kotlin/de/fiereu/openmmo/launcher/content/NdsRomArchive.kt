package de.fiereu.openmmo.launcher.content

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Path

/**
 * Minimal reader for an NDS ROM's filesystem and its NARC archives.
 *
 * Only what the map extraction needs: resolve a file by its `a/x/y/z` path, then pull one member
 * out of the NARC it holds. The formats are the documented Nitro ones - a FAT of start/end pairs, a
 * FNT directory tree, and inside a NARC the BTAF/BTNF/GMIF triple.
 */
class NdsRomArchive(romPath: Path) : AutoCloseable {
  private val channel = FileChannel.open(romPath)
  private val rom: ByteBuffer =
      channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.LITTLE_ENDIAN)

  private val fntOffset = rom.getInt(0x40)
  private val fatOffset = rom.getInt(0x48)

  /** File ids by full path, e.g. `a/0/6/5`. */
  val filesByPath: Map<String, Int> by lazy {
    val result = linkedMapOf<String, Int>()
    walk(ROOT_DIRECTORY, "", result)
    result
  }

  val title: String
    get() = buildString { (0 until 12).forEach { append(rom.get(it).toInt().toChar()) } }.trim()

  /** Every member of the NARC stored at [path], as independent byte arrays. */
  fun narcMembers(path: String): List<ByteArray> {
    val fileId = filesByPath[path] ?: error("No file $path in this ROM")
    val start = rom.getInt(fatOffset + fileId * 8)
    check(magic(start) == "NARC") { "$path is not a NARC" }
    val btaf = start + 16
    val count = rom.getShort(btaf + 8).toInt() and 0xFFFF
    val btnf = btaf + rom.getInt(btaf + 4)
    val dataStart = btnf + rom.getInt(btnf + 4) + 8
    return List(count) { index ->
      val from = rom.getInt(btaf + 12 + index * 8)
      val to = rom.getInt(btaf + 12 + index * 8 + 4)
      ByteArray(to - from) { rom.get(dataStart + from + it) }
    }
  }

  override fun close() = channel.close()

  private fun magic(offset: Int): String = buildString {
    (0 until 4).forEach { append(rom.get(offset + it).toInt().toChar()) }
  }

  private fun walk(directoryId: Int, prefix: String, into: MutableMap<String, Int>) {
    val entryOffset = fntOffset + (directoryId and 0xFFF) * 8
    var cursor = fntOffset + rom.getInt(entryOffset)
    var fileId = rom.getShort(entryOffset + 4).toInt() and 0xFFFF
    while (true) {
      val length = rom.get(cursor++).toInt() and 0xFF
      if (length == 0) break
      val isDirectory = (length and 0x80) != 0
      val nameLength = length and 0x7F
      val name = buildString {
        (0 until nameLength).forEach { append(rom.get(cursor + it).toInt().toChar()) }
      }
      cursor += nameLength
      if (isDirectory) {
        val sub = rom.getShort(cursor).toInt() and 0xFFFF
        cursor += 2
        walk(sub, "$prefix$name/", into)
      } else {
        into["$prefix$name"] = fileId
        fileId++
      }
    }
  }

  private companion object {
    const val ROOT_DIRECTORY = 0xF000
  }
}
