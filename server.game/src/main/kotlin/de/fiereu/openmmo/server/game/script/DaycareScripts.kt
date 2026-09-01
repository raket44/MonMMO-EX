package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.dialog.DialogLine
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

private data class Line(override val textId: Int) : DialogLine

/**
 * One shared daycare interaction for EVERY daycare npc, per the operator: the ROM's
 * would-you-like-us-to-raise question, and a YES opens the client's own breed-selection window
 * (pick two party monsters). A NO gets the ROM's come-again line. Breeding RESULTS are not modeled
 * yet - the window's response is logged so the egg system can build on real payloads.
 */
internal object DaycareScripts {

  private fun daycare(ask: Int, whichMon: Int, decline: Int) = Script { ctx ->
    if (ctx.askYesNo(Line(ask))) {
      val response = ctx.breedSelection(Line(whichMon))
      log.info { "Daycare breed selection responded $response" }
    } else {
      ctx.say(Line(decline))
    }
  }

  val byLabel: Map<String, Script> = buildMap {
    // FRLG day_care.inc shared texts serve both Kanto daycares.
    val kanto = daycare(ask = 1832932, whichMon = 1833017, decline = 1833238)
    val hoenn = daycare(ask = 271131380, whichMon = 271131465, decline = 271131670)
    put("Route5_PokemonDayCare_EventScript_DaycareMan", kanto)
    put("FourIsland_EventScript_DaycareMan", kanto)
    put("FourIsland_PokemonDayCare_EventScript_DaycareWoman", kanto)
    put("Route117_EventScript_DaycareMan", hoenn)
    put("Route117_PokemonDayCare_EventScript_DaycareWoman", hoenn)
  }
}
