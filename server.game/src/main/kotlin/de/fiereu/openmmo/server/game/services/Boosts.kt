package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.server.game.storage.StoredCharacter

/**
 * The client's timed "Boost Items" (its own HUD label, string 10106): charms that are CONSUMED on
 * use and then run for a fixed time, one of each type at a time - "You already have a Charm of that
 * type active." (string 5991).
 *
 * The client's item text is the spec for each one, and every charm reads "Effects last 1 hour"
 * ([DURATION_SECONDS]); the separate 1.50x EXP booster is the only 30-minute item in the table.
 * Link members receive a share (10%, or 5% for the Shiny Charm) which we do not model yet.
 *
 * Only the Shiny Charm is wired so far, because it is the one breeding needs: it feeds the egg's
 * shiny roll in BreedingService. The rest are listed so the ids and durations live in one place.
 */
object Boosts {

  /** One hour, the duration every charm's own description states. */
  const val DURATION_SECONDS = 3600

  /**
   * A charm the player can have running. [itemId] 0 means we do not know the bag item yet, so that
   * charm cannot be activated; fill it in as the ids are confirmed (the Shiny Charm is 1409,
   * owner-supplied 2026-09-16).
   */
  enum class Kind(val key: String, val itemId: Int, val label: String) {
    SHINY("boost/shiny-charm", 1409, "Shiny Charm"),
    EXP("boost/exp-charm", 0, "EXP Charm"),
    ITEM_MAGNET("boost/item-magnet-charm", 0, "Item Magnet Charm"),
    BATTLE_POINTS("boost/battle-points-charm", 0, "Battle Points Charm"),
    RICHES("boost/riches-charm", 0, "Riches Charm");

    companion object {
      fun byItem(itemId: Int): Kind? = entries.firstOrNull { it.itemId != 0 && it.itemId == itemId }
    }
  }

  private fun nowSecond(): Int = (System.currentTimeMillis() / 1000).toInt()

  /** Epoch second this charm runs until, 0 when it has never been used. */
  fun activeUntil(storyVars: Map<String, Int>, kind: Kind): Int = storyVars[kind.key] ?: 0

  fun isActive(storyVars: Map<String, Int>, kind: Kind): Boolean =
      activeUntil(storyVars, kind) > nowSecond()

  fun secondsLeft(storyVars: Map<String, Int>, kind: Kind): Int =
      (activeUntil(storyVars, kind) - nowSecond()).coerceAtLeast(0)

  fun isActive(stored: StoredCharacter, kind: Kind): Boolean = isActive(stored.storyVars, kind)

  fun secondsLeft(stored: StoredCharacter, kind: Kind): Int = secondsLeft(stored.storyVars, kind)

  /** The expiry to store when a charm is used now. */
  fun expiryFromNow(): Int = nowSecond() + DURATION_SECONDS
}
