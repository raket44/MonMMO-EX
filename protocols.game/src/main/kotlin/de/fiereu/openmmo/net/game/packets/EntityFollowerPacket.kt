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
    val gender: Byte = 0,
    val shiny: Boolean = false,
)

object EntityFollowerPacketCodec : PacketCodec<EntityFollowerPacket>() {
  override fun CodecScope<EntityFollowerPacket>.body(): EntityFollowerPacket {
    val entityId = field(S64LE) { it.entityId }
    val species = field(S16LE) { it.species }
    val gender = field(S8) { it.gender }
    val shiny = field(Bool) { it.shiny }
    return EntityFollowerPacket(entityId, species, gender, shiny)
  }
}
