package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.net.game.packets.FlyRequestPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Fly. The client picks a spot on its town map and sends c2s 0x10 with the spot's id
 * ([FlyRequestPacket]); the id numbers the region's ROM heal locations 0-based, which is also
 * where the GBA lands the player (the town's Pokemon Center door). Resource
 * monmmo/fly-destinations.csv holds that table for Kanto and Hoenn, generated from the decomps'
 * heal_locations.json; Sinnoh rows follow Platinum's sSpawnLocations (spawn_locations.c) in the town
 * map's fly-location order, Johto rows the pokegear flypoint index (gMapFlypointParams) with the
 * landing left to the map's entry tile. The engine's field-move gate applies (badge plus a party member with Fly
 * or the Thunder Fly ocarina); the client already greys out towns not yet visited.
 */
@Singleton
class FlyService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val mapManager: MapManager,
    private val warpService: WarpService,
    private val battleService: BattleService,
    private val banners: FieldMoveBanners,
    private val ndsWarps: NdsWarps,
) {
  private data class Destination(val mapName: String, val x: Int, val y: Int, val healLocation: String)

  private val destinations: Map<Pair<Int, Int>, Destination> =
      FlyService::class.java.getResourceAsStream("/monmmo/fly-destinations.csv")?.bufferedReader()?.readLines()
          ?.drop(1)?.filter { it.isNotBlank() }?.associate { line ->
            val f = line.split(',')
            (f[0].toInt() to f[1].toInt()) to Destination(f[2], f[3].toInt(), f[4].toInt(), f[5])
          } ?: emptyMap()

  /** The floor tile's elevation in the client's scale (GBA elevation - 1); 0, "any", when unknown. */
  private fun landingElevation(map: de.fiereu.openmmo.maps.MapDef, x: Int, y: Int): Int =
      map.tileAt(x, y)?.let { ((it.collision.toInt() and 0xFF) shr 2) - 1 }?.takeIf { it >= 0 } ?: 0

  suspend fun onFly(event: PacketEvent<FlyRequestPacket>) {
    val session = event.session
    val state = session.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val stored = characterStore.getCharacter(charId) ?: return
    val id = event.packet.destination.toInt() and 0xFF
    log.info { "[fly] char=$charId region=${state.regionId} destination=$id" }
    if (state.blocksNewScript || battleService.inBattle(charId)) return
    if (!FieldMoves.canUse(stored, state.regionId, FieldMoves.FLY)) {
      session.send(notice("Fly needs the badge and a party member that knows it."))
      return
    }
    val dest = destinations[state.regionId to id]
    if (dest == null) {
      session.send(notice("That place cannot be flown to yet."))
      log.info { "[fly] no destination for region=${state.regionId} id=$id" }
      return
    }
    // DS regions: the row's map is "bank:map" (ROM header = map * 256 + bank) and the landing is
    // the decomp's fly spawn in world tiles, or -1 to land on the map's own entry tile. The client
    // renders those maps itself, so the raw-warp path applies (WarpService.executeRawWarp).
    if (state.regionId >= 2) {
      val (bank, mapId) = dest.mapName.split(':').map { it.toInt() }
      val landing =
          if (dest.x >= 0) dest.x to dest.y
          else ndsWarps.spawnOf(state.regionId, bank, mapId)
              ?: run {
                log.warn { "[fly] no landing for ${state.regionId}:$bank:$mapId (${dest.healLocation})" }
                session.send(notice("That place has no landing spot yet."))
                return
              }
      if (banners.send(session, stored, state.regionId, FieldMoves.FLY)) kotlinx.coroutines.delay(FieldMoveBanners.FLY_HOLD_MILLIS)
      warpService.executeRawWarp(session, charId, state.regionId, bank, mapId, landing.first, landing.second)
      return
    }
    val map =
        mapManager.getMapsByName(dest.mapName).firstOrNull { it.regionId.toInt() == state.regionId }
            ?: run {
              log.warn { "[fly] map ${dest.mapName} missing for ${dest.healLocation}" }
              return
            }
    // The banner ("{mon} used Fly!", the pose) plays before the player leaves, like every HM.
    if (banners.send(session, stored, state.regionId, FieldMoves.FLY)) kotlinx.coroutines.delay(FieldMoveBanners.FLY_HOLD_MILLIS)
    warpService.executeWarp(
        session,
        charId,
        WarpTile(
            x = stored.info.positionX.toInt(),
            y = stored.info.positionY.toInt(),
            targetRegionId = map.regionId,
            targetBankId = map.bankId,
            targetMapId = map.mapId,
            targetX = dest.x,
            targetY = dest.y,
            // The landing tile's own floor elevation, client scale (GBA - 1), as npc spawns do.
            // A fixed 3 put the player a level above Cerulean's ground, and the client refuses
            // every step between mismatched elevations: stuck until the door, whose warp tile
            // is elevation "any" (2026-09-08). The GM teleport lands at 0 and always walked.
            targetElevation = landingElevation(map, dest.x, dest.y),
            exitFacing = de.fiereu.openmmo.common.enums.Direction.DOWN,
        ))
  }
}
