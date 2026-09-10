package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8

/**
 * The client's Safari Game line packet, s2c 0x3A (client f/HB -> f/L6, bytecode-verified
 * 2026-09-09): [action u8][side u8][mood u8].
 *
 * [action] is a battle action id (f/sV) looked up in f/private.B00: 6 BAIT prints the bait throw,
 * 7 ROCK the rock throw, 8 the wild monster's mood. [side] is the battle side (0 = the player, 1 =
 * the wild): a throw names the trainer of [side] and the monster on the other side, a mood names
 * the monster on [side]. [mood] matters for action 8 only: 0 watching carefully, 1 angry, 2 eating,
 * 3 fled.
 *
 * The text is the ROM's in the DS regions (HeartGold bank 197 / Platinum bank 368, lines 842
 * watching, 855 angry, 852 eating, 469 fled, 851 bait, 854 mud, chosen by the current game) and,
 * in the GBA regions, the client strings 200273 / 200274 / 200275 / 200148 / 200272 / 200271
 * formatted with {06} = monster, {23} = trainer, {0F} = the fled monster. Those six strings are
 * missing from this build's strings_en.xml, so the launcher stages them in FireRed's words.
 */
data class SafariEventPacket(
    val action: Byte,
    val side: Byte,
    val mood: Byte = 0,
) {
  companion object {
    const val ACTION_BAIT: Byte = 6
    const val ACTION_ROCK: Byte = 7
    const val ACTION_MOOD: Byte = 8
    const val SIDE_PLAYER: Byte = 0
    const val SIDE_WILD: Byte = 1
    const val MOOD_WATCHING: Byte = 0
    const val MOOD_ANGRY: Byte = 1
    const val MOOD_EATING: Byte = 2
    const val MOOD_FLED: Byte = 3

    fun bait() = SafariEventPacket(ACTION_BAIT, SIDE_PLAYER)

    fun rock() = SafariEventPacket(ACTION_ROCK, SIDE_PLAYER)

    fun mood(mood: Byte) = SafariEventPacket(ACTION_MOOD, SIDE_WILD, mood)
  }
}

object SafariEventPacketCodec : PacketCodec<SafariEventPacket>() {
  override fun CodecScope<SafariEventPacket>.body(): SafariEventPacket {
    val action = field(S8) { it.action }
    val side = field(S8) { it.side }
    val mood = field(S8) { it.mood }
    return SafariEventPacket(action, side, mood)
  }
}
