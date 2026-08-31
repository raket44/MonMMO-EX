package de.fiereu.openmmo.launcher.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Per-species front animation scripts from the Expansion's species tables.
 *
 * The Expansion animates a front sprite the same way the GBA games did: `anim_front.png` stacks the
 * animation frames vertically, and `.frontAnimFrames = ANIM_FRAMES(ANIMCMD_FRAME(frame, duration),
 * ...)` is the playback script - which stacked frame to show, for how many 60fps ticks. The client,
 * meanwhile, plays whatever animated GIF a mod provides for a battle sprite; the shipped HD mod is
 * two gigabytes of exactly that. Converting the script to GIF frame delays is therefore the entire
 * animation port for species sprites: the Expansion supplies the choreography, the GIF carries it.
 */
object ExpansionFrontAnims {

  data class Step(val frame: Int, val durationTicks: Int)

  /** Parses every species-info file; keyed by bare species symbol, e.g. `SYLVEON`. */
  fun parse(expansionRoot: Path): Map<String, List<Step>> {
    val speciesInfo = expansionRoot.resolve("src/data/pokemon/species_info")
    val result = linkedMapOf<String, List<Step>>()
    Files.list(speciesInfo).use { files ->
      files
          .filter { it.name.endsWith(".h") }
          .sorted()
          .forEach { file ->
            val text = Files.readString(file)
            ENTRY.findAll(text).forEach { entry ->
              val block = FRAMES.find(entry.groupValues[2])?.groupValues?.get(1) ?: return@forEach
              val steps =
                  STEP.findAll(block)
                      .map { Step(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
                      .toList()
              if (steps.isNotEmpty()) result[entry.groupValues[1]] = steps
            }
          }
    }
    return result
  }

  private val ENTRY =
      Regex(
          """\[SPECIES_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[SPECIES_[A-Z0-9_]+]\s*=|\Z)""",
          RegexOption.DOT_MATCHES_ALL,
      )

  // The command list closes with the same `),` every ANIMCMD_FRAME ends in, so a lazy match to
  // the first close truncates the list to nothing; matching whole commands is what works.
  private val FRAMES =
      Regex("""\.frontAnimFrames\s*=\s*ANIM_FRAMES\(\s*((?:ANIMCMD_FRAME\([^)]*\)\s*,?\s*)+)\)""")

  private val STEP = Regex("""ANIMCMD_FRAME\(\s*(\d+)\s*,\s*(\d+)\s*\)""")
}
