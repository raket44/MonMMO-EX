package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.CosmeticSlotApplyPacket
import de.fiereu.openmmo.net.game.packets.CustomizeCharacterAppearancePacket
import de.fiereu.openmmo.net.game.packets.EntitySpriteChangePacket
import de.fiereu.openmmo.net.game.packets.EntityTransportationPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/** Body-feature slots with no backing items (client f.ne0.F71 false): always customizable. */
private val INNATE_SLOTS =
    setOf(SkinSlot.FOREHEAD, SkinSlot.HAIR, SkinSlot.EYES, SkinSlot.FACIAL_HAIR)

/**
 * The in-game customization dialog (client f.Te). The client builds its option list from the bag -
 * every item whose catalog record links a cosmetic addon shows up as a choice - and sends the whole
 * chosen appearance as one 0x29 when the player confirms. Retail's contract is that the SERVER
 * enforces ownership and answers with the committed appearance, so that is what this does: validate
 * each slot against the addon registry and the bag, persist, and re-announce the look via
 * EntitySpriteChange (0x90), which the client applies to the live player.
 */
@Singleton
class AppearanceService
@Inject
constructor(
    private val characters: CharacterStore,
) {

  fun onCustomizeAppearance(event: PacketEvent<CustomizeCharacterAppearancePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val packet = event.packet
    if (packet.entityId != charId) {
      log.warn { "[Appearance] char=$charId tried to customize entity ${packet.entityId}" }
      return
    }
    val stored = characters.getCharacter(charId) ?: return

    // Ownership per slot: bike colors are always selectable (the client's dialog offers them
    // unconditionally - they are not bag items), an addon the character already wears stays
    // free, defaults are free, everything else needs its bag item.
    for ((slot, skin) in packet.appearance) {
      val type = (skin.type ?: 0u).toInt()
      if (slot == SkinSlot.BIKE) continue
      if (stored.skins[slot]?.type == skin.type) continue
      val addon = CosmeticsRegistry.get(slot, type)
      if (addon == null) {
        log.info { "[Appearance] char=$charId rejected: unknown addon $slot/$type" }
        ctx.send(notice("That $slot option does not exist."))
        resend(ctx, charId)
        return
      }
      if (addon.free) continue
      if (addon.itemId !in stored.items) {
        log.info { "[Appearance] char=$charId rejected: ${addon.name} needs item ${addon.itemId}" }
        ctx.send(notice("You need ${addon.name} in your bag to wear it."))
        resend(ctx, charId)
        return
      }
    }

    // The wire set's per-slot extras are the VARIANTS - fold them into the stored skins.
    val skins =
        packet.appearance.toMap().mapValues { (slot, skin) ->
          packet.extras[slot]?.let { skin.copy(variant = (it.toInt() and 0xFF).toUByte()) } ?: skin
        }
    characters.setAppearance(
        charId,
        skins = skins,
        gender = packet.gender,
        skinRegionSelectionIndex = packet.appearance.regionSelectionIndex,
    )
    characters.flushCharacterAsync(charId)
    log.info {
      "[Appearance] char=$charId applied " +
          packet.appearance.entries.joinToString { "${it.key}=${it.value.type}" }
    }
    resend(ctx, charId)
  }

  /**
   * One dropdown click in the customization dialog (0x30, client f.PQ): apply a single slot
   * immediately. The selection arrives either as the addon id itself or as the bag stack backing
   * the cosmetic (item id above bit 16 of the stack object id).
   */
  fun onSlotApply(event: PacketEvent<CosmeticSlotApplyPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val packet = event.packet
    val slot = SkinSlot.entries.getOrNull(packet.slot)
    if (slot == null) {
      log.info { "[Appearance] char=$charId slot apply with unknown slot ${packet.slot}" }
      return
    }
    val stored = characters.getCharacter(charId) ?: return

    val addon =
        if (packet.byAddonId) {
          if (packet.addonId < 0) null // explicit "None"
          else CosmeticsRegistry.get(slot, packet.addonId.toInt())
        } else {
          val itemId = (packet.stackObjectId ushr 16).toInt() and 0xFFFF
          val resolved = CosmeticsRegistry.byItemId(itemId)
          if (resolved == null) {
            // A stack reference that resolves to nothing must NOT fall through to the clear
            // path - one such click silently stripped the stored bike skin.
            log.info { "[Appearance] char=$charId slot=$slot unresolved stack item $itemId" }
            return
          }
          resolved
        }
    if (addon == null && packet.byAddonId && packet.addonId >= 0) {
      // An id we cannot resolve could still be a legitimate innate option (hair styles etc.
      // have no item and only item-backed addons are in the registry) - allow innate slots.
      if (slot !in INNATE_SLOTS) {
        log.info { "[Appearance] char=$charId rejected: unknown $slot addon ${packet.addonId}" }
        return
      }
    }
    if (addon != null) {
      if (addon.slot != slot) {
        log.info { "[Appearance] char=$charId rejected: ${addon.name} is not a $slot addon" }
        return
      }
      if (!addon.free && addon.itemId !in stored.items) {
        ctx.send(notice("You need ${addon.name} in your bag to wear it."))
        return
      }
    }

    val type =
        addon?.type ?: if (packet.byAddonId && packet.addonId >= 0) packet.addonId.toInt() else -1
    if (type < 0) {
      characters.setSkin(charId, slot, null)
    } else {
      // The click's trailing byte is the VARIANT (alternate form); the color survives from
      // whatever this slot already wears - colors commit through the full-set save (0x29).
      val color = if (stored.skins[slot]?.type?.toInt() == type) stored.skins[slot]?.color else 0u
      characters.setSkin(
          charId,
          slot,
          de.fiereu.openmmo.common.Skin(
              slot, type.toUShort(), color ?: 0u, (packet.variant.toInt() and 0xFF).toUByte()))
    }
    characters.flushCharacterAsync(charId)
    log.info {
      "[Appearance] char=$charId slot=$slot -> ${addon?.name ?: type} variant=${packet.variant}"
    }
    resend(ctx, charId)
    // Swapping bikes mid-ride: the skin refresh above changes the art; re-assert the ride bit
    // so the mounted frame set survives the sprite change.
    if (slot == SkinSlot.BIKE && state.riding) {
      ctx.send(EntityTransportationPacket(charId, 0x02))
    }
  }

  /**
   * Announce the stored appearance to the live entity - 0x90 applies it without a respawn. The
   * boolean routes the set client-side (f.uz.X91): true stages it into IL0.JQ1 where nothing reads
   * it, FALSE applies into IL0.v4 - the set the renderer and the bike frame selector (jR1) actually
   * draw from. Every earlier resend sent true and changed nothing.
   */
  private fun resend(ctx: de.fiereu.network.SessionContext, charId: Long) {
    val stored = characters.getCharacter(charId) ?: return
    ctx.send(
        EntitySpriteChangePacket(
            entityId = charId,
            staged = false,
            appearance = SkinSet(stored.info.skinRegionSelectionIndex, stored.skins),
            gender = stored.info.rivalSex,
        ))
  }
}

/**
 * The client's cosmetic-addon catalog, dumped from data/sprites/addons.pak by
 * tools/addons/AddonIndex.java (format decoded from f.oF1.L2). One line per addon:
 * `slot|addonId|itemId|flags|variants|name`. itemId -1 means no item exists (innate body features);
 * flags bit 4 (client J61.q4) marks a default addon that needs no item either.
 */
object CosmeticsRegistry {
  data class Addon(
      val slot: SkinSlot,
      val type: Int,
      val itemId: Int,
      val flags: Int,
      val name: String,
  ) {
    val free: Boolean
      get() = itemId < 0 || (flags and 16) != 0 || alwaysSelectable

    /** The twelve basic bicycle colors: not bag items, the dialog simply offers them. */
    val alwaysSelectable: Boolean
      get() = slot == SkinSlot.BIKE && type in 0..11
  }

  private val bySlot: Map<SkinSlot, Map<Int, Addon>> by lazy {
    val stream =
        CosmeticsRegistry::class.java.getResourceAsStream("/monmmo/addons.csv")
            ?: return@lazy emptyMap()
    stream
        .bufferedReader()
        .useLines { lines ->
          lines
              .mapNotNull { line ->
                val parts = line.split('|')
                if (parts.size < 6) return@mapNotNull null
                val slot =
                    runCatching { SkinSlot.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
                Addon(
                    slot = slot,
                    type = parts[1].toIntOrNull() ?: return@mapNotNull null,
                    itemId = parts[2].toIntOrNull() ?: return@mapNotNull null,
                    flags = parts[3].toIntOrNull() ?: return@mapNotNull null,
                    name = parts[5],
                )
              }
              .toList()
        }
        .groupBy { it.slot }
        .mapValues { (_, addons) -> addons.associateBy { it.type } }
  }

  fun get(slot: SkinSlot, type: Int): Addon? = bySlot[slot]?.get(type)

  /** All addons whose linked item exists - the grantable cosmetic items. */
  fun itemBacked(): List<Addon> = bySlot.values.flatMap { it.values }.filter { it.itemId > 0 }

  private val byItem: Map<Int, Addon> by lazy { itemBacked().associateBy { it.itemId } }

  /** The addon a cosmetic bag item unlocks, or null for a non-cosmetic id. */
  fun byItemId(itemId: Int): Addon? = byItem[itemId]

  /**
   * Items whose addon is a VARIANT ALT in the client's hardcoded table (f.Ur0.AL1): HAT addons
   * 360/362/366/368/397/400/406 (item = addon + 4320), BACK 93, BIKE 67 (Noble Steed (Alt)). The
   * BASE item's variant window selects these - held as bag items they list as bogus standalone
   * entries, so they are never granted and get reclaimed at login.
   */
  val variantAltItems: Set<Int> = setOf(4680, 4682, 4686, 4688, 4717, 4720, 4726, 3373, 4883)
}
