package de.fiereu.openmmo.server.game.battle

/** The client's gender codes: what f/gT0.Ug1 returns and what a battle record's gender byte carries. */
object Gender {
  const val MALE: Byte = 0
  const val FEMALE: Byte = 1
  const val GENDERLESS: Byte = -1

  /**
   * The client's own derivation (f/gT0.Ug1), so the server never disagrees with a party screen:
   * ratio 0 is always male, 254 always female, 255 genderless, and anything else is female when
   * the low byte of the seed is below the ratio. Battle records used to ship a constant 0 here,
   * which drew every monster in a battle as male - Latias included.
   */
  fun of(genderRatio: Int, seed: Int): Byte =
      when (genderRatio) {
        0 -> MALE
        254 -> FEMALE
        255 -> GENDERLESS
        else -> if ((seed and 0xFF) < genderRatio) FEMALE else MALE
      }
}
