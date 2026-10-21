package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE

data class CurrencyBalancePacket(
    val primaryBalance: Int,
    val secondaryBalance: Int,
)

object CurrencyBalancePacketCodec : PacketCodec<CurrencyBalancePacket>() {
  override fun CodecScope<CurrencyBalancePacket>.body(): CurrencyBalancePacket {
    val primaryBalance = field(S32LE) { it.primaryBalance }
    val secondaryBalance = field(S32LE) { it.secondaryBalance }
    return CurrencyBalancePacket(primaryBalance, secondaryBalance)
  }
}
