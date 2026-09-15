package de.fiereu.openmmo.common

object CharacterPermissions {
  /**
   * Server-side permission bits. Only [CLIENT_STAFF_LEVEL_MASK] reaches the client; the rest gate
   * chat commands here.
   */
  const val CLIENT_GM_MENU = 0x01

  const val DEVELOPER = 0x100

  /**
   * The staff level the client itself reads (CharacterInfo byte after `permissions`, decompiled as
   * `f.ZZ.tI`). `f.Ot.Hv1(level)` answers `staffLevel >= level`, and the built-in GM Menu (`f.sb`,
   * with Player Search / Teleport / Profiler) opens on level 1; other staff UI checks up to 3.
   * Stored in bits 16-23 so it survives alongside the server-side bits.
   */
  const val CLIENT_STAFF_LEVEL_SHIFT = 16
  const val CLIENT_STAFF_LEVEL_MASK = 0xFF shl CLIENT_STAFF_LEVEL_SHIFT

  /**
   * Highest staff rank the client knows. Its rank table (decompiled `f.lP1`) reads: 1 = CM, 2-4 =
   * MOD, 5-6 = GM, 7 = SGM, 8 = HGM, 9 = DEV, 10 = ADM.
   */
  const val CLIENT_STAFF_LEVEL_FULL = 10

  /** Client rank DEV: the lowest staff rank that carries [DEVELOPER]. */
  const val CLIENT_STAFF_LEVEL_DEVELOPER = 9

  /** The lowest client staff rank that carries [permission], or null when only its bit grants it. */
  fun minimumStaffLevel(permission: Int): Int? =
      when (permission) {
        DEVELOPER -> CLIENT_STAFF_LEVEL_DEVELOPER
        CLIENT_GM_MENU -> 1
        else -> null
      }
}

/**
 * Whether this character holds [permission]: its bit, or a staff rank at or above the rank that
 * carries it. Higher ranks inherit everything below them, as the client's own staff checks do
 * (f.Ot.Hv1: staffLevel >= level), so an ADM runs every developer command without the bit
 * (project owner, 2026-09-14).
 */
fun CharacterInfo.hasPermission(permission: Int): Boolean =
    permissions and permission == permission ||
        CharacterPermissions.minimumStaffLevel(permission)?.let { clientStaffLevel >= it } == true

/** The staff level this character presents to the client. */
val CharacterInfo.clientStaffLevel: Int
  get() =
      (permissions and CharacterPermissions.CLIENT_STAFF_LEVEL_MASK) shr
          CharacterPermissions.CLIENT_STAFF_LEVEL_SHIFT

/** Returns [permissions] with the client-visible staff level replaced by [level]. */
fun withClientStaffLevel(permissions: Int, level: Int): Int =
    (permissions and CharacterPermissions.CLIENT_STAFF_LEVEL_MASK.inv()) or
        ((level and 0xFF) shl CharacterPermissions.CLIENT_STAFF_LEVEL_SHIFT)
