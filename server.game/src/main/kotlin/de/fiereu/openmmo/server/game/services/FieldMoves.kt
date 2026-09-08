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

  /** The engine's gate: the region's badge, and a party member with the move or its ocarina. */
  fun canUse(stored: StoredCharacter, regionId: Int, moveId: Int): Boolean =
      badgeHeld(stored, regionId, moveId) && (partyKnows(stored, moveId) || ocarinaOwned(stored, moveId))
}
