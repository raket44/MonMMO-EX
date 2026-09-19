package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path

/**
 * BW-style animated battlers in Pokemon Essentials' Elite Battle System layout: one PNG per
 * species and side, a horizontal strip of square frames (frame = the strip's height), under
 * `Graphics/Battlers/{Front,FrontShiny,Back,BackShiny}/<nationalDex>.png`. Real shiny animations,
 * unlike Showdown's Gen 5-style set.
 *
 * Source: Sturdy-Ghost's "Pokemon Essentials EBS Gen6+Gen7 ANIMATED Sprites" compilation of the
 * DeviantArt BW-style animators (#650-807, no forms), fetched 2026-09-19 at the owner's request
 * into `reference/sprite-packs/ebs-gen67/`; its terms ask for a credits entry with the source
 * links (see reference/sprite-packs/CREDITS.md). Base species only: the files are keyed by Dex
 * number, so forms never resolve here.
 */
class EbsStripPack(private val root: Path) {
  val available: Boolean
    get() = Files.isDirectory(root.resolve("Graphics/Battlers/Front"))

  data class Set(val front: Path, val frontShiny: Path?, val back: Path?, val backShiny: Path?)

  fun resolve(nationalDex: Int?): Set? {
    if (nationalDex == null) return null
    fun side(folder: String): Path? =
        root.resolve("Graphics/Battlers/$folder/$nationalDex.png").takeIf { Files.isRegularFile(it) }
    val front = side("Front") ?: return null
    return Set(front, side("FrontShiny"), side("Back"), side("BackShiny"))
  }

  companion object {
    /** Where the pack sits relative to the Showdown root's parent, reference/sprite-packs. */
    const val DIR = "ebs-gen67"
  }
}
