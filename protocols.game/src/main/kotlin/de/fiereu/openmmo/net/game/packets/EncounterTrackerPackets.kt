package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.Bool
import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S32LE
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.S8
import de.fiereu.bytecodec.U16LE
import de.fiereu.bytecodec.U8
import de.fiereu.bytecodec.listPrefixed

/**
 * The client's Encounter Tracker (bytecode, client 31914, 2026-09-11). The client keeps one
 * counter set per KIND (f/Qm0, by byte id: 0 Last Shiny, 1 Species Rollover, 2 Wild, 3 Alpha,
 * 4 Raid, 5 Mysterious, 6 Egg, 7 Fossil, 8 Trip, 9 Wild Sweet Scent, 10 Wild Other) in
 * f/ln1.mm.zl1 (f/pR1: IO1 = the running total, MI1 = the all-time total, e20 = species entries).
 * It never counts on its own: the server sends the sets (0x8D) and every encounter (0x8E), and
 * the client adds the packet's amount to the running and all-time totals of every kind the
 * encounter type maps to (f/CE0.lU0) plus the three always-counted kinds (f/Qm0.vQ1: 0, 1, 8).
 */

/** One species row of a kind (f/Dm.jP0): species, running count, all-time count, last seen, pinned. */
data class EncounterTrackerEntry(
    val species: Short,
    val count: Int,
    val allTime: Int,
    val lastMillis: Long,
    val pinned: Boolean,
)

object EncounterTrackerEntryCodec : PacketCodec<EncounterTrackerEntry>() {
  override fun CodecScope<EncounterTrackerEntry>.body(): EncounterTrackerEntry {
    val species = field(S16LE) { it.species }
    val count = field(S32LE) { it.count }
    val allTime = field(S32LE) { it.allTime }
    val lastMillis = field(S64LE) { it.lastMillis }
    val pinned = field(Bool) { it.pinned }
    return EncounterTrackerEntry(species, count, allTime, lastMillis, pinned)
  }
}

/**
 * Opcode 0x8D (s2c, f/tJ1): one kind's counter set. [reset] clears the client's rows and sets
 * both totals before the rows are added; [refresh] redraws the HUD frame afterwards.
 */
data class EncounterTrackerStatePacket(
    val kind: Byte,
    val reset: Boolean,
    val refresh: Boolean,
    val total: Int,
    val allTime: Int,
    val entries: List<EncounterTrackerEntry>,
)

object EncounterTrackerStatePacketCodec : PacketCodec<EncounterTrackerStatePacket>() {
  override fun CodecScope<EncounterTrackerStatePacket>.body(): EncounterTrackerStatePacket {
    val kind = field(S8) { it.kind }
    val reset = field(Bool) { it.reset }
    val refresh = field(Bool) { it.refresh }
    val total = field(S32LE) { it.total }
    val allTime = field(S32LE) { it.allTime }
    val entries = field(EncounterTrackerEntryCodec.listPrefixed(U16LE)) { it.entries }
    return EncounterTrackerStatePacket(kind, reset, refresh, total, allTime, entries)
  }
}

/** One monster of an encounter (0x8E rows): the species and whether it was an alpha. */
data class EncounterTrackerHit(val species: Short, val alpha: Boolean)

/**
 * Opcode 0x8E (s2c, f/U7): an encounter happened. [type] is the client's encounter type (f/CE0:
 * 0 wild, 1 sweet scent, 2 raid, 3 fossil, 4 mysterious, 5 egg); the client adds [amount] to
 * every kind that type maps to, to the three always-counted kinds, and to Alpha for alpha rows.
 */
data class EncounterTrackerUpdatePacket(
    val type: Byte,
    val hits: List<EncounterTrackerHit>,
    val amount: Byte,
)

object EncounterTrackerUpdatePacketCodec : PacketCodec<EncounterTrackerUpdatePacket>() {
  override fun CodecScope<EncounterTrackerUpdatePacket>.body(): EncounterTrackerUpdatePacket {
    val type = field(S8) { it.type }
    val hits = field(EncounterTrackerHitCodec.listPrefixed(U8)) { it.hits }
    val amount = field(S8) { it.amount }
    return EncounterTrackerUpdatePacket(type, hits, amount)
  }
}

object EncounterTrackerHitCodec : PacketCodec<EncounterTrackerHit>() {
  override fun CodecScope<EncounterTrackerHit>.body(): EncounterTrackerHit {
    val species = field(S16LE) { it.species }
    val alpha = field(Bool) { it.alpha }
    return EncounterTrackerHit(species, alpha)
  }
}

/** Opcode 0x43 (c2s, f/cM0): the player pinned or unpinned a species row of a kind. */
data class EncounterTrackerPinPacket(
    val kind: Byte,
    val species: Short,
    val pinned: Boolean,
)

object EncounterTrackerPinPacketCodec : PacketCodec<EncounterTrackerPinPacket>() {
  override fun CodecScope<EncounterTrackerPinPacket>.body(): EncounterTrackerPinPacket {
    val kind = field(S8) { it.kind }
    val species = field(S16LE) { it.species }
    val pinned = field(Bool) { it.pinned }
    return EncounterTrackerPinPacket(kind, species, pinned)
  }
}
