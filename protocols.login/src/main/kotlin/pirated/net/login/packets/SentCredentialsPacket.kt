package pirated.net.login.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.Utf16LeNullTerminated
import java.util.Base64

data class SentCredentialsPacket(val username: String, val key: String) {
  constructor(
      username: String,
      keyBytes: ByteArray?,
  ) : this(username, keyBytes?.let { Base64.getEncoder().encodeToString(it) } ?: "")

  constructor(username: String) : this(username, "")
}

object SentCredentialsPacketCodec : PacketCodec<SentCredentialsPacket>() {
  override fun CodecScope<SentCredentialsPacket>.body(): SentCredentialsPacket {
    val username = field(Utf16LeNullTerminated) { it.username }
    val key = field(Utf16LeNullTerminated) { it.key }
    return SentCredentialsPacket(username, key)
  }
}
