package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.server.game.script.MovementStep
import kotlinx.coroutines.launch
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.net.game.packets.EntityFaceTurnPacket
import de.fiereu.openmmo.net.game.packets.FaceDirectionPacket
import de.fiereu.openmmo.net.game.packets.GbaEntityMovePacket
import de.fiereu.openmmo.net.game.packets.MapData
import de.fiereu.openmmo.net.game.packets.MovementPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import de.fiereu.openmmo.server.game.script.ScriptResolutionException
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.script.gbaScriptSource
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/** Resolve a cardinal Gen-3 ledge hop to its tile two spaces away. */
/** How far a client claim may run ahead of the server and still be walked, not reset. */
private const val CATCH_UP_TILES = 6

/** Committed tiles remembered per map for phantom-step rewinds. */
private const val RECENT_TILES = 8

// A spin-tile slide never runs longer than this; the hideout mazes are far shorter.
private const val MAX_SPIN_STEPS = 64

/** The window after a desync reset in which pre-reset claims are dropped, about one round trip. */
private const val RESET_ECHO_MILLIS = 600L

internal fun ledgeLanding(
    map: MapDef,
    fromX: Int,
    fromY: Int,
    direction: Direction,
): Pair<Int, Int>? {
  val expectedBehavior =
      when (direction) {
        Direction.DOWN -> TileBehavior.JUMP_SOUTH
        Direction.UP -> TileBehavior.JUMP_NORTH
        Direction.LEFT -> TileBehavior.JUMP_WEST
        Direction.RIGHT -> TileBehavior.JUMP_EAST
        Direction.DIVE,
        Direction.EMERGE -> return null
      }
  val ledgeX = fromX + direction.dx
  val ledgeY = fromY + direction.dy
  if (map.tileAt(ledgeX, ledgeY)?.behavior != expectedBehavior) return null

  val landingX = fromX + direction.dx * 2
  val landingY = fromY + direction.dy * 2
  if (landingX !in 0 until map.width || landingY !in 0 until map.height) return null
  if (map.tileAt(landingX, landingY)?.blocksMovement() == true) return null
  return landingX to landingY
}

@Singleton
class MovementService
@Inject
constructor(
    private val warpService: WarpService,
    private val mapLoadService: MapLoadService,
    private val npcService: NpcService,
    private val presenceService: PresenceService,
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val encounterService: EncounterService,
    private val mapScriptService: MapScriptService,
    private val customWarps: CustomWarps,
    private val ndsWarps: NdsWarps,
    private val warpRules: WarpRules,
    private val trainerSight: TrainerSightService,
    private val scriptMovement: ScriptMovementService,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val scriptRegistry: ScriptRegistry? = null,
    private val scriptRunner: Provider<ScriptRunner>? = null,
) {

  /** One step. The client sends the tile it left and the direction, the server derives the rest. */
  fun onMovement(event: PacketEvent<MovementPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val msg = event.packet
    // Temporarily INFO: hunting the phantom steps around script locks - shows every client
    // step with the lock state it met.
    log.info {
      "Movement: char=$charId (${msg.x}, ${msg.y}) dir=${msg.direction} state=0x%02x".format(msg.stateRaw) +
          " locked=${state.blocksPlayerInput} script=${state.scriptRunning}"
    }

    val stored = characterStore.getCharacter(charId) ?: return
    val currentMap =
        mapManager.getMap(
            stored.info.positionRegionId,
            stored.info.positionBankId,
            stored.info.positionMapId,
        )
    if (currentMap == null) {
      // A region the server does not host yet - Johto, Sinnoh, Unova render entirely from the
      // client's own ROM data, and its collision already stops walls locally. Until those maps
      // exist server-side, the client's steps are trusted and tracked, so exploration works and
      // position survives a relog. Admin-placed custom warps are the one server feature here:
      // they are exactly how a door that black-screens gets cured from inside the game.
      // The arrival-step choreography window refuses ALL movement: stale moves raced the
      // input-lock packet and fired fresh warps mid-step.
      if (System.currentTimeMillis() < state.moveIgnoreUntil) return
      // Same guard the hosted branch always had and this one lacked: between a warp firing and
      // the client asking for its player, moves still describe the OLD map - processing them
      // against the new map's warp table was the phantom "new warp info" during desync.
      if (state.justWarped) return
      val toX = msg.x + msg.direction.dx
      val toY = msg.y + msg.direction.dy
      // Calibration logging for the Gen 5 rail maps (Castelia's main city, Skyarrow, the League
      // lobby): every unhosted move, with the raw state byte. Temporary but cheap on a dev server.
      log.info {
        "NDS move: char=$charId ${state.bankId}:${state.mapId} (${msg.x}, ${msg.y}) " +
            "dir=${msg.direction} state=0x%02x".format(msg.stateRaw)
      }
      characterStore.updatePosition(charId, toX.toShort(), toY.toShort(), facing = msg.direction)
      state.x = toX.toShort()
      state.y = toY.toShort()
      state.facingDirection = msg.direction
      if (executeCustomWarp(ctx, charId, state.regionId, state.bankId, state.mapId, toX, toY)) {
        return
      }
      // The ROM's own doors, so NDS maps connect the way they do in the source game instead of
      // black-screening. A warp fires when the player moves in the direction it faces, either
      // stepping onto it (walking up into a shop door) or standing on it (walking down off the
      // mat inside to leave). Arriving on a map puts the player on its own exit tile, so the
      // standing case is what gets them back out.
      // The tracked bank/map goes stale when the client silently crosses a seam between maps
      // stitched on the same ROM matrix, so the fallback index is keyed by the TRACKED map's
      // matrix plus coordinates - seams never leave a matrix, so the matrix stays correct even
      // when bank/map is stale, and maps on other matrices can never collide.
      // The patched client transmits its Gen 5 rail line in movement state bits 2-5 (0 when not
      // on a rail or unpatched); rail warp rows are picked by it, so Castelia's overlapping
      // street mouths route to the street the player is actually riding toward.
      val railLine = (msg.stateRaw shr 2) and 0x0F
      fun doorAt(x: Int, y: Int) =
          ndsWarps.warpAt(state.regionId, state.bankId, state.mapId, x, y, railLine)
              ?: ndsWarps.warpAtMatrix(state.regionId, state.bankId, state.mapId, x, y, railLine)
      // After an arrival the player stands on the partner warp (often a wide box). Only the
      // DIRECTION-LESS boundary warps are held back until the player has stood on one warp-free
      // tile - those fire on any step and bounced the player straight back. Facing-gated doors
      // (walk down off the Pokecenter mat) keep working immediately.
      val onArrivalBox = state.ndsWarpGuard && doorAt(msg.x, msg.y) != null
      if (!onArrivalBox) state.ndsWarpGuard = false
      val stepped = doorAt(toX, toY)
      val standing = doorAt(msg.x, msg.y)
      val facing = msg.direction.ordinal
      // Direction-less warps also fire from STANDING once the arrival guard has cleared: rail
      // movement (Castelia, Skyarrow) reports arc-length coordinates where the pressed direction
      // means nothing, and the client stops one step short of a rail boundary, parking the player
      // on the approach tile.
      // Rail rows (srcLine >= 0) never fire from the EXTRAPOLATED step target: on rails the
      // direction byte does not match how coordinates move, so the prediction lands on tiles the
      // player is not walking onto and bounced them out of Castelia's lower city.
      // Directional fires (facing must match the door) are ALWAYS answered: when the player
      // presses into a door the client has already begun its animation and fade, and a muted
      // reply is a permanent black screen. The guard only gates direction-less boundary fires,
      // which are the ones that loop.
      // Gen 4 warp records carry no direction, but the ROM's movement-permission TYPE byte
      // under the tile does (extracted by tools/nds/Perm4.java). Trigger rules, vanilla-shaped:
      // - 0x69 blocked building door: fires stepping INTO it, up-press only (doors face south).
      // - Stair types (0x62/0x63 east/west walls, 0x5E/0x5F interior mats, HGSS 0x3C/0x3D/0x3E):
      //   NEVER fire from stepping onto the tile - you stand on the mat freely and the warp
      //   fires only when you press INTO the staircase from it. This directional standing fire
      //   also bypasses the arrival-box guard: the direction gate itself prevents bounce-back,
      //   and without the bypass a rest-on-mat arrival was a trap (press out -> server muted the
      //   warp while the client's own ROM logic faded -> black screen).
      // - 0x65 exit mats: contact fire as always; a DOWN press additionally fires even while
      //   the arrival guard is armed, so resting on the mat can always walk back out.
      // Unknown tiles (all of Gen 5) return -1 and keep the original rules.
      fun bh(tx: Int, ty: Int) =
          ndsWarps.behaviorAt(state.regionId, state.bankId, state.mapId, tx, ty)
      // The universal rulebook (WarpRules, per-region editable list): each fixture type says
      // HOW it fires. STAND never fires from the step-toward packet - the step onto the
      // fixture is a plain step the client animates freely, and the follow-up press fires in
      // sync (firing early was the "instant warp" that skipped animations). STEP is for
      // impassable fixtures (doors) entered pressing into them. CONTACT keeps the guarded
      // any-direction fire. Untyped tiles (all of Gen 5) keep the original guarded rules.
      fun ruleAt(tx: Int, ty: Int): WarpRules.Rule? =
          warpRules.forTile(state.regionId, state.bankId, state.mapId, tx, ty)
              ?: bh(tx, ty).takeIf { it >= 0 }?.let { warpRules.forType(state.regionId, it) }
      fun steppedFires(r: WarpRules.Rule?): Boolean =
          when (r?.fire) {
            null -> !onArrivalBox
            WarpRules.Fire.STEP -> r.press == msg.direction
            WarpRules.Fire.CONTACT -> !onArrivalBox
            WarpRules.Fire.STAND -> false
          }
      fun standingFires(r: WarpRules.Rule?): Boolean =
          when (r?.fire) {
            null -> !onArrivalBox
            // A deliberate directional press fires even on the arrival box.
            WarpRules.Fire.STAND -> r.press == msg.direction
            WarpRules.Fire.CONTACT -> !onArrivalBox
            WarpRules.Fire.STEP -> false
          }
      val door =
          stepped?.takeIf {
            it.srcLine < 0 &&
                ((it.direction < 0 && steppedFires(ruleAt(toX, toY))) || it.direction == facing)
          }
              ?: standing?.takeIf {
                it.direction == facing || (it.direction < 0 && standingFires(ruleAt(msg.x, msg.y)))
              }
      if (door != null) {
        // Arrivals that land NEXT TO other warp tiles (bridge ends, boundary boxes, Castelia's
        // packed doorways) get walked one tile clear of the mat, so a sidestep cannot slide onto
        // a neighbouring warp. Step against the landing warp's exit direction when it has one
        // (into the room/area), otherwise keep the player's direction of travel (bridges and
        // edges). A lone door mat with no warp neighbours keeps the vanilla on-the-mat arrival.
        var ax = door.x
        var ay = door.y
        val landing = ndsWarps.warpAt(state.regionId, door.bank, door.map, door.x, door.y)
        // A RETURN through the door just entered mirrors the entry: walked in leftward, walk out
        // facing (and stepping) rightward - Castelia's sideways alley doors. Other arrivals face
        // into the destination, opposite the landing door's exit direction.
        val returning =
            door.bank == state.lastWarpFromBank &&
                door.map == state.lastWarpFromMap &&
                state.lastWarpEntryDir != null
        // The LANDING tile's own ROM data beats every guess (the first trip down an escalator
        // walked the ENTRY press direction because nothing else was known - later trips only
        // looked right because the returning-mirror kicked in):
        // 1. the landing tile's typed entry press, reversed = its walk-off direction;
        // 2. an untyped landing (escalators) with exactly one non-fixture side = that side.
        val landingBehaviorType = ndsWarps.behaviorAt(state.regionId, door.bank, door.map, ax, ay)
        val landingTypeRule =
            landingBehaviorType.takeIf { it >= 0 }?.let { warpRules.forType(state.regionId, it) }
        val maskDir =
            when (ndsWarps.fixtureMaskAt(state.regionId, door.bank, door.map, ax, ay)) {
              0b1110 -> Direction.UP // only the up side is open
              0b1101 -> Direction.DOWN
              0b1011 -> Direction.LEFT
              0b0111 -> Direction.RIGHT
              else -> null
            }
        val arriveDir =
            when {
              // ESCALATOR_FLIP_FACE (0x6A): the decomp's own name IS the mechanic - the ride
              // turns you around, so you leave the way you came. Diagnosed from the log: the
              // first trip down the 236<->241 escalator walked the ENTRY direction (wrong);
              // later trips only looked right because the returning-mirror equals the flip.
              landingBehaviorType == 0x6A -> msg.direction.opposite()
              landingTypeRule?.press != null -> landingTypeRule.press.opposite()
              maskDir != null -> maskDir
              returning -> state.lastWarpEntryDir!!.opposite()
              landing != null && landing.direction in 0..3 ->
                  Direction.entries[landing.direction].opposite()
              else -> msg.direction
            }
        // Vanilla end-states (verified by playtest, the earlier guess was backwards): INTERIOR
        // arrivals rest ON the entry mat; EXTERIOR arrivals end one tile off the door - the
        // emergence walk-out. The client PLAYS that walk itself: arrival lands on the mat and
        // onRequestPlayer follows the LoadEntity with an EntityMove (0xE4), which the client
        // routes through its animated movement path even for the local player. Rail destinations
        // keep direct placement - their coordinates are not cartesian, so a dx/dy walk target
        // would be wrong there.
        // The user's system: every NDS warp arrival takes ONE step the way the player faces.
        // Gen 4 exemptions were tried TWICE (matrix-based interiors-only, then a flat
        // no-step-in-3..4) and BOTH were reverted on the user's order - the universal step
        // stays for all NDS regions.
        val stepDir: Direction = arriveDir
        val stepX = -1
        val stepY = -1
        state.facingDirection = arriveDir
        state.lastWarpEntryDir = msg.direction
        state.lastWarpFromBank = state.bankId
        state.lastWarpFromMap = state.mapId
        log.info {
          "NDS warp: char=$charId ${state.bankId}:${state.mapId} ($toX, $toY) -> " +
              "${door.bank}:${door.map} ($ax, $ay) destLine=${door.destLine} " +
              "arriveDir=$arriveDir landingDir=${landing?.direction}"
        }
        warpService.executeRawWarp(
            ctx, charId, state.regionId, door.bank, door.map, ax, ay, door.destLine)
        // After executeRawWarp - it clears any stale step from an earlier arrival.
        // What the ROM says about the LANDING tile decides step vs stay (the data-driven rule
        // that replaces the universal forced step in Gen 4): interior mats and stairs
        // (0x65/0x5E/0x5F/0x62/0x63) REST the player on the tile, vanilla-style; doors,
        // openings and unknown tiles (all of Gen 5) keep the walk-out step.
        // The landing tile.s rule decides step vs rest (REST for interior mats/stairs/ladders,
        // STEP for doors/escalators/passages and untyped tiles incl all of Gen 5). A no-step
        // experiment proved the client does NOT animate door walk-outs on its own - the
        // server.s emergence step IS that walk-out, so STEP arrivals must keep it.
        val landingRule =
            warpRules.forTile(state.regionId, door.bank, door.map, ax, ay)
                ?: ndsWarps
                    .behaviorAt(state.regionId, door.bank, door.map, ax, ay)
                    .takeIf { it >= 0 }
                    ?.let { warpRules.forType(state.regionId, it) }
        val restOnTile = landingRule?.arrival == WarpRules.Arrival.REST
        state.pendingStepDir = if (restOnTile) null else stepDir
        state.pendingStepX = if (restOnTile) -1 else stepX
        state.pendingStepY = if (restOnTile) -1 else stepY
      }
      // A plain step on a DS map: the ROM's land data says whether it was grass or cave floor,
      // and the region's dex tables roll the encounter.
      if (door == null && trainerSight.onNdsStep(ctx, state, state.regionId, state.bankId, state.mapId, toX, toY)) return
      if (door == null) encounterService.onNdsStep(ctx, charId, state.regionId, state.bankId, state.mapId, toX, toY)
      return
    }
    var fromX = stored.info.positionX.toInt()
    var fromY = stored.info.positionY.toInt()

    // The drop guards run BEFORE the one-shot source trust: a stale move landing inside the
    // window (or while a script owns the player) used to consume the trust AND rewrite the
    // stored position to its claim while pretending to be dropped - the seed of the
    // door-cycling desyncs.
    when {
      // The arrival-step choreography window refuses ALL movement, same as the unhosted branch:
      // the scripted-state lock removes input client-side, but a move already in flight when it
      // was sent can still land here and must not commit or fire warps mid-emergence.
      System.currentTimeMillis() < state.moveIgnoreUntil -> return
      // Drop every step until the client asks for its player, else one left over from the old map
      // can fire a second warp.
      state.justWarped -> return
      // A script owns the player, like the decomp's lockall. The position reset re-asserts the
      // tile; the scripted face action after it SEIZES the movement controller (the only
      // channel proven to control the local player), interrupting the walk-in-place animation
      // and snapping the facing back to what the script holds.
      state.blocksPlayerInput -> {
        sendPositionReset(ctx, charId, currentMap, fromX, fromY, state.facingDirection)
        scriptMovement.reassertScriptedFacing(ctx, state)
        return
      }
    }

    if (state.acceptNextMoveSource &&
        msg.x in 0 until currentMap.width &&
        msg.y in 0 until currentMap.height) {
      // Trust the client's claimed source tile after scripted movement and the emergence step -
      // this is where a dropped emergence walk reconciles: the server held the mat, the client
      // reports where it really stands (the stepped tile if it walked, the mat if it did not),
      // and both sides agree from here on.
      fromX = msg.x
      fromY = msg.y
      characterStore.updatePosition(charId, fromX.toShort(), fromY.toShort())
      state.x = fromX.toShort()
      state.y = fromY.toShort()
      state.acceptNextMoveSource = false
    }

    if (msg.x != fromX || msg.y != fromY) {
      if (!reconcileClaim(ctx, charId, state, currentMap, fromX, fromY, msg)) return
      fromX = state.x.toInt()
      fromY = state.y.toInt()
    }

    // Only once the step is accepted, so a locked player keeps the facing its script left.
    state.facingDirection = msg.direction

    var toX = fromX + msg.direction.dx
    var toY = fromY + msg.direction.dy

    // Ledge hops land two tiles away.
    ledgeLanding(currentMap, fromX, fromY, msg.direction)?.let { landing ->
      toX = landing.first
      toY = landing.second
    }

    // Stairs and arrow warps fire from the tile the player stands on - routed through the same
    // per-region rulebook as the NDS regions (warp-rules.txt overrides without a recompile).
    val gbaRegion = currentMap.regionId.toInt()
    val standingBehavior = currentMap.tileAt(fromX, fromY)?.behavior
    val standingRule =
        warpRules.forTile(
            gbaRegion, currentMap.bankId.toInt(), currentMap.mapId.toInt(), fromX, fromY)
            ?: standingBehavior?.let { warpRules.forName(gbaRegion, it.name) }
    if (standingRule?.fire == WarpRules.Fire.STAND && standingRule.press == msg.direction) {
      val onTileWarp = currentMap.warps.find { it.x == fromX && it.y == fromY }
      if (onTileWarp != null) {
        warpService.executeWarp(ctx, charId, onTileWarp)
        return
      }
    }

    // Walking off the edge of a map hands the player to the neighbouring map, if there is one.
    if (toX !in 0 until currentMap.width || toY !in 0 until currentMap.height) {
      val connection = currentMap.connections.find { it.direction == msg.direction }
      // Connections stay inside one region.
      val targetMap =
          connection?.let {
            mapManager.getMap(currentMap.regionId, it.targetBank.toByte(), it.targetMap.toByte())
          }
      if (connection == null || targetMap == null) {
        bonk(ctx, charId, state, msg.direction)
        return
      }
      val entryX =
          when (msg.direction) {
            Direction.LEFT -> targetMap.width - 1
            Direction.RIGHT -> 0
            else -> (fromX - connection.unknown).coerceIn(0, targetMap.width - 1)
          }
      val entryY =
          when (msg.direction) {
            Direction.UP -> targetMap.height - 1
            Direction.DOWN -> 0
            else -> (fromY - connection.unknown).coerceIn(0, targetMap.height - 1)
          }
      // The landing tile must be walkable. Pallet Town's bottom row is open across its width but
      // Route 21's top row is a fence with one gap: the client bonks on the fence while the server
      // crossed the seam anyway, snapping NPCs back and desyncing the player (2026-09-07).
      if (!isWalkable(targetMap, entryX, entryY, state.surfing)) {
        bonk(ctx, charId, state, msg.direction)
        return
      }
      edgeTransition(ctx, charId, currentMap.regionId, connection, entryX.toByte(), entryY.toByte())
      return
    }

    // A door only warps when walked into from below, a ladder warps on the step itself.
    // COLLISION GATES THE STEP-ONTO WARPS (vanilla order: an unwalkable tile bonks before any
    // warp logic runs, so a sideways step toward a door tile can never fire it). Animated
    // doors are the one exception - their tiles are impassable and vanilla's door sequence
    // deliberately bypasses collision for the up-press entry.
    val targetBehavior = currentMap.tileAt(toX, toY)?.behavior
    val targetRule =
        warpRules.forTile(gbaRegion, currentMap.bankId.toInt(), currentMap.mapId.toInt(), toX, toY)
            ?: targetBehavior?.let { warpRules.forName(gbaRegion, it.name) }
    val stepsIntoWarp =
        when (targetRule?.fire) {
          WarpRules.Fire.STEP -> targetRule.press == msg.direction
          WarpRules.Fire.CONTACT -> state.creative || isWalkable(currentMap, toX, toY, state.surfing, state.tileOverrides)
          // STAND fixtures never fire from the step toward them; no rule = not a warp fixture.
          WarpRules.Fire.STAND,
          null -> false
        }
    val warp =
        if (stepsIntoWarp) currentMap.warps.find { w -> w.x == toX && w.y == toY }
        else if (targetRule == null && msg.direction == Direction.UP && !isWalkable(currentMap, toX, toY, state.surfing, state.tileOverrides))
        // The side tiles of a wide door (Rocket Hideout's elevator, three warps on an impassable
        // row with one DOOR tile in the middle) carry the ROM's warp events too: a walk-up into
        // any of them is the door.
        currentMap.warps.find { w -> w.x == toX && w.y == toY }
        else null
    if (warp != null) {
      log.info { "WARP at ($toX, $toY) facing ${msg.direction}" }
      warpService.executeWarp(ctx, charId, warp)
      return
    }

    // Waterfall: pushing up into the fall while surfing is the ROM's prompt, not a step. The
    // script either climbs (the field effect walks the surfer up) or explains the wall of water.
    if (state.surfing && msg.direction == Direction.UP && targetBehavior == TileBehavior.WATERFALL && !state.creative) {
      sendPositionReset(ctx, charId, currentMap, fromX, fromY, msg.direction)
      val hoenn = Region.byId(state.regionId) == Region.HOENN
      val label =
          if (FieldMoves.badgeHeld(stored, state.regionId, FieldMoves.WATERFALL)) {
            if (hoenn) "EventScript_UseWaterfall" else "EventScript_Waterfall"
          } else if (hoenn) "EventScript_CannotUseWaterfall" else "EventScript_CantUseWaterfall"
      runFieldScript(ctx, state, label)
      return
    }
    // Strength: a pushable boulder in the way slides one tile on, when that tile is free.
    pushBoulder(ctx, charId, stored, state, currentMap, toX, toY, msg.direction)

    // Creative admins walk through anything; the client shows the wall, the server allows it.
    if (!state.creative && !isWalkable(currentMap, toX, toY, state.surfing, state.tileOverrides)) {
      log.debug { "WALL: char=$charId blocked at ($toX, $toY)" }
      if (currentMap.warps.any { it.x == toX && it.y == toY }) {
        log.info { "WALL onto warp tile ($toX, $toY) behavior=$targetBehavior rule=${targetRule?.fire} dir=${msg.direction}" }
      }
      bonk(ctx, charId, state, msg.direction)
      return
    }

    characterStore.updatePosition(charId, toX.toShort(), toY.toShort(), facing = msg.direction)
    state.x = toX.toShort()
    state.y = toY.toShort()
    rememberTile(state, currentMap, toX, toY)
    dismountIfAshore(ctx, charId, state, currentMap, toX, toY)
    if (startSpin(ctx, charId, state, currentMap, toX, toY)) return

    // The client already walked itself there, so only the observers need telling.
    presenceService.broadcastToObservers(
        ctx,
        gbaMovePacket(charId, currentMap, toX, toY, msg.direction),
    )

    // An admin-placed warp on the destination tile fires like a map's own warp would.
    if (executeCustomWarp(ctx, charId, state.regionId, state.bankId, state.mapId, toX, toY)) {
      return
    }

    // Story coordinate events take precedence over random encounters on the same step. Creative
    // mode meets nothing - a world builder mid-placement does not want a Zubat.
    if (!state.creative && !mapScriptService.onStep(ctx, state, currentMap, toX, toY)) {
      // A trainer whose gaze crosses the landing tile approaches and battles; the encounter
      // roll is skipped for that step, like vanilla.
      if (trainerSight.onStep(ctx, state, currentMap, toX, toY)) return
      encounterService.onStep(ctx, charId, currentMap, toX, toY)
    }
  }

  /** Runs the admin-placed warp on this tile, if any; true when one fired. */
  private fun executeCustomWarp(
      ctx: SessionContext,
      charId: Long,
      region: Int,
      bank: Int,
      map: Int,
      x: Int,
      y: Int,
  ): Boolean {
    val warp = customWarps.at(region, bank, map, x, y) ?: return false
    val hosted = mapManager.getMap(warp.destRegion, warp.destBank, warp.destMap)
    if (hosted != null) {
      warpService.executeWarp(
          ctx,
          charId,
          de.fiereu.openmmo.maps.WarpTile(
              x = x,
              y = y,
              targetRegionId = warp.destRegion.toByte(),
              targetBankId = warp.destBank.toByte(),
              targetMapId = warp.destMap.toByte(),
              targetX = warp.destX,
              targetY = warp.destY,
          ),
      )
    } else {
      warpService.executeRawWarp(
          ctx, charId, warp.destRegion, warp.destBank, warp.destMap, warp.destX, warp.destY)
    }
    return true
  }

  /** Snap the client back to the position the server considers authoritative. */
  private fun sendPositionReset(
      ctx: SessionContext,
      charId: Long,
      map: MapDef,
      x: Int,
      y: Int,
      direction: Direction,
  ) {
    ctx.send(gbaMovePacket(charId, map, x, y, direction))
  }

  private fun gbaMovePacket(
      charId: Long,
      map: MapDef,
      x: Int,
      y: Int,
      direction: Direction,
  ): GbaEntityMovePacket =
      GbaEntityMovePacket(
          entityId = charId,
          bankId = map.bankId.toInt() and 0xff,
          mapId = map.mapId.toInt() and 0xff,
          x = x,
          y = y,
          movementMode = 2,
          direction = direction,
      )

  /** Turning in place. Only observers need it, the client has already turned itself. */
  fun onFaceDirection(event: PacketEvent<FaceDirectionPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    // A turn that raced the arrival lock is dropped without a correction - a face action sent
    // now would join the queue the emergence walk owns and disturb it; the walk sets the
    // facing itself anyway.
    if (System.currentTimeMillis() < state.moveIgnoreUntil) return
    if (state.blocksPlayerInput) {
      // The client already turned itself; a position reset does NOT override the local player's
      // facing (proved during the Oak scene). The scripted face action does - it seizes the
      // movement controller and turns the sprite back to the facing the script holds.
      scriptMovement.reassertScriptedFacing(ctx, state)
      return
    }
    val msg = event.packet
    state.facingDirection = msg.direction
    characterStore.updatePosition(charId, state.x, state.y, facing = msg.direction)

    presenceService.broadcastToObservers(
        ctx,
        EntityFaceTurnPacket(entityId = charId, facing = msg.direction.ordinal.toByte()),
    )
  }

  /** Runs a shared ROM field script (Surf, Waterfall, Dive prompts) for this region's game. */
  private fun runFieldScript(ctx: SessionContext, state: PlayerState, label: String) {
    val registry = scriptRegistry ?: return
    val runner = scriptRunner ?: return
    val script =
        try {
          registry.forLabel(label, gbaScriptSource(state.regionId))
        } catch (e: ScriptResolutionException) {
          log.info { "Field script $label unavailable: ${e.message}" }
          null
        } ?: return
    runner.get().run(ctx, state, script, entityId = -1)
  }

  /**
   * Strength's boulder push (the GBA engine's, not a script's): with FLAG_SYS_USE_STRENGTH set
   * for this region, a pushable boulder on the tile being stepped onto moves one tile further in
   * the walking direction if that tile is walkable and no other object stands there. The new
   * place rides the same story var setobjectxyperm uses, which the map reset clears on the way
   * out - boulders reset when the map reloads, as on the cartridge. True when a boulder moved.
   */
  private fun pushBoulder(
      ctx: SessionContext,
      charId: Long,
      stored: StoredCharacter,
      state: PlayerState,
      map: MapDef,
      toX: Int,
      toY: Int,
      direction: Direction,
  ): Boolean {
    val region = map.regionId.toInt()
    val bank = map.bankId.toInt()
    val mapId = map.mapId.toInt()
    val namespace = Region.byId(region)?.name?.lowercase() ?: return false
    if ("$namespace/FLAG_SYS_USE_STRENGTH" !in stored.storyFlags) return false
    val placed = map.npcs.map { npcService.effectiveNpc(region, bank, mapId, it, stored.storyFlags, stored.storyVars) }
    val boulder =
        placed.firstOrNull {
          it.script == "EventScript_StrengthBoulder" && it.x == toX && it.y == toY && (it.hideFlag.isEmpty() || it.hideFlag !in stored.storyFlags)
        } ?: return false
    val beyondX = toX + direction.dx
    val beyondY = toY + direction.dy
    if (!isWalkable(map, beyondX, beyondY, overrides = state.tileOverrides)) return false
    if (placed.any { it !== boulder && it.x == beyondX && it.y == beyondY && (it.hideFlag.isEmpty() || it.hideFlag !in stored.storyFlags) }) return false
    characterStore.setStoryVar(charId, npcService.xyOverrideKey(region, bank, mapId, boulder.entityIdx), (beyondX shl 12) or beyondY)
    npcService.repositionNpc(ctx, region, bank, mapId, boulder.entityIdx, beyondX, beyondY)
    log.info { "Strength: char=$charId pushed boulder ${boulder.entityIdx} to ($beyondX, $beyondY)" }
    return true
  }

  /**
   * The client says it stands on a tile other than the server's. Free movement is the client's:
   * at bike speed with a round trip in between, the two drift by a tile or three through steps the
   * client announced and cancelled, packets in flight after a reset, and bonks the server does not
   * model. The rules, in order, and each one is the cartridge's outcome for the walk it describes:
   *
   * 1. Inside the window after a desync reset, a claim that still describes the old position is
   *    dropped: the client has not seen the reset yet, and answering again only snaps it twice.
   * 2. A claim naming a tile the server itself committed just now is a phantom-step rewind: the
   *    server goes back there without hooks - it ran that tile's hooks once already.
   * 3. A claim a few walkable tiles away is a walk the server missed: it walks the shortest path
   *    itself, tile by tile, through the same hooks a reported step runs - fixture warps, custom
   *    warps, coordinate triggers, trainer sight, encounters. The first hook that fires stops the
   *    walk there, and the client is put back on that tile: the only reset free movement gets,
   *    because a script really did stop the player.
   * 4. Anything else - out of bounds, through a wall, too far - is a real desync and is reset.
   *
   * True when the caller may go on processing the step from the server's (possibly moved)
   * position; false when the packet is consumed (dropped, reset, or a hook took over).
   */
  private fun reconcileClaim(
      ctx: SessionContext,
      charId: Long,
      state: PlayerState,
      map: MapDef,
      fromX: Int,
      fromY: Int,
      msg: MovementPacket,
  ): Boolean {
    val now = System.currentTimeMillis()
    if (now < state.claimIgnoreUntil) {
      val nearReset = Math.abs(msg.x - state.lastResetX) + Math.abs(msg.y - state.lastResetY) <= 1
      if (!nearReset) {
        log.debug { "Dropping pre-reset claim (${msg.x}, ${msg.y}) from char=$charId" }
        return false
      }
    }
    if (msg.x !in 0 until map.width || msg.y !in 0 until map.height) {
      desyncReset(ctx, charId, state, map, fromX, fromY, msg)
      return false
    }
    val claim = packTile(msg.x, msg.y)
    if (state.recentTilesMapKey == mapKey(map)) {
      val index = state.recentTiles.indexOf(claim)
      if (index >= 0) {
        while (state.recentTiles.size > index + 1) state.recentTiles.removeLast()
        log.info { "Rewind: char=$charId from ($fromX, $fromY) back to (${msg.x}, ${msg.y})" }
        characterStore.updatePosition(charId, msg.x.toShort(), msg.y.toShort())
        state.x = msg.x.toShort()
        state.y = msg.y.toShort()
        presenceService.broadcastToObservers(ctx, gbaMovePacket(charId, map, msg.x, msg.y, state.facingDirection))
        return true
      }
    }
    val path = walkablePath(map, state, fromX, fromY, msg.x, msg.y)
    if (path == null) {
      desyncReset(ctx, charId, state, map, fromX, fromY, msg)
      return false
    }
    log.info { "Catch-up: char=$charId walks ${path.size} tile(s) from ($fromX, $fromY) to (${msg.x}, ${msg.y})" }
    var x = fromX
    var y = fromY
    for ((nx, ny, dir) in path) {
      // A fixture warp on the way fires the way a reported step into it would (STEP with the
      // press, CONTACT on any walkable entry); the warp takes over from here.
      val region = map.regionId.toInt()
      val behavior = map.tileAt(nx, ny)?.behavior
      val rule =
          warpRules.forTile(region, map.bankId.toInt(), map.mapId.toInt(), nx, ny)
              ?: behavior?.let { warpRules.forName(region, it.name) }
      val stepsIntoWarp =
          when (rule?.fire) {
            WarpRules.Fire.STEP -> rule.press == dir
            WarpRules.Fire.CONTACT -> true
            else -> false
          }
      val warp = if (!stepsIntoWarp) null else map.warps.find { it.x == nx && it.y == ny }
      if (warp != null) {
        log.info { "WARP at ($nx, $ny) facing $dir (catch-up)" }
        warpService.executeWarp(ctx, charId, warp)
        return false
      }
      x = nx
      y = ny
      state.facingDirection = dir
      characterStore.updatePosition(charId, x.toShort(), y.toShort(), facing = dir)
      state.x = x.toShort()
      state.y = y.toShort()
      rememberTile(state, map, x, y)
      dismountIfAshore(ctx, charId, state, map, x, y)
      presenceService.broadcastToObservers(ctx, gbaMovePacket(charId, map, x, y, dir))
      if (executeCustomWarp(ctx, charId, state.regionId, state.bankId, state.mapId, x, y)) return false
      if (state.creative) continue
      val stopped =
          mapScriptService.onStep(ctx, state, map, x, y) ||
              trainerSight.onStep(ctx, state, map, x, y) ||
              run {
                encounterService.onStep(ctx, charId, map, x, y)
                state.encounterHold
              }
      if (stopped) {
        // The encounter freeze places the player itself; a script or a trainer only removes
        // input, so the client is put back on the tile the hook fired on.
        if (!state.encounterHold) sendPositionReset(ctx, charId, map, x, y, dir)
        log.info { "Catch-up stopped at ($x, $y) for char=$charId: a hook took the player" }
        return false
      }
    }
    return true
  }

  /**
   * The client walked into something and stayed put; it reported the attempt (every bonk arrives
   * as a step packet from the same tile). It already stands where the server has it, so nothing
   * is sent back to it - the self-move packet it used to receive was the hitch on every wall,
   * worst on a bike (2026-09-08). Observers see the turn toward the obstacle.
   */
  private fun bonk(ctx: SessionContext, charId: Long, state: PlayerState, direction: Direction) {
    state.facingDirection = direction
    presenceService.broadcastToObservers(
        ctx,
        EntityFaceTurnPacket(entityId = charId, facing = direction.ordinal.toByte()),
    )
  }

  /**
   * Spin tiles (Rocket Hideout's arrow floor). The cartridge (field_player_avatar.c) spins the
   * player along from a spin tile: each step goes the way the last spin tile pointed, a new spin
   * tile turns the spin, a STOP_SPINNING tile ends it, and so does a wall. The client knows
   * nothing of these tiles, so the server drives the slide as a scripted walk with input held,
   * and the position commits at the end. Returns true when a slide started for this step.
   */
  private fun startSpin(ctx: SessionContext, charId: Long, state: PlayerState, map: MapDef, x: Int, y: Int): Boolean {
    var direction = map.tileAt(x, y)?.behavior?.spinDirection ?: return false
    if (state.spinning) return false
    val steps = mutableListOf<MovementStep>()
    var cx = x
    var cy = y
    while (steps.size < MAX_SPIN_STEPS) {
      val nx = cx + direction.dx
      val ny = cy + direction.dy
      if (!isWalkable(map, nx, ny, state.surfing, state.tileOverrides)) break
      steps += spinStep(direction)
      cx = nx
      cy = ny
      val behavior = map.tileAt(cx, cy)?.behavior ?: break
      if (behavior == TileBehavior.STOP_SPINNING) break
      behavior.spinDirection?.let { direction = it }
    }
    if (steps.isEmpty()) return false
    log.info { "[Spin] char=$charId from ($x, $y) ${steps.size} steps to ($cx, $cy)" }
    state.spinning = true
    scope.launch {
      try {
        scriptMovement.holdPlayer(ctx, state)
        scriptMovement.moveSelf(ctx, state, steps)
      } catch (e: Exception) {
        log.warn(e) { "[Spin] char=$charId slide failed" }
      } finally {
        state.spinning = false
        scriptMovement.releasePlayerHold(ctx, state)
      }
    }
    return true
  }

  private fun spinStep(direction: Direction): MovementStep =
      when (direction) {
        Direction.UP -> MovementStep.FAST_UP
        Direction.DOWN -> MovementStep.FAST_DOWN
        Direction.LEFT -> MovementStep.FAST_LEFT
        Direction.RIGHT -> MovementStep.FAST_RIGHT
        else -> MovementStep.FAST_DOWN
      }

  /** A real desync: the server's tile is re-asserted and the echo window opens. */
  private fun desyncReset(
      ctx: SessionContext,
      charId: Long,
      state: PlayerState,
      map: MapDef,
      fromX: Int,
      fromY: Int,
      msg: MovementPacket,
  ) {
    log.info {
      "DESYNC: char=$charId claims (${msg.x}, ${msg.y}), server has ($fromX, $fromY) on " +
          "${state.regionId}:${state.bankId}:${state.mapId}, resetting"
    }
    state.claimIgnoreUntil = System.currentTimeMillis() + RESET_ECHO_MILLIS
    state.lastResetX = fromX
    state.lastResetY = fromY
    sendPositionReset(ctx, charId, map, fromX, fromY, msg.direction)
  }

  /**
   * The shortest walkable path from (fromX, fromY) to (toX, toY) as (x, y, direction) steps,
   * ledge hops included, or null when none exists within [CATCH_UP_TILES] steps.
   */
  private fun walkablePath(
      map: MapDef,
      state: PlayerState,
      fromX: Int,
      fromY: Int,
      toX: Int,
      toY: Int,
  ): List<Triple<Int, Int, Direction>>? {
    if (Math.abs(toX - fromX) + Math.abs(toY - fromY) > CATCH_UP_TILES) return null
    val start = packTile(fromX, fromY)
    val goal = packTile(toX, toY)
    val cameFrom = HashMap<Int, Pair<Int, Direction>>()
    val depth = HashMap<Int, Int>()
    val queue = ArrayDeque<Int>()
    queue.add(start)
    depth[start] = 0
    while (queue.isNotEmpty()) {
      val here = queue.removeFirst()
      if (here == goal) break
      val d = depth.getValue(here)
      if (d >= CATCH_UP_TILES) continue
      val hx = here shr 16
      val hy = here and 0xFFFF
      for (dir in listOf(Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT)) {
        val landing = ledgeLanding(map, hx, hy, dir) ?: (hx + dir.dx to hy + dir.dy)
        val (nx, ny) = landing
        if (!isWalkable(map, nx, ny, state.surfing, state.tileOverrides)) continue
        val key = packTile(nx, ny)
        if (key in depth) continue
        depth[key] = d + 1
        cameFrom[key] = here to dir
        queue.add(key)
      }
    }
    if (goal !in depth) return null
    val path = ArrayList<Triple<Int, Int, Direction>>()
    var cursor = goal
    while (cursor != start) {
      val (prev, dir) = cameFrom.getValue(cursor)
      path.add(Triple(cursor shr 16, cursor and 0xFFFF, dir))
      cursor = prev
    }
    path.reverse()
    return path
  }

  private fun rememberTile(state: PlayerState, map: MapDef, x: Int, y: Int) {
    val key = mapKey(map)
    if (state.recentTilesMapKey != key) {
      state.recentTiles.clear()
      state.recentTilesMapKey = key
    }
    state.recentTiles.addLast(packTile(x, y))
    while (state.recentTiles.size > RECENT_TILES) state.recentTiles.removeFirst()
  }

  private fun packTile(x: Int, y: Int): Int = (x shl 16) or (y and 0xFFFF)

  private fun mapKey(map: MapDef): Long =
      (map.regionId.toLong() and 0xFF shl 40) or (map.bankId.toLong() and 0xFF shl 20) or (map.mapId.toLong() and 0xFF)

  private fun isWalkable(
      map: MapDef,
      x: Int,
      y: Int,
      surfing: Boolean = false,
      overrides: Map<Int, de.fiereu.openmmo.common.Tile2D> = emptyMap(),
  ): Boolean {
    if (x !in 0 until map.width || y !in 0 until map.height) return false
    // A tile a script replaced (setmetatile) carries its own collision until the map reloads.
    val tile = overrides[(x shl 16) or (y and 0xFFFF)] ?: map.tileAt(x, y) ?: return true
    // Water blocks feet and carries a surfer.
    if (tile.behavior.isSurfable) return surfing
    return !tile.blocksMovement()
  }

  /** Stepping from water onto land ends the surf: the client's own bit clears the same way. */
  private fun dismountIfAshore(ctx: SessionContext, charId: Long, state: PlayerState, map: MapDef, x: Int, y: Int) {
    if (!state.surfing) return
    // Under the surface the whole floor is ridden; only Dive's emerge ends that.
    if (state.underwater) return
    if (map.tileAt(x, y)?.behavior?.isSurfable == true) return
    state.surfing = false
    ctx.send(de.fiereu.openmmo.net.game.packets.EntityTransportationPacket(charId, 0))
    log.info { "Surf ended for char=$charId at ($x, $y)" }
  }

  private fun edgeTransition(
      ctx: SessionContext,
      charId: Long,
      regionId: Byte,
      connection: MapData.GbaConnection,
      targetX: Byte,
      targetY: Byte,
  ) {
    val targetBank = connection.targetBank.toByte()
    val targetMap = connection.targetMap.toByte()
    val map = mapManager.getMap(regionId, targetBank, targetMap) ?: return

    val state = ctx.attributes[PLAYER_STATE]
    if (state != null) {
      state.bankId = targetBank.toInt()
      state.mapId = targetMap.toInt()
      state.x = targetX.toShort()
      state.y = targetY.toShort()
    }
    characterStore.updatePosition(
        charId,
        targetX.toShort(),
        targetY.toShort(),
        targetBank,
        targetMap,
    )
    characterStore.flushCharacterAsync(charId)
    presenceService.refresh(ctx)

    mapLoadService.preloadConnectedMaps(ctx, map, depth = 1)
    npcService.spawnNpcsWithNeighbors(ctx, targetBank.toInt(), targetMap.toInt(), regionId.toInt())

    if (state != null) mapScriptService.onMapEnter(ctx, state, map)

    log.info { "Player $charId edge-transitioned to bank=$targetBank map=$targetMap" }
  }

  /**
   * The GBA whiteout return: warp to the spot that last healed the party (recorded by the nurse
   * heal), or the region's new-game start when nothing has healed the player yet. The caller heals
   * the party first.
   */
  fun respawnAfterWhiteout(ctx: SessionContext, state: PlayerState) {
    val charId = state.characterId ?: return
    val stored = characterStore.getCharacter(charId) ?: return
    val packedMap = stored.storyVars[RespawnPoint.MAP_KEY]
    val packedXy = stored.storyVars[RespawnPoint.XY_KEY]

    val target =
        if (packedMap != null && packedXy != null) {
          WarpTile(
              x = state.x.toInt(),
              y = state.y.toInt(),
              targetRegionId = (packedMap shr 16).toByte(),
              targetBankId = ((packedMap shr 8) and 0xFF).toByte(),
              targetMapId = (packedMap and 0xFF).toByte(),
              targetX = packedXy shr 12,
              targetY = packedXy and 0xFFF,
              exitFacing = Direction.DOWN,
          )
        } else {
          val region =
              de.fiereu.openmmo.common.enums.Region.byId(stored.info.positionRegionId.toInt())
                  ?: return
          val start =
              de.fiereu.openmmo.server.game.storage.NewGameStarts.forRegion(region, female = false)
          WarpTile(
              x = state.x.toInt(),
              y = state.y.toInt(),
              targetRegionId = region.wireValue,
              targetBankId = start.bankId,
              targetMapId = start.mapId,
              targetX = start.x.toInt(),
              targetY = start.y.toInt(),
              exitFacing = Direction.DOWN,
          )
        }
    log.info { "Whiteout: respawning $charId at ${target.targetBankId}:${target.targetMapId}" }
    // NDS regions have no hosted maps; a raw warp is how the client is moved there. This is also
    // the only exit from the Elite Four rooms besides winning, so it must work everywhere.
    val hosted =
        mapManager.getMap(target.targetRegionId, target.targetBankId, target.targetMapId) != null
    if (hosted) {
      warpService.executeWarp(ctx, charId, target)
    } else {
      warpService.executeRawWarp(
          ctx,
          charId,
          target.targetRegionId.toInt(),
          target.targetBankId.toInt(),
          target.targetMapId.toInt(),
          target.targetX,
          target.targetY,
      )
    }
  }
}
