package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.server.game.script.Script
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The DS games' ENGINE-STANDARD npc scripts: ids that no script file holds because the engine runs
 * them itself, so the corpus has nothing to bind and the npc answered nothing at all. Unova's
 * Pokemon Center nurse is 2100 (fourteen of them, all sprite 78) and the mart clerk is 2101
 * (thirteen, sprite 79) - the owner was soft-locked in a Pokemon Center by the nurse not healing
 * (2026-09-20).
 *
 * Their words are the ROM's own: text bank 346 belongs to no map header, which is what an engine
 * script's text looks like.
 */
@Singleton
class NdsStandardScripts @Inject constructor() {

  /** The script for an engine-standard id, or null when it is not one. */
  fun scriptFor(scriptId: Int, regionId: Int): Script? {
    if (regionId != UNOVA) return null
    return when (scriptId) {
      NURSE -> Script { ctx -> nurse(ctx) }
      MART -> Script { ctx -> ctx.pokemartCommon() }
      else -> null
    }
  }

  /**
   * pokemon_center.c: the greeting asks, "OK, I'll take your Pokemon for a few seconds", the heal,
   * "we've restored your Pokemon to full health", then the send-off - which plays whether or not
   * the offer was taken.
   */
  private suspend fun nurse(ctx: de.fiereu.openmmo.server.game.script.ScriptContext) {
    if (ctx.askYesNo(line(GREETING))) {
      ctx.say(line(TAKING))
      ctx.healParty()
      ctx.say(line(RESTORED))
    }
    ctx.say(line(GOODBYE))
  }

  private fun line(textId: Int) = object : DialogLine { override val textId = textId }

  private companion object {
    const val UNOVA = 2
    /** Unova npc script ids the engine owns. */
    const val NURSE = 2100
    const val MART = 2101

    /** A Unova ROM text id: region 2, the DS bit, then the bank and entry (the corpus' own T-label). */
    private fun text(bank: Int, entry: Int) = (UNOVA shl 28) or (1 shl 27) or (bank shl 16) or entry

    /** The nurse's bank; it belongs to no map, as an engine script's text does. */
    const val NURSE_BANK = 346
    val GREETING = text(NURSE_BANK, 0)
    val TAKING = text(NURSE_BANK, 6)
    val RESTORED = text(NURSE_BANK, 7)
    val GOODBYE = text(NURSE_BANK, 8)
  }
}
