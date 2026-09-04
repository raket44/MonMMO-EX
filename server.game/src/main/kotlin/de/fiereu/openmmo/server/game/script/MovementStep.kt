package de.fiereu.openmmo.server.game.script

import de.fiereu.openmmo.common.enums.Direction

/**
 * One step of an applymovement sequence: either a walk one tile in [direction] or a turn in place
 * to face [direction]. [action] is the client movement-action byte (from packet captures: face
 * 0x00-0x03, walk 0x10-0x13); a whole sequence of these is sent in one movement packet. This is the
 * small game agnostic vocabulary scripts use.
 */
enum class MovementStep(
    val direction: Direction,
    val walks: Boolean,
    val action: Int,
    val changesFacing: Boolean = true,
    val fast: Boolean = false,
    /** Client-side duration override in ms, for actions that hold (emote bubbles run 750ms). */
    val holdMs: Long? = null,
) {
  FACE_DOWN(Direction.DOWN, false, 0x00),
  FACE_UP(Direction.UP, false, 0x01),
  FACE_LEFT(Direction.LEFT, false, 0x02),
  FACE_RIGHT(Direction.RIGHT, false, 0x03),
  WALK_DOWN(Direction.DOWN, true, 0x10),
  WALK_UP(Direction.UP, true, 0x11),
  WALK_LEFT(Direction.LEFT, true, 0x12),
  WALK_RIGHT(Direction.RIGHT, true, 0x13),
  FAST_DOWN(Direction.DOWN, true, 0x1D, fast = true),
  FAST_UP(Direction.UP, true, 0x1E, fast = true),
  FAST_LEFT(Direction.LEFT, true, 0x1F, fast = true),
  FAST_RIGHT(Direction.RIGHT, true, 0x20, fast = true),
  FASTER_DOWN(Direction.DOWN, true, 0x35, fast = true),
  FASTER_UP(Direction.UP, true, 0x36, fast = true),
  FASTER_LEFT(Direction.LEFT, true, 0x37, fast = true),
  FASTER_RIGHT(Direction.RIGHT, true, 0x38, fast = true),
  WALK_IN_PLACE_FAST_LEFT(Direction.LEFT, false, 0x23, fast = true),
  WALK_IN_PLACE_FAST_RIGHT(Direction.RIGHT, false, 0x24, fast = true),
  WALK_IN_PLACE_FASTER_DOWN(Direction.DOWN, false, 0x2D, fast = true),
  WALK_IN_PLACE_FASTER_UP(Direction.UP, false, 0x2E, fast = true),
  WALK_IN_PLACE_FASTER_LEFT(Direction.LEFT, false, 0x2F, fast = true),
  WALK_IN_PLACE_FASTER_RIGHT(Direction.RIGHT, false, 0x30, fast = true),
  // Captured delay actions use Emerald ids plus eight. holdMs is the real client time (the
  // name's frame count at ~60fps) - counting delays as face-turns starved the duration
  // estimate and made script-end queue clears cut walks short.
  DELAY_1(Direction.DOWN, false, 0x18, changesFacing = false, holdMs = 17),
  DELAY_2(Direction.DOWN, false, 0x19, changesFacing = false, holdMs = 34),
  DELAY_4(Direction.DOWN, false, 0x1A, changesFacing = false, holdMs = 67),
  DELAY_8(Direction.DOWN, false, 0x1B, changesFacing = false, holdMs = 134),
  DELAY_16(Direction.DOWN, false, 0x1C, changesFacing = false, holdMs = 268),
  // Hides the entity in place, used at the end of a walk into a door (decomp set_invisible).
  SET_INVISIBLE(Direction.DOWN, false, 0x60, changesFacing = false),
  // Bytecode-verified (f/l31 G5 -> f/yy.CG -> balloon model 50 + spot SFX): the "!" bubble,
  // 750ms. 0x63 is the silent "?" bubble. Emerald ids 0x56/0x57 shifted +12 like set_invisible.
  EMOTE_EXCLAMATION(Direction.DOWN, false, 0x62, changesFacing = false, holdMs = 750),
  EMOTE_QUESTION(Direction.DOWN, false, 0x63, changesFacing = false, holdMs = 750);

  companion object {
    /** Translates source-level pret action names into the existing client movement vocabulary. */
    fun fromPretCommand(command: String): MovementStep? =
        when (command) {
          "face_down" -> FACE_DOWN
          "face_up" -> FACE_UP
          "face_left" -> FACE_LEFT
          "face_right" -> FACE_RIGHT
          "walk_down" -> WALK_DOWN
          "walk_up" -> WALK_UP
          "walk_left" -> WALK_LEFT
          "walk_right" -> WALK_RIGHT
          "walk_fast_down" -> FAST_DOWN
          "walk_fast_up" -> FAST_UP
          "walk_fast_left" -> FAST_LEFT
          "walk_fast_right" -> FAST_RIGHT
          "walk_faster_down" -> FASTER_DOWN
          "walk_faster_up" -> FASTER_UP
          "walk_faster_left" -> FASTER_LEFT
          "walk_faster_right" -> FASTER_RIGHT
          "walk_in_place_faster_down" -> WALK_IN_PLACE_FASTER_DOWN
          "walk_in_place_faster_up" -> WALK_IN_PLACE_FASTER_UP
          "walk_in_place_faster_left" -> WALK_IN_PLACE_FASTER_LEFT
          "walk_in_place_faster_right" -> WALK_IN_PLACE_FASTER_RIGHT
          "delay_1" -> DELAY_1
          "delay_2" -> DELAY_2
          "delay_4" -> DELAY_4
          "delay_8" -> DELAY_8
          "delay_16" -> DELAY_16
          "set_invisible" -> SET_INVISIBLE
          // The felled Cut tree vanishes in place; the script removes the object right after.
          "cut_tree" -> SET_INVISIBLE
          // The rest are approximations onto capture-verified action bytes: slow walks play at
          // normal speed, in-place walks at the faster tempo, and pure animations (emotes, the
          // nurse's bow) become a beat of delay so sequence timing survives.
          "walk_slow_down" -> WALK_DOWN
          "walk_slow_up" -> WALK_UP
          "walk_slow_left" -> WALK_LEFT
          "walk_slow_right" -> WALK_RIGHT
          "walk_in_place_fast_left" -> WALK_IN_PLACE_FAST_LEFT
          "walk_in_place_fast_right" -> WALK_IN_PLACE_FAST_RIGHT
          "walk_in_place_down" -> WALK_IN_PLACE_FASTER_DOWN
          "walk_in_place_up" -> WALK_IN_PLACE_FASTER_UP
          "walk_in_place_left" -> WALK_IN_PLACE_FASTER_LEFT
          "walk_in_place_right" -> WALK_IN_PLACE_FASTER_RIGHT
          "emote_exclamation_mark",
          "emote_question_mark",
          "emote_heart",
          "nurse_joy_bow" -> DELAY_16
          else -> null
        }

    /**
     * Actions with no fixed byte: their direction exists only at run time (where is the player, how
     * did the NPC originally face). The executor handles them between fixed-step segments.
     */
    fun isRuntimeResolved(command: String): Boolean =
        command == "face_player" || command == "face_original_direction"
  }
}
