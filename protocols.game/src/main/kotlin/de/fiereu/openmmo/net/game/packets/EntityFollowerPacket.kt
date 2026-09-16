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
     * the renderer looks them up (f/xX1: species | byte << 16). Re-verified on r32645 (f/im7.pR1,
     * 2026-09-15): low 5 bits form, 0x20 female (f/o80.Hi picks the gender sprite), 0x40 shiny
     * (sheet id + 2000), and 0x80 is read by the follower entity itself (f/vc1.eL0): the sheet is
     * drawn at 4/3 scale - the "one size bigger" overworld render retail uses for alphas.
     */
    const val FOLLOWER_FEMALE = 0x20
    const val FOLLOWER_SHINY = 0x40
    const val FOLLOWER_LARGE = 0x80

    fun followerFlags(shiny: Boolean, female: Boolean = false, large: Boolean = false): Byte =
        ((if (shiny) FOLLOWER_SHINY else 0) or
                (if (female) FOLLOWER_FEMALE else 0) or
                (if (large) FOLLOWER_LARGE else 0))
            .toByte()
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
