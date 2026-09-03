package de.fiereu.openmmo.server.game.session

import de.fiereu.openmmo.common.enums.Direction
import java.util.concurrent.ConcurrentHashMap

enum class ScriptLockScope {
  NONE,
  LOCAL,
  ALL,
}

enum class DialogMessageMode {
  NORMAL,
  SIGN,
}

enum class DialogTextColor(val gbaValue: Int) {
  MALE(0),
  FEMALE(1),
  MON(2),
  NEUTRAL(3),
  DEFAULT(0xFF),
  ;

  companion object {
    fun fromGbaValue(value: Int): DialogTextColor? = entries.firstOrNull { it.gbaValue == value }
  }
}

/**
 * Per-session player state. Scripts run on their own coroutine while packets are answered on the
 * mailbox coroutine, and both threads touch every field here, so all of them are volatile.
 */
data class PlayerState(
    val userId: Int,
    @field:Volatile var characterId: Long? = null,
    @field:Volatile var justWarped: Boolean = false,
    @field:Volatile var facingDirection: Direction = Direction.DOWN,
    @field:Volatile var dialogVisible: Boolean = false,
    @field:Volatile var scriptRunning: Boolean = false,
    @field:Volatile var scriptLockScope: ScriptLockScope = ScriptLockScope.NONE,
    @field:Volatile var scriptLockedEntityId: Long? = null,
    @field:Volatile var dialogNpcEntityId: Long = 0,
    @field:Volatile var dialogSeqId: Int = 0,
    /** FireRed presentation state; the current PokeMMO packet has no text-color field. */
    @field:Volatile var dialogMessageMode: DialogMessageMode = DialogMessageMode.NORMAL,
    @field:Volatile var dialogTextColor: DialogTextColor = DialogTextColor.DEFAULT,
    @field:Volatile var regionId: Int = 1,
    @field:Volatile var bankId: Int = 51,
    @field:Volatile var mapId: Int = 3,
    @field:Volatile var x: Short = 4,
    @field:Volatile var y: Short = 2,
    @field:Volatile var elevation: Int = 0,
    /** Trusts one source tile after scripted movement. */
    @field:Volatile var acceptNextMoveSource: Boolean = false,
    /**
     * The client's 0x2A story-flag handler silently DROPS updates until its game state exists
     * (f/eO0.X91 bails on a null Sw()), so the login-time world-state block never lands - badges
     * looked reset on every relog. The first RequestPlayer re-sends the flags once, in-world.
     */
    @field:Volatile var storyFlagsSynced: Boolean = false,
    /**
     * Set after an NDS warp or teleport whose arrival tile is itself a warp (the norm: paired doors
     * land on the partner's mat). Warps stay quiet until the player reports one position with no
     * warp under it, which stops arrive-and-bounce loops on wide warp boxes.
     */
    @field:Volatile var ndsWarpGuard: Boolean = false,
    /**
     * Rail line of the last NDS warp destination (-1 = not a rail). Sent with the next LoadEntity
     * so the client attaches the player (and its camera) to the Gen 5 rail system - without it,
     * rail maps are a blue void with a frozen player.
     */
    @field:Volatile var pendingRailLine: Int = -1,
    /**
     * Developer probe: transportation byte for the player's own LoadEntity (0 = the normal walk
     * state). Set by /probe transport; applied on every arrival until changed, because the client
     * ignores a re-sent LoadEntity for an entity it already has - only a fresh spawn shows it.
     */
    @field:Volatile var transportOverride: Int = 0,
    /**
     * Live mount, session-scoped: LoadEntity's flags&0x02 pair (type byte, id short) reaches
     * f.E41.mX1 which attaches/detaches the mount renderer even on the EXISTING player. The
     * bicycle's mount id is 285 (the client's hardcoded wheel-animation case); -1 = on foot.
     */
    @field:Volatile var mountType: Int = -1,
    @field:Volatile var mountId: Int = -1,
    /**
     * On the bike right now. The visual is two decoded pieces: the BIKE skin slot supplies the
     * drawn bike (type 0 = Red Bicycle, strings 31000+), and transportation bit 1 (f.ti.aU1, gate
     * f.ti.U7) switches the player to the mounted frame set. Bit 1 is toggled LIVE with
     * EntityTransportationPacket (0x28 -> f.tS1.D40) - no respawn, no map reload. Warps clear this:
     * doors lead indoors, and vanilla kicks you off the bike at the doorway.
     */
    @field:Volatile var riding: Boolean = false,
    /**
     * The emergence step: after this arrival's LoadEntity, the server sends an EntityMove one tile
     * in this direction and the CLIENT walks it - the client's 0xE4 handler routes any entity, the
     * local player included, through its animated movement path (f.pC -> f.NV0.AX1). This is what
     * plays the vanilla door walk-out instead of teleport-placing past the mat.
     */
    @field:Volatile var pendingStepDir: Direction? = null,
    /**
     * How the player walked through the last NDS door, and from which map: leaving that same
     * building again mirrors it - entered walking left, walk out facing right.
     */
    @field:Volatile var lastWarpEntryDir: Direction? = null,
    @field:Volatile var lastWarpFromBank: Int = -1,
    @field:Volatile var lastWarpFromMap: Int = -1,
    /**
     * The tile the player stood on when the last NDS warp fired. On RAIL maps this is the only
     * trustworthy emergence target for the return trip: rail coordinates are arc-space, so a dx/dy
     * guess walks the wrong way (below the Castelia Pokecenter mat (8,3) is (8,2), not (8,4)) - but
     * the tile they entered from is right by construction.
     */
    @field:Volatile var lastWarpFromX: Int = -1,
    @field:Volatile var lastWarpFromY: Int = -1,
    /** Explicit emergence-step target; -1 = derive from [pendingStepDir] instead. */
    @field:Volatile var pendingStepX: Int = -1,
    @field:Volatile var pendingStepY: Int = -1,
    /**
     * Movement reports are DROPPED until this clock time (epoch ms): the arrival-step choreography
     * window. Stale client moves that raced the input-lock packet used to land here and fire fresh
     * warps off tiles the player never really stood on.
     */
    @field:Volatile var moveIgnoreUntil: Long = 0,
    /**
     * Approach tile per door, session-scoped: key packs (region, map, partner-door address), the
     * value packs the tile the player stood on when that warp fired. Any later arrival through the
     * same pairing steps to exactly that tile - the only target that is right by construction on
     * rail maps, where coordinates are arc-space and dx/dy guesses walk the wrong way. Written on
     * every NDS warp fire, read on every arrival that wants a step.
     */
    val doorApproach: MutableMap<Long, Long> = ConcurrentHashMap(),
    /**
     * A script is warping and will run the destination's entry scripts itself. The arrival must not
     * start a second copy; [scriptRunning] cannot distinguish that owned entry from an unrelated
     * attempt to start another script.
     */
    @field:Volatile var scriptOwnsMapEntry: Boolean = false,
    /**
     * Creative mode for world-building admins: collision and wild encounters are skipped, so
     * walking anywhere to place warps is unobstructed. Toggled by /gm, developer-gated.
     */
    @field:Volatile var creative: Boolean = false,
    /** Where /warp from was called, pending its /warp to. Region, bank, map, x, y. */
    @field:Volatile var pendingWarpSource: IntArray? = null,
    /**
     * Developer experiment: suppress all map-npc spawn packets for this session. If a map still
     * shows people with this on, the client populates NPCs itself; if it is empty, they are
     * server-fed. Toggled by /probe npcs.
     */
    @field:Volatile var suppressNpcSpawns: Boolean = false,
    /**
     * When the client should be done ANIMATING the last scripted player movement (epoch ms). The
     * hold-release queue clear must not fire before this: server-side step timing is an estimate,
     * and clearing mid-walk snapped the player to the endpoint (the lab pull-back "poof"). Hold
     * delays are exempt - clearing those instantly is the whole point.
     */
    @field:Volatile var selfActionsEndAt: Long = 0,
    /**
     * Map-directory tour: the server auto-warps through raw map ids and the player's next plain
     * chat line names the map on screen. Chat is captured, not broadcast, while this is on.
     */
    @field:Volatile var touring: Boolean = false,
    /** Maps the client already holds. A warp sends deleteCache, which empties this. */
    val loadedMaps: MutableSet<Int> = ConcurrentHashMap.newKeySet(),
    /**
     * The map whose entry scripts already ran for this arrival. The client re-requests its player
     * several times while loading an outdoor map (once per connection), and each request used to
     * re-run the ON_TRANSITION script and re-take the script lock - one logical arrival must run
     * its entry scripts once.
     */
    @field:Volatile var entryScriptsMapKey: Long = -1,
    /**
     * Evolutions offered to the client (s2c 0x18) and not yet answered, keyed by monster uid. The
     * species only changes when the client's evolution cinematic finishes and it answers c2s 0x0B
     * with accepted=true; a cancel drops the entry and leaves the monster as it was.
     */
    val pendingEvolutions: MutableMap<Long, PendingEvolution> = ConcurrentHashMap(),
    /**
     * The party monster chosen as the overworld follower ("Set X as Follower", c2s 0x11), by uid;
     * null follows the lead slot. Session-scoped, like the client's own choice.
     */
    @field:Volatile var followerMonId: Long? = null,
) {
  val blocksPlayerInput: Boolean
    get() = dialogVisible || scriptLockScope != ScriptLockScope.NONE

  val blocksNewScript: Boolean
    get() = dialogVisible || scriptRunning

  fun lockLocal(entityId: Long) {
    scriptLockScope = ScriptLockScope.LOCAL
    scriptLockedEntityId = entityId.takeIf { it >= 0 }
  }

  fun lockAll() {
    scriptLockScope = ScriptLockScope.ALL
    scriptLockedEntityId = null
  }

  fun releaseScriptLock() {
    scriptLockScope = ScriptLockScope.NONE
    scriptLockedEntityId = null
  }
}

/**
 * One offered evolution: the client wire id the monster becomes, and the bag item (a stone) that
 * is consumed once the client confirms - 0 when the trigger was a level.
 */
data class PendingEvolution(val targetWire: Int, val consumeItemId: Int = 0)

/** Packs a map address into one key for [PlayerState.loadedMaps]. */
fun mapCacheKey(regionId: Int, bankId: Int, mapId: Int): Int =
    (regionId shl 16) or (bankId shl 8) or mapId

/**
 * Key for [PlayerState.doorApproach]: the map a warp fires IN plus the partner-door tile its row
 * points at. An arrival looks itself up with its own map plus its landing row's destination - the
 * same pairing seen from the other side, since NDS warps are mat-to-mat.
 */
fun doorApproachKey(
    regionId: Int,
    bankId: Int,
    mapId: Int,
    destBank: Int,
    destMap: Int,
    destX: Int,
    destY: Int,
): Long =
    ((regionId.toLong() and 0xFF) shl 56) or
        ((bankId.toLong() and 0xFF) shl 48) or
        ((mapId.toLong() and 0xFF) shl 40) or
        ((destBank.toLong() and 0xFF) shl 32) or
        ((destMap.toLong() and 0xFF) shl 24) or
        ((destX.toLong() and 0xFFF) shl 12) or
        (destY.toLong() and 0xFFF)
