package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.MapLayoutSwitchPacket
import de.fiereu.openmmo.server.game.session.PlayerState
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Story-dependent map variants: when a flag is set, the client swaps the map's block grid for an
 * alternate footer our mod ships (s2c 0x2D, [MapLayoutSwitchPacket]). The ROM does these edits
 * with setmetatile at load time; the per-tile packet drew wrong in our client, while a whole-grid
 * swap goes through the same code path as the initial map load. The server's own collision for
 * the changed tiles still comes from the ROM script's setmetatile calls (PlayerState.tileOverrides),
 * which run on entry (ON_LOAD) and when the flag gets set, so both sides agree.
 *
 * Footer ids must exist in the client: launcher ClientMapFooters lists the files the mod carries.
 */
@Singleton
class LayoutVariants @Inject constructor() {
  data class Variant(val regionId: Int, val bankId: Int, val mapId: Int, val flag: String, val footerId: Int)

  private val variants =
      listOf<Variant>(
          // Vermilion Gym: both switches found -> the electric barrier is down
          // (VermilionCity_Gym_EventScript_SetBeamsOff baked into footer 0-450).
          // Vermilion Gym footer 450 is off while the ROM's own setmetatile path (s2c 0x22) is tested.
          // Variant(0, 9, 6, "kanto/FLAG_FOUND_BOTH_VERMILION_GYM_SWITCHES", 450),
      )

  /** On arrival: every variant of this map whose flag is already set. Send after LoadMap. */
  fun onMapEnter(session: SessionContext, state: PlayerState, isFlagSet: (String) -> Boolean) {
    variants
        .filter { it.matches(state) && isFlagSet(it.flag) }
        .forEach { send(session, state, it) }
  }

  /** A flag just got set while the player stands on the map: swap live. */
  fun onFlagSet(session: SessionContext, state: PlayerState, flag: String) {
    variants.filter { it.matches(state) && it.flag == flag }.forEach { send(session, state, it) }
  }

  private fun Variant.matches(state: PlayerState) =
      regionId == state.regionId && bankId == state.bankId && mapId == state.mapId

  private fun send(session: SessionContext, state: PlayerState, variant: Variant) {
    log.info { "[layout] ${state.regionId}:${state.bankId}:${state.mapId} -> footer ${variant.footerId} (${variant.flag})" }
    session.send(
        MapLayoutSwitchPacket(
            state.regionId.toByte(), state.bankId.toByte(), state.mapId.toByte(), variant.footerId.toShort()))
  }
}
