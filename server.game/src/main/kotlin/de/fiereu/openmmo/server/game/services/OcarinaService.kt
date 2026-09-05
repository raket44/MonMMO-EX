package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The Sweet Scent Ocarina (bag item 1179). The client handles it on its own terms (f/ZO1.k70,
 * f/ZB1.mf1): it synthesizes the region's stand-in monster itself (Tropius, Teddiursa...), keeps
 * the pp spent in one byte of the local character record (f/ZZ.Og1, sent in the character info
 * and updated by the character delta's 0x100 field) as the pp LEFT, uses the ocarina while it
 * covers the move's cost, and otherwise offers Leppa Berries for the 32 minus it that is missing. When it does use it,
 * it sends the ordinary use-item packet (0x26) with the item id.
 *
 * Here a use spends 5 pp (six uses per 32) and starts a horde on the tile; a Pokemon Center heal
 * or an opened PC refills. The pp spent lives in a story var.
 */
@Singleton
class OcarinaService
@Inject
constructor(
    private val characters: CharacterStore,
    private val encounters: javax.inject.Provider<EncounterService>,
    private val maps: MapManager,
) {

  fun isOcarina(itemId: Int): Boolean = itemId == SWEET_SCENT_OCARINA

  private fun ppSpent(stored: StoredCharacter): Int = (stored.storyVars[SPENT_KEY] ?: 0).coerceIn(0, MAX_PP)

  /** The pp the ocarina has left, for the character record's byte. */
  fun ppLeft(stored: StoredCharacter): Int = MAX_PP - ppSpent(stored)

  /** The bag's use of the ocarina: the stand-in uses Sweet Scent here, spending pp. */
  suspend fun use(ctx: SessionContext, state: PlayerState, charId: Long, itemId: Int) {
    if (itemId != SWEET_SCENT_OCARINA) return
    val stored = characters.getCharacter(charId) ?: return
    val spent = ppSpent(stored)
    if (spent + USE_COST > MAX_PP) {
      ctx.send(notice("The Sweet Scent Ocarina has no pp left. Heal at a Pokemon Center or open a PC to refill it."))
      ctx.send(LocalCharacterDeltaPacket(value256 = (MAX_PP - spent).toByte()))
      return
    }
    val problem = encounters.get().startHorde(ctx, charId, state, maps.getMap(state.regionId, state.bankId, state.mapId), HORDE_SIZE)
    if (problem != null) {
      ctx.send(notice(problem))
      return
    }
    val now = spent + USE_COST
    characters.setStoryVar(charId, SPENT_KEY, now)
    ctx.send(LocalCharacterDeltaPacket(value256 = (MAX_PP - now).toByte()))
    log.info { "[Ocarina] char=$charId Sweet Scent used, pp spent $now/$MAX_PP" }
  }

  /** A Pokemon Center heal or an opened PC restores the ocarina. */
  fun refill(ctx: SessionContext, charId: Long) {
    val stored = characters.getCharacter(charId) ?: return
    if (SWEET_SCENT_OCARINA !in stored.items || ppSpent(stored) == 0) return
    characters.setStoryVar(charId, SPENT_KEY, 0)
    ctx.send(LocalCharacterDeltaPacket(value256 = MAX_PP.toByte()))
    ctx.send(notice("Sweet Scent Ocarina refilled."))
  }

  private companion object {
    const val SWEET_SCENT_OCARINA = 1179
    const val SPENT_KEY = "ocarina/1179/spent"
    const val MAX_PP = 32
    const val USE_COST = 5
    const val HORDE_SIZE = 5
  }
}
