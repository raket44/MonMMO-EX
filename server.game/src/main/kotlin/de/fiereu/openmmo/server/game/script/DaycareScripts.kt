package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.dialog.DialogLine
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

private data class Line(override val textId: Int) : DialogLine

/**
 * The daycare pair, split the way retail splits them (operator-specified):
 * - the MEN offer BREEDING - their yes leads to the client's breed-selection window (pick two).
 *   Retail gives them custom lead-up dialog; until the text-override pipeline exists they ask with
 *   the ROM's would-you-like-us-to-raise line, the closest yes/no the ROM offers.
 * - the WOMEN offer RAISING - the ROM's ask, and a yes opens the storage window to choose a
 *   monster.
 *
 * Neither result is modeled yet: the breed window's response and the storage picks are logged so
 * the egg/raising systems can build on real payloads.
 */
internal object DaycareScripts {

  private fun daycareMan(ask: Int, whichMon: Int, decline: Int) = Script { ctx ->
    if (ctx.askYesNo(Line(ask))) {
      val response = ctx.breedSelection(Line(whichMon))
      log.info { "Daycare breed selection responded $response" }
    } else {
      ctx.say(Line(decline))
    }
  }

  private fun daycareWoman(ask: Int, decline: Int) = Script { ctx ->
    if (ctx.askYesNo(Line(ask))) {
      ctx.openPcStorageWindow()
    } else {
      ctx.say(Line(decline))
    }
  }

  /**
   * The DS daycares share one chunk pair in both ROMs (NDS_CHUNK_9500 the man, 9501 the lady;
   * Platinum's DayCareCommon_Man/Lady, HeartGold's scr_seq_0265_000/001), so the region picks
   * the ROM's own lines: Platinum bank 547 (day_care_common) entries 15/16/20, HeartGold bank 439
   * entries 21/22/26 - the lady's "raise a Pokemon?", "which one?", and "oh, fine then".
   */
  private fun dsDaycare(man: Boolean) = Script { ctx ->
    val johto = de.fiereu.openmmo.common.enums.Region.byId(ctx.state.regionId) == de.fiereu.openmmo.common.enums.Region.JOHTO
    val ask = if (johto) 1102512149 else 841154575
    val whichMon = if (johto) 1102512150 else 841154576
    val decline = if (johto) 1102512154 else 841154580
    if (man) daycareMan(ask, whichMon, decline).run(ctx) else daycareWoman(ask, decline).run(ctx)
  }

  val byLabel: Map<String, Script> = buildMap {
    // FRLG day_care.inc shared texts serve both Kanto daycares.
    val kantoMan = daycareMan(ask = 1832932, whichMon = 1833017, decline = 1833238)
    val kantoWoman = daycareWoman(ask = 1832932, decline = 1833238)
    val hoennMan = daycareMan(ask = 271131380, whichMon = 271131465, decline = 271131670)
    val hoennWoman = daycareWoman(ask = 271131380, decline = 271131670)
    put("Route5_PokemonDayCare_EventScript_DaycareMan", kantoMan)
    put("FourIsland_EventScript_DaycareMan", kantoMan)
    put("FourIsland_PokemonDayCare_EventScript_DaycareWoman", kantoWoman)
    put("Route117_EventScript_DaycareMan", hoennMan)
    put("Route117_PokemonDayCare_EventScript_DaycareWoman", hoennWoman)
    put("NDS_CHUNK_9500", dsDaycare(man = true))
    put("NDS_CHUNK_9501", dsDaycare(man = false))
  }
}
