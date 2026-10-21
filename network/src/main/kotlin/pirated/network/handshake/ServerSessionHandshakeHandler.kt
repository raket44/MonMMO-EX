package pirated.network.handshake

import pirated.network.PipelineNames
import pirated.network.PipelineOptions
import pirated.network.Protocol
import pirated.network.ProtocolHandler
import pirated.network.SessionPhase
import pirated.network.Side
import pirated.network.StaleClientHelloException
import pirated.network.TypedProtocolHandler
import pirated.network.addBeforeProtocolLogger
import pirated.network.cipher.AesCtrSessionCipher
import pirated.network.handlers.ChecksumFrameDecoder
import pirated.network.handlers.ChecksumFrameEncoder
import pirated.network.handlers.CipherDecoder
import pirated.network.handlers.CipherEncoder
import pirated.network.handlers.CompressionEncoder
import pirated.network.internal.MutableSessionContext
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.interfaces.ECPrivateKey
import kotlin.math.abs

private val log = KotlinLogging.logger {}

internal class ServerSessionHandshakeHandler(
    private val rootPrivate: ECPrivateKey,
    private val applicationProtocol: Protocol,
    private val applicationHandlerFactory: () -> ProtocolHandler,
    private val options: PipelineOptions,
) : TypedProtocolHandler<SessionHandshakeProtocol>(SessionHandshakeProtocol, Side.SERVER) {

  private val ephemeralKeyPair = EcKeys.generateEphemeralKeyPair()

  /**
   * Port scanners hold sockets open without ever saying hello, which counts as a player to
   * anything that reads the socket table. A session still in the handshake when the timer fires
   * is closed; a completed handshake has replaced this handler, so the check is by identity.
   */
  override fun onActive() {
    val channel = session.channel
    channel.eventLoop().schedule(
        {
          if (channel.isActive && channel.pipeline().get(PipelineNames.PROTOCOL_HANDLER) === this) {
            log.info { "Closing ${session.remoteAddress}: no handshake within ${options.handshakeTimeout}" }
            channel.close()
          }
        },
        options.handshakeTimeout.inWholeMilliseconds,
        java.util.concurrent.TimeUnit.MILLISECONDS,
    )
  }

  init {
    on<ClientHelloPacket> { event ->
      val skew = abs(System.currentTimeMillis() - event.packet.timestamp)
      if (skew > options.maxHelloSkew.inWholeMilliseconds) {
        throw StaleClientHelloException(skew)
      }
      val publicBytes =
          EcKeys.toUncompressedPoint(
              ephemeralKeyPair.public as java.security.interfaces.ECPublicKey,
          )
      val signature = EcKeys.sign(rootPrivate, publicBytes)
      event.session.send(
          ServerHelloPacket(
              ephemeralPublic = ephemeralKeyPair.public as java.security.interfaces.ECPublicKey,
              signature = signature,
              checksumSize = options.checksumSize,
          ),
      )
    }

    on<ClientReadyPacket> { event ->
      val crypto =
          SessionCryptoState.derive(
              localPrivate = ephemeralKeyPair.private,
              remotePublic = event.packet.ephemeralPublic,
              role = AesCtrSessionCipher.Role.SERVER,
              checksumSize = options.checksumSize,
          )
      val pipeline = event.session.channel.pipeline()
      (pipeline.get(PipelineNames.CHECKSUM_DECODER) as ChecksumFrameDecoder).checksum =
          crypto.incomingChecksum
      (pipeline.get(PipelineNames.CHECKSUM_ENCODER) as ChecksumFrameEncoder).checksum =
          crypto.outgoingChecksum
      (pipeline.get(PipelineNames.CIPHER_DECODER) as CipherDecoder).cipher = crypto.cipher
      (pipeline.get(PipelineNames.CIPHER_ENCODER) as CipherEncoder).cipher = crypto.cipher
      if (applicationProtocol.compressed) {
        pipeline.addBeforeProtocolLogger(
            PipelineNames.COMPRESSION_ENCODER,
            CompressionEncoder(options.compressionThreshold),
        )
      }
      val appHandler = applicationHandlerFactory()
      pipeline.replace(
          PipelineNames.PROTOCOL_HANDLER,
          PipelineNames.PROTOCOL_HANDLER,
          appHandler,
      )
      (event.session as MutableSessionContext).transitionTo(SessionPhase.ESTABLISHED)
      log.debug { "Handshake complete for ${event.session.remoteAddress}" }
    }
  }
}
