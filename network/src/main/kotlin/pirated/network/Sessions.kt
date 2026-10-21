package pirated.network

import pirated.network.internal.SESSION_KEY
import io.netty.channel.Channel

/** Look up the [SessionContext] attached by [installPipeline] for this channel. */
fun Channel.session(): SessionContext? = attr(SESSION_KEY).get()
