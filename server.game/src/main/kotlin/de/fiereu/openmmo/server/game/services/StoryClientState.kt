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
    // The client whitelists the story ids it mirrors (badges, fly points, gates) and its 0x2A
    // handler throws on anything else - every other flag is server-side save state only.
    if (!ClientStoryWhitelist.accepts(regionId.toInt(), id)) return null
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

  /**
   * The retail badge popup - the same grey server-message box item pickups use. String 5964: "Your
   * {STRING_0} feel you've grown stronger from your adventure!\n{STRING_0} up to lv. {00} will now
   * obey you." - {STRING_0} ("Pokemon") resolves client-side, {00} is our arg: the new obedience
   * cap. Sent when a SET flag is one of the region's badge ids; the cap comes from the client's own
   * per-badge-count table (f/LG0.cU1, docs/client-story-ids.md).
   */
  fun badgeAnnouncement(
      regionId: Byte,
      flagId: Int,
      setFlagKeys: Collection<String>,
  ): de.fiereu.openmmo.net.game.packets.ServerMessagePacket? {
    val region = regionId.toInt()
    val badges = ClientStoryWhitelist.badgeIds[region] ?: return null
    if (flagId !in badges) return null
    val setIds = flags(regionId, setFlagKeys).map { it.flagId }.toSet()
    val caps = LEVEL_CAPS[region] ?: return null
    val cap = caps.getOrNull(badges.count { it in setIds }) ?: return null
    return de.fiereu.openmmo.net.game.packets.ServerMessagePacket(
        OBEY_CAP_STRING,
        listOf(
            de.fiereu.openmmo.net.game.packets.ServerMessageArg(
                argId = 0,
                type = 5,
                hasExtra = false,
                extra = 0,
                longValue = null,
                intValue = null,
                stringValue = cap.toString(),
                shortValues = null,
            )),
        showOnMap = true,
        mode = null,
    )
  }

  private fun flagId(regionId: Byte, key: String): Int? =
      when (regionId.toInt()) {
        // GBA regions: the client's whitelisted ids ARE the ROM flag ids, so the generated
        // decomp mapping is also the wire mapping.
        KANTO_REGION -> KantoFlags.numericId(key)
        HOENN_REGION -> HoennFlags.numericId(key)
        // NDS regions: NEVER forward ROM ids (HGSS badges are a save bitfield; the ROM's own
        // 1360+ flags are trainer-defeated flags). The DS scripts keep badges as the synthetic
        // FLAG_DS_BADGE_<n>, which maps onto the client's own per-region badge ids.
        else ->
            Regex("FLAG_DS_BADGE_(\\d+)$").find(key)?.groupValues?.get(1)?.toIntOrNull()?.let { n ->
              ClientStoryWhitelist.badgeIds[regionId.toInt()]?.getOrNull(n)
            }
                ?: DS_RUNNING_SHOES_IDS[regionId.toInt()]?.takeIf { key.endsWith("FLAG_DS_RUNNING_SHOES") }
      }

  private fun varId(regionId: Byte, key: String): Int? =
      when (regionId.toInt()) {
        KANTO_REGION -> KantoVars.numericId(key)
        HOENN_REGION -> HoennVars.numericId(key)
        else -> null
      }

  /**
   * The client gates running on ONE story flag per region, read by its own `f/ey7.Yb0()`:
   * Kanto 0x82F, Hoenn 0x8C0, Unova 0x963, and no check at all for Sinnoh and Johto. The GBA
   * regions reach it through their real ROM flag (Kanto FLAG_SYS_B_DASH IS 2095); a DS region has
   * no forwardable ROM id, so its scripts set the synthetic key and it maps here, the same way
   * FLAG_DS_BADGE_n maps onto the client badge ids. Unova sets it in Mom's Route 2 scene.
   */
  private val DS_RUNNING_SHOES_IDS: Map<Int, Int> = mapOf(2 to 2403)

  private const val KANTO_REGION = 0
  private const val HOENN_REGION = 1
  private const val GBA_VARS_START = 0x4000
  private const val GBA_VARS_END = 0x40ff

  /** "Your Pokemon feel you've grown stronger..." - the retail badge/obedience popup. */
  private const val OBEY_CAP_STRING = 5964

  /**
   * Obedience cap per SET badge count (index 0 = no badges), the client's own tables (`f/LG0.cU1` +
   * `LG0.HA`). The count includes the ninth champion/game-clear slot. Johto's conditional overrides
   * (flag-gated 39/40/55) are not modeled; the base table is what the popup quotes.
   */
  private val LEVEL_CAPS: Map<Int, List<Int>> =
      mapOf(
          0 to listOf(20, 26, 32, 37, 46, 47, 50, 55, 62, 100),
          1 to listOf(20, 24, 28, 33, 35, 38, 44, 48, 58, 100),
          2 to listOf(20, 24, 27, 31, 35, 38, 43, 46, 56, 100),
          3 to listOf(20, 27, 29, 34, 37, 43, 46, 52, 60, 100),
          4 to listOf(20, 24, 29, 32, 37, 39, 41, 46, 48, 100),
      )
}
