package de.fiereu.network

import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent

private val log = KotlinLogging.logger {}

/**
 * Closes a connection that has sent nothing for the configured reader-idle time. The client
 * heartbeats while alive, so silence that long means the peer is gone without a FIN or RST
 * reaching us - a dropped NAT mapping, a sleeping laptop. Without this the kernel keeps
 * retransmitting for 15-30 minutes and the session lingers as a ghost: still in its map's
 * presence group, still bound to its character (2026-09-08: seven dead sockets from one player
 * after an evening of drops). Closing here runs the ordinary channelInactive cleanup.
 */
class ReadIdleCloser : ChannelDuplexHandler() {
  override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    if (evt is IdleStateEvent && evt.state() == IdleState.READER_IDLE) {
      log.info { "Closing ${ctx.channel().remoteAddress()}: nothing received for the reader-idle time" }
      ctx.close()
      return
    }
    ctx.fireUserEventTriggered(evt)
  }
}
