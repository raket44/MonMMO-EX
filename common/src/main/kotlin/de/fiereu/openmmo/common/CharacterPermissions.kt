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
}

fun CharacterInfo.hasPermission(permission: Int): Boolean = permissions and permission == permission

/** The staff level this character presents to the client. */
val CharacterInfo.clientStaffLevel: Int
  get() =
      (permissions and CharacterPermissions.CLIENT_STAFF_LEVEL_MASK) shr
          CharacterPermissions.CLIENT_STAFF_LEVEL_SHIFT

/** Returns [permissions] with the client-visible staff level replaced by [level]. */
fun withClientStaffLevel(permissions: Int, level: Int): Int =
    (permissions and CharacterPermissions.CLIENT_STAFF_LEVEL_MASK.inv()) or
        ((level and 0xFF) shl CharacterPermissions.CLIENT_STAFF_LEVEL_SHIFT)
