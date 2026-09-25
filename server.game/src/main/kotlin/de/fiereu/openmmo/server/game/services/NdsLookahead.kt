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
    val sight = trainerSight.ndsArmedSightTiles(charId, region, bank, map, trainerSight.playerHeightOn(state, region, bank, map)).toHashSet()
    // A walkway map (Driftveil Gym) flags nearly every tile blocked; there a tile the map has
    // is walkable, or the ray died on its first tile and no wall ever stood (owner, 2026-09-25).
    val walkable: (Int, Int) -> Boolean =
        if (ndsLand.mostlyBlocked(region, bank, map)) { x, y -> ndsLand.heightAt(region, bank, map, x, y) != null }
        else { x, y -> ndsLand.blocked(region, bank, map, x, y) == false }
    // The facing ray, plus a short look the other three ways: coming round a corner the stop is
    // beside the player, one turn away, and on a bike the turn and the step past it are announced
    // together (owner, 2026-09-25: "coming around corners has a block waiting for you").
    val facing = state.facingDirection
    val stops =
        (listOfNotNull(stopAhead(px, py, facing, triggers, sight, walkable)) +
                Direction.entries.filter { it != facing && (it.dx != 0 || it.dy != 0) }.mapNotNull { stopWithin(px, py, it, SIDE_RANGE, triggers, sight, walkable) })
            .distinctBy { it.wallX to it.wallY }
            .take(MAX_WALLS)
    // Standing on a stop: the wall past it stays until the script's hold clears it. Dropped here
    // it left with the same packet that started the hold, and a step the client had already
    // sent could pass - the very race the wall exists for (VPS log 11:29:29, 2026-09-24).
    val onStop = (px to py) in triggers || (px to py) in sight
    apply(session, state, region, bank, map, stops, keepStanding = onStop)
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

  private fun apply(session: SessionContext, state: PlayerState, region: Int, bank: Int, map: Int, stops: List<Stop>, keepStanding: Boolean = false) {
    val wanted = stops.associateBy { ndsWallKey(bank, map, it.wallX, it.wallY) }
    for ((key, id) in state.lookaheadWalls.entries.toList()) {
      if (key in wanted || keepStanding) continue
      npcService.despawnDsWall(session, region, bank, map, id)
      state.lookaheadWalls.remove(key)
      log.info { "[Lookahead] char=${state.characterId} wall $id down: the ray moved on" }
    }
    if (stops.isEmpty() && !keepStanding) return
    for ((wantedKey, stop) in wanted) {
      if (wantedKey in state.lookaheadWalls) continue
      val id = (FIRST_ID until FIRST_ID + MAX_WALLS).firstOrNull { it !in state.lookaheadWalls.values } ?: break
      state.lookaheadWalls[wantedKey] = id
      npcService.spawnDsWall(session, region, bank, map, id, stop.wallX, stop.wallY)
      log.info {
        "[Lookahead] char=${state.characterId} wall $id at (${stop.wallX}, ${stop.wallY}) past the ${stop.kind} at (${stop.stopX}, ${stop.stopY}), ${stop.distance} away ${stop.direction} (facing ${state.facingDirection})"
      }
    }
    val stamp = System.currentTimeMillis()
    state.lookaheadStamp = stamp
    scope.launch {
      delay(DECAY_MS)
      if (state.lookaheadStamp == stamp) clear(session, state, "decayed")
    }
  }

  data class Stop(val stopX: Int, val stopY: Int, val wallX: Int, val wallY: Int, val distance: Int, val kind: String, val direction: Direction = Direction.UP)

  companion object {
    /** How far ahead the facing ray looks, in tiles. */
    const val RANGE = 5
    /** How far the other three rays look: the stop beside the player, one turn away. */
    const val SIDE_RANGE = 2
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
    ): Stop? = stopWithin(px, py, facing, RANGE, triggers, sight, walkable)

    /** [stopAhead] over [range] tiles: the short side looks. */
    fun stopWithin(
        px: Int,
        py: Int,
        facing: Direction,
        range: Int,
        triggers: Set<Pair<Int, Int>>,
        sight: Set<Pair<Int, Int>>,
        walkable: (Int, Int) -> Boolean,
    ): Stop? {
      val dx = facing.dx
      val dy = facing.dy
      if (dx == 0 && dy == 0) return null
      var x = px
      var y = py
      for (d in 1..range) {
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
        return Stop(x, y, wx, wy, d, kind, facing)
      }
      return null
    }
  }
}
