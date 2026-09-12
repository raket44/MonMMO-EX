package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.net.game.packets.EntityLeavePacket
import de.fiereu.openmmo.net.game.packets.LoadEntityPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.world.interest.InterestManager
import de.fiereu.openmmo.server.game.world.interest.InterestPolicy
import de.fiereu.openmmo.server.game.world.interest.MapInterestKey
import javax.inject.Inject
import javax.inject.Singleton
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Overworld presence built on the generic [InterestManager]. A player's map is one interest group.
 * This layer adds the entity spawn/despawn semantics and the area-of-interest filter that only make
 * sense for spatial presence.
 */
@Singleton
class PresenceService
@Inject
constructor(
    private val interestManager: InterestManager,
    private val policy: InterestPolicy,
    private val mapLoadService: MapLoadService,
    private val characterStore: CharacterStore,
    private val mapManager: de.fiereu.openmmo.maps.MapManager? = null,
) {

  /** Spawn the player into its map group and exchange entity snapshots with its observers. */
  fun enter(ctx: SessionContext) {
    val key = mapKeyFor(ctx) ?: return
    spawnInto(ctx, key)
  }

  /** Despawn the player from its map observers and drop it from the map group. */
  fun leave(ctx: SessionContext) {
    val key = currentMapKey(ctx) ?: mapKeyFor(ctx) ?: return
    interestManager.leave(ctx, key)
    val entityId = entityIdOf(ctx) ?: return
    for (other in observers(ctx, key)) other.send(EntityLeavePacket(entityId))
  }

  /**
   * Send a packet to the player AND everyone observing them. Every live change to the player's
   * entity that observers render - skins (0x90), the mount byte (0x28), addon animations (0x6C) -
   * goes this way; sending it to the player alone left observers on the old look and on walking
   * speed for a rider, which the server then corrected by teleporting the rider (2026-09-12).
   */
  fun announce(ctx: SessionContext, packet: Any) {
    ctx.send(packet)
    broadcastToObservers(ctx, packet)
  }

  /** Send a packet to everyone observing the player on its current map (excludes the player). */
  fun broadcastToObservers(ctx: SessionContext, packet: Any) {
    val key = currentMapKey(ctx) ?: mapKeyFor(ctx) ?: return
    for (other in observers(ctx, key)) other.send(packet)
  }

  /**
   * Recompute the player's map group from its (already updated) state: despawn it from the map it
   * left, spawn it into the map it joined. Call after changing a player's map.
   */
  fun refresh(ctx: SessionContext) {
    val newKey = mapKeyFor(ctx)
    val oldKey = currentMapKey(ctx)
    if (oldKey == newKey) return
    // Observers span connected maps, so a seam crossing keeps everyone still in range: only the
    // sessions that fall out of range get a leave, only the newly in range get a spawn.
    val oldObservers = if (oldKey != null) observers(ctx, oldKey).toSet() else emptySet()
    if (oldKey != null) interestManager.leave(ctx, oldKey)
    val newObservers = if (newKey != null) observers(ctx, newKey).toSet() else emptySet()
    val entityId = entityIdOf(ctx)
    for (other in oldObservers - newObservers) {
      if (entityId != null) other.send(EntityLeavePacket(entityId))
      entityIdOf(other)?.let { ctx.send(EntityLeavePacket(it)) }
    }
    val self = loadEntityFor(ctx)
    for (other in newObservers - oldObservers) {
      if (self != null) other.send(self)
      loadEntityFor(other)?.let { ctx.send(it) }
    }
    if (newKey != null) interestManager.join(ctx, newKey)
  }

  private fun spawnInto(ctx: SessionContext, key: MapInterestKey) {
    val observers = observers(ctx, key)
    val self = loadEntityFor(ctx)
    for (other in observers) {
      if (self != null) other.send(self)
      loadEntityFor(other)?.let { ctx.send(it) }
    }
    interestManager.join(ctx, key)
  }

  /**
   * Everyone on this map or a map connected to it: outdoor GBA maps are stitched at their seams
   * and the client draws the neighbour, so a player one step across the seam must stay visible.
   */
  private fun observers(ctx: SessionContext, key: MapInterestKey): List<SessionContext> =
      policy.filter(ctx, neighbourhood(key).flatMap { interestManager.members(it) }.toSet())

  private fun neighbourhood(key: MapInterestKey): List<MapInterestKey> {
    val map = mapManager?.getMap(key.regionId, key.bankId, key.mapId) ?: return listOf(key)
    return listOf(key) + map.connections.map { MapInterestKey(key.regionId, it.targetBank, it.targetMap) }
  }

  private fun mapKeyFor(ctx: SessionContext): MapInterestKey? {
    val state = ctx.attributes[PLAYER_STATE] ?: return null
    return MapInterestKey(state.regionId, state.bankId, state.mapId)
  }

  private fun currentMapKey(ctx: SessionContext): MapInterestKey? =
      interestManager.keysOf(ctx).filterIsInstance<MapInterestKey>().firstOrNull()

  private fun entityIdOf(ctx: SessionContext): Long? = ctx.attributes[PLAYER_STATE]?.characterId

  private fun loadEntityFor(ctx: SessionContext): LoadEntityPacket? {
    val state = ctx.attributes[PLAYER_STATE] ?: return null
    val charId = state.characterId ?: return null
    val stored = characterStore.getCharacter(charId) ?: return null
    return mapLoadService.createLoadEntity(
        stored.info,
        state.facingDirection,
        party = stored.pokemon,
        skins = stored.skins,
        transportation = state.mountByte(),
        followerId = state.followerMonId)
  }

  /**
   * The party window's follower choice (c2s 0x11 = client f/ET): type 1 names a monster by uid
   * ("Set X as Follower"), type 0 names a party slot ("Set Slot #N as Follower"), anything else
   * dismisses the follower. The client changes nothing on its own; s2c 0x2B (client f/lPT6 ->
   * ti.gw0) sets the entity's follower live for the player and everyone watching. Imported species
   * have no ROM overworld descriptor yet, so they are refused rather than crash the client.
   */
  fun onPartyMemberSelect(
      event: PacketEvent<de.fiereu.openmmo.net.game.packets.PartyMemberSelectPacket>
  ) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val stored = characterStore.getCharacter(charId) ?: return
    val packet = event.packet
    val chosen =
        when (packet.selectionType.toInt()) {
          1 -> stored.pokemon.firstOrNull { it.id == packet.entityId }
          0 -> stored.pokemon.getOrNull(packet.entityId.toInt())
          else -> null
        }
    log.info { "[Follower] char=$charId $packet -> ${chosen?.let { "${it.dexId}#${it.id}" } ?: "none"}" }
    state.followerMonId = chosen?.id
    sendFollower(ctx, charId, chosen)
  }

  /**
   * Re-derive the follower after the party changed (a drag into the PC, a new lead): the chosen
   * monster if it is still in the party, else the lead, matching what the spawn packet resolves.
   * The client keeps the old sprite walking until told, so this is sent whenever the party moves.
   */
  fun refreshFollower(ctx: SessionContext) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val party = characterStore.getCharacter(charId)?.pokemon ?: return
    val chosen = party.firstOrNull { it.id == state.followerMonId } ?: party.firstOrNull()
    if (chosen?.id != state.followerMonId) state.followerMonId = chosen?.id
    sendFollower(ctx, charId, chosen)
  }

  private fun sendFollower(
      ctx: SessionContext,
      charId: Long,
      chosen: de.fiereu.openmmo.common.Pokemon?,
  ) {
    val species = chosen?.let { clientSpeciesId(it.dexId) } ?: 0
    val update =
        de.fiereu.openmmo.net.game.packets.EntityFollowerPacket(
            entityId = charId,
            species = species.toShort(),
            flags =
                de.fiereu.openmmo.net.game.packets.EntityFollowerPacket.followerFlags(
                    shiny = chosen?.isShiny ?: false),
        )
    ctx.send(update)
    broadcastToObservers(ctx, update)
  }
}
