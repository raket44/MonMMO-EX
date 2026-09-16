package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.server.game.storage.StoredCharacter

/**
 * The client's timed "Boost Items" (its own HUD label, string 10106): charms that are CONSUMED on
 * use and then run for a fixed time, one of each type at a time - "You already have a Charm of that
 * type active." (string 5991).
 *
 * Every charm's own item text reads "Effects last 1 hour" ([DURATION_SECONDS]); the separate 1.50x
 * EXP booster is the only 30-minute item in the table. Link members receive a share (10%, or 5% for
 * the Shiny Charm) which we do not model yet.
 *
 * **The clock is PLAY TIME, not wall clock** (owner, 2026-09-16): buffs and egg timers pause when
 * the player logs out. `CharacterInfo.playTimeSeconds` only advances while the character is in the
 * world, so every deadline here is a play-time reading, and callers should bank the current session
 * (`CharacterStore.bankPlayTime`) before comparing.
 *
 * Only the Shiny Charm is wired so far, because it is the one breeding needs: it feeds the egg's
 * shiny roll in BreedingService. The rest are listed so the ids and durations live in one place.
 */
object Boosts {

  /** One hour of PLAY TIME, the duration every charm's own description states. */
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

  /** The play-time reading this charm runs until, 0 when it has never been used. */
  fun activeUntil(storyVars: Map<String, Int>, kind: Kind): Int = storyVars[kind.key] ?: 0

  fun isActive(playTimeSeconds: Int, storyVars: Map<String, Int>, kind: Kind): Boolean =
      activeUntil(storyVars, kind) > playTimeSeconds

  fun secondsLeft(playTimeSeconds: Int, storyVars: Map<String, Int>, kind: Kind): Int =
      (activeUntil(storyVars, kind) - playTimeSeconds).coerceAtLeast(0)

  fun isActive(stored: StoredCharacter, kind: Kind): Boolean =
      isActive(stored.info.playTimeSeconds, stored.storyVars, kind)

  fun secondsLeft(stored: StoredCharacter, kind: Kind): Int =
      secondsLeft(stored.info.playTimeSeconds, stored.storyVars, kind)

  /** The play-time deadline to store when a charm is used by a character at [playTimeSeconds]. */
  fun expiryFrom(playTimeSeconds: Int): Int = playTimeSeconds + DURATION_SECONDS
}
