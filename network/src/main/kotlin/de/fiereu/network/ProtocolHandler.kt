package de.fiereu.network

import de.fiereu.bytecodec.Codec
import de.fiereu.network.internal.OutgoingPacket
import de.fiereu.network.internal.SESSION_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise

private val log = KotlinLogging.logger {}

private val DUMP_OPCODES: Set<Int> =
    (System.getProperty("monmmo.dumpOpcodes") ?: "").split(",").mapNotNull { it.trim().ifEmpty { null }?.toIntOrNull(16) }.toSet()

abstract class ProtocolHandler(
    val protocol: Protocol,
    val side: Side,
) : ChannelDuplexHandler() {

  protected lateinit var session: SessionContext
    private set

  override fun handlerAdded(ctx: ChannelHandlerContext) {
    val attached =
        ctx.channel().attr(SESSION_KEY).get()
            ?: error("No SessionContext attached to channel ${ctx.channel()}")
    session = attached
  }

  override fun channelActive(ctx: ChannelHandlerContext) {
    try {
      onActive()
    } catch (t: Throwable) {
      onErrorInternal(ctx, t)
      return
    }
    ctx.fireChannelActive()
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    try {
      onInactive()
    } finally {
      ctx.fireChannelInactive()
    }
  }

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg !is ByteBuf) {
      ctx.fireChannelRead(msg)
      return
    }
    try {
      if (msg.readableBytes() < 1) throw EmptyFrameException()
      // The whole frame, captured before any read moves the index: when a frame cannot be
      // decoded, its bytes are the diagnostic.
      val frameHex = io.netty.buffer.ByteBufUtil.hexDump(msg)
      val opcode = (msg.readByte().toInt() and 0xFF).toUByte()
      val registration = protocol.incomingRegistration(side, opcode)
      if (registration == null) {
        log.error {
          "No incoming codec for opcode 0x${opcode.toString(16)} on $side frame=$frameHex"
        }
        return
      }
      val packet =
          try {
            val decoded = decode(registration.codec, msg)
            val trailing = msg.readableBytes()
            if (trailing > 0) throw TrailingBytesException(opcode, trailing)
            decoded
          } catch (t: Throwable) {
            // The transport is framed, so one undecodable frame cannot desync the stream. While
            // packet shapes are still being reverse-engineered, a mis-sized codec is data to
            // collect, not a reason to drop the session.
            log.warn {
              "Dropping undecodable frame opcode=0x${opcode.toString(16)} on $side " +
                  "frame=$frameHex reason=${t.message}"
            }
            return
          }
      try {
        onPacket(PacketEvent(packet, session))
      } catch (t: Throwable) {
        onErrorInternal(ctx, t)
      }
    } catch (t: Throwable) {
      onErrorInternal(ctx, t)
    } finally {
      msg.release()
    }
  }

  override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
    val (registration, value) =
        when (msg) {
          is OutgoingPacket -> msg.registration to msg.value
          is ByteBuf -> {
            ctx.write(msg, promise)
            return
          }
          else -> {
            val reg =
                protocol.outgoingRegistration(side, msg::class)
                    ?: throw UnknownPacketTypeException(msg::class, side)
            reg to msg
          }
        }
    val buffer = ctx.alloc().buffer()
    var success = false
    try {
      buffer.writeByte(registration.opcode.toInt())
      encode(registration.codec, value, buffer)
      success = true
      // -Dmonmmo.dumpOpcodes=30,52 logs the exact bytes of those outgoing packets, for feeding a
      // frame to the client's own reader when it rejects one.
      if (registration.opcode.toInt() in DUMP_OPCODES) {
        log.info { "DUMP opcode=0x${registration.opcode.toString(16)} bytes=${io.netty.buffer.ByteBufUtil.hexDump(buffer)}" }
      }
    } finally {
      if (!success) buffer.release()
    }
    ctx.write(buffer, promise)
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    onErrorInternal(ctx, cause)
  }

  private fun onErrorInternal(ctx: ChannelHandlerContext, cause: Throwable) {
    try {
      onError(cause)
    } catch (t: Throwable) {
      log.error(t) { "onError handler threw; closing channel" }
      ctx.close()
    }
  }

  open fun onActive() {}

  abstract fun onPacket(event: PacketEvent<*>)

  open fun onInactive() {}

  open fun onError(cause: Throwable) {
    log.warn(cause) { "Handler error on ${session.remoteAddress}" }
    session.close { "Handler error: ${cause.message}" }
  }

  private fun <T : Any> decode(codec: Codec<T>, buf: ByteBuf): T = codec.read(NettyReadBuffer(buf))

  @Suppress("UNCHECKED_CAST")
  private fun encode(codec: Codec<*>, value: Any, buf: ByteBuf) {
    (codec as Codec<Any>).write(NettyWriteBuffer(buf), value)
  }
}
