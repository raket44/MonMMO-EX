package de.fiereu.openmmo.common.enums

enum class MoveFlag(val bit: Int) {
  MAKES_CONTACT(1 shl 0),
  PROTECT_AFFECTED(1 shl 1),
  MAGIC_COAT_AFFECTED(1 shl 2),
  SNATCH_AFFECTED(1 shl 3),
  MIRROR_MOVE_AFFECTED(1 shl 4),
  KINGS_ROCK_AFFECTED(1 shl 5),
  /** Hits 2-5 times (the Expansion's `multiHit`). */
  MULTI_HIT(1 shl 6),
  /** A frozen user can use it and thaws out. */
  THAWS_USER(1 shl 7),
  ALWAYS_CRIT(1 shl 8),
  POWDER(1 shl 9),
  SOUND(1 shl 10),
  HEALING(1 shl 11),
  PUNCH(1 shl 12),
  BITE(1 shl 13),
  MINIMIZE_DOUBLE_DAMAGE(1 shl 14),
  /** The user faints after using it (Explosion, Self-Destruct). */
  EXPLOSION(1 shl 15),
  /** Reaches a target in the air (Gust, Thunder, Sky Uppercut). */
  DAMAGES_AIRBORNE(1 shl 16),
  /** Reaches a target in the air for double damage (Gust, Twister). */
  DAMAGES_AIRBORNE_DOUBLE(1 shl 17),
  /** Reaches a target underground for double damage (Earthquake, Magnitude). */
  DAMAGES_UNDERGROUND(1 shl 18),
  /** Reaches a target underwater for double damage (Surf, Whirlpool). */
  DAMAGES_UNDERWATER(1 shl 19),
  /** Never misses in rain (Thunder, Hurricane). */
  ALWAYS_HITS_IN_RAIN(1 shl 20),
  /** Never misses in hail (Blizzard). */
  ALWAYS_HITS_IN_HAIL(1 shl 21),
}
