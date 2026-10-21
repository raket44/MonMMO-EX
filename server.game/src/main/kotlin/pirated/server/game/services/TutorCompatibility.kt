package pirated.server.game.services

import pirated.common.EXPANSION_SERVER_SPECIES_BASE
import pirated.common.enums.Region
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Which monsters a ROM move tutor will teach.
 *
 * Retail species follow each ROM's tutor_learnsets.h (`pirated/tutor-learnsets-<region>.csv`, keyed
 * by national dex id). Expansion species live at server ids from [EXPANSION_SERVER_SPECIES_BASE]
 * and never appear there, so they follow the Expansion's own taught lists instead
 * (`pirated/tutor-learnsets-expansion.csv`, keyed by server id, staged by the launcher's
 * stageTutorLearnsets). The three starter moves (Frenzy Plant, Blast Burn, Hydro Cannon) are gated
 * on the final evolutions like party_menu.c, whatever either table says.
 */
internal class TutorCompatibility(
    private val kanto: Map<Int, Set<Int>>,
    private val hoenn: Map<Int, Set<Int>>,
    private val expansion: Map<Int, Set<Int>>,
) {
  fun canLearn(region: Region?, dexId: Int, moveId: Int): Boolean =
      when (moveId) {
        FRENZY_PLANT -> dexId == VENUSAUR
        BLAST_BURN -> dexId == CHARIZARD
        HYDRO_CANNON -> dexId == BLASTOISE
        else -> {
          val learnsets =
              when {
                dexId >= EXPANSION_SERVER_SPECIES_BASE -> expansion
                region == Region.HOENN -> hoenn
                else -> kanto
              }
          learnsets[dexId]?.contains(moveId) == true
        }
      }

  companion object {
    const val FRENZY_PLANT = 338
    const val BLAST_BURN = 307
    const val HYDRO_CANNON = 308
    private const val VENUSAUR = 3
    private const val CHARIZARD = 6
    private const val BLASTOISE = 9

    fun load(): TutorCompatibility =
        TutorCompatibility(
            load("/pirated/tutor-learnsets-kanto.csv"),
            load("/pirated/tutor-learnsets-hoenn.csv"),
            load("/pirated/tutor-learnsets-expansion.csv"),
        )

    private fun load(resource: String): Map<Int, Set<Int>> {
      val stream = TutorCompatibility::class.java.getResourceAsStream(resource)
      if (stream == null) {
        log.warn { "missing tutor learnset table $resource" }
        return emptyMap()
      }
      return stream.bufferedReader().useLines { lines ->
        lines
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line -> line.split(',').map { it.trim().toInt() } }
            .associate { it.first() to it.drop(1).toSet() }
      }
    }
  }
}
