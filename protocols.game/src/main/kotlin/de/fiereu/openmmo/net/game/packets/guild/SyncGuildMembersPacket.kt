package de.fiereu.openmmo.net.game.packets.guild

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.net.game.packets.PlayerHead
import de.fiereu.openmmo.net.game.packets.PlayerSummaryCodec

/**
 * One team roster row as r32645 reads it (f/vo, f/q74): entity id, rank, join date (epoch seconds),
 * the player summary (f/ih6.bK1: name, last seen, head) and the online flag (qd6.XD0, which prints
 * "Online" instead of the last-seen date). The last byte was sent as the leader flag, so only the
 * leader ever read as online.
 */
data class GuildMemberEntry(
    val entityId: Long,
    val rank: Byte,
    val joinedAt: Int,
    val name: String,
    val lastSeen: Int,
    val head: PlayerHead = PlayerHead(),
    val online: Boolean,
)

data class SyncGuildMembersPacket(
    val replace: Boolean,
    val members: List<GuildMemberEntry>,
)

private object GuildMemberEntryCodec : PacketCodec<GuildMemberEntry>() {
  private val summary = PlayerSummaryCodec<GuildMemberEntry>({ it.lastSeen }, { it.head })

  override fun CodecScope<GuildMemberEntry>.body(): GuildMemberEntry {
    val entityId = field(S64LE, GuildMemberEntry::entityId)
    val rank = field(S8, GuildMemberEntry::rank)
    val joinedAt = field(S32LE, GuildMemberEntry::joinedAt)
    val name = field(Utf16LeNullTerminated, GuildMemberEntry::name)
    val (lastSeen, head) = summary.write(this)
    val online = field(Bool, GuildMemberEntry::online)
    return GuildMemberEntry(entityId, rank, joinedAt, name, lastSeen, head, online)
  }
}

private val GuildMemberEntryListPrefixedU8: Codec<List<GuildMemberEntry>> =
    object : Codec<List<GuildMemberEntry>> {
      override fun read(buf: ReadBuffer): List<GuildMemberEntry> {
        val n = U8.read(buf)
        return List(n) { GuildMemberEntryCodec.read(buf) }
      }

      override fun write(buf: WriteBuffer, value: List<GuildMemberEntry>) {
        U8.write(buf, value.size)
        value.forEach { GuildMemberEntryCodec.write(buf, it) }
      }
    }

object SyncGuildMembersPacketCodec : PacketCodec<SyncGuildMembersPacket>() {
  override fun CodecScope<SyncGuildMembersPacket>.body(): SyncGuildMembersPacket {
    val replace = field(Bool, SyncGuildMembersPacket::replace)
    val members = field(GuildMemberEntryListPrefixedU8, SyncGuildMembersPacket::members)
    return SyncGuildMembersPacket(replace, members)
  }
}
