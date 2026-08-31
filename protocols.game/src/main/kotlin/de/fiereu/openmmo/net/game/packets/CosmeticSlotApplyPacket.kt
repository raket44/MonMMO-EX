package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/**
 * C2S 0x30 - one click in the character-customization dialog (client f.PQ, verified in bytecode;
 * the client's own c2s registry f.Mw1 maps 48 -> f/PQ). Every dropdown selection sends one of
 * these - it is the LIVE per-slot apply, separate from the full-set save (0x29).
 *
 * Wire: `u8 slot (SkinSlot ordinal), u8 byAddonId, then s16 addonId when byAddonId else s64 stack
 * object id (the bag stack whose item backs the cosmetic; item id sits above bit 16), u8 variant`.
 *
 * The trailing byte is the VARIANT (f.PQ.Jq <- ce.AUX): the alternate-form index for addons with
 * flag bit 15 (Noble Steed 0=brown, 1=Alt). Colors travel in the skin short via 0x29.
 *
 * This opcode was previously misassigned to BattleActionPacket - dialog clicks showed up in the log
 * as phantom "battle actions" (slot 11 = BIKE read as actionTypeId=11).
 */
data class CosmeticSlotApplyPacket(
    val slot: Int,
    val byAddonId: Boolean,
    val addonId: Short,
    val stackObjectId: Long,
    val variant: Byte,
)

object CosmeticSlotApplyPacketCodec : PacketCodec<CosmeticSlotApplyPacket>() {
  override fun CodecScope<CosmeticSlotApplyPacket>.body(): CosmeticSlotApplyPacket {
    val slot = field(U8) { it.slot }
    val byAddonId = field(U8) { if (it.byAddonId) 1 else 0 } != 0
    var addonId: Short = -1
    var stackObjectId = 0L
    if (byAddonId) {
      addonId = field(S16LE) { it.addonId }
    } else {
      stackObjectId = field(S64LE) { it.stackObjectId }
    }
    val variant = field(S8) { it.variant }
    return CosmeticSlotApplyPacket(slot, byAddonId, addonId, stackObjectId, variant)
  }
}
