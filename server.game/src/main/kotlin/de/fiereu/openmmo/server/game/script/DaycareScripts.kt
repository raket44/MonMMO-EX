package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.dialog.DialogLine
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

private data class Line(override val textId: Int) : DialogLine

/**
 * The daycare pair, split the way retail splits them (operator-specified):
 * - the MEN offer BREEDING - their yes leads to the client's breed-selection window (pick two).
 *   ONE script for every daycare man in every region (owner, 2026-09-16), in the owner's words
 *   (staged client strings), with FRLG's own decline line.
 * - the WOMEN offer RAISING - the ROM's ask, and a yes opens the storage window to choose a
 *   monster.
 *
 * Neither result is modeled yet: the breed window's response and the storage picks are logged so
 * the egg/raising systems can build on real payloads.
 */
internal object DaycareScripts {

  /**
   * The owner's daycare man (2026-09-16): his bargain, then a warning that the two breeders are
   * gone for good, then the client's pick-two window; a no at either step gets the decline.
   */
  private fun daycareMan(ask: Int, confirm: Int, whichMon: Int, decline: Int) = Script { ctx ->
    if (ctx.askYesNo(Line(ask)) && ctx.askYesNo(Line(confirm))) {
      val response = ctx.breedSelection(Line(whichMon))
      log.info { "Daycare breed selection responded $response" }
    } else {
      ctx.say(Line(decline))
    }
  }

  /** Client strings staged by the launcher (ExpansionClientContentMain DAYCARE_STRINGS). */
  private const val MAN_BARGAIN = 16790020
  private const val MAN_CONFIRM = 16790021

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
  private fun dsDaycareWoman() = Script { ctx ->
    val johto = de.fiereu.openmmo.common.enums.Region.byId(ctx.state.regionId) == de.fiereu.openmmo.common.enums.Region.JOHTO
    val ask = if (johto) 1102512149 else 841154575
    val decline = if (johto) 1102512154 else 841154580
    daycareWoman(ask, decline).run(ctx)
  }

  val byLabel: Map<String, Script> = buildMap {
    // FRLG day_care.inc shared texts serve both Kanto daycares.
    val kantoMan = daycareMan(ask = MAN_BARGAIN, confirm = MAN_CONFIRM, whichMon = 1833017, decline = 1833238)
    val kantoWoman = daycareWoman(ask = 1832932, decline = 1833238)
    val hoennWoman = daycareWoman(ask = 271131380, decline = 271131670)
    put("Route5_PokemonDayCare_EventScript_DaycareMan", kantoMan)
    put("FourIsland_EventScript_DaycareMan", kantoMan)
    put("FourIsland_PokemonDayCare_EventScript_DaycareWoman", kantoWoman)
    put("Route117_EventScript_DaycareMan", kantoMan)
    put("Route117_PokemonDayCare_EventScript_DaycareWoman", hoennWoman)
    put("NDS_CHUNK_9500", kantoMan)
    put("NDS_CHUNK_9501", dsDaycareWoman())
  }
}
