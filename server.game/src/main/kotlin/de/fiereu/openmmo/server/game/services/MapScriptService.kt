package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.session.DeferredTrigger
import de.fiereu.openmmo.server.game.session.PlayerState
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a map's scripts when a player enters it. The decomp ON_TRANSITION script fires on every
 * entry however the player got there (login, a warp, or walking across a map connection), followed
 * by the first ON_FRAME entry whose story var matches. The conditional ON_WARP table is a later
 * addition.
 */
private val log = KotlinLogging.logger {}

@Singleton
class MapScriptService
@Inject
constructor(
    private val entryScripts: MapEntryScripts,
    private val scriptRunner: ScriptRunner,
    private val npcService: NpcService,
    private val characterStore: de.fiereu.openmmo.server.game.storage.CharacterStore,
    private val layoutVariants: LayoutVariants? = null,
    private val presence: PresenceService? = null,
) {
  fun onMapEnter(session: SessionContext, state: PlayerState, map: MapDef) {
    // A script is already running for this player, do not start a second one on top of it.
    if (state.scriptOwnsMapEntry || state.blocksNewScript) return
    // The client re-requests its player once per map connection while loading an outdoor map;
    // one logical arrival runs its entry scripts exactly once.
    val arrivalKey = entryScriptsKey(map)
    if (state.entryScriptsMapKey == arrivalKey) return
    state.entryScriptsMapKey = arrivalKey
    arrive(session, state, map)
    val sequence = entrySequence(session, state, map) ?: return
    scriptRunner.run(session, state, sequence, entityId = -1)
  }

  /**
   * A script's own `warp` (the Champion's Room into the Hall of Fame): the client's arrival
   * request finds the warping script still running and must not start a second one, so the
   * arrival's bookkeeping and entry scripts run here, on the warping script's coroutine. Without
   * the bookkeeping VAR_TEMP_1 stayed at the 1 the Champion's Room set, the Hall of Fame's frame
   * script (VAR_TEMP_1 == 0) never ran and the player stood free at the door (2026-09-11).
   */
  suspend fun onScriptedArrival(ctx: de.fiereu.openmmo.server.game.script.ScriptContext, map: MapDef) {
    val state = ctx.state
    val session = ctx.session
    state.entryScriptsMapKey = entryScriptsKey(map)
    arrive(session, state, map)
    entrySequence(session, state, map)?.run(ctx)
  }

  /** The GBA's map-load bookkeeping: temp flags and vars, object placements, flash, layout. */
  private fun arrive(session: SessionContext, state: PlayerState, map: MapDef) {
    val charId = state.characterId
    // The GBA forgets two things on every map load, and ON_TRANSITION rewrites what still
    // applies: the FLAG_TEMP_* flags and setobjectxyperm placements. Keeping them made the
    // Cerulean policeman stay in front of the door after the S.S. Ticket, and cut trees cut.
    state.tileOverrides.clear()
    if (charId != null) applyFlashState(session, charId, map)
    if (charId != null) {
      val moved = resetMapLocalState(charId, map)
      // The npcs were spawned before this reset, at their overridden tiles; a wiped override no
      // longer marks them, so name them outright or a pushed Strength boulder stays where it was
      // left on every later visit (Victory Road, 2026-09-11).
      if (moved != null) {
        npcService.refreshDynamicNpcs(
            session, map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt(), forceEntities = moved)
      }
    }
    if (charId != null && state.arrivedByFall) {
      state.arrivedByFall = false
      // FallWarpEffect_7: landing on surfable water after a drop (Seafoam Islands) sets VAR_TEMP_1
      // and mounts the surf, and the floor's frame script rides the current from there.
      if (map.tileAt(state.x.toInt(), state.y.toInt())?.behavior?.isSurfable == true) {
        val namespace = de.fiereu.openmmo.common.enums.Region.byId(map.regionId.toInt())?.name?.lowercase()
        if (namespace != null) characterStore.setStoryVar(charId, "$namespace/VAR_TEMP_1", 1)
        state.surfing = true
        state.riding = false
        val mount = de.fiereu.openmmo.net.game.packets.EntityTransportationPacket(charId, 0x01)
        presence?.announce(session, mount) ?: session.send(mount)
        state.mountResendPending = true
        log.info { "Fell onto water at (${state.x}, ${state.y}) on ${map.bankId}:${map.mapId}: surfing, VAR_TEMP_1 = 1" }
      }
    }
    // Story-dependent map variants: the client's block grid follows the flags already set.
    if (charId != null) {
      val flags = characterStore.getCharacter(charId)?.storyFlags ?: emptySet()
      layoutVariants?.onMapEnter(session, state) { it in flags }
    }
  }

  /** The arrival's scripts as one sequence, or null when the map has nothing to run. */
  private fun entrySequence(session: SessionContext, state: PlayerState, map: MapDef): Script? {
    val charId = state.characterId
    val (setup, frame) = entryScripts.onEntryPhases(state, map)
    val hasArrivalTrigger = entryScripts.hasCoordinate(map, state.x.toInt(), state.y.toInt())
    val polish = MapEntryPolish.touchFor(map)
    if (setup.isEmpty() && frame == null && !hasArrivalTrigger && polish == null) return null

    // Entry scripts may trigger their landing coordinate.
    return Script { ctx ->
      polish?.apply(ctx)
      setup.forEach { it.run(ctx) }
      // ON_TRANSITION just wrote the vars that dynamic npc sprites and positions read (the
      // decomp runs it before objects load); re-send the affected npcs with the fresh values -
      // BEFORE the frame scene, which walks them (One Island's Bill snapped back to his
      // setobjectxyperm tile when this ran after the scene).
      npcService.refreshDynamicNpcs(
          session, map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
      frame?.run(ctx)
      // Silph Co's barriers and every other ON_LOAD setmetatile: the client can still be loading
      // the map when these went out, so the first step on the map re-sends them.
      if (state.tileOverrides.isNotEmpty()) state.tileOverridesPendingResend = true
      if (charId != null) {
        entryScripts.atCoordinate(charId, map, state.x.toInt(), state.y.toInt())?.run(ctx)
      }
    }
  }

  /** A DS map arrival: the header's init and frame-table scripts, once per logical arrival. */
  /**
   * [spawnNpcs], when given, is the map's npc spawn, and this call owns it: the cartridge runs a
   * map's on-load script BEFORE it draws anything, so the script's placements (Bianca and her dad
   * set up for their argument, Bianca stood by the player's house) are where the actors first
   * appear. Spawned first and placed 1 ms later, an actor flickered at its ROM tile - and outdoors
   * the map's npcs were spawned a second time from their ROM tiles, so Bianca walked home from the
   * lab (2026-09-20). The on-load script runs with placements only recorded, then the spawn puts
   * everyone where it left them, then the frame scene plays. Called exactly once, whatever happens.
   */
  fun onNdsEnter(
      session: SessionContext,
      state: PlayerState,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      spawnNpcs: (() -> Unit)? = null,
  ) {
    val spawned = java.util.concurrent.atomic.AtomicBoolean(false)
    fun spawnOnce() {
      if (spawned.compareAndSet(false, true)) spawnNpcs?.invoke()
    }
    if (state.scriptOwnsMapEntry || state.blocksNewScript) return spawnOnce()
    val arrivalKey = (regionId.toLong() and 0xFF shl 40) or (bankId.toLong() and 0xFF shl 20) or (mapId.toLong() and 0xFF)
    if (state.entryScriptsMapKey == arrivalKey) return spawnOnce()
    state.entryScriptsMapKey = arrivalKey
    val (onLoad, frame) = entryScripts.onNdsEntryPhases(regionId, bankId, mapId)
    if (onLoad == null && frame == null) return spawnOnce()
    try {
      scriptRunner.run(
          session,
          state,
          Script { ctx ->
            try {
              if (onLoad != null) {
                state.ndsPlacementOnly = spawnNpcs != null
                try {
                  onLoad.run(ctx)
                } finally {
                  state.ndsPlacementOnly = false
                }
              }
            } finally {
              spawnOnce()
            }
            frame?.run(ctx)
          },
          entityId = -1)
      // The runner declines silently when it cannot start (a dead scope, a script that slipped in):
      // the npcs must still appear. Idempotent, so a script that did start is unaffected.
      if (!state.scriptRunning) spawnOnce()
    } catch (e: Exception) {
      spawnOnce()
      throw e
    }
  }

  /**
   * overworld.c on a map load: outdoors clears FLAG_SYS_FLASH_ACTIVE; a dark cave with the flag
   * still set loads fully lit (SetDefaultFlashLevel), so Flash carries from Rock Tunnel 1F to
   * B1F instead of asking again on every floor (2026-09-08).
   */
  private fun applyFlashState(session: SessionContext, charId: Long, map: MapDef) {
    val flag = FieldMoves.flashActiveFlag(map.regionId.toInt())
    val stored = characterStore.getCharacter(charId) ?: return
    if (flag !in stored.storyFlags) return
    if (map.mapType in OUTDOORS) {
      characterStore.clearStoryFlag(charId, flag)
      return
    }
    if (map.lighting == de.fiereu.openmmo.common.enums.Lighting.DARK_FLASH_USABLE) {
      session.send(de.fiereu.openmmo.net.game.packets.MapLightingPacket(de.fiereu.openmmo.net.game.packets.MapLightingPacket.LIT))
    }
  }

  /**
   * Clears this map's object placements and every temporary flag. Null when nothing was set, else
   * the entity indices whose placement was cleared (possibly empty).
   */
  private fun resetMapLocalState(charId: Long, map: MapDef): Set<Int>? {
    val stored = characterStore.getCharacter(charId) ?: return null
    val prefix = npcService.xyOverridePrefix(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
    val movementPrefix = npcService.movementOverridePrefix(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
    var changed = false
    val moved = mutableSetOf<Int>()
    stored.storyVars.keys.filter { it.startsWith(prefix) || it.startsWith(movementPrefix) }.forEach {
      characterStore.setStoryVar(charId, it, 0)
      if (it.startsWith(prefix)) it.removePrefix(prefix).toIntOrNull()?.let(moved::add)
      changed = true
    }
    // ClearTempFieldEventData (event_data.c, both games) on every map load: the temp flags and
    // the per-map system flags - Strength's activation, the flutes, the special wild battle
    // (FireRed), the encounter-rate items and the deletable-object control (Emerald). Strength
    // otherwise stayed on for good once used (2026-09-12).
    stored.storyFlags.filter { flag -> flag.contains("/FLAG_TEMP_") || MAP_LOCAL_SYS_FLAGS.any { flag.endsWith("/$it") } }.forEach {
      characterStore.clearStoryFlag(charId, it)
      changed = true
    }
    // VAR_TEMP_0..F are wiped on every map load too. Pallet Town arms its sign-lady trigger
    // through VAR_TEMP_2 and a stale 1 re-fired it on every later visit (2026-09-07).
    stored.storyVars.keys.filter { it.contains("/VAR_TEMP_") && stored.storyVars[it] != 0 }.forEach {
      characterStore.setStoryVar(charId, it, 0)
      changed = true
    }
    return if (changed) moved else null
  }

  companion object {
    /** The system flags ClearTempFieldEventData clears with the temp flags (FireRed + Emerald). */
    private val MAP_LOCAL_SYS_FLAGS =
        setOf(
            "FLAG_SYS_USE_STRENGTH",
            "FLAG_SYS_WHITE_FLUTE_ACTIVE",
            "FLAG_SYS_BLACK_FLUTE_ACTIVE",
            "FLAG_SYS_SPECIAL_WILD_BATTLE",
            "FLAG_SYS_INFORMED_OF_LOCAL_WIRELESS_PLAYER",
            "FLAG_SYS_ENC_UP_ITEM",
            "FLAG_SYS_ENC_DOWN_ITEM",
            "FLAG_SYS_CTRL_OBJ_DELETE",
            "FLAG_NURSE_UNION_ROOM_REMINDER",
        )

    /** IsMapTypeOutdoors: town, city, route, underwater and ocean route. */
    private val OUTDOORS =
        setOf(
            de.fiereu.openmmo.common.enums.MapType.VILLAGE,
            de.fiereu.openmmo.common.enums.MapType.CITY,
            de.fiereu.openmmo.common.enums.MapType.ROUTE,
            de.fiereu.openmmo.common.enums.MapType.UNDERWATER,
            de.fiereu.openmmo.common.enums.MapType.UNKNOWN_0x06,
        )

    fun entryScriptsKey(map: MapDef): Long =
        (map.regionId.toLong() and 0xFF shl 40) or
            (map.bankId.toLong() and 0xFF shl 20) or
            (map.mapId.toLong() and 0xFF)
  }

  /**
   * A completed step on a DS map: the ROM's step trigger on that tile when its var matches, the
   * same rule [onStep] applies to a GBA map's coord events.
   */
  fun onNdsStep(session: SessionContext, state: PlayerState, regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int): Boolean {
    if (state.blocksNewScript) {
      if (entryScripts.hasNdsCoordinate(regionId, bankId, mapId, x, y)) {
        log.info { "DS floor trigger at ($x, $y) skipped: dialog=${state.dialogVisible} script=${state.scriptRunning}" }
      }
      return false
    }
    val charId = state.characterId ?: return false
    val script = entryScripts.atNdsCoordinate(charId, regionId, bankId, mapId, x, y) ?: return false
    scriptRunner.run(session, state, script, entityId = -1)
    return true
  }

  /** Run the matching conditional coordinate script after a completed player step. */
  fun onStep(
      session: SessionContext,
      state: PlayerState,
      map: MapDef,
      x: Int,
      y: Int,
  ): Boolean {
    if (state.blocksNewScript) {
      // The previous tile's script is still finishing (no dialog is up, or this step would have
      // been refused): remember this tile so the runner fires its trigger when that script ends.
      // Counted as handled so the step rolls no encounter on top of it.
      if (state.scriptRunning && !state.dialogVisible && entryScripts.hasCoordinate(map, x, y)) {
        state.deferredTrigger =
            DeferredTrigger(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt(), x, y)
        return true
      }
      if (entryScripts.hasCoordinate(map, x, y)) log.info { "Floor trigger at ($x, $y) skipped: dialog=${state.dialogVisible} script=${state.scriptRunning}" }
      return false
    }
    state.deferredTrigger = null
    val charId = state.characterId ?: return false
    val script = entryScripts.atCoordinate(charId, map, x, y) ?: return false
    scriptRunner.run(session, state, script, entityId = -1)
    return true
  }
}
