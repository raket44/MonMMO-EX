package de.fiereu.network

/**
 * The session whose packet is being encoded or decoded right now, for codecs whose wire shape
 * depends on the peer - a client revision negotiated at join, say.
 *
 * Codecs get a bare buffer and no session, and threading one through every `Codec.write` would
 * touch hundreds of call sites for a handful of revision-dependent fields. Instead the protocol
 * handler publishes the session's attributes around each encode and decode, both of which run
 * synchronously on the channel's event loop, so a thread-local is exact: nothing else runs on that
 * thread in between. Outside a handler (tests, tools) there is no session and [attributes] is null,
 * which codecs must treat as "the captured layout".
 */
object WireContext {
  private val current = ThreadLocal<SessionAttributes?>()

  /** The attributes of the session being served, or null when no packet is in flight. */
  fun attributes(): SessionAttributes? = current.get()

  fun <T> with(attributes: SessionAttributes?, block: () -> T): T {
    val previous = current.get()
    current.set(attributes)
    try {
      return block()
    } finally {
      current.set(previous)
    }
  }
}
