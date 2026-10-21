package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S32LE

data class ClientSettingUpdatePacket(val settingsValue: Int)

object ClientSettingUpdatePacketCodec : PacketCodec<ClientSettingUpdatePacket>() {
  override fun CodecScope<ClientSettingUpdatePacket>.body(): ClientSettingUpdatePacket {
    val settingsValue = field(S32LE) { it.settingsValue }
    return ClientSettingUpdatePacket(settingsValue)
  }
}
