package de.fiereu.openmmo.server.game.battle

/**
 * Field weather. [wireValue] is the byte the client's weather event (f/W31, sub-event 12) and
 * the weather-damage line (f/uC, sub-event 13) look up through f/xG1's byte table. Decoded from
 * the client's switch map (f/R11.xS0) against the lines it prints: byte 1 "The harsh sunlight
 * beats down.", 2 and 10 "It is raining.", 3 "The sandstorm rages." (and the sandstorm buffet
 * line), 5 "The fog is thick.", 6 "Snow is falling." - the client's ice weather, which the
 * buffet line calls hail. Byte 0 clears the weather with the matching "stopped" line.
 */
enum class Weather(val wireValue: Int) {
  RAIN(2),
  SANDSTORM(3),
  SUN(1),
  HAIL(6),
}

/** Per-side field effects: screens, Safeguard, Mist, Tailwind and Lucky Chant as turns remaining, plus the entry hazards. */
class SideState {
  var reflectTurns = 0
  var lightScreenTurns = 0
  var safeguardTurns = 0
  var mistTurns = 0
  var tailwindTurns = 0
  var luckyChantTurns = 0
  /** Spikes layers (up to 3) and Toxic Spikes layers (up to 2); Stealth Rock and Sticky Web are on or off. */
  var spikes = 0
  var toxicSpikes = 0
  var stealthRock = false
  var stickyWeb = false
  /** Healing Wish (1) or Lunar Dance (2) waiting for the next monster to come in. */
  var healingWish = 0
  /** A monster on this side fainted during the last turn (Retaliate). */
  var faintedLastTurn = false
  /** A monster on this side used Round earlier this turn, doubling the next one's. */
  var roundUsedThisTurn = false

  fun tick() {
    if (reflectTurns > 0) reflectTurns--
    if (lightScreenTurns > 0) lightScreenTurns--
    if (safeguardTurns > 0) safeguardTurns--
    if (mistTurns > 0) mistTurns--
    if (tailwindTurns > 0) tailwindTurns--
    if (luckyChantTurns > 0) luckyChantTurns--
  }

  /** Court Change: the two sides trade every screen, tailwind and hazard. */
  fun swapWith(other: SideState) {
    fun swapInt(get: (SideState) -> Int, set: SideState.(Int) -> Unit) {
      val mine = get(this)
      set(get(other))
      other.set(mine)
    }
    fun swapBool(get: (SideState) -> Boolean, set: SideState.(Boolean) -> Unit) {
      val mine = get(this)
      set(get(other))
      other.set(mine)
    }
    swapInt({ it.reflectTurns }, { reflectTurns = it })
    swapInt({ it.lightScreenTurns }, { lightScreenTurns = it })
    swapInt({ it.safeguardTurns }, { safeguardTurns = it })
    swapInt({ it.mistTurns }, { mistTurns = it })
    swapInt({ it.tailwindTurns }, { tailwindTurns = it })
    swapInt({ it.luckyChantTurns }, { luckyChantTurns = it })
    swapInt({ it.spikes }, { spikes = it })
    swapInt({ it.toxicSpikes }, { toxicSpikes = it })
    swapBool({ it.stealthRock }, { stealthRock = it })
    swapBool({ it.stickyWeb }, { stickyWeb = it })
  }
}

/** Whole-field effects as turns remaining, and the streaks the field remembers. */
class FieldEffects {
  var trickRoomTurns = 0
  var gravityTurns = 0
  var wonderRoomTurns = 0
  /** Mud Sport weakens Electric moves, Water Sport Fire moves, to a third. */
  var mudSportTurns = 0
  var magicRoomTurns = 0
  /** Electric, Grassy, Misty or Psychic Terrain and its turns left. */
  var terrain: Terrain? = null
  var terrainTurns = 0
  /** The move used just before in this turn (Fusion Flare and Fusion Bolt). */
  var lastMoveThisTurn = 0
  /** Ion Deluge this turn: Normal moves become Electric. */
  var ionDeluge = false
  /** Fairy Lock: nobody may flee until it runs out. */
  var fairyLockTurns = 0
  var waterSportTurns = 0
  /** Turns in a row Echoed Voice was used, which raises its power. */
  var echoedVoice = 0
  var echoedVoiceUsedThisTurn = false
  /** The last move anyone used (Copycat). */
  var lastMoveId = 0

  fun tick() {
    if (trickRoomTurns > 0) trickRoomTurns--
    if (gravityTurns > 0) gravityTurns--
    if (wonderRoomTurns > 0) wonderRoomTurns--
    if (mudSportTurns > 0) mudSportTurns--
    if (magicRoomTurns > 0) magicRoomTurns--
    if (fairyLockTurns > 0) fairyLockTurns--
    if (terrainTurns > 0 && --terrainTurns == 0) terrain = null
    if (waterSportTurns > 0) waterSportTurns--
    echoedVoice = if (echoedVoiceUsedThisTurn) (echoedVoice + 1).coerceAtMost(4) else 0
    echoedVoiceUsedThisTurn = false
  }
}

/** A move ended the battle outright: Roar or Dragon Tail in a wild battle, or Teleport. */
enum class MoveEnding {
  BLOWN_AWAY,
  PLAYER_FLED,
  WILD_FLED,
}

/** The player owes a pick for [position] before the paused turn goes on (U-turn, Baton Pass). */
class SelfSwitch(val position: Int, val batonPass: Boolean)

enum class Terrain {
  ELECTRIC,
  GRASSY,
  MISTY,
  PSYCHIC,
}

/** Future Sight and Doom Desire: aimed at a field position, landing when [turns] runs out. */
class DelayedAttack(
    val attacker: BattleMonState,
    val side: Int,
    val position: Int,
    val move: de.fiereu.openmmo.moves.MoveDef,
    var turns: Int,
)
