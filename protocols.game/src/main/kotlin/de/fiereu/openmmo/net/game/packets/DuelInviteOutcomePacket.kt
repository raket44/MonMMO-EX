package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8

/**
 * s2c 0x51 (client reader f/Yd1): a trade state change. [code] is f/kG1's wire value - 1
 * completed, 2 canceled, 3 locked, 4 confirmed - and [side] the trade side it applies to.
 */
data class DuelInviteOutcomePacket(val code: Byte, val side: Byte)

object DuelInviteOutcomePacketCodec : PacketCodec<DuelInviteOutcomePacket>() {
  override fun CodecScope<DuelInviteOutcomePacket>.body(): DuelInviteOutcomePacket {
    val code = field(S8) { it.code }
    val side = field(S8) { it.side }
    return DuelInviteOutcomePacket(code, side)
  }
}
