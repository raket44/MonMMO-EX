package de.fiereu.openmmo.server.game.battle

/**
 * Weight mechanics: Gen 5's weight-based move power and PokeMMO's Heavy Ball. Weights are
 * hectograms (0.1 kg), the unit [SpeciesDef.weight] stores; a species no source weighs (0) counts
 * as the 0.1 kg minimum.
 *
 * Not modelled, because the engine has neither yet: Autotomize (MoveEffect.AUTOTOMIZE has no
 * handler, so no 100 kg reduction to track), Float Stone (no held-item hook), and the Heavy Metal /
 * Light Metal abilities (enum entries only). Once any lands, the weight passed in here should be the
 * battle-modified one rather than the species' own.
 */
object WeightMechanics {

  /** Low Kick and Grass Knot: power by the target's weight band. */
  fun lowKickPower(targetWeight: Int): Int {
    val weight = targetWeight.coerceAtLeast(1)
    return when {
      weight < 100 -> 20
      weight < 250 -> 40
      weight < 500 -> 60
      weight < 1000 -> 80
      weight < 2000 -> 100
      else -> 120
    }
  }

  /** Heavy Slam and Heat Crash: power by how many times the target the user weighs. */
  fun heavySlamPower(userWeight: Int, targetWeight: Int): Int {
    val user = userWeight.coerceAtLeast(1)
    val target = targetWeight.coerceAtLeast(1)
    return when {
      user >= target * 5 -> 120
      user >= target * 4 -> 100
      user >= target * 3 -> 80
      user >= target * 2 -> 60
      else -> 40
    }
  }

  /**
   * Heavy Ball, PokeMMO's rule (a multiplier, not Gen 5's catch-rate addition), in hundredths: 1x
   * under 100 kg, 2x under 200 kg, 3x under 300 kg, 4x from 300 kg.
   */
  fun heavyBallRate(targetWeight: Int): Int =
      when {
        targetWeight < 1000 -> 100
        targetWeight < 2000 -> 200
        targetWeight < 3000 -> 300
        else -> 400
      }
}
