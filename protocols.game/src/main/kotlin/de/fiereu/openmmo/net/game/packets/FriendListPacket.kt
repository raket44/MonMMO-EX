package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/**
 * One friend: entity id, when they were added (epoch seconds), online, then the r32645 player
 * summary (f/ih6.bK1): name, last seen (epoch seconds) and the head the list draws.
 */
data class FriendListEntry(
    val player: Long,
    val friendsSince: Int,
    val online: Boolean,
    val name: String,
    val lastSeen: Int,
    val head: PlayerHead = PlayerHead(),
)

data class FriendListPacket(
    val mode: Int,
    val entries: List<FriendListEntry>,
)

private val FriendListEntryCodec: Codec<FriendListEntry> =
    object : PacketCodec<FriendListEntry>() {
      private val summary = PlayerSummaryCodec<FriendListEntry>({ it.lastSeen }, { it.head })

      override fun CodecScope<FriendListEntry>.body(): FriendListEntry {
        val player = field(S64LE) { it.player }
        val friendsSince = field(S32LE) { it.friendsSince }
        val online = field(Bool) { it.online }
        val name = field(Utf16LeNullTerminated) { it.name }
        val (lastSeen, head) = summary.write(this)
        return FriendListEntry(player, friendsSince, online, name, lastSeen, head)
      }
    }

object FriendListPacketCodec : PacketCodec<FriendListPacket>() {
  override fun CodecScope<FriendListPacket>.body(): FriendListPacket {
    val mode = field(U8) { it.mode }
    val entries = field(FriendListEntryCodec.listPrefixed(U8)) { it.entries }
    return FriendListPacket(mode, entries)
  }
}
