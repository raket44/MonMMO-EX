package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/** s2c 0xD1: one member joins the link (r32645 f/n09, same member layout as the snapshot). */
data class EntityGroupMemberAddPacket(
    val member: EntityGroupMember,
)

object EntityGroupMemberAddPacketCodec : PacketCodec<EntityGroupMemberAddPacket>() {
  override fun CodecScope<EntityGroupMemberAddPacket>.body(): EntityGroupMemberAddPacket {
    val member = field(EntityGroupMemberCodec) { it.member }
    return EntityGroupMemberAddPacket(member)
  }
}
