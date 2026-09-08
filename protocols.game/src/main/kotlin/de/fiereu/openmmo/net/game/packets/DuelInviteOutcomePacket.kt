package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8

/**
 * s2c 0x51 (client reader f/Yd1): a trade state change, ONE packed byte: `(code shl 2) or
 * (side + 1)` - the reader takes `(b shr 2) and 7` through f/kG1 (1 completed, 2 canceled,
 * 3 confirmed -> nr0.LpT6, 4 locked -> nr0.D21) and `(b and 3) - 1` as the side. Sent as two
 * plain bytes (code, side) the client read only the first: a lock (4) arrived as code 1,
 * "Trade completed." (2026-09-08).
 */
data class DuelInviteOutcomePacket(val code: Byte, val side: Byte)

object DuelInviteOutcomePacketCodec : PacketCodec<DuelInviteOutcomePacket>() {
  override fun CodecScope<DuelInviteOutcomePacket>.body(): DuelInviteOutcomePacket {
    val packed = field(S8) { (((it.code.toInt() and 7) shl 2) or ((it.side.toInt() + 1) and 3)).toByte() }
    val code = ((packed.toInt() shr 2) and 7).toByte()
    val side = ((packed.toInt() and 3) - 1).toByte()
    return DuelInviteOutcomePacket(code, side)
  }
}
