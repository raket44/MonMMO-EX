package pirated.network

import pirated.network.checksum.NoOpChecksum
import pirated.network.cipher.NoOpSessionCipher
import pirated.network.handlers.ChecksumFrameDecoder
import pirated.network.handlers.ChecksumFrameEncoder
import pirated.network.handlers.CipherDecoder
import pirated.network.handlers.CipherEncoder
import pirated.network.handlers.PacketFrameDecoder
import pirated.network.handlers.PacketFrameEncoder
import pirated.network.handshake.ClientSessionHandshakeHandler
import pirated.network.handshake.ServerSessionHandshakeHandler
import pirated.network.internal.MutableSessionContext
import pirated.network.internal.SESSION_KEY
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelPipeline
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.util.concurrent.TimeUnit

fun installPipeline(
    pipeline: ChannelPipeline,
    side: Side,
    identity: SessionIdentity,
    applicationProtocol: Protocol,
    applicationHandlerFactory: () -> ProtocolHandler,
    options: PipelineOptions = PipelineOptions(),
) {
  val channel = pipeline.channel()
  val session = MutableSessionContext(side, channel, applicationProtocol)
  channel.attr(SESSION_KEY).set(session)

  val handshakeHandler: ProtocolHandler =
      when (side) {
        Side.SERVER -> {
          require(identity is SessionIdentity.ServerRoot) {
            "Server side requires SessionIdentity.ServerRoot"
          }
          ServerSessionHandshakeHandler(
              rootPrivate = identity.rootPrivate,
              applicationProtocol = applicationProtocol,
              applicationHandlerFactory = applicationHandlerFactory,
              options = options,
          )
        }
        Side.CLIENT -> {
          require(identity is SessionIdentity.ClientTrust) {
            "Client side requires SessionIdentity.ClientTrust"
          }
          ClientSessionHandshakeHandler(
              rootPublic = identity.rootPublic,
              applicationProtocol = applicationProtocol,
              applicationHandlerFactory = applicationHandlerFactory,
              options = options,
          )
        }
      }

  pipeline.addLast(
      PipelineNames.WRITE_TIMEOUT,
      WriteTimeoutHandler(options.writeTimeout.inWholeSeconds, TimeUnit.SECONDS),
  )
  pipeline.addLast(
      PipelineNames.READ_IDLE,
      io.netty.handler.timeout.IdleStateHandler(options.readIdleTimeout.inWholeSeconds, 0, 0, TimeUnit.SECONDS),
  )
  pipeline.addLast(PipelineNames.READ_IDLE_CLOSER, ReadIdleCloser())
  if (options.frameLogging) {
    pipeline.addLast(PipelineNames.FRAME_LOGGER, LoggingHandler(LogLevel.TRACE))
  }
  pipeline.addLast(PipelineNames.FRAME_DECODER, PacketFrameDecoder(options.maxFrameLength))
  pipeline.addLast(PipelineNames.FRAME_ENCODER, PacketFrameEncoder())
  pipeline.addLast(PipelineNames.CHECKSUM_DECODER, ChecksumFrameDecoder(NoOpChecksum))
  pipeline.addLast(PipelineNames.CHECKSUM_ENCODER, ChecksumFrameEncoder(NoOpChecksum))
  pipeline.addLast(PipelineNames.CIPHER_DECODER, CipherDecoder(NoOpSessionCipher))
  pipeline.addLast(PipelineNames.CIPHER_ENCODER, CipherEncoder(NoOpSessionCipher))
  if (options.frameLogging) {
    pipeline.addLast(PipelineNames.PROTOCOL_LOGGER, LoggingHandler(LogLevel.TRACE))
  }
  pipeline.addLast(PipelineNames.PROTOCOL_HANDLER, handshakeHandler)
}

/**
 * Adds a handler in front of the protocol logger, so the logger only sees uncompressed packets:
 * decompressed on the way in, not yet compressed on the way out.
 */
internal fun ChannelPipeline.addBeforeProtocolLogger(name: String, handler: ChannelHandler) {
  val anchor =
      if (get(PipelineNames.PROTOCOL_LOGGER) != null) {
        PipelineNames.PROTOCOL_LOGGER
      } else {
        PipelineNames.PROTOCOL_HANDLER
      }
  addBefore(anchor, name, handler)
}
