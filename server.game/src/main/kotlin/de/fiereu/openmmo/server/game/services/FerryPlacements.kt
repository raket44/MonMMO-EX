package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MovementType
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.NpcLook
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the region-link ferry captain stands: one sailor per hosted region's harbour town. He is
 * not in any ROM, so he is spawned next to the map's own npcs under a local id above theirs, and
 * the interaction service routes his talks to [FerryTravel].
 */
@Singleton
class FerryPlacements @Inject constructor(private val mapManager: MapManager) {

  data class Placement(
      val regionId: Int,
      val mapName: String,
      val x: Int,
      val y: Int,
      /** The region's own sailor sprite (FireRed 62, Emerald 49); unused while [look] dresses him. */
      val graphicsId: Int,
      val look: NpcLook = CAPTAIN,
  ) {
    fun npc(): NpcDef =
        NpcDef(
            entityIdx = LOCAL_ID,
            graphicsId = graphicsId,
            x = x,
            y = y,
            elevation = 0,
            movementType = MovementType.FACE_DOWN,
            movementRangeX = 0,
            movementRangeY = 0,
            trainerType = 0,
            facing = Direction.DOWN,
            script = "ferry",
        )
  }

  private val placements =
      listOf(
          // Vermilion City, on the pier east of the S.S. Anne dock.
          Placement(0, "VermilionCity", 33, 33, 62),
          // Slateport City, on the plaza in front of the harbour.
          Placement(1, "SlateportCity", 29, 14, 49),
      )

  private val byMap: Map<Triple<Int, Int, Int>, Placement> by lazy {
    placements
        .mapNotNull { p ->
          val map = mapManager.getMapsByName(p.mapName).firstOrNull { it.regionId.toInt() == p.regionId } ?: return@mapNotNull null
          Triple(p.regionId, map.bankId.toInt(), map.mapId.toInt()) to p
        }
        .toMap()
  }

  fun at(regionId: Int, bankId: Int, mapId: Int): Placement? = byMap[Triple(regionId, bankId, mapId)]

  /** The harbour map of [regionId], where a returning traveller lands. */
  fun harbour(regionId: Int): Pair<MapDef, Placement>? {
    val p = placements.firstOrNull { it.regionId == regionId } ?: return null
    val map = mapManager.getMapsByName(p.mapName).firstOrNull { it.regionId.toInt() == regionId } ?: return null
    return map to p
  }

  /** A walkable tile next to the captain, where a traveller steps off the ferry. */
  fun landing(map: MapDef, p: Placement): Pair<Int, Int> {
    val candidates = listOf(p.x to p.y + 1, p.x - 1 to p.y, p.x + 1 to p.y, p.x to p.y - 1)
    return candidates.firstOrNull { (x, y) -> map.tileAt(x, y)?.blocksMovement() == false } ?: (p.x to p.y + 1)
  }

  companion object {
    /** Local npc id of the captain; ROM maps stay well under this. */
    const val LOCAL_ID = 250

    /** Skin colors are f/An indexes, named by strings 32000 + index: 5 Black, 26 Dark Yellow. */
    private const val BLACK: UByte = 5u
    private const val GOLD: UByte = 26u

    /** The captain as a trainer model: the client's own Pirate Hat and Pirate Outfit, black with gold boots, over a bearded face. */
    val CAPTAIN: NpcLook =
        NpcLook(
            gender = 0,
            skins =
                SkinSet(
                    skins =
                        listOf(
                                Skin(SkinSlot.FOREHEAD, 0u, 0u),
                                Skin(SkinSlot.HAT, 158u, BLACK),
                                Skin(SkinSlot.HAIR, 0u, 0u),
                                Skin(SkinSlot.EYES, 1u, 0u),
                                Skin(SkinSlot.FACIAL_HAIR, 3u, 0u),
                                Skin(SkinSlot.TOP, 119u, BLACK),
                                Skin(SkinSlot.LEGGINGS, 0u, BLACK),
                                Skin(SkinSlot.FOOTWEAR, 0u, GOLD),
                            )
                            .associateBy { it.slot }))
  }
}
