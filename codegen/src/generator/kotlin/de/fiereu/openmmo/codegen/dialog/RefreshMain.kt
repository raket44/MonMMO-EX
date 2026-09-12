@file:JvmName("RefreshMain")

package de.fiereu.openmmo.codegen.dialog

import de.fiereu.openmmo.common.enums.Region
import java.io.File

// A GBA dialog id is the retail ROM file offset with the region on top, which is how the client
// knows the ROM to resolve it in. Captured Kanto ids carry 0 there and Hoenn ids carry 1.
private const val REGION_SHIFT = 28

/** The header version byte of the ROM the client reads braille from: FireRed v1.1 (the only BPRE the client ships offsets for). */
private val BRAILLE_ROM_VERSION = mapOf("BPRE" to 1)

private fun regionMode(region: String): Int {
  val known =
      Region.entries.find { it.name.equals(region, ignoreCase = true) }
          ?: error("unknown region '$region', its dialog ids cannot be built")
  return known.wireValue.toInt() shl REGION_SHIFT
}

fun main(args: Array<String>) {
  require(args.size >= 3) {
    "Usage: <roms-dir> <data-dir> <region|gameCode|decomp>... got ${args.toList()}"
  }
  val romsDir = File(args[0])
  val dataDir = File(args[1])

  for (spec in args.drop(2)) {
    val (region, gameCode, decomp) = spec.split("|")
    refreshRegion(region, gameCode, File(decomp), romsDir, dataDir)
  }
}

private fun refreshRegion(
    region: String,
    gameCode: String,
    decompDir: File,
    romsDir: File,
    dataDir: File,
) {
  val rom =
      RomIndex.find(romsDir, gameCode)
          ?: error("no $gameCode ROM in $romsDir, so $region dialog ids cannot be resolved")
  // Braille signs are read RAW by the client: its braille window positions the ROM file at the id
  // and draws the bytes (f/iq1.Yq1, parse case 31), with none of the v1.0 -> v1.1 translation its
  // offsets table (data/offsets/BPREv1.1.dat) gives every ordinary text. The client only accepts
  // FireRed v1.1, so braille ids must be v1.1 file offsets even though every other Kanto id stays
  // the v1.0 canonical the table translates. Emerald has one version.
  val brailleRom =
      BRAILLE_ROM_VERSION[gameCode]?.let { v ->
        RomIndex.find(romsDir, gameCode, v)
            ?: error("no $gameCode v1.$v ROM in $romsDir; the client reads braille signs from that version raw")
      } ?: rom

  val texts = TextParser(decompDir).parseAll()
  val charmap = Charmap.load(File(decompDir, "charmap.txt"))
  val mode = regionMode(region)

  var unencodable = 0
  var notFound = 0
  val lines =
      texts.mapNotNull { t ->
        val bytes = if (t.braille) BrailleCharmap.encode(t.content) else charmap.encode(t.content)
        if (bytes == null) {
          unencodable++
          return@mapNotNull null
        }
        val offset = (if (t.braille) brailleRom else rom).offsetOf(bytes)
        if (offset < 0) {
          notFound++
          return@mapNotNull null
        }
        DialogLine(t.label, mode or offset, t.content)
      }

  val file = DialogTable.file(dataDir, region)
  DialogTable.write(file, region, gameCode, lines)
  println(
      "[dialog] $region: wrote ${lines.size} lines to $file (skipped $unencodable unencodable, $notFound not in ROM)")
}
