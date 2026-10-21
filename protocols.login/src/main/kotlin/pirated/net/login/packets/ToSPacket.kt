package pirated.net.login.packets

import pirated.bytecodec.Codec
import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S8
import pirated.bytecodec.U16LE
import pirated.bytecodec.bytesPrefixed
import pirated.bytecodec.imap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class ToSPacket(val confirmationKey: Byte, val tosText: String)

private fun gzipDecompress(data: ByteArray): String =
    GZIPInputStream(ByteArrayInputStream(data)).use {
      String(it.readAllBytes(), StandardCharsets.UTF_8)
    }

private fun gzipCompress(text: String): ByteArray {
  val baos = ByteArrayOutputStream()
  GZIPOutputStream(baos).use { it.write(text.toByteArray(StandardCharsets.UTF_8)) }
  return baos.toByteArray()
}

private val GzippedTextU16: Codec<String> =
    bytesPrefixed(U16LE).imap(decode = ::gzipDecompress, encode = ::gzipCompress)

object ToSPacketCodec : PacketCodec<ToSPacket>() {
  override fun CodecScope<ToSPacket>.body(): ToSPacket {
    val confirmationKey = field(S8) { it.confirmationKey }
    val tosText = field(GzippedTextU16) { it.tosText }
    return ToSPacket(confirmationKey, tosText)
  }
}
