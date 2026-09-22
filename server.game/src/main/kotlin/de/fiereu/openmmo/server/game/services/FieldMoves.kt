package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.storage.StoredCharacter

/**
 * The HM field moves, the badge each one needs per region, and the client's ocarina item that
 * stands in for a party member knowing the move.
 *
 * Badges are the cartridge engine's rules (field_control_avatar.c, party_menu.c): FireRed Boulder
 * Flash, Cascade Cut, Thunder Fly, Rainbow Strength, Soul Surf, Marsh Rock Smash, Volcano
 * Waterfall; Emerald Stone Cut, Knuckle Flash, Dynamo Rock Smash, Heat Strength, Balance Surf,
 * Feather Fly, Mind Dive, Rain Waterfall. The ocarinas are the client's global catalog items
 * 1180-1187 (names "{move} Ocarina", strings 101300+), whose own description says they need the
 * region's badge. Sweet Scent's ocarina (1179) is the OcarinaService's own.
 */
object FieldMoves {
  data class FieldMove(
      val moveId: Int,
      val ocarinaItemId: Int,
      /** Badge flag (1-8) in FireRed, or 0 when Kanto has no such move. */
      val kantoBadge: Int,
      /** Badge flag (1-8) in Emerald. */
      val hoennBadge: Int,
  )

  /**
   * The ROM's FLAG_SYS_FLASH_ACTIVE for the region: set when Flash is used (fldeff_flash.c), it
   * keeps every dark cave floor lit on arrival, and stepping outdoors clears it (overworld.c).
   */
  fun flashActiveFlag(regionId: Int): String =
      "${(Region.byId(regionId) ?: Region.KANTO).name.lowercase()}/FLAG_SYS_FLASH_ACTIVE"

  /** Every move the party menu may offer for field use. */
  val MOVE_IDS: Set<Int> get() = setOf(CUT, FLY, SURF, STRENGTH, FLASH, ROCK_SMASH, WATERFALL, DIVE)

  const val CUT = 15
  const val FLY = 19
  const val SURF = 57
  const val STRENGTH = 70
  const val FLASH = 148
  const val ROCK_SMASH = 249
  const val WATERFALL = 127
  const val DIVE = 291

  val ALL: List<FieldMove> =
      listOf(
          FieldMove(CUT, 1180, kantoBadge = 2, hoennBadge = 1),
          FieldMove(FLY, 1181, kantoBadge = 3, hoennBadge = 6),
          FieldMove(SURF, 1182, kantoBadge = 5, hoennBadge = 5),
          FieldMove(STRENGTH, 1183, kantoBadge = 4, hoennBadge = 4),
          FieldMove(FLASH, 1184, kantoBadge = 1, hoennBadge = 2),
          FieldMove(ROCK_SMASH, 1185, kantoBadge = 6, hoennBadge = 3),
          FieldMove(WATERFALL, 1186, kantoBadge = 7, hoennBadge = 8),
          FieldMove(DIVE, 1187, kantoBadge = 0, hoennBadge = 7),
      )

  private val byMove = ALL.associateBy { it.moveId }
  private val byOcarina = ALL.associateBy { it.ocarinaItemId }

  fun byMove(moveId: Int): FieldMove? = byMove[moveId]

  fun byOcarina(itemId: Int): FieldMove? = byOcarina[itemId]

  /** The badge (1-8) [region] asks for before [move], 0 when it gates on something else. */
  fun badgeNumber(region: Region, move: FieldMove): Int =
      when (region) {
        Region.KANTO -> move.kantoBadge
        Region.HOENN -> move.hoennBadge
        else -> 0
      }

  /** The move whose gate in [region] is badge [badge], if any (a script's FLAG_BADGE0N_GET). */
  fun moveGatedBy(region: Region, badge: Int): FieldMove? =
      ALL.firstOrNull { badge != 0 && badgeNumber(region, it) == badge }

  /**
   * The flag that says [region] handed the player the HM for [moveId] - the RECEIPT. There is one
   * HM item per move for the whole game (ItemRegistry folds every band's copy onto it), so the
   * item cannot say where it came from; this can. Kanto and Hoenn: the ROM's own flags, which the
   * scripts set (FireRed has no Dive and its Waterfall is an item ball). The DS regions: a server
   * flag the grant path sets, since Black/White tracks its HMs by other means. Null: never.
   */
  fun receiptFlag(region: Region, moveId: Int): String? {
    val move = byMove[moveId] ?: return null
    return when (region) {
      Region.KANTO -> KANTO_RECEIPTS[moveId]
      Region.HOENN -> HOENN_RECEIPTS[moveId]
      else -> "${region.name.lowercase()}/HM_RECEIVED_${move.moveId}"
    }
  }

  private val KANTO_RECEIPTS =
      mapOf(
          CUT to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM01,
          FLY to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM02,
          SURF to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM03,
          STRENGTH to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM04,
          FLASH to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM05,
          ROCK_SMASH to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_GOT_HM06,
          WATERFALL to de.fiereu.openmmo.story.generated.kanto.KantoFlags.FLAG_HIDE_FOUR_ISLAND_ICEFALL_CAVE_1F_HM07,
      )

  private val HOENN_RECEIPTS =
      mapOf(
          CUT to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_CUT,
          FLY to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_FLY,
          SURF to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_SURF,
          STRENGTH to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_STRENGTH,
          FLASH to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_FLASH,
          ROCK_SMASH to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_ROCK_SMASH,
          WATERFALL to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_WATERFALL,
          DIVE to de.fiereu.openmmo.story.generated.hoenn.HoennFlags.FLAG_RECEIVED_HM_DIVE,
      )

  /** [region] handed over the HM for [moveId]. */
  fun received(storyFlags: Collection<String>, region: Region, moveId: Int): Boolean =
      receiptFlag(region, moveId)?.let { it in storyFlags } == true

  /** Every region that handed over the HM for [moveId]. */
  fun receivedIn(storyFlags: Collection<String>, moveId: Int): List<Region> =
      Region.entries.filter { received(storyFlags, it, moveId) }

  /**
   * The region's gate for [moveId]. Kanto and Hoenn: the badge, the cartridge rule. The DS
   * regions: the receipt - owner's call (2026-09-22), Black/White has no badge rule for HMs
   * ("badges are not required to use any of the HMs outside of battle" - Bulbapedia), and rather
   * than invent one, being handed the region's HM is the grant; the story hands it out where a
   * badge would have gated it anyway.
   */
  fun gateHeld(stored: StoredCharacter, regionId: Int, moveId: Int): Boolean {
    val region = Region.byId(regionId) ?: return badgeHeld(stored, regionId, moveId)
    return when (region) {
      Region.KANTO, Region.HOENN -> badgeHeld(stored, regionId, moveId)
      else -> received(stored.storyFlags, region, moveId)
    }
  }

  /** The story flag guarding [moveId] in [regionId], or null where the move is not gated or unknown. */
  fun badgeFlag(regionId: Int, moveId: Int): String? {
    val move = byMove[moveId] ?: return null
    val region = Region.byId(regionId) ?: return null
    val badge =
        when (region) {
          Region.KANTO -> move.kantoBadge
          Region.HOENN -> move.hoennBadge
          else -> 0
        }
    if (badge == 0) return null
    return "${region.name.lowercase()}/FLAG_BADGE0${badge}_GET"
  }

  fun badgeHeld(stored: StoredCharacter, regionId: Int, moveId: Int): Boolean {
    val flag = badgeFlag(regionId, moveId) ?: return true
    return flag in stored.storyFlags
  }

  fun ocarinaOwned(stored: StoredCharacter, moveId: Int): Boolean {
    val move = byMove[moveId] ?: return false
    return move.ocarinaItemId in stored.items
  }

  fun partyKnows(stored: StoredCharacter, moveId: Int): Boolean =
      stored.pokemon.any { mon -> mon.moves.any { it.id.toInt() == moveId } }

  /** The engine's gate: the region's gate (badge or HM item), and a party member with the move or its ocarina. */
  fun canUse(stored: StoredCharacter, regionId: Int, moveId: Int): Boolean =
      gateHeld(stored, regionId, moveId) && (partyKnows(stored, moveId) || ocarinaOwned(stored, moveId))
}
