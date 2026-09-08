package de.fiereu.network

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class PipelineOptions(
    val checksumSize: Int = 16,
    val writeTimeout: Duration = 25.minutes,
    val maxFrameLength: Int = 0xFFFF,
    val compressionThreshold: Int = 256,
    val maxHelloSkew: Duration = 10.seconds,
    val frameLogging: Boolean = false,
    /** A connection that has not completed the session handshake by then is closed (port scanners). */
    val handshakeTimeout: Duration = 60.seconds,
    /**
     * A connection that has sent nothing for this long is closed (ReadIdleCloser). The client
     * heartbeats far more often than this while alive; only a peer that vanished without a FIN or
     * RST reaching us goes quiet this long.
     */
    val readIdleTimeout: Duration = 3.minutes,
)
