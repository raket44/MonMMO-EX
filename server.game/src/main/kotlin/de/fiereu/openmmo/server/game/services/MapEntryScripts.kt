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

  /** [onNdsEntry] split: the on-load (TRANSITION) script and the frame-table scene, either may be absent. */
  fun onNdsEntryPhases(regionId: Int, bankId: Int, mapId: Int): Pair<Script?, Script?> {
    val header = (mapId shl 8) or bankId
    val source = gbaScriptSource(regionId) ?: return null to null
    fun find(label: String) = runCatching { scriptRegistry.forLabel(label, source) }.getOrNull()
    return find("NDS_INIT_${header}_TRANSITION") to find("NDS_INIT_${header}_FRAME")
  }

  /** One DS step trigger (the ROM's coord event): its rectangle, the story var and value it waits for, its script id. */
  private data class NdsCoordTrigger(
      val x: Int,
      val y: Int,
      val width: Int,
      val height: Int,
      val varKey: String,
      val value: Int,
      val scriptId: Int,
  )

  /** Every DS step trigger by (region, bank, map), from the corpus (NdsScriptCorpusGenerator.coordTriggerRows). */
  private val ndsCoordTriggers: Map<Triple<Int, Int, Int>, List<NdsCoordTrigger>> by lazy {
    de.fiereu.openmmo.script.GeneratedScriptCorpus.sources
        .flatMap { source ->
          val region = when (source.source) { "platinum" -> 3; "heartgold" -> 4; "white" -> 2; else -> return@flatMap emptyList() }
          source.coordTriggers.mapNotNull { row ->
            val p = row.split(';')
            if (p.size != 9) return@mapNotNull null
            val n = listOf(p[0], p[1], p[2], p[3], p[4], p[5], p[7], p[8]).map { it.toIntOrNull() ?: return@mapNotNull null }
            // The scripts write the var under their story namespace ("sinnoh/VAR_...").
            Triple(region, n[0], n[1]) to NdsCoordTrigger(n[2], n[3], n[4], n[5], "${source.storyNamespace}/${p[6]}", n[6], n[7])
          }
        }
        .groupBy({ it.first }, { it.second })
  }

  private fun ndsTriggersAt(regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int): List<NdsCoordTrigger> =
      ndsCoordTriggers[Triple(regionId, bankId, mapId)].orEmpty().filter {
        x in it.x until it.x + it.width.coerceAtLeast(1) && y in it.y until it.y + it.height.coerceAtLeast(1)
      }

  /** True when a DS tile has a step trigger at all, whatever its var currently says. */
  fun hasNdsCoordinate(regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int): Boolean =
      ndsTriggersAt(regionId, bankId, mapId, x, y).isNotEmpty()

  /**
   * The DS step trigger on a tile whose var holds its value, resolved the way a DS npc's script id is
   * (InteractionService.onNdsEntityInteract): ids from 2000 up are shared chunks, the rest index the
   * map header's own script file.
   */
  fun atNdsCoordinate(charId: Long, regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int): Script? {
    val trigger =
        ndsTriggersAt(regionId, bankId, mapId, x, y).firstOrNull { storyService.getVar(charId, it.varKey) == it.value }
            ?: return null
    val label =
        if (trigger.scriptId >= 2000) "NDS_CHUNK_${trigger.scriptId}"
        else "NDS_${(mapId shl 8) or bankId}_${trigger.scriptId}"
    return resolve(label, regionId)
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
