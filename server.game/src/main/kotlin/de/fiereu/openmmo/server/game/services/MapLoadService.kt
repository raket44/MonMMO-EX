package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.CharacterInfo
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.EntityStatus
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.LoadEntityPacket
import de.fiereu.openmmo.net.game.packets.MapData
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.mapCacheKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapLoadService
@Inject
constructor(
    private val mapManager: MapManager,
) {

  fun createLoadEntity(
      info: CharacterInfo,
      facing: Direction = Direction.DOWN,
      z: Int = 0,
      party: List<Pokemon> = emptyList(),
      skins: Map<SkinSlot, Skin> = emptyMap(),
      railLine: Int = -1,
      transportation: Int = 0,
      mountType: Int = -1,
      mountId: Int = -1,
      /** Uid of the party monster chosen as follower ("Set X as Follower"); null = the lead. */
      followerId: Long? = null,
  ): LoadEntityPacket {
    val follower = party.firstOrNull { it.id == followerId } ?: party.firstOrNull()
    return LoadEntityPacket(
        entityId = info.id,
        skin = SkinSet(info.skinRegionSelectionIndex, skins),
        name = info.name,
        // Unsigned on purpose: NDS banks run past 127 (Cold Storage is 192). A signed widening
        // makes the U8 codec throw during Netty encode - AFTER the "Sending LoadEntity" log and
        // into a write future nobody reads - so the client never receives its player and every
        // bank>127 interior froze in the doorway with no error anywhere.
        regionId = info.positionRegionId.toInt() and 0xFF,
        bankId = info.positionBankId.toInt() and 0xFF,
        mapId = info.positionMapId.toInt() and 0xFF,
        x = info.positionX.toInt(),
        y = info.positionY.toInt(),
        z = z,
        facing = facing,
        status = EntityStatus.NONE,
        // Imported species follow from the mod's sheets; the mod ships the atlasdata.txt grid
        // descriptor the client's follower renderer needs (without it the mod atlas sliced to
        // zero and crashed the client at spawn, which is what the old species cap guarded).
        hasFollower = follower != null,
        followerDexId = clientSpeciesId(follower?.dexId ?: 0).toShort(),
        followerFlags =
            de.fiereu.openmmo.net.game.packets.EntityFollowerPacket.followerFlags(
                    shiny = follower?.isShiny ?: false)
                .toInt() and 0xFF,
        railLine = railLine,
        transportation = transportation,
        mountType = mountType,
        mountId = mountId,
    )
  }

  /**
   * Forget what the client has cached. Call this alongside a LoadMap that carries deleteCache,
   * since the client throws its own cache away when it sees that flag.
   */
  fun resetClientCache(ctx: SessionContext, map: MapDef) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    state.loadedMaps.clear()
    state.loadedMaps.add(mapCacheKey(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt()))
  }

  fun preloadConnectedMaps(
      ctx: SessionContext,
      map: MapDef,
      depth: Int = 2,
      reloadPlayer: Boolean = false,
  ) {
    val loaded = ctx.attributes[PLAYER_STATE]?.loadedMaps ?: mutableSetOf()
    val regionId = map.regionId.toInt()
    loaded.add(mapCacheKey(regionId, map.bankId.toInt(), map.mapId.toInt()))
    fun preload(connections: List<MapData.GbaConnection>, remaining: Int) {
      if (remaining <= 0) return
      for (conn in connections) {
        val key = mapCacheKey(regionId, conn.targetBank, conn.targetMap)
        if (!loaded.add(key)) continue
        // Connections stay inside one region.
        val connected = mapManager.getMap(regionId, conn.targetBank, conn.targetMap)
        if (connected != null) {
          ctx.send(
              mapManager.createLoadMapPacket(
                  connected,
                  reloadPlayer = reloadPlayer,
                  deleteCache = false,
              ))
          preload(connected.connections, remaining - 1)
        }
      }
    }
    preload(map.connections, depth)
  }
}

/** The last species with a ROM overworld sprite the client can walk as a follower. */
const val LAST_ROM_FOLLOWER_SPECIES = 649
