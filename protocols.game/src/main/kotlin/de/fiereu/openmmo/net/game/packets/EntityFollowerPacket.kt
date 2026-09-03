package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.Bool
import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.S8

/**
 * s2c 0x2B - sets an entity's overworld follower live, decoded from client 31914's reader
 * `f/lPT6`: entity uid, follower species s16, a gender byte and a shiny flag, handed straight to
 * the entity's follower setter (`f/ti.gw0`). Species 0 clears the follower. This is the answer to
 * the party window's "Set X as Follower" (c2s 0x11).
 *
 * Previously mis-imported as "FollowerAdvancePacket" (step/direction/flag) - same bytes.
 */
data class EntityFollowerPacket(
    val entityId: Long,
    val species: Short,
    /** [FOLLOWER_SHINY] / [FOLLOWER_FEMALE] plus the form in the low bits; see [followerFlags]. */
    val flags: Byte = 0,
    /** The setter's third argument; the spawn path passes true. */
    val refresh: Boolean = true,
) {
  companion object {
    /**
     * Bits of the follower flag byte, as the client's mod loader keys follower sheets (f/EP: a
     * `-f-` sheet registers under 0x20, an `-s-` sheet under 0x40, the low bits are the form) and
     * the renderer looks them up (f/xX1: species | byte << 16).
     */
    const val FOLLOWER_FEMALE = 0x20
    const val FOLLOWER_SHINY = 0x40

    fun followerFlags(shiny: Boolean, female: Boolean = false): Byte =
        ((if (shiny) FOLLOWER_SHINY else 0) or (if (female) FOLLOWER_FEMALE else 0)).toByte()
  }
}

object EntityFollowerPacketCodec : PacketCodec<EntityFollowerPacket>() {
  override fun CodecScope<EntityFollowerPacket>.body(): EntityFollowerPacket {
    val entityId = field(S64LE) { it.entityId }
    val species = field(S16LE) { it.species }
    val flags = field(S8) { it.flags }
    val refresh = field(Bool) { it.refresh }
    return EntityFollowerPacket(entityId, species, flags, refresh)
  }
}
