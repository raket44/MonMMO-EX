package de.fiereu.openmmo.server.game.battle

/**
 * Field weather. [wireValue] is what the client's weather event (f/W31, sub-event 12) reads
 * through its weather enum's byte lookup (f/xG1.U00). The enum carries eight constants with
 * bytes 0, 1, 2, 3, 5, 6, 10 and one more; 2 and 10 are its two "damaging" weathers. Rain 1,
 * sandstorm 2, sun 3 and hail 10 is the working assignment until the in-game text confirms it.
 */
enum class Weather(val wireValue: Int) {
  RAIN(1),
  SANDSTORM(2),
  SUN(3),
  HAIL(10),
}

/** Per-side field effects: screens, Safeguard and Mist, each as turns remaining. */
class SideState {
  var reflectTurns = 0
  var lightScreenTurns = 0
  var safeguardTurns = 0
  var mistTurns = 0

  fun tick() {
    if (reflectTurns > 0) reflectTurns--
    if (lightScreenTurns > 0) lightScreenTurns--
    if (safeguardTurns > 0) safeguardTurns--
    if (mistTurns > 0) mistTurns--
  }
}
