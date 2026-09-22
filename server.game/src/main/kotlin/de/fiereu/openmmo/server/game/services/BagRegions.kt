package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ClientTools
import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import de.fiereu.openmmo.story.generated.kanto.KantoFlags

/**
 * Which bag page a stack belongs on: retail's "key items and HMs are per region", done the way the
 * client already does it. The bag list (f/kY1) draws a stack only when its region byte (f/zG.KX0)
 * is -1 or the region the player stands in. Consumables, TMs, berries and cosmetics stay -1 (every
 * page); a region's key items and HMs carry that region's id and are off the bag the moment the
 * player leaves it. Owner's rule: leave Kanto and its HMs and key items are gone until you return,
 * for all five regions (2026-09-22).
 *
 * Where an item's region comes from:
 * - Unova (client band 5000, mirrored at 6000): the ROM's own pocket table, pocket 4 = key items;
 *   the HMs are Gen 5 420..425.
 * - Sinnoh (band 8000) and Johto (band 9000): the Gen 4 item index - HMs 420..427, key items from
 *   428 (Explorer Kit). 9481 is HGSS's Machine Part, which is how the two bands were told apart.
 * - Kanto and Hoenn share one Gen 3 index, at raw ids (the 1000 band is PokeMMO's own items -
 *   1259 is Super Carbos, not a Hoenn table). Ids only FireRed or only Emerald defines (the registry's
 *   per-region key-item tables) tag that region outright. Ids BOTH define - the eight HMs, the
 *   rods, the Coin Case, the Itemfinder, the S.S. Ticket - are one stored item that either region
 *   can hand out, so they become ONE STACK PER REGION whose receipt flag is set (the trick the
 *   dyeable garments already use: the region rides in the stack id's low byte, and the use path
 *   reads the item id from the action, not the stack id). No flag set at all: -1, visible, rather
 *   than lose a real item.
 * - The client's own Bicycle (360) is FireRed's Bicycle, so it is Kanto's page: riding does not need it
 *   in the bag (the hotkeyed bike mounts regardless - one client bike, owner's design), and the
 *   other regions have their own bike items.
 */
internal object BagRegions {
  private const val EVERYWHERE: Byte = -1

  /**
   * The (region byte, quantity) stacks one stored bag entry becomes. An HM is one item for the
   * whole game (ItemRegistry folds every band's copy onto it), so it becomes one stack per region
   * that handed it over (FieldMoves.receiptFlag) - on that region's page and no other. The Gen 3
   * key items both GBA games hand out work the same way from their own receipt flags.
   */
  fun stacks(itemId: Int, quantity: Int, storyFlags: Collection<String>): List<Pair<Byte, Int>> {
    ClientTools.itemToMove[itemId]?.takeIf { isHm(itemId) }?.let { moveId ->
      val earnedIn = FieldMoves.receivedIn(storyFlags, moveId)
      if (earnedIn.isNotEmpty()) return earnedIn.map { it.wireValue to 1 }
    }
    shared[itemId]?.let { (kanto, hoenn) ->
      val earnedIn = buildList {
        if (kanto != null && kanto in storyFlags) add(Region.KANTO)
        if (hoenn != null && hoenn in storyFlags) add(Region.HOENN)
      }
      if (earnedIn.isNotEmpty()) return earnedIn.map { it.wireValue to 1 }
    }
    return listOf(single(itemId) to quantity)
  }

  /** The item has a stack on [regionId]'s bag page (or on every page), so it can be used there. */
  fun visibleIn(itemId: Int, regionId: Int, storyFlags: Collection<String>): Boolean =
      stacks(itemId, 1, storyFlags).any { (page, _) -> page == EVERYWHERE || page.toInt() == regionId }

  /** An HM in any region's band (the client's own HM blocks, ItemRegistry.isHmId). */
  fun isHm(itemId: Int): Boolean =
      itemId in 339..346 || itemId in 5420..5425 || itemId in 6420..6425 || itemId in 8420..8427 || itemId in 9420..9427

  /** The region byte of an item that lives on one page, or -1 for every page. */
  fun single(itemId: Int): Byte {
    if (itemId in CosmeticsRegistry.wardrobeStock) return HIDDEN_BAG_REGION
    if (CosmeticsRegistry.byItemId(itemId) != null) return EVERYWHERE
    if (itemId in DS_BICYCLE_ITEMS) return (itemId / 1000).toByte()
    return when (itemId) {
      in 5000..5999 -> unova(itemId - 5000)
      in 6000..6999 -> unova(itemId - 6000)
      in 8000..8999 -> gen4(itemId - 8000, Region.SINNOH)
      in 9000..9999 -> gen4(itemId - 9000, Region.JOHTO)
      else -> gen3(itemId)
    }
  }

  private fun unova(index: Int): Byte =
      if (index in 420..425 || unovaPockets.getOrNull(index) == KEY_ITEMS_POCKET) Region.UNOVA.wireValue
      else EVERYWHERE

  private fun gen4(index: Int, region: Region): Byte =
      if (index in 420..427 || index >= 428) region.wireValue else EVERYWHERE

  private fun gen3(id: Int): Byte =
      when (id) {
        in KANTO_ONLY -> Region.KANTO.wireValue
        in HOENN_ONLY -> Region.HOENN.wireValue
        else -> EVERYWHERE
      }

  /** FireRed's key items Emerald has no use for (ItemRegistry.registerGbaKeyItems, region 0). */
  private val KANTO_ONLY =
      setOf(349, 350, 351, 352, 353, 355, 356, 359, 360, 363, 366, 367, 368, 369, 372, 373, 374)

  /** Emerald's key items FireRed has no use for (ItemRegistry.registerGbaKeyItems, region 1). */
  private val HOENN_ONLY =
      setOf(259, 266, 268, 269, 270, 271, 272, 273, 274, 275, 278, 279, 280, 281, 282, 283, 284,
          285, 288, 375, 376)

  /** Gen 3 key items both GBA games hand out (the HMs are FieldMoves.receiptFlag's): (Kanto, Hoenn) receipt flag. */
  private val shared: Map<Int, Pair<String?, String?>> =
      buildMap {
        put(260, KantoFlags.FLAG_GOT_COIN_CASE to HoennFlags.FLAG_RECEIVED_COIN_CASE)
        // Emerald's Itemfinder has no receipt flag of its own in the decomp.
        put(261, KantoFlags.FLAG_GOT_ITEMFINDER to null)
        put(262, KantoFlags.FLAG_GOT_OLD_ROD to HoennFlags.FLAG_RECEIVED_OLD_ROD)
        put(263, KantoFlags.FLAG_GOT_GOOD_ROD to HoennFlags.FLAG_RECEIVED_GOOD_ROD)
        put(264, KantoFlags.FLAG_GOT_SUPER_ROD to HoennFlags.FLAG_RECEIVED_SUPER_ROD)
        put(265, KantoFlags.FLAG_GOT_SS_TICKET to HoennFlags.FLAG_RECEIVED_SS_TICKET)
      }

  private const val KEY_ITEMS_POCKET = 4

  /** Pocket per Gen 5 item index, from the ROM item table (tools/nds/ItemPockets5). */
  private val unovaPockets: List<Int> by lazy {
    BagRegions::class.java.getResourceAsStream("/monmmo/unova-item-pockets.txt")
        ?.bufferedReader()?.readLines().orEmpty()
        .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
        ?.trim()?.map { it.digitToInt(16) }
        .orEmpty()
  }
}
