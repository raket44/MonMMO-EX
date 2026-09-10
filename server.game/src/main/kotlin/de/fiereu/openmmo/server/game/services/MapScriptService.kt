package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.session.DeferredTrigger
import de.fiereu.openmmo.server.game.session.PlayerState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs a map's scripts when a player enters it. The decomp ON_TRANSITION script fires on every
 * entry however the player got there (login, a warp, or walking across a map connection), followed
 * by the first ON_FRAME entry whose story var matches. The conditional ON_WARP table is a later
 * addition.
 */
@Singleton
class MapScriptService
@Inject
constructor(
    private val entryScripts: MapEntryScripts,
    private val scriptRunner: ScriptRunner,
    private val npcService: NpcService,
    private val characterStore: de.fiereu.openmmo.server.game.storage.CharacterStore,
    private val layoutVariants: LayoutVariants? = null,
) {
  fun onMapEnter(session: SessionContext, state: PlayerState, map: MapDef) {
    // A script is already running for this player, do not start a second one on top of it.
    if (state.scriptOwnsMapEntry || state.blocksNewScript) return
    // The client re-requests its player once per map connection while loading an outdoor map;
    // one logical arrival runs its entry scripts exactly once.
    val arrivalKey = entryScriptsKey(map)
    if (state.entryScriptsMapKey == arrivalKey) return
    state.entryScriptsMapKey = arrivalKey
    val charId = state.characterId
    // The GBA forgets two things on every map load, and ON_TRANSITION rewrites what still
    // applies: the FLAG_TEMP_* flags and setobjectxyperm placements. Keeping them made the
    // Cerulean policeman stay in front of the door after the S.S. Ticket, and cut trees cut.
    state.tileOverrides.clear()
    if (charId != null) applyFlashState(session, charId, map)
    if (charId != null && resetMapLocalState(charId, map)) {
      npcService.refreshDynamicNpcs(session, map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
    }
    // Story-dependent map variants: the client's block grid follows the flags already set.
    if (charId != null) {
      val flags = characterStore.getCharacter(charId)?.storyFlags ?: emptySet()
      layoutVariants?.onMapEnter(session, state) { it in flags }
    }
    val entry = entryScripts.onEntry(state, map)
    val hasArrivalTrigger = entryScripts.hasCoordinate(map, state.x.toInt(), state.y.toInt())
    if (entry.isEmpty() && !hasArrivalTrigger) return

    // Entry scripts may trigger their landing coordinate.
    val entrySequence = Script { ctx ->
      entry.forEach { it.run(ctx) }
      // Silph Co's barriers and every other ON_LOAD setmetatile: the client can still be loading
      // the map when these went out, so the first step on the map re-sends them.
      if (state.tileOverrides.isNotEmpty()) state.tileOverridesPendingResend = true
      // ON_TRANSITION just wrote the vars that dynamic npc sprites and positions read (the
      // decomp runs it before objects load); re-send the affected npcs with the fresh values.
      npcService.refreshDynamicNpcs(
          session, map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
      if (charId != null) {
        entryScripts.atCoordinate(charId, map, state.x.toInt(), state.y.toInt())?.run(ctx)
      }
    }
    scriptRunner.run(session, state, entrySequence, entityId = -1)
  }

  /** A DS map arrival: the header's init and frame-table scripts, once per logical arrival. */
  fun onNdsEnter(session: SessionContext, state: PlayerState, regionId: Int, bankId: Int, mapId: Int) {
    if (state.scriptOwnsMapEntry || state.blocksNewScript) return
    val arrivalKey = (regionId.toLong() and 0xFF shl 40) or (bankId.toLong() and 0xFF shl 20) or (mapId.toLong() and 0xFF)
    if (state.entryScriptsMapKey == arrivalKey) return
    state.entryScriptsMapKey = arrivalKey
    val entry = entryScripts.onNdsEntry(regionId, bankId, mapId)
    if (entry.isEmpty()) return
    scriptRunner.run(session, state, Script { ctx -> entry.forEach { it.run(ctx) } }, entityId = -1)
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

  /** Clears this map's object placements and every temporary flag; true when anything was set. */
  private fun resetMapLocalState(charId: Long, map: MapDef): Boolean {
    val stored = characterStore.getCharacter(charId) ?: return false
    val prefix = npcService.xyOverridePrefix(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
    val movementPrefix = npcService.movementOverridePrefix(map.regionId.toInt(), map.bankId.toInt(), map.mapId.toInt())
    var changed = false
    stored.storyVars.keys.filter { it.startsWith(prefix) || it.startsWith(movementPrefix) }.forEach {
      characterStore.setStoryVar(charId, it, 0)
      changed = true
    }
    stored.storyFlags.filter { it.contains("/FLAG_TEMP_") }.forEach {
      characterStore.clearStoryFlag(charId, it)
      changed = true
    }
    // VAR_TEMP_0..F are wiped on every map load too. Pallet Town arms its sign-lady trigger
    // through VAR_TEMP_2 and a stale 1 re-fired it on every later visit (2026-09-07).
    stored.storyVars.keys.filter { it.contains("/VAR_TEMP_") && stored.storyVars[it] != 0 }.forEach {
      characterStore.setStoryVar(charId, it, 0)
      changed = true
    }
    return changed
  }

  companion object {
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
      return false
    }
    state.deferredTrigger = null
    val charId = state.characterId ?: return false
    val script = entryScripts.atCoordinate(charId, map, x, y) ?: return false
    scriptRunner.run(session, state, script, entityId = -1)
    return true
  }
}
