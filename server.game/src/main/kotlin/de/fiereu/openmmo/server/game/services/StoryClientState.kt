package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.PlayerVariableEntry
import de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket
import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import de.fiereu.openmmo.story.generated.hoenn.HoennVars
import de.fiereu.openmmo.story.generated.kanto.KantoFlags
import de.fiereu.openmmo.story.generated.kanto.KantoVars

/** Converts persisted story keys to client ids. */
internal object StoryClientState {
  fun flags(regionId: Byte, flags: Collection<String>): List<StoryFlagUpdatePacket> =
      flags.mapNotNull { flagUpdate(regionId, it, enabled = true) }.sortedBy { it.flagId }

  fun flagUpdate(regionId: Byte, key: String, enabled: Boolean): StoryFlagUpdatePacket? {
    val id = flagId(regionId, key) ?: return null
    return StoryFlagUpdatePacket(regionId, id, enabled)
  }

  /**
   * The "variables" list is per-ITEM unlock flags: the client (f.Za0) stores each (s16 itemId, u8
   * enabled) pair and its apply pass (X91) runs dY(itemId, enabled) -> Gc0.tv0 for every entry. tv0
   * is what the customization dialog checks when deciding whether a cosmetic is an OPTION - so this
   * list, not the bag, is what makes cosmetics selectable.
   *
   * Server policy, matching retail as the operator described it:
   * - every item the player owns is enabled (obtaining a thing unlocks using it),
   * - the twelve basic bicycle colors (BIKE addons 0..11) are ALWAYS enabled - they are not bag
   *   items, the dialog just offers them,
   * - every other item-backed cosmetic is explicitly DISABLED unless its item is in the bag. The
   *   explicit zero matters: the retail pak ships some sub-frame leftovers (horse gallop frames,
   *   sparkles) default-unlocked, and they polluted the dialog until overridden.
   */
  fun itemUnlocks(items: Map<Int, Int>): List<PlayerVariableEntry> {
    val entries = mutableMapOf<Int, Byte>()
    for (itemId in items.keys) {
      if (itemId in 1..Short.MAX_VALUE.toInt()) entries[itemId] = 1
    }
    for (addon in CosmeticsRegistry.itemBacked()) {
      entries[addon.itemId] = if (addon.alwaysSelectable || addon.itemId in items) 1 else 0
    }
    return entries.entries
        .sortedBy { it.key }
        .map { PlayerVariableEntry(it.key.toShort(), it.value) }
  }

  private fun flagId(regionId: Byte, key: String): Int? =
      when (regionId.toInt()) {
        KANTO_REGION -> KantoFlags.numericId(key)
        HOENN_REGION -> HoennFlags.numericId(key)
        else -> null
      }

  private fun varId(regionId: Byte, key: String): Int? =
      when (regionId.toInt()) {
        KANTO_REGION -> KantoVars.numericId(key)
        HOENN_REGION -> HoennVars.numericId(key)
        else -> null
      }

  private const val KANTO_REGION = 0
  private const val HOENN_REGION = 1
  private const val GBA_VARS_START = 0x4000
  private const val GBA_VARS_END = 0x40ff
}
