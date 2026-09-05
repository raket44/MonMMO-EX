package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.ServerMessageArg
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The Sweet Scent Ocarina (bag item 1179). The client handles it on its own terms (f/ZO1.k70,
 * f/ZB1.mf1): it synthesizes the region's stand-in monster itself, keeps the pp left in one byte
 * of the local character record (f/ZZ.Og1, sent in the character info and updated by the
 * character delta's 0x100 field), uses the ocarina while that covers the move's cost, and
 * otherwise offers Leppa Berries for the 32 minus it that is missing. When it does use it, it
 * sends the ordinary use-item packet (0x26) with the item id.
 *
 * Here a use announces the stand-in in the message box, spends 5 pp (six uses per 32) and starts
 * a horde on the tile; a Pokemon Center heal or an opened PC refills with the grey notice box.
 * The pp spent lives in a story var.
 */
@Singleton
class OcarinaService
@Inject
constructor(
    private val characters: CharacterStore,
    private val encounters: javax.inject.Provider<EncounterService>,
    private val maps: MapManager,
    private val dialog: DialogService,
    private val species: SpeciesRegistry,
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
    val map = maps.getMap(state.regionId, state.bankId, state.mapId)
    val problem = encounters.get().hordeAvailable(charId, state, map)
    if (problem != null) {
      ctx.send(notice(problem))
      return
    }
    // "{00} used its {01}!" (client string 6068) in the ordinary message box, with the stand-in
    // the client itself shows for this region.
    val standIn = species.get(STAND_INS[state.regionId] ?: STAND_INS.getValue(0))?.name ?: "Pokemon"
    dialog.showAndWait(
        ctx,
        state,
        USED_MOVE_STRING,
        MESSAGE_BOX,
        -1,
        DialogPresentation(
            messageArgs =
                listOf(
                    RawMessageArg(0, RAW_STRING, text = "${stored.info.name}'s summoned $standIn"),
                    RawMessageArg(1, RAW_STRING, text = "Sweet Scent"))))
    val started = encounters.get().startHorde(ctx, charId, state, map, HORDE_SIZE)
    if (started != null) {
      ctx.send(notice(started))
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
    // "{00} has been refreshed!" (client string 16777290) in the grey server-message box.
    ctx.send(
        ServerMessagePacket(
            REFRESHED_STRING,
            listOf(ServerMessageArg(0, RAW_STRING.toInt(), false, 0, null, null, "Sweet Scent Ocarina", null)),
            showOnMap = true,
            mode = null))
  }

  private companion object {
    const val SWEET_SCENT_OCARINA = 1179
    const val SPENT_KEY = "ocarina/1179/spent"
    const val MAX_PP = 32
    const val USE_COST = 5
    const val HORDE_SIZE = 5
    /** Client string "{00} used its {01}!". */
    const val USED_MOVE_STRING = 6068
    /** Client string "{00} has been refreshed!". */
    const val REFRESHED_STRING = 16777290
    /** Dialog action type of a plain message box (the sign kind, no speaker entity). */
    const val MESSAGE_BOX = 3
    /** Message argument kind for a raw string (client f/RO0 kind 5). */
    const val RAW_STRING: Byte = 5
    /** The stand-in the client itself summons per region (f/ZB1.mf1's table), by region id. */
    val STAND_INS = mapOf(0 to 71, 1 to 357, 2 to 549, 3 to 415, 4 to 216)
  }
}
