package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.codecs.SkinSet

// Type and color share one 16 bit word, exactly as in the skin-set codec.
private const val TYPE_MASK = 0x3FF
private const val COLOR_SHIFT = 10
private const val COLOR_MASK = 0x3F

/**
 * C2S 0x29 - the in-game customization dialog's apply (client f.cb1, built by f.Te.W50 when the
 * player confirms). The wire is the client's own writer, verified in bytecode:
 *
 * `s64 entityId, s8 gender (f.r4), s8 skinTone, [skin set via f.Em1.Sv0], s8 regionOutfit`
 *
 * Sv0 writes: u8 leading byte (the region-outfit index, same value as the trailing byte), s16 slot
 * mask (bit 15 set when per-slot extra bytes follow), then per set slot an s16 `type | color <<
 * 10` - plus one extra byte per entry when bit 15 is set.
 *
 * The dialog lists an option for every bag item whose catalog record links a cosmetic addon
 * (f.Gc0.ka1), so ownership enforcement is the server's job when this arrives.
 */
data class CustomizeCharacterAppearancePacket(
    val entityId: Long,
    val gender: Byte,
    val skinTone: Byte,
    val appearance: SkinSet,
    val extras: Map<SkinSlot, Byte>,
    val regionOutfit: Byte,
)

object CustomizeCharacterAppearancePacketCodec : PacketCodec<CustomizeCharacterAppearancePacket>() {
  override fun CodecScope<CustomizeCharacterAppearancePacket>.body():
      CustomizeCharacterAppearancePacket {
    val entityId = field(S64LE) { it.entityId }
    val gender = field(S8) { it.gender }
    val skinTone = field(S8) { it.skinTone }
    val regionSelectionIndex = field(U8) { it.appearance.regionSelectionIndex }
    val mask =
        field(S16LE) { packet ->
          var m = packet.appearance.keys.fold(0) { acc, slot -> acc or (1 shl slot.ordinal) }
          if (packet.extras.isNotEmpty()) m = m or 0x8000
          m.toShort()
        }
    val hasExtras = (mask.toInt() and 0x8000) != 0
    val appearance = SkinSet(regionSelectionIndex)
    val extras = mutableMapOf<SkinSlot, Byte>()
    SkinSlot.entries.forEach { slot ->
      if ((mask.toInt() and (1 shl slot.ordinal)) != 0) {
        val packed =
            field(S16LE) {
              val skin = it.appearance[slot] ?: Skin(slot, 0u, 0u)
              val type = (skin.type ?: 0u).toInt()
              val color = (skin.color ?: 0u).toInt()
              ((type and TYPE_MASK) or ((color and COLOR_MASK) shl COLOR_SHIFT)).toShort()
            }
        appearance.put(
            Skin(
                slot,
                (packed.toInt() and TYPE_MASK).toUShort(),
                ((packed.toInt() shr COLOR_SHIFT) and COLOR_MASK).toUByte()))
        if (hasExtras) {
          extras[slot] = field(S8) { it.extras[slot] ?: 0 }
        }
      }
    }
    val regionOutfit = field(S8) { it.regionOutfit }
    return CustomizeCharacterAppearancePacket(
        entityId, gender, skinTone, appearance, extras, regionOutfit)
  }
}
