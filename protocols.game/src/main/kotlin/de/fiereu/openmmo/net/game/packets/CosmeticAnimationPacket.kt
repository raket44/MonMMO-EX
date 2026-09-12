package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.enums.SkinSlot

/**
 * s2c 0x6C (client f/Sv1): plays the one-shot animation of the addon a PLAYER entity wears in
 * [slot] - the Werewolf Masks' howl, the Golden Tiger Mask's roar. The client looks the entity up,
 * requires it to be a player (f/E41), checks that [addonId] is the addon worn in that slot (or one
 * of its variants, f/Ur0), and then arms the per-slot one-shot state (f/Di0.Jy1: addon id + elapsed
 * ms) that the renderer (f/F90.fR) reads to draw the addon's second animation instead of its idle
 * loop until it has run out. [playSound] plays the addon's sound at a distance-based volume, rate
 * limited for other players' entities. Nothing else animates a player's addon: the npc animation
 * packet 0xB2 only drives npc sprite sheets.
 */
data class CosmeticAnimationPacket(
    val entityId: Long,
    val slot: SkinSlot,
    val addonId: Short,
    val playSound: Boolean,
)

object CosmeticAnimationPacketCodec : PacketCodec<CosmeticAnimationPacket>() {
  override fun CodecScope<CosmeticAnimationPacket>.body(): CosmeticAnimationPacket {
    val entityId = field(S64LE) { it.entityId }
    val slot = SkinSlot.entries[field(U8) { it.slot.ordinal }]
    val addonId = field(S16LE) { it.addonId }
    val playSound = field(U8) { if (it.playSound) 1 else 0 } == 1
    return CosmeticAnimationPacket(entityId, slot, addonId, playSound)
  }
}
