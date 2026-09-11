package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import de.fiereu.openmmo.server.game.script.ScriptResolutionException
import de.fiereu.openmmo.server.game.script.gbaScriptSource
import de.fiereu.openmmo.server.game.session.PlayerState
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Works out which scripts a map runs when a player arrives on it: the ON_TRANSITION script, then
 * the first ON_FRAME entry whose story var matches, then the coordinate trigger the player landed
 * on. [MapScriptService] runs these on a fresh script coroutine when the player walks or logs in,
 * and a script that warps runs them inline on its own coroutine, the way the decomp's warp
 * continues into the destination map's scripts.
 */
@Singleton
class MapEntryScripts
@Inject
constructor(
    private val scriptRegistry: ScriptRegistry,
    private val storyService: StoryService,
) {
  /**
   * [onEntry] split the way the cartridge orders them: ON_TRANSITION and ON_LOAD first, then the
   * frame-table scene - the caller re-sends the npcs those two placed BEFORE the scene walks them.
   */
  fun onEntryPhases(state: PlayerState, map: MapDef): Pair<List<Script>, Script?> {
    val setup = buildList {
      resolve(map.onTransitionScript, map.regionId.toInt())?.let { add(it) }
      resolve(map.onLoadScript, map.regionId.toInt())?.let { add(it) }
    }
    val charId = state.characterId ?: return setup to null
    val frame =
        map.onFrameScripts
            .firstOrNull { storyService.getVar(charId, it.varKey) == it.value }
            ?.let { resolve(it.script, map.regionId.toInt()) }
    return setup to frame
  }

  fun onEntry(state: PlayerState, map: MapDef): List<Script> {
    val charId = state.characterId
    return buildList {
      resolve(map.onTransitionScript, map.regionId.toInt())?.let { add(it) }
      // ON_LOAD follows the layout load on the cartridge: the setmetatile fixes a map re-applies.
      resolve(map.onLoadScript, map.regionId.toInt())?.let { add(it) }
      if (charId != null) {
        map.onFrameScripts
            .firstOrNull { storyService.getVar(charId, it.varKey) == it.value }
            ?.let { resolve(it.script, map.regionId.toInt()) }
            ?.let { add(it) }
      }
    }
  }

  /** The conditional coordinate trigger on one tile, the decomp coord_events table. */
  /**
   * A DS map: the corpus binds the ROM header's init script as NDS_INIT_<header>_TRANSITION and
   * its frame table (var == value -> script) as NDS_INIT_<header>_FRAME. Most headers have neither.
   */
  fun onNdsEntry(regionId: Int, bankId: Int, mapId: Int): List<Script> {
    val header = (mapId shl 8) or bankId
    val source = gbaScriptSource(regionId) ?: return emptyList()
    return listOf("NDS_INIT_${header}_TRANSITION", "NDS_INIT_${header}_FRAME").mapNotNull { label ->
      runCatching { scriptRegistry.forLabel(label, source) }.getOrNull()
    }
  }

  fun atCoordinate(charId: Long, map: MapDef, x: Int, y: Int): Script? {
    val trigger =
        map.coordScripts.firstOrNull {
          it.x == x && it.y == y && storyService.getVar(charId, it.varKey) == it.value
        } ?: return null
    return resolve(trigger.script, map.regionId.toInt())
  }

  /** True when the tile has a trigger at all, whatever its var currently says. */
  fun hasCoordinate(map: MapDef, x: Int, y: Int): Boolean =
      map.coordScripts.any { it.x == x && it.y == y }

  private fun resolve(label: String, regionId: Int): Script? {
    if (label.isEmpty()) return null
    val script =
        try {
          scriptRegistry.forLabel(label, gbaScriptSource(regionId))
        } catch (cause: ScriptResolutionException) {
          log.warn(cause) { "Skipping unavailable map-entry script $label" }
          null
        }
    if (script == null) log.debug { "Map script $label is not ported yet" }
    return script
  }
}
