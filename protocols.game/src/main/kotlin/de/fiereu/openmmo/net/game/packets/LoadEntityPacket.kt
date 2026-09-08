package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.EntityStatus
import de.fiereu.openmmo.net.game.codecs.DefaultSkinSetCodec
import de.fiereu.openmmo.net.game.codecs.SkinSet

data class LoadEntityPacket(
    val entityId: Long,
    /**
     * 0 male, 1 female: the client's f/IL0.an0 (its debug dump calls it "gender"). Sent as a
     * reserved zero until 2026-09-08, so every player spawned as the male body.
     */
    val gender: Byte = 0,
    val skin: SkinSet,
    val name: String,
    val regionId: Int,
    val bankId: Int,
    val mapId: Int,
    val x: Int,
    val y: Int,
    val z: Int,
    val facing: Direction,
    val transportation: Int = 0,
    val entityNameplateType: Int = 0,
    val status: EntityStatus = EntityStatus.NONE,
    val hasFollower: Boolean,
    val followerDexId: Short,
    /** Follower flag byte (bit 0x40 shiny, 0x20 female, low bits form); see EntityFollowerPacket. */
    val followerFlags: Int = 0,
    /**
     * Gen 5 rail placement (Castelia's main city, Skyarrow Bridge...). The client's position struct
     * (f.Wi1) resolves coordinates two ways: normally `map.Pp(x, y, z)` with z as elevation, but
     * when bit 3 of the facing byte is set (f.Wi1.KS1) it calls `map.Ea1(z, x, y)` - the Z BYTE
     * becomes the RAIL LINE and x/y are rail coordinates. Without this a player warped onto a rail
     * map floats in a blue void. -1 = not on a rail.
     */
    val railLine: Int = -1,
    /**
     * MOUNT (type, id) - the flags&0x02 optional pair (f.pJ0 fields iJ + Ny0). The client routes
     * them to f.E41.mX1(type, id): both != -1 attaches a mount renderer (f.wt0, models from
     * f.RO.fH1(type, id)); -1/-1 dismounts. Applied LIVE on entity updates (f.tS1.NQ0's
     * existing-player branch calls mX1), so a re-sent LoadEntity mounts without a respawn. Mount id
     * 285 is the client's animated special case - the bicycle's spinning wheels.
     */
    val mountType: Int = -1,
    val mountId: Int = -1,
)

object LoadEntityPacketCodec : PacketCodec<LoadEntityPacket>() {
  override fun CodecScope<LoadEntityPacket>.body(): LoadEntityPacket {
    val entityId = field(S64LE, LoadEntityPacket::entityId)
    val gender = field(S8, LoadEntityPacket::gender)
    val skin = field(DefaultSkinSetCodec, LoadEntityPacket::skin)
    val name = field(Utf16LeNullTerminated, LoadEntityPacket::name)
    val regionId = field(U8, LoadEntityPacket::regionId)
    val bankId = field(U8, LoadEntityPacket::bankId)
    val mapId = field(U8, LoadEntityPacket::mapId)
    val x = field(S16LE) { it.x.toShort() }.toInt()
    val y = field(S16LE) { it.y.toShort() }.toInt()
    // The client's f.Wi1 reads this byte as elevation for tile positions and as the RAIL LINE
    // when the rail bit is set on the facing byte below.
    val z = field(U8) { if (it.railLine >= 0) it.railLine and 0xFF else it.z }
    val facing =
        Direction.entries[
                field(U8) { it.facing.ordinal or (if (it.railLine >= 0) 0x08 else 0) } and 0x03]
    val transportation = field(U8, LoadEntityPacket::transportation)
    val entityNameplateType = field(U8, LoadEntityPacket::entityNameplateType)
    val flags =
        field(U8) {
          var f = 0
          if (it.mountId >= 0) f = f or 0x02
          if (it.hasFollower) f = f or 0x04
          // Bit 0x08 carries the follower flag byte (client pJ0.DK0 -> ti.gw0's byte): shiny
          // and gender pick the follower sheet variant. Without it a shiny led a normal follower.
          if (it.hasFollower && it.followerFlags != 0) f = f or 0x08
          f
        }
    if (flags and 0x01 != 0) field(S8) { 0 }
    if (flags and 0x02 != 0) {
      field(S8) { it.mountType.toByte() }
      field(U16LE) { it.mountId }
    }
    val hasFollower = (flags and 0x04) != 0
    val followerDexId: Short = if (hasFollower) field(S16LE, LoadEntityPacket::followerDexId) else 0
    val followerFlags = if (flags and 0x08 != 0) field(U8) { it.followerFlags } else 0
    if (flags and 0x10 != 0) {
      field(S32LE) { 0 }
      field(Utf16LeNullTerminated) { "" }
    }
    return LoadEntityPacket(
        entityId = entityId,
        gender = gender,
        skin = skin,
        name = name,
        regionId = regionId,
        bankId = bankId,
        mapId = mapId,
        x = x,
        y = y,
        z = z,
        facing = facing,
        transportation = transportation,
        entityNameplateType = entityNameplateType,
        status = EntityStatus.NONE,
        hasFollower = hasFollower,
        followerDexId = followerDexId,
        followerFlags = followerFlags,
    )
  }
}
