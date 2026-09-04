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
