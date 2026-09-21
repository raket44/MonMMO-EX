package de.fiereu.openmmo.server.game.services

/**
 * The lure items, whose numbers are the client's OWN item descriptions rather than anything we
 * chose (strings 101655-101665 and 101710, read 2026-09-21). A lure is consumed on use, runs for a
 * number of steps shown in the HUD beside the repel counter, and while it runs:
 *
 *  - raises the encounter rate
 *  - makes the foes a few levels stronger (owner: "just 1-3 lvls stronger.. random")
 *  - may turn a single encounter into two or three foes, each rolled on its own, so unlike a Sweet
 *    Scent horde they can be DIFFERENT species
 *  - adds lure-exclusive species to the table
 *  - on the premium tier only, "small hordes (3) may increase in size"
 *
 * The HUD counter rides the local delta's 0x40 group as [kind] plus the steps left; [kind] is the
 * client's f/ig7 enum byte, where -1 is "no lure" (LURE_KIND_NONE) and 0/1/2 pick which of the
 * three labels it prints - "{00} lure step(s)", "premium", "legendary".
 */
object Lures {
  const val KIND_STANDARD = 0
  const val KIND_PREMIUM = 1
  const val KIND_LEGENDARY = 2

  /** How much stronger a lured foe is, in levels, inclusive (owner: 1-3, random). */
  const val MIN_LEVEL_BONUS = 1
  const val MAX_LEVEL_BONUS = 3

  /**
   * One tier. [encounterRatePercent] is the client's "+10%/+25%/+15% encounter rates";
   * [exclusivePercent] its "5%/10%/8% chance to encounter lure-exclusive species"; [growsHordes]
   * its "small hordes (3) may increase in size", which only the premium tier claims.
   */
  data class Tier(
      val itemId: Int,
      val name: String,
      val steps: Int,
      val kind: Int,
      val encounterRatePercent: Int,
      val exclusivePercent: Int,
      val growsHordes: Boolean,
      /** The premium tier's "Secret Shiny rates are increased by +25%"; 0 on the others. */
      val secretBonusPercent: Int,
  )

  private val tiers =
      listOf(
          Tier(1041, "Lure", 100, KIND_STANDARD, 10, 5, false, 0),
          Tier(1042, "Super Lure", 200, KIND_STANDARD, 10, 5, false, 0),
          Tier(1043, "Expert Lure", 250, KIND_STANDARD, 10, 5, false, 0),
          Tier(1044, "Premium Lure", 200, KIND_PREMIUM, 25, 10, true, 25),
          Tier(1045, "Premium Super Lure", 300, KIND_PREMIUM, 25, 10, true, 25),
          Tier(1046, "Premium Max Lure", 400, KIND_PREMIUM, 25, 10, true, 25),
          Tier(1475, "Legendary Lure", 300, KIND_LEGENDARY, 15, 8, false, 0),
      )

  private val byItem = tiers.associateBy { it.itemId }

  fun of(itemId: Int): Tier? = byItem[itemId]

  fun isLure(itemId: Int): Boolean = byItem.containsKey(itemId)
}
