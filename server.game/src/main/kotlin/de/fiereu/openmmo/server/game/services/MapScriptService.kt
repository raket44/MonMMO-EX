package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRunner
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
    val entry = entryScripts.onEntry(state, map)
    val hasArrivalTrigger = entryScripts.hasCoordinate(map, state.x.toInt(), state.y.toInt())
    if (entry.isEmpty() && !hasArrivalTrigger) return

    // Entry scripts may trigger their landing coordinate.
    val entrySequence = Script { ctx ->
      entry.forEach { it.run(ctx) }
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

  companion object {
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
    if (state.blocksNewScript) return false
    val charId = state.characterId ?: return false
    val script = entryScripts.atCoordinate(charId, map, x, y) ?: return false
    scriptRunner.run(session, state, script, entityId = -1)
    return true
  }
}
