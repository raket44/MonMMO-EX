package pirated.server.login

import pirated.network.PipelineOptions
import pirated.network.SessionIdentity
import pirated.network.installNetwork
import pirated.net.login.LoginProtocol
import pirated.server.login.config.LoginServerConfig
import pirated.server.login.handler.LoginAppHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.bootstrap.ServerBootstrap
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
class LoginServer
@Inject
constructor(
    private val config: LoginServerConfig,
    @param:Named("boss") private val bossGroup: EventLoopGroup,
    @param:Named("worker") private val workerGroup: EventLoopGroup,
    private val rootKey: ECPrivateKey,
    private val handlerProvider: Provider<LoginAppHandler>,
) {
  fun start() {
    val channel =
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .installNetwork(
                identity = SessionIdentity.ServerRoot(rootKey),
                applicationProtocol = LoginProtocol,
                applicationHandlerFactory = { handlerProvider.get() },
                options = PipelineOptions(checksumSize = config.checksumSize),
            )
            .bind(config.host, config.port)
            .sync()
            .channel()
    log.info { "Login server listening on ${channel.localAddress()}" }
    channel.closeFuture().sync()
  }

  fun shutdown() {
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}
