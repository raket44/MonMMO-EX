package de.fiereu.openmmo.net.game

import de.fiereu.network.SessionAttribute
import de.fiereu.network.WireContext

/**
 * The client revision the peer declared in its join packet, stored on the session so codecs whose
 * wire shape changed between client builds can pick the right layout for this connection.
 */
val CLIENT_REVISION: SessionAttribute<Int> = SessionAttribute.of("client.revision")

/** Desktop build 31914, the revision the protocol was reverse-engineered against. */
const val DESKTOP_REVISION_31914 = 31914

/**
 * From this revision on the monster record carries one more trailing byte than 31914 does. Every
 * newer build seen so far - the r32645 Android client and the 32710 desktop captures - sends and
 * expects the longer record; only the installed 31914 client consumes one byte fewer and
 * mis-parses the longer form. So the cut is "anything after 31914", not the capture's revision.
 */
const val LONG_MONSTER_TRAILER_MIN_REVISION = DESKTOP_REVISION_31914 + 1

/**
 * The revision of the client being served by the packet in flight, or null outside a session -
 * tests and tools, which then get the captured (long) layout.
 */
fun currentClientRevision(): Int? = WireContext.attributes()?.get(CLIENT_REVISION)

/** Whether the connection in flight takes the longer monster record. Null revision = captured form. */
fun monsterRecordHasLongTrailer(): Boolean =
    (currentClientRevision() ?: LONG_MONSTER_TRAILER_MIN_REVISION) >= LONG_MONSTER_TRAILER_MIN_REVISION
