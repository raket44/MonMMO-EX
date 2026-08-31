package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.MovementType
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.net.game.packets.EntityLeavePacket
import de.fiereu.openmmo.net.game.packets.NpcSpawnPacket
import de.fiereu.openmmo.net.game.packets.NpcUpdatePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class NpcService
@Inject
constructor(
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
) {

  private val npcEntityIdCounter = AtomicLong(0x1A69000000000000L)
  private val npcEntityIds = mutableMapOf<String, Long>()

  fun getNpcEntityId(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): Long? {
    return npcEntityIds[key(regionId, bankId, mapId, entityIdx)]
  }

  fun spawnNpcsForMap(ctx: SessionContext, bankId: Int, mapId: Int, regionId: Int) {
    val map = mapManager.getMap(regionId, bankId, mapId) ?: return
    val stored = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)
    val storyFlags = stored?.storyFlags.orEmpty()
    val storyVars = stored?.storyVars.orEmpty()

    for (npc in map.npcs) {
      // Decoration slots are not normal NPCs.
      if (npc.hideFlag.substringAfter('/').startsWith(DECORATION_FLAG_PREFIX)) continue

      // Only set hide flags suppress NPCs.
      if (shouldHideNpc(bankId, mapId, npc, storyFlags)) continue
      val resolved =
          resolveDynamicGraphics(
              ctx,
              applyXyOverride(
                  regionId,
                  bankId,
                  mapId,
                  applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
                  storyVars),
              regionId,
              storyVars)
      // A still-unresolved VAR sprite would be an invisible but solid entity on the client; the
      // GBA does not spawn these either. The post-script refresh sends it once its var is set.
      if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) continue
      ctx.send(
          buildSpawnPacket(
              resolved,
              entityIdFor(regionId, bankId, mapId, npc.entityIdx),
              regionId,
              bankId,
              mapId,
          ))
    }
  }

  /**
   * Re-sends the npcs whose sprite or position depends on story state, after a map's ON_TRANSITION
   * script wrote the vars they read. The decomp runs that script before objects load; here the
   * script runs after the spawn, so these npcs are corrected in place.
   */
  fun refreshDynamicNpcs(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int) {
    val map = mapManager.getMap(regionId, bankId, mapId) ?: return
    val stored = ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)
    val storyFlags = stored?.storyFlags.orEmpty()
    val storyVars = stored?.storyVars.orEmpty()
    for (npc in map.npcs) {
      val dynamicGfx = npc.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3
      val hasOverride = xyOverrideKey(regionId, bankId, mapId, npc.entityIdx) in storyVars
      if (!dynamicGfx && !hasOverride) continue
      if (npc.hideFlag.substringAfter('/').startsWith(DECORATION_FLAG_PREFIX)) continue
      if (shouldHideNpc(bankId, mapId, npc, storyFlags)) continue
      val resolved =
          resolveDynamicGraphics(
              ctx,
              applyXyOverride(
                  regionId,
                  bankId,
                  mapId,
                  applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
                  storyVars),
              regionId,
              storyVars)
      if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) continue
      // Replace, not stack: despawn whatever the client holds under this entity id first.
      despawnNpc(ctx, regionId, bankId, mapId, npc.entityIdx)
      ctx.send(
          buildSpawnPacket(
              resolved,
              entityIdFor(regionId, bankId, mapId, npc.entityIdx),
              regionId,
              bankId,
              mapId,
          ))
    }
  }

  /**
   * The npc as this character actually sees it spawned - the template adjusted by story placement
   * and any setobjectxyperm override, the same pipeline the spawn uses. Position math (facing,
   * adjacency) must use this, never the raw MapDef template: a story-moved npc read from the
   * template fails the adjacency test and faces the wrong way.
   */
  fun effectiveNpc(
      regionId: Int,
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
      storyVars: Map<String, Int>,
  ): NpcDef =
      applyXyOverride(
          regionId,
          bankId,
          mapId,
          applyStoryPlacement(bankId, mapId, npc, storyFlags, storyVars),
          storyVars)

  private fun sessionStoryVars(ctx: SessionContext): Map<String, Int> =
      ctx.attributes[PLAYER_STATE]
          ?.characterId
          ?.let(characterStore::getCharacter)
          ?.storyVars
          .orEmpty()

  /** The story-var key setobjectxyperm writes an npc's overridden tile into (x shl 12 or y). */
  fun xyOverrideKey(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): String {
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    return "$namespace/objxy/$bankId:$mapId:$entityIdx"
  }

  private fun applyXyOverride(
      regionId: Int,
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyVars: Map<String, Int>,
  ): NpcDef {
    val packed = storyVars[xyOverrideKey(regionId, bankId, mapId, npc.entityIdx)] ?: return npc
    return npc.copy(x = packed shr 12, y = packed and 0xFFF)
  }

  /** Allocate (or return) the stable entity id for a map npc by its decomp local id. */
  fun entityIdFor(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int): Long =
      npcEntityIds.getOrPut(key(regionId, bankId, mapId, entityIdx)) {
        npcEntityIdCounter.incrementAndGet()
      }

  /** Spawn a single npc (including a normally hidden one) for one player, for cutscenes. */
  fun spawnNpc(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, localId: Int) {
    val npc = findNpc(regionId, bankId, mapId, localId) ?: return
    val storyVars = sessionStoryVars(ctx)
    val resolved =
        resolveDynamicGraphics(
            ctx, applyXyOverride(regionId, bankId, mapId, npc, storyVars), regionId, storyVars)
    if (resolved.graphicsId in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) return
    ctx.send(
        buildSpawnPacket(
            resolved,
            entityIdFor(regionId, bankId, mapId, localId),
            regionId,
            bankId,
            mapId,
        ))
  }

  /** Spawns an NPC at a cutscene position. */
  fun spawnNpcAt(
      ctx: SessionContext,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      localId: Int,
      x: Int,
      y: Int,
  ) {
    val npc = findNpc(regionId, bankId, mapId, localId) ?: return
    ctx.send(
        buildSpawnPacket(
            resolveDynamicGraphics(ctx, npc.copy(x = x, y = y), regionId, sessionStoryVars(ctx)),
            entityIdFor(regionId, bankId, mapId, localId),
            regionId,
            bankId,
            mapId,
        ))
  }

  /** Repositions an existing NPC. */
  fun repositionNpc(
      ctx: SessionContext,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      localId: Int,
      x: Int,
      y: Int,
  ) {
    val npc = findNpc(regionId, bankId, mapId, localId) ?: return
    ctx.send(
        NpcUpdatePacket(
            entityId = entityIdFor(regionId, bankId, mapId, localId),
            regionId = regionId,
            bankId = bankId,
            mapId = mapId,
            x = x,
            y = y,
            // Captures use 0xF6 followed by the direction for this update packet.
            facing = 0xF6,
            unk = npc.facing.ordinal,
        ))
  }

  /** Removes a cutscene NPC. */
  fun despawnNpc(ctx: SessionContext, regionId: Int, bankId: Int, mapId: Int, localId: Int) {
    val entityId = getNpcEntityId(regionId, bankId, mapId, localId) ?: return
    ctx.send(EntityLeavePacket(entityId))
  }

  private fun findNpc(regionId: Int, bankId: Int, mapId: Int, localId: Int): NpcDef? {
    val npc =
        mapManager.getMap(regionId, bankId, mapId)?.npcs?.firstOrNull { it.entityIdx == localId }
    if (npc == null) log.warn { "npc $localId not found on $regionId:$bankId:$mapId" }
    return npc
  }

  private fun key(regionId: Int, bankId: Int, mapId: Int, entityIdx: Int) =
      "$regionId:$bankId:$mapId:$entityIdx"

  private fun buildSpawnPacket(
      npc: NpcDef,
      entityId: Long,
      regionId: Int,
      bankId: Int,
      mapId: Int,
  ): NpcSpawnPacket {
    val region = requireNotNull(Region.byId(regionId)) { "Unknown region id $regionId" }
    val movementId = npc.movementType.forRegion(region).id
    val unk3 = ((movementId and 0xFF) shl 8) or 0x02
    val unk4 =
        if (movementId in 1..6 || (movementId in 25..52)) {
          ((npc.movementRangeX and 0xFF) shl 8) or (npc.movementRangeY and 0xFF)
        } else {
          0
        }
    return NpcSpawnPacket(
        entityId = entityId,
        spriteRegionId = regionId,
        graphicsId = npc.graphicsId,
        unk3 = unk3,
        unk4 = unk4,
        regionId = regionId,
        bankId = bankId,
        mapId = mapId,
        x = npc.x,
        y = npc.y,
        facing = npc.facing.ordinal,
        unk5 = 2,
        unk6 = 8,
    )
  }

  private fun resolveDynamicGraphics(
      ctx: SessionContext,
      npc: NpcDef,
      regionId: Int,
      storyVars: Map<String, Int>,
  ): NpcDef {
    if (npc.graphicsId !in DYNAMIC_GFX_VAR_0..DYNAMIC_GFX_VAR_3) return npc

    // The map's ON_TRANSITION script chose the sprite (setvar VAR_OBJ_GFX_ID_N, gfx) - the
    // Viridian old man and friends. The rival heuristic below covers npcs whose var no script
    // has written yet.
    val namespace = Region.byId(regionId)?.name?.lowercase() ?: regionId.toString()
    val slot = npc.graphicsId - DYNAMIC_GFX_VAR_0
    storyVars["$namespace/VAR_OBJ_GFX_ID_$slot"]?.let {
      return npc.copy(graphicsId = it)
    }

    if (!(npc.script.contains("Rival", ignoreCase = true) ||
        npc.hideFlag.contains("RIVAL", ignoreCase = true))) {
      return npc
    }
    val playerGender =
        ctx.attributes[PLAYER_STATE]?.characterId?.let(characterStore::getCharacter)?.info?.rivalSex
    // The rival is the opposite gender from the player.
    val graphicsId = if (playerGender == FEMALE) RIVAL_BRENDAN_NORMAL else RIVAL_MAY_NORMAL
    return npc.copy(graphicsId = graphicsId)
  }

  private fun applyStoryPlacement(
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
      storyVars: Map<String, Int>,
  ): NpcDef {
    if (bankId != LITTLEROOT_BANK || mapId != LITTLEROOT_MAP || npc.entityIdx != LITTLEROOT_TWIN) {
      return npc
    }
    if (RESCUED_BIRCH_FLAG in storyFlags) return npc
    return if (storyVars[LITTLEROOT_STATE_VAR].orZero() == 0) {
      npc.copy(x = 7, y = 2, facing = Direction.DOWN, movementType = MovementType.FACE_DOWN)
    } else {
      npc.copy(x = 10, y = 1, facing = Direction.UP, movementType = MovementType.FACE_UP)
    }
  }

  private fun Int?.orZero(): Int = this ?: 0

  private fun shouldHideNpc(
      bankId: Int,
      mapId: Int,
      npc: NpcDef,
      storyFlags: Set<String>,
  ): Boolean {
    if (npc.hideFlag in storyFlags) return true
    // Completed rescues must not restore Zigzagoon.
    return bankId == ROUTE_101_BANK &&
        mapId == ROUTE_101_MAP &&
        npc.entityIdx == ROUTE_101_ZIGZAGOON &&
        (RESCUED_BIRCH_FLAG in storyFlags || ROUTE_101_RESCUE_HIDDEN_FLAG in storyFlags)
  }

  private companion object {
    const val DECORATION_FLAG_PREFIX = "FLAG_DECORATION_"
    const val DYNAMIC_GFX_VAR_0 = 240
    const val DYNAMIC_GFX_VAR_3 = 243
    const val RIVAL_BRENDAN_NORMAL = 100
    const val RIVAL_MAY_NORMAL = 105
    const val FEMALE: Byte = 1
    const val LITTLEROOT_BANK = 50
    const val LITTLEROOT_MAP = 9
    const val LITTLEROOT_TWIN = 0
    const val RESCUED_BIRCH_FLAG = "hoenn/FLAG_RESCUED_BIRCH"
    const val LITTLEROOT_STATE_VAR = "hoenn/VAR_LITTLEROOT_TOWN_STATE"
    const val ROUTE_101_BANK = 50
    const val ROUTE_101_MAP = 16
    const val ROUTE_101_ZIGZAGOON = 3
    const val ROUTE_101_RESCUE_HIDDEN_FLAG = "hoenn/FLAG_HIDE_ROUTE_101_BIRCH_ZIGZAGOON_BATTLE"
  }
}
