package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.ndsWallKey
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

/**
 * The player's own line of sight, so the server gets a head start on the client (owner,
 * 2026-09-24): the client finishes every step it has announced before it reads the server, so
 * on a bike two tiles land after a script's hold. Nothing sent after the fact can stop that;
 * only an obstacle that is already there does. Each step this looks along the player's facing
 * up to [RANGE] tiles on a DS map for the first stop - a tile of an armed coord trigger, or a
 * tile an unbeaten trainer can see - and stands one temp invisible npc (the gyms' wall sprite)
 * on the tile just past it, so the player lands on the stop and cannot slide beyond. The wall
 * is refreshed every step, dropped the moment the ray no longer finds that stop, when a script
 * takes hold, when the player warps, and by a timed decay [DECAY_MS] after the last refresh.
 * Only this session sees it. Rail positions are line-local, so a player riding a rail gets none.
 */
@Singleton
class NdsLookahead
@Inject
constructor(
    private val npcService: NpcService,
    private val ndsLand: NdsLand,
    private val mapEntryScripts: MapEntryScripts,
    private val trainerSight: TrainerSightService,
    private val scope: CoroutineScope,
) {
  /** Recompute after a committed or refused step, or a script release. */
  fun update(session: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    val region = state.regionId
    if (region !in 2..4 || !state.lookaheadEnabled || state.blocksPlayerInput || state.railLine >= 0) {
      clear(session, state, "not applicable")
      return
    }
    val bank = state.bankId
    val map = state.mapId
    val px = state.x.toInt()
    val py = state.y.toInt()
    val triggers = mapEntryScripts.ndsArmedTriggerTiles(charId, region, bank, map).toHashSet()
    val sight = trainerSight.ndsArmedSightTiles(charId, region, bank, map).toHashSet()
    val stop = stopAhead(px, py, state.facingDirection, triggers, sight) { x, y -> ndsLand.blocked(region, bank, map, x, y) == false }
    apply(session, state, region, bank, map, stop)
  }

  /** Everything down, e.g. a script took hold of the player. */
  fun clear(session: SessionContext, state: PlayerState, reason: String) {
    if (state.lookaheadWalls.isEmpty()) return
    for ((key, id) in state.lookaheadWalls.entries.toList()) {
      npcService.despawnDsWall(session, state.regionId, state.bankId, state.mapId, id)
      state.lookaheadWalls.remove(key)
    }
    log.info { "[Lookahead] char=${state.characterId} walls down: $reason" }
  }

  private fun apply(session: SessionContext, state: PlayerState, region: Int, bank: Int, map: Int, stop: Stop?) {
    val wantedKey = stop?.let { ndsWallKey(bank, map, it.wallX, it.wallY) }
    for ((key, id) in state.lookaheadWalls.entries.toList()) {
      if (key == wantedKey) continue
      npcService.despawnDsWall(session, region, bank, map, id)
      state.lookaheadWalls.remove(key)
      log.info { "[Lookahead] char=${state.characterId} wall $id down: the ray moved on" }
    }
    if (stop == null) return
    if (wantedKey !in state.lookaheadWalls) {
      val id = (FIRST_ID until FIRST_ID + MAX_WALLS).firstOrNull { it !in state.lookaheadWalls.values } ?: return
      state.lookaheadWalls[wantedKey!!] = id
      npcService.spawnDsWall(session, region, bank, map, id, stop.wallX, stop.wallY)
      log.info {
        "[Lookahead] char=${state.characterId} wall $id at (${stop.wallX}, ${stop.wallY}) past the ${stop.kind} at (${stop.stopX}, ${stop.stopY}), ${stop.distance} ahead facing ${state.facingDirection}"
      }
    }
    val stamp = System.currentTimeMillis()
    state.lookaheadStamp = stamp
    scope.launch {
      delay(DECAY_MS)
      if (state.lookaheadStamp == stamp) clear(session, state, "decayed")
    }
  }

  data class Stop(val stopX: Int, val stopY: Int, val wallX: Int, val wallY: Int, val distance: Int, val kind: String)

  companion object {
    /** How far ahead the ray looks, in tiles. */
    const val RANGE = 5
    /** How long a wall outlives its last refresh. */
    const val DECAY_MS = 1500L
    /** Npc indices the walls use: above the gyms' bands (NdsCurtainWalls.FIRST_WALL_ID). */
    const val FIRST_ID = 300
    const val MAX_WALLS = 4

    /**
     * The first stop on the ray from (px, py) along [facing] within [RANGE], with the wall tile
     * just past it: the tile after the trigger's whole run of tiles (a row is crossed as one),
     * or the next tile after a sight tile. Null when there is none, or the wall tile is not
     * walkable (the map already stops the player there).
     */
    fun stopAhead(
        px: Int,
        py: Int,
        facing: Direction,
        triggers: Set<Pair<Int, Int>>,
        sight: Set<Pair<Int, Int>>,
        walkable: (Int, Int) -> Boolean,
    ): Stop? {
      val dx = facing.dx
      val dy = facing.dy
      if (dx == 0 && dy == 0) return null
      var x = px
      var y = py
      for (d in 1..RANGE) {
        x += dx
        y += dy
        if (!walkable(x, y)) return null
        val t = x to y
        val kind = when {
          t in triggers -> "trigger"
          t in sight -> "sight line"
          else -> continue
        }
        // Past the stop: for a trigger, past its run of tiles along the ray (a two-deep box is
        // one stop); for a sight tile, the very next tile.
        var wx = x + dx
        var wy = y + dy
        if (kind == "trigger") while ((wx to wy) in triggers) { wx += dx; wy += dy }
        if (!walkable(wx, wy)) return null
        return Stop(x, y, wx, wy, d, kind)
      }
      return null
    }
  }
}
