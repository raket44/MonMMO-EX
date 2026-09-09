package de.fiereu.openmmo.common.enums

/**
 * What kind of tile the player is standing on, normalized across games from the decomp metatile
 * behaviors. Only the behaviors we act on are named, everything else is [NORMAL].
 */
enum class TileBehavior {
  NORMAL,
  TALL_GRASS,
  LONG_GRASS,
  JUMP_EAST,
  JUMP_WEST,
  JUMP_NORTH,
  JUMP_SOUTH,
  DOOR,
  /** Cave and water doors. They look like doors but warp like a ladders. */
  NON_ANIMATED_DOOR,
  /** Ladders, escalators and warp pads, which warp as soon as the player steps on them. */
  LADDER,
  STAIR_WARP_EAST,
  STAIR_WARP_WEST,
  /** Arrow warps are named after the direction the player walks to use them. */
  NORTH_ARROW_WARP,
  SOUTH_ARROW_WARP,
  EAST_ARROW_WARP,
  WEST_ARROW_WARP,
  /** A storage PC (MB_PC): pressing A while facing it opens the client's storage UI. */
  PC,
  /** Surfable water (pond, ocean, seaweed): blocked on foot, open while surfing. Appended - ordinals are baked into generated maps. */
  WATER,
  /** Deep water (MB_DEEP_WATER and kin): surfable like [WATER], and a Dive spot. */
  DEEP_WATER,
  /** A waterfall (MB_WATERFALL): Waterfall climbs it upward while surfing. */
  WATERFALL,
  /** Spin tiles (MB_SPIN_RIGHT..DOWN): the player is spun along until a stop tile or a wall. */
  SPIN_RIGHT,
  SPIN_LEFT,
  SPIN_UP,
  SPIN_DOWN,
  /** MB_STOP_SPINNING: the spin ends on this tile. */
  STOP_SPINNING;

  /** The direction a spin tile sends the player, null for any other tile. */
  val spinDirection: Direction?
    get() =
        when (this) {
          SPIN_RIGHT -> Direction.RIGHT
          SPIN_LEFT -> Direction.LEFT
          SPIN_UP -> Direction.UP
          SPIN_DOWN -> Direction.DOWN
          else -> null
        }

  /** Any water a surfer rides on. */
  val isSurfable: Boolean
    get() = this == WATER || this == DEEP_WATER || this == WATERFALL

  /** The direction the player must walk while standing here to be warped. */
  val warpsWhenWalking: Direction?
    get() =
        when (this) {
          STAIR_WARP_EAST,
          EAST_ARROW_WARP -> Direction.RIGHT
          STAIR_WARP_WEST,
          WEST_ARROW_WARP -> Direction.LEFT
          NORTH_ARROW_WARP -> Direction.UP
          SOUTH_ARROW_WARP -> Direction.DOWN
          else -> null
        }

  val warpsOnStep: Boolean
    get() = this == LADDER || this == NON_ANIMATED_DOOR
}
