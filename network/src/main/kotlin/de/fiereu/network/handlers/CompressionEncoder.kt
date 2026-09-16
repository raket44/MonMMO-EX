package de.fiereu.network.handlers

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.io.ByteArrayOutputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.zip.Deflater

private val log = KotlinLogging.logger {}

// One continuous raw deflate stream per connection: the deflater is never reset, so its sliding
// window carries across packets. Each compressed packet is a Z_SYNC_FLUSH segment ending in
// 00 00 FF FF, which the client re-appends, so those four bytes are dropped here.
class CompressionEncoder(private val threshold: Int = 256) : MessageToByteEncoder<ByteBuf>() {

  private val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
  private val chunk = ByteArray(0x4000)

  override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    if (msg.readableBytes() < 1) return
    val opcode = msg.readByte()
    out.writeByte(opcode.toInt())
    val payloadLen = msg.readableBytes()
    if (payloadLen < threshold) {
      out.writeByte(0)
      out.writeBytes(msg, msg.readerIndex(), payloadLen)
      msg.skipBytes(payloadLen)
      return
    }
    if (payloadLen > CLIENT_INFLATE_BUFFER) {
      // The r32645 client inflates into a fixed 30000-byte buffer (f/od4.oP): the overflow stays in
      // its inflater and every later compressed packet on this connection decodes as garbage.
      log.warn { "Packet 0x${"%02x".format(opcode.toInt() and 0xFF)} payload $payloadLen bytes exceeds the client inflate buffer ($CLIENT_INFLATE_BUFFER); split it" }
    }
    val input = ByteArray(payloadLen)
    msg.getBytes(msg.readerIndex(), input, 0, payloadLen)
    msg.skipBytes(payloadLen)
    out.writeByte(1)
    deflater.setInput(input)
    val buffer = ByteArrayOutputStream(payloadLen)
    var written: Int
    do {
      written = deflater.deflate(chunk, 0, chunk.size, Deflater.SYNC_FLUSH)
      buffer.write(chunk, 0, written)
    } while (written == chunk.size)
    val deflated = buffer.toByteArray()
    val bodyLen = (deflated.size - SYNC_MARKER).coerceAtLeast(0)
    out.writeBytes(deflated, 0, bodyLen)
  }

  override fun handlerRemoved(ctx: ChannelHandlerContext) {
    deflater.end()
  }

  private companion object {
    const val SYNC_MARKER = 4
    const val CLIENT_INFLATE_BUFFER = 30_000
  }
}
