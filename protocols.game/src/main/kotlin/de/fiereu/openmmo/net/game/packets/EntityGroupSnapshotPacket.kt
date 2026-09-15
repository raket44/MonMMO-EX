package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/** A link member's name, head and (unused here) last-seen int - the r32645 player summary, f/ih6.bK1. */
data class EntityAppearanceInfo(
    val name: String,
    val head: PlayerHead = PlayerHead(),
    val lastSeen: Int = 0,
)

data class EntityGroupMember(
    val entityId: Long,
    val appearance: EntityAppearanceInfo,
    val frames: GroupListFrameSet,
)

data class EntityGroupSnapshotPacket(
    val present: Boolean,
    val leaderId: Long?,
    val members: List<EntityGroupMember>?,
)

internal val EntityAppearanceInfoCodec: Codec<EntityAppearanceInfo> =
    object : PacketCodec<EntityAppearanceInfo>() {
      private val summary = PlayerSummaryCodec<EntityAppearanceInfo>({ it.lastSeen }, { it.head })

      override fun CodecScope<EntityAppearanceInfo>.body(): EntityAppearanceInfo {
        val name = field(Utf16LeNullTerminated) { it.name }
        val (lastSeen, head) = summary.write(this)
        return EntityAppearanceInfo(name, head, lastSeen)
      }
    }

/** r32645 f/vl4 and f/n09: entity id, player summary, party icons (f/ih6.OW). */
internal val EntityGroupMemberCodec: Codec<EntityGroupMember> =
    object : PacketCodec<EntityGroupMember>() {
      override fun CodecScope<EntityGroupMember>.body(): EntityGroupMember {
        val entityId = field(S64LE) { it.entityId }
        val appearance = field(EntityAppearanceInfoCodec) { it.appearance }
        val frames = field(GroupListFrameSetCodec) { it.frames }
        return EntityGroupMember(entityId, appearance, frames)
      }
    }

object EntityGroupSnapshotPacketCodec : PacketCodec<EntityGroupSnapshotPacket>() {
  override fun CodecScope<EntityGroupSnapshotPacket>.body(): EntityGroupSnapshotPacket {
    val present = field(U8) { if (it.present) 1 else 0 } == 1
    val leaderId = if (present) field(S64LE) { it.leaderId!! } else null
    val members =
        if (present) field(EntityGroupMemberCodec.listPrefixed(U8)) { it.members!! } else null
    return EntityGroupSnapshotPacket(present, leaderId, members)
  }
}
