package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MovementType
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.CrystalOnixRaid
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the Crystal Onix raid boss stands: Rock Tunnel 1F. It is not in the ROM, so it is spawned beside the map's own npcs
 * under a local id above theirs, and the interaction service routes its talks to
 * [CrystalOnixRaidService].
 */
@Singleton
class CrystalOnixRaidPlacement @Inject constructor(private val mapManager: MapManager) {

  private val key: Triple<Int, Int, Int>? by lazy {
    mapManager.getMapsByName(MAP_NAME).firstOrNull { it.regionId.toInt() == REGION }?.let {
      Triple(REGION, it.bankId.toInt(), it.mapId.toInt())
    }
  }

  fun isHere(regionId: Int, bankId: Int, mapId: Int): Boolean = key == Triple(regionId, bankId, mapId)

  fun npc(): NpcDef =
      NpcDef(
          entityIdx = LOCAL_ID,
          graphicsId = SPRITE_ID,
          x = X,
          y = Y,
          elevation = 0,
          // Walking in place keeps the entity animating (project owner, 2026-09-14).
          movementType = MovementType.WALK_IN_PLACE_DOWN,
          movementRangeX = 0,
          movementRangeY = 0,
          trainerType = 0,
          facing = Direction.DOWN,
          script = "crystal_onix_raid",
      )

  companion object {
    const val REGION = 0
    /** Rock Tunnel 1F, the open floor at (32,19) (project owner, 2026-09-14; was Cerulean Cave B1F). */
    const val MAP_NAME = "RockTunnel_1F"
    const val X = 32
    const val Y = 19

    /** Local npc id of the boss; ROM maps stay well under it, and the ferry captain is 250. */
    const val LOCAL_ID = 251

    /**
     * The npc is drawn by the client's follower renderer, like a player's walking monster (project
     * owner, 2026-09-14). The client's own npc follower mapping (f/dw2.Kk0 via f/o80.T02) only
     * reaches the retail species, so the MonMMO client patch in dw2.Kk0 adds: graphics id
     * [FOLLOWER_GRAPHICS_BASE] + N in any sprite set but 4 and 10 draws species N's follower
     * sprite. Retail and ROM npcs never use ids that high. Kanto's set is the one sent.
     */
    const val SPRITE_REGION = 0
    const val FOLLOWER_GRAPHICS_BASE = 20000

    /** The client id of the Crystal Onix form, whose follower sheet the player's own follower uses. */
    val FOLLOWER_SPECIES: Int by lazy {
      checkNotNull(ExpansionSpeciesRegistry().get(CrystalOnixRaid.SPECIES_SYMBOL)?.clientWireId) {
        "${CrystalOnixRaid.SPECIES_SYMBOL} has no client id"
      }
    }

    val SPRITE_ID: Int
      get() = FOLLOWER_GRAPHICS_BASE + FOLLOWER_SPECIES
  }
}
