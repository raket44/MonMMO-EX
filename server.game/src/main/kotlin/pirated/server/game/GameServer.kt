package pirated.server.game

import pirated.network.PipelineNames
import pirated.network.PipelineOptions
import pirated.network.SessionIdentity
import pirated.network.installPipeline
import pirated.network.session
import pirated.net.game.GameProtocol
import pirated.server.game.config.GameServerConfig
import pirated.server.game.handler.GameAppHandler
import pirated.server.game.session.SessionRegistry
import pirated.server.game.session.SessionRegistryHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.security.interfaces.ECPrivateKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class GameServer
@Inject
constructor(
    private val config: GameServerConfig,
    @param:Named("boss") private val bossGroup: EventLoopGroup,
    @param:Named("worker") private val workerGroup: EventLoopGroup,
    private val rootKey: ECPrivateKey,
    private val handlerProvider: Provider<GameAppHandler>,
    private val sessionRegistry: SessionRegistry,
) {
  fun start() {
    val channel =
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(
                object : ChannelInitializer<Channel>() {
                  override fun initChannel(ch: Channel) {
                    installPipeline(
                        pipeline = ch.pipeline(),
                        side = pirated.network.Side.SERVER,
                        identity = SessionIdentity.ServerRoot(rootKey),
                        applicationProtocol = GameProtocol,
                        applicationHandlerFactory = { handlerProvider.get() },
                        options = PipelineOptions(checksumSize = config.checksumSize),
                    )
                    val session = ch.session() ?: error("session missing after installPipeline")
                    ch.pipeline()
                        .addAfter(
                            PipelineNames.PROTOCOL_HANDLER,
                            "session-registry",
                            SessionRegistryHandler(sessionRegistry, session),
                        )
                  }
                },
            )
            .bind(config.host, config.port)
            .sync()
            .channel()
    log.info { "Game server listening on ${channel.localAddress()}" }
    channel.closeFuture().sync()
  }

  fun shutdown() {
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}
