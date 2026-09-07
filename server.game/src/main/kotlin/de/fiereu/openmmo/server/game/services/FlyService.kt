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
 * heal_locations.json. The engine's field-move gate applies (badge plus a party member with Fly
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
) {
  private data class Destination(val mapName: String, val x: Int, val y: Int, val healLocation: String)

  private val destinations: Map<Pair<Int, Int>, Destination> =
      FlyService::class.java.getResourceAsStream("/monmmo/fly-destinations.csv")?.bufferedReader()?.readLines()
          ?.drop(1)?.filter { it.isNotBlank() }?.associate { line ->
            val f = line.split(',')
            (f[0].toInt() to f[1].toInt()) to Destination(f[2], f[3].toInt(), f[4].toInt(), f[5])
          } ?: emptyMap()

  fun onFly(event: PacketEvent<FlyRequestPacket>) {
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
    val map =
        mapManager.getMapsByName(dest.mapName).firstOrNull { it.regionId.toInt() == state.regionId }
            ?: run {
              log.warn { "[fly] map ${dest.mapName} missing for ${dest.healLocation}" }
              return
            }
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
            targetElevation = 3,
            exitFacing = de.fiereu.openmmo.common.enums.Direction.DOWN,
        ))
  }
}
