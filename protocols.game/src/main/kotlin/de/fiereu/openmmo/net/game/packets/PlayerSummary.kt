package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot

/**
 * The head a player list draws - the friends list and the link overlay (r32645 f/ih6.bK1 -> f/uq3):
 * the skin tone (uq3.V80, the same leading byte as the outfit's skin set, f/ko4.yH) and the four
 * head slots. Without it every friend and link member was the client's default head.
 */
data class PlayerHead(
    val tone: Int = 0,
    val skins: Map<SkinSlot, Skin> = emptyMap(),
)

/**
 * One party monster in a link overlay (r32645 f/ih6.OW -> f/vm1): its entity id, client species,
 * form, gender (1 = female picks the female icon, f/pr.TO1) and the record's rarity bits (shiny or
 * secret shiny 0x9 and alpha 0x4 pick the icon, f/rm6.CL0).
 */
data class GroupListFrame(
    val monsterId: Long,
    val species: Short,
    val form: Byte,
    val gender: Byte,
    val rarity: Short,
)

/** A member's party icons; the list type is the client's container byte (1 = party). */
data class GroupListFrameSet(
    val listType: Byte?,
    val frames: List<GroupListFrame>,
)

/** The client's head slots in wire order (f/gi.Kj) and each one's 2-bit variant position (f/gi.cv1). */
private val HEAD_SLOTS = listOf(SkinSlot.HAIR, SkinSlot.FACIAL_HAIR, SkinSlot.EYES, SkinSlot.HAT)
private val VARIANT_INDEX = mapOf(SkinSlot.HAIR to 0, SkinSlot.EYES to 1, SkinSlot.FACIAL_HAIR to 2, SkinSlot.HAT to 3)
private const val TYPE_MASK = 0x3FF
private const val COLOR_SHIFT = 10
private const val COLOR_MASK = 0x3F

/**
 * The summary tail f/ih6.bK1 reads after the name: a byte it skips, the last-seen int (uq3.DJ, epoch
 * seconds), the skin tone, one byte of 2-bit variants, then a short per head slot packing the type
 * (low 10 bits, 0x3FF empty) and colour (high 6 bits, 0x3F none).
 */
internal class PlayerSummaryCodec<T>(
    private val lastSeen: (T) -> Int,
    private val head: (T) -> PlayerHead,
) {
  fun write(scope: CodecScope<T>): Pair<Int, PlayerHead> =
      with(scope) {
        field(S8) { 0 }
        val seen = field(S32LE) { lastSeen(it) }
        val tone = field(U8) { head(it).tone and 0xFF }
        val variants =
            field(U8) {
              HEAD_SLOTS.fold(0) { bits, slot ->
                val variant = (head(it).skins[slot]?.variant?.toInt() ?: 0) and 0x3
                bits or (variant shl (2 * VARIANT_INDEX.getValue(slot)))
              }
            }
        val skins = LinkedHashMap<SkinSlot, Skin>()
        for (slot in HEAD_SLOTS) {
          val packed =
              field(U16LE) {
                val skin = head(it).skins[slot]
                if (skin == null) 0xFFFF
                else (skin.type?.toInt() ?: TYPE_MASK) or ((skin.color?.toInt() ?: COLOR_MASK) shl COLOR_SHIFT)
              }
          val type = packed and TYPE_MASK
          val color = (packed shr COLOR_SHIFT) and COLOR_MASK
          if (type != TYPE_MASK || color != COLOR_MASK) {
            val variant = (variants shr (2 * VARIANT_INDEX.getValue(slot))) and 0x3
            skins[slot] = Skin(slot, type.toUShort(), color.toUByte(), variant.toUByte())
          }
        }
        seen to PlayerHead(tone, skins)
      }
}

internal val GroupListFrameCodec: Codec<GroupListFrame> =
    object : PacketCodec<GroupListFrame>() {
      override fun CodecScope<GroupListFrame>.body(): GroupListFrame {
        val monsterId = field(S64LE) { it.monsterId }
        val species = field(S16LE) { it.species }
        val form = field(S8) { it.form }
        val gender = field(S8) { it.gender }
        val rarity = field(S16LE) { it.rarity }
        return GroupListFrame(monsterId, species, form, gender, rarity)
      }
    }

internal val GroupListFrameSetCodec: Codec<GroupListFrameSet> =
    object : PacketCodec<GroupListFrameSet>() {
      override fun CodecScope<GroupListFrameSet>.body(): GroupListFrameSet {
        val count = field(U8) { it.frames.size }
        val listType = if (count >= 1) field(S8) { it.listType ?: PARTY_LIST } else null
        val frames = (0 until count).map { i -> field(GroupListFrameCodec) { it.frames[i] } }
        return GroupListFrameSet(listType, frames)
      }
    }

/** The client's party container byte (f/xe1, ordinal 1 of size 6). */
const val PARTY_LIST: Byte = 1
